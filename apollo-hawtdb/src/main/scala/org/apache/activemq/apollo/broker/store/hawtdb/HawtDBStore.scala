/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.apollo.broker.store.hawtdb

import collection.mutable.ListBuffer
import dto.{HawtDBStoreStatusDTO, HawtDBStoreDTO}
import java.util.HashMap
import collection.{Seq}
import org.fusesource.hawtdispatch._
import java.io.File
import java.util.concurrent._
import atomic.{AtomicInteger, AtomicLong}
import org.apache.activemq.apollo.dto._
import org.apache.activemq.apollo.broker.store._
import org.apache.activemq.apollo.util._
import ReporterLevel._
import org.fusesource.hawtdispatch.{DispatchQueue, BaseRetained, ListEventAggregator}
import org.apache.activemq.apollo.util.OptionSupport._
import java.io.{InputStream, OutputStream}
import scala.util.continuations._

object HawtDBStore extends Log {
  val DATABASE_LOCKED_WAIT_DELAY = 10 * 1000;

  /**
   * Creates a default a configuration object.
   */
  def defaultConfig() = {
    val rc = new HawtDBStoreDTO
    rc.directory = new File("activemq-data")
    rc
  }

  /**
   * Validates a configuration object.
   */
  def validate(config: HawtDBStoreDTO, reporter:Reporter):ReporterLevel = {
    new Reporting(reporter) {
      if( config.directory==null ) {
        error("The HawtDB Store directory property must be configured.")
      }
    }.result
  }
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class HawtDBStore extends DelayingStoreSupport with DispatchLogging {

  import HawtDBStore._
  override protected def log = HawtDBStore

  var next_queue_key = new AtomicLong(1)
  var next_msg_key = new AtomicLong(1)

  var executor_pool:ExecutorService = _
  val schedule_version = new AtomicInteger()
  var config:HawtDBStoreDTO = defaultConfig
  val client = new HawtDBClient(this)

  val load_source = createSource(new ListEventAggregator[(Long, (Option[MessageRecord])=>Unit)](), dispatch_queue)
  load_source.setEventHandler(^{drain_loads});

  override def toString = "hawtdb store"

  def flush_delay = config.flush_delay.getOrElse(100)
  
  protected def get_next_msg_key = next_msg_key.getAndIncrement

  protected def store(uows: Seq[DelayableUOW])(callback: =>Unit) = {
    executor_pool {
      client.store(uows, ^{
        dispatch_queue {
          callback
        }
      })
    }
  }

  def configure(config: StoreDTO, reporter: Reporter) = configure(config.asInstanceOf[HawtDBStoreDTO], reporter)

  def configure(config: HawtDBStoreDTO, reporter: Reporter) = {
    if ( HawtDBStore.validate(config, reporter) < ERROR ) {
      if( service_state.is_started ) {
        // TODO: apply changes while he broker is running.
        reporter.report(WARN, "Updating hawtdb store configuration at runtime is not yet supported.  You must restart the broker for the change to take effect.")
      } else {
        this.config = config
      }
    }
  }

  protected def _start(on_completed: Runnable) = {
    info("Starting hawtdb store at: '%s'", config.directory)
    executor_pool = Executors.newFixedThreadPool(1, new ThreadFactory(){
      def newThread(r: Runnable) = {
        val rc = new Thread(r, "hawtdb store client")
        rc.setDaemon(true)
        rc
      }
    })
    client.config = config
    poll_stats
    executor_pool {
      client.start(^{
        next_msg_key.set( client.rootBuffer.getLastMessageKey.longValue +1 )
        next_queue_key.set( client.rootBuffer.getLastQueueKey.longValue +1 )
        val v = schedule_version.incrementAndGet
        scheduleCleanup(v)
        scheduleFlush(v)
        load_source.resume
        on_completed.run
      })
    }
  }

  def scheduleFlush(version:Int): Unit = {
    def try_flush() = {
      if (version == schedule_version.get) {
        executor_pool {
          client.flush
          scheduleFlush(version)
        }
      }
    }
    dispatch_queue.dispatchAfter(client.index_flush_interval, TimeUnit.MILLISECONDS, ^ {try_flush})
  }

  def scheduleCleanup(version:Int): Unit = {
    def try_cleanup() = {
      if (version == schedule_version.get) {
        executor_pool {
          client.cleanup()
          scheduleCleanup(version)
        }
      }
    }
    dispatch_queue.dispatchAfter(client.cleanup_interval, TimeUnit.MILLISECONDS, ^ {try_cleanup})
  }

  protected def _stop(on_completed: Runnable) = {
    info("Stopping hawtdb store at: '%s'", config.directory)
    schedule_version.incrementAndGet
    new Thread() {
      override def run = {
        load_source.suspend
        executor_pool.shutdown
        executor_pool.awaitTermination(86400, TimeUnit.SECONDS)
        executor_pool = null
        client.stop
        on_completed.run
      }
    }.start
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Implementation of the Store interface
  //
  /////////////////////////////////////////////////////////////////////

  /**
   * Deletes all stored data from the store.
   */
  def purge(callback: =>Unit) = {
    executor_pool {
      client.purge(^{
        next_queue_key.set(1)
        next_msg_key.set(1)
        callback
      })
    }
  }


  /**
   * Ges the last queue key identifier stored.
   */
  def get_last_queue_key(callback:(Option[Long])=>Unit):Unit = {
    executor_pool {
      callback(Some(client.rootBuffer.getLastQueueKey.longValue))
    }
  }

  def add_queue(record: QueueRecord)(callback: (Boolean) => Unit) = {
    executor_pool {
     client.addQueue(record, ^{ callback(true) })
    }
  }

  def remove_queue(queueKey: Long)(callback: (Boolean) => Unit) = {
    executor_pool {
      client.removeQueue(queueKey,^{ callback(true) })
    }
  }

  def get_queue(queueKey: Long)(callback: (Option[QueueRecord]) => Unit) = {
    executor_pool {
      callback( client.getQueue(queueKey) )
    }
  }

  def list_queues(callback: (Seq[Long]) => Unit) = {
    executor_pool {
      callback( client.listQueues )
    }
  }

  def load_message(messageKey: Long)(callback: (Option[MessageRecord]) => Unit) = {
    message_load_latency_counter.start { end=>
      load_source.merge((messageKey, { (result)=>
        end()
        callback(result)
      }))
    }
  }

  def drain_loads = {
    var data = load_source.getData
    message_load_batch_size_counter += data.size
    executor_pool ^{
      client.loadMessages(data)
    }
  }

  def list_queue_entry_ranges(queueKey: Long, limit: Int)(callback: (Seq[QueueEntryRange]) => Unit) = {
    executor_pool ^{
      callback( client.listQueueEntryGroups(queueKey, limit) )
    }
  }

  def list_queue_entries(queueKey: Long, firstSeq: Long, lastSeq: Long)(callback: (Seq[QueueEntryRecord]) => Unit) = {
    executor_pool ^{
      callback( client.getQueueEntries(queueKey, firstSeq, lastSeq) )
    }
  }

  def poll_stats:Unit = {
    def displayStats = {
      if( service_state.is_started ) {

        flush_latency = flush_latency_counter(true)
        message_load_latency = message_load_latency_counter(true)
        client.metric_journal_append = client.metric_journal_append_counter(true)
        client.metric_index_update = client.metric_index_update_counter(true)
        commit_latency = commit_latency_counter(true)
        message_load_batch_size =  message_load_batch_size_counter(true)

        poll_stats
      }
    }

    dispatch_queue.dispatchAfter(1, TimeUnit.SECONDS, ^{ displayStats })
  }

  def get_store_status(callback:(StoreStatusDTO)=>Unit) = dispatch_queue {
    val rc = new HawtDBStoreStatusDTO

    rc.state = service_state.toString
    rc.state_since = service_state.since

    rc.flush_latency = flush_latency
    rc.message_load_latency = message_load_latency
    rc.message_load_batch_size = message_load_batch_size

    rc.journal_append_latency = client.metric_journal_append
    rc.index_update_latency = client.metric_index_update

    rc.canceled_message_counter = metric_canceled_message_counter
    rc.canceled_enqueue_counter = metric_canceled_enqueue_counter
    rc.flushed_message_counter = metric_flushed_message_counter
    rc.flushed_enqueue_counter = metric_flushed_enqueue_counter

    callback(rc)
  }

  /**
   * Exports the contents of the store to the provided streams.  Each stream should contain
   * a list of framed protobuf objects with the corresponding object types.
   */
  def export_pb(streams:StreamManager[OutputStream]):Result[Zilch,String] @suspendable = executor_pool ! {
    Failure("not supported")// client.export_pb(queue_stream, message_stream, queue_entry_stream)
  }

  /**
   * Imports a previously exported set of streams.  This deletes any previous data
   * in the store.
   */
  def import_pb(streams:StreamManager[InputStream]):Result[Zilch,String] @suspendable = executor_pool ! {
    Failure("not supported")//client.import_pb(queue_stream, message_stream, queue_entry_stream)
  }
}
