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
package org.apache.activemq.apollo.broker.store.bdb

import dto.{BDBStoreDTO, BDBStoreStatusDTO}
import java.util.concurrent.atomic.AtomicLong
import collection.Seq
import org.fusesource.hawtdispatch._
import java.util.concurrent._
import org.apache.activemq.apollo.broker.store._
import org.apache.activemq.apollo.util._
import ReporterLevel._
import org.fusesource.hawtdispatch.ListEventAggregator
import org.apache.activemq.apollo.dto.{StoreStatusDTO, IntMetricDTO, TimeMetricDTO, StoreDTO}
import org.apache.activemq.apollo.util.OptionSupport._
import java.io.{InputStream, OutputStream, File}
import scala.util.continuations._

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object BDBStore extends Log {
  val DATABASE_LOCKED_WAIT_DELAY = 10 * 1000;

  /**
   * Creates a default a configuration object.
   */
  def defaultConfig() = {
    val rc = new BDBStoreDTO
    rc.directory = new File("activemq-data")
    rc
  }

  /**
   * Validates a configuration object.
   */
  def validate(config: BDBStoreDTO, reporter:Reporter):ReporterLevel = {
    new Reporting(reporter) {
      if( config.directory==null ) {
        error("The BDB Store directory property must be configured.")
      }
    }.result
  }
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class BDBStore extends DelayingStoreSupport with DispatchLogging {

  import BDBStore._

  override def log: Log = BDBStore

  var next_queue_key = new AtomicLong(1)
  var next_msg_key = new AtomicLong(1)

  var write_executor:ExecutorService = _
  var read_executor:ExecutorService = _
  var config:BDBStoreDTO = defaultConfig
  val client = new BDBClient(this)

  override def toString = "bdb store"

  def flush_delay = config.flush_delay.getOrElse(100)
  
  protected def get_next_msg_key = next_msg_key.getAndIncrement

  override def zero_copy_buffer_allocator():ZeroCopyBufferAllocator = client.zero_copy_buffer_allocator

  protected def store(uows: Seq[DelayableUOW])(callback: =>Unit) = {
    write_executor {
      client.store(uows, ^{
        dispatch_queue {
          callback
        }
      })
    }
  }

  def configure(config: StoreDTO, reporter: Reporter) = configure(config.asInstanceOf[BDBStoreDTO], reporter)

  def configure(config: BDBStoreDTO, reporter: Reporter) = {
    if ( BDBStore.validate(config, reporter) < ERROR ) {
      if( service_state.is_started ) {
        // TODO: apply changes while he broker is running.
        reporter.report(WARN, "Updating bdb store configuration at runtime is not yet supported.  You must restart the broker for the change to take effect.")
      } else {
        this.config = config
      }
    }
  }

  protected def _start(on_completed: Runnable) = {
    info("Starting bdb store at: '%s'", config.directory)
    write_executor = Executors.newFixedThreadPool(1, new ThreadFactory(){
      def newThread(r: Runnable) = {
        val rc = new Thread(r, "bdb store io write")
        rc.setDaemon(true)
        rc
      }
    })
    read_executor = Executors.newFixedThreadPool(config.read_threads.getOrElse(10), new ThreadFactory(){
      def newThread(r: Runnable) = {
        val rc = new Thread(r, "bdb store io read")
        rc.setDaemon(true)
        rc
      }
    })
    client.config = config
    poll_stats
    write_executor {
      client.start()
      next_msg_key.set( client.getLastMessageKey +1 )
      next_queue_key.set( client.getLastQueueKey +1 )
      on_completed.run
    }
  }

  protected def _stop(on_completed: Runnable) = {
    new Thread() {
      override def run = {
        info("Stopping BDB store at: '%s'", config.directory)
        write_executor.shutdown
        write_executor.awaitTermination(60, TimeUnit.SECONDS)
        write_executor = null
        read_executor.shutdown
        read_executor.awaitTermination(60, TimeUnit.SECONDS)
        read_executor = null
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
    write_executor {
      client.purge()
      next_queue_key.set(1)
      next_msg_key.set(1)
      callback
    }
  }


  /**
   * Ges the last queue key identifier stored.
   */
  def get_last_queue_key(callback:(Option[Long])=>Unit):Unit = {
    write_executor {
      callback(Some(client.getLastQueueKey))
    }
  }

  def add_queue(record: QueueRecord)(callback: (Boolean) => Unit) = {
    write_executor {
     client.addQueue(record, ^{ callback(true) })
    }
  }

  def remove_queue(queueKey: Long)(callback: (Boolean) => Unit) = {
    write_executor {
      client.removeQueue(queueKey,^{ callback(true) })
    }
  }

  def get_queue(queueKey: Long)(callback: (Option[QueueRecord]) => Unit) = {
    write_executor {
      callback( client.getQueue(queueKey) )
    }
  }

  def list_queues(callback: (Seq[Long]) => Unit) = {
    write_executor {
      callback( client.listQueues )
    }
  }

  val load_source = createSource(new ListEventAggregator[(Long, (Option[MessageRecord])=>Unit)](), dispatch_queue)
  load_source.setEventHandler(^{drain_loads});
  load_source.resume


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
    read_executor ^{
      client.loadMessages(data)
    }
  }

  def list_queue_entry_ranges(queueKey: Long, limit: Int)(callback: (Seq[QueueEntryRange]) => Unit) = {
    write_executor ^{
      callback( client.listQueueEntryGroups(queueKey, limit) )
    }
  }

  def list_queue_entries(queueKey: Long, firstSeq: Long, lastSeq: Long)(callback: (Seq[QueueEntryRecord]) => Unit) = {
    write_executor ^{
      callback( client.getQueueEntries(queueKey, firstSeq, lastSeq) )
    }
  }

  def poll_stats:Unit = {
    def displayStats = {
      if( service_state.is_started ) {

        flush_latency = flush_latency_counter(true)
        message_load_latency = message_load_latency_counter(true)
//        client.metric_journal_append = client.metric_journal_append_counter(true)
//        client.metric_index_update = client.metric_index_update_counter(true)
        commit_latency = commit_latency_counter(true)
        message_load_batch_size =  message_load_batch_size_counter(true)

        poll_stats
      }
    }

    dispatch_queue.dispatchAfter(1, TimeUnit.SECONDS, ^{ displayStats })
  }

  def get_store_status(callback:(StoreStatusDTO)=>Unit) = dispatch_queue {
    val rc = new BDBStoreStatusDTO

    rc.state = service_state.toString
    rc.state_since = service_state.since

    rc.flush_latency = flush_latency
    rc.message_load_latency = message_load_latency
    rc.message_load_batch_size = message_load_batch_size

//    rc.journal_append_latency = client.metric_journal_append
//    rc.index_update_latency = client.metric_index_update

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
  def export_pb(streams:StreamManager[OutputStream]):Result[Zilch,String] @suspendable = write_executor ! {
    client.export_pb(streams)
  }

  /**
   * Imports a previously exported set of streams.  This deletes any previous data
   * in the store.
   */
  def import_pb(streams:StreamManager[InputStream]):Result[Zilch,String] @suspendable = write_executor ! {
    client.import_pb(streams)
  }

}
