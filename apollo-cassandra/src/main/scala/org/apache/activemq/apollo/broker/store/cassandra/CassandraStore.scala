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
package org.apache.activemq.apollo.broker.store.cassandra

import dto.CassandraStoreDTO
import org.apache.activemq.apollo.dto._
import org.fusesource.hawtdispatch.BaseRetained
import com.shorrockin.cascal.session._
import java.util.concurrent.atomic.AtomicLong
import collection.mutable.ListBuffer
import java.util.HashMap
import collection.{JavaConversions, Seq}
import com.shorrockin.cascal.utils.Conversions._
import org.fusesource.hawtdispatch._
import org.fusesource.hawtdispatch.ListEventAggregator
import java.util.concurrent._
import org.apache.activemq.apollo.broker.store._
import org.apache.activemq.apollo.util._
import ReporterLevel._
import org.apache.activemq.apollo.util.OptionSupport._
import java.io.{InputStream, OutputStream}
import scala.util.continuations._

object CassandraStore extends Log {

  /**
   * Creates a default a configuration object.
   */
  def defaultConfig() = {
    val rc = new CassandraStoreDTO
    rc.hosts.add("localhost:9160")
    rc
  }

  /**
   * Validates a configuration object.
   */
  def validate(config: CassandraStoreDTO, reporter:Reporter):ReporterLevel = {
    new Reporting(reporter) {
      if( config.hosts.isEmpty ) {
        error("At least one cassandra host must be configured.")
      }
    }.result
  }
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class CassandraStore extends DelayingStoreSupport with Logging {

  import CassandraStore._
  override protected def log = CassandraStore
                                
  var next_msg_key = new AtomicLong(1)

  val client = new CassandraClient()
  var config:CassandraStoreDTO = defaultConfig
  var blocking:ExecutorService = null

  def flush_delay = config.flush_delay.getOrElse(100)

  override def toString = "cassandra store"

  protected def get_next_msg_key = next_msg_key.getAndIncrement

  protected def store(uows: Seq[DelayableUOW])(callback: =>Unit) = {
    blocking {
      client.store(uows)
      dispatch_queue {
        callback
      }
    }
  }

  def configure(config: StoreDTO, reporter: Reporter):Unit = configure(config.asInstanceOf[CassandraStoreDTO], reporter)


  def get_store_status(callback:(StoreStatusDTO)=>Unit) = dispatch_queue {
    val rc = new SimpleStoreStatusDTO
    rc.state = service_state.toString
    rc.state_since = service_state.since
    callback(rc)
  }

  def configure(config: CassandraStoreDTO, reporter: Reporter):Unit = {
    if ( CassandraStore.validate(config, reporter) < ERROR ) {
      if( service_state.is_started ) {
        // TODO: apply changes while he broker is running.
        reporter.report(WARN, "Updating cassandra store configuration at runtime is not yet supported.  You must restart the broker for the change to take effect.")
      } else {
        this.config = config
      }
    }
  }

  protected def _start(on_completed: Runnable) = {
    info("Starting cassandra store at: '%s'", config.hosts.toList.mkString(", "))
    blocking = Executors.newFixedThreadPool(20, new ThreadFactory(){
      def newThread(r: Runnable) = {
        val rc = new Thread(r, "cassandra client")
        rc.setDaemon(true)
        rc
      }
    })
    client.schema = Schema(config.keyspace.getOrElse("ActiveMQ"))

    // TODO: move some of this parsing code into validation too.
    val HostPort = """([^:]+)(:(\d+))?""".r
    import JavaConversions._
    client.hosts = config.hosts.flatMap { x=>
      x match {
        case HostPort(host,_,port)=>
          Some(Host(host, port.toInt, 3000))
        case _=> None
      }
    }.toList

    client.start
    schedualDisplayStats
    on_completed.run
  }

  protected def _stop(on_completed: Runnable) = {
    info("Stopping cassandra store at: '%s'", config.hosts.toList.mkString(", "))
    blocking.shutdown
    new Thread("casandra client shutdown") {
      override def run = {
        while( !blocking.awaitTermination(5, TimeUnit.SECONDS) ) {
          warn("cassandra thread pool is taking a long time to shutdown.")
        }
        client.stop
        on_completed.run
      }
    }.start
  }


  /////////////////////////////////////////////////////////////////////
  //
  // Implementation of the BrokerDatabase interface
  //
  /////////////////////////////////////////////////////////////////////
  val storeLatency = new TimeCounter
  def schedualDisplayStats:Unit = {
    def displayStats = {
      if( service_state.is_started ) {
        val cl = storeLatency.apply(true)
        info("metrics: store latency: %,.3f ms", cl.avgTime(TimeUnit.MILLISECONDS))
        schedualDisplayStats
      }
    }
    dispatch_queue.dispatchAfter(5, TimeUnit.SECONDS, ^{ displayStats })
  }

  /**
   * Deletes all stored data from the store.
   */
  def purge(callback: =>Unit) = {
    blocking {
      client.purge
      next_msg_key.set(1)
      callback
    }
  }

  /**
   * Ges the next queue key identifier.
   */
  def get_last_queue_key(callback:(Option[Long])=>Unit):Unit = {
    // TODO:
    callback( Some(1L) )
  }

  def add_queue(record: QueueRecord)(callback: (Boolean) => Unit) = {
    blocking {
      client.addQueue(record)
      callback(true)
    }
  }

  def remove_queue(queueKey: Long)(callback: (Boolean) => Unit) = {
    blocking {
      callback(client.removeQueue(queueKey))
    }
  }

  def get_queue(id: Long)(callback: (Option[QueueRecord]) => Unit) = {
    blocking {
      callback( client.getQueue(id) )
    }
  }

  def list_queues(callback: (Seq[Long]) => Unit) = {
    blocking {
      callback( client.listQueues )
    }
  }

  def load_message(id: Long)(callback: (Option[MessageRecord]) => Unit) = {
    blocking {
      callback( client.loadMessage(id) )
    }
  }


  def list_queue_entry_ranges(queueKey: Long, limit: Int)(callback: (Seq[QueueEntryRange]) => Unit) = {
    blocking {
      callback( client.listQueueEntryGroups(queueKey, limit) )
    }
  }


  def list_queue_entries(queueKey: Long, firstSeq: Long, lastSeq: Long)(callback: (Seq[QueueEntryRecord]) => Unit) = {
    blocking {
      callback( client.getQueueEntries(queueKey, firstSeq, lastSeq) )
    }
  }

  /**
   * Exports the contents of the store to the provided streams.  Each stream should contain
   * a list of framed protobuf objects with the corresponding object types.
   */
  def export_pb(streams:StreamManager[OutputStream]):Result[Zilch,String] @suspendable = blocking ! {
    Failure("not supported")// client.export_pb(queue_stream, message_stream, queue_entry_stream)
  }

  /**
   * Imports a previously exported set of streams.  This deletes any previous data
   * in the store.
   */
  def import_pb(streams:StreamManager[InputStream]):Result[Zilch,String] @suspendable = blocking ! {
    Failure("not supported")//client.import_pb(queue_stream, message_stream, queue_entry_stream)
  }
}
