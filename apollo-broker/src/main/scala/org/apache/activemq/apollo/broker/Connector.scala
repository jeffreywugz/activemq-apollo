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
package org.apache.activemq.apollo.broker

import org.fusesource.hawtdispatch._
import org.fusesource.hawtdispatch.{Dispatch}
import org.apache.activemq.apollo.dto.{ConnectorDTO}
import protocol.{ProtocolFactory, Protocol}
import collection.mutable.HashMap
import org.apache.activemq.apollo.transport._
import org.apache.activemq.apollo.util._
import ReporterLevel._
import org.apache.activemq.apollo.util.OptionSupport._


/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object Connector extends Log {

  val STICK_ON_THREAD_QUEUES = Broker.STICK_ON_THREAD_QUEUES

  /**
   * Creates a default a configuration object.
   */
  def defaultConfig() = {
    val rc = new ConnectorDTO
    rc.id = "default"
    rc.advertise = "tcp://localhost:61613"
    rc.bind = "tcp://0.0.0.0:61613"
    rc.protocol = "multi"
    rc.connection_limit = 1000
    rc
  }

  /**
   * Validates a configuration object.
   */
  def validate(config: ConnectorDTO, reporter:Reporter):ReporterLevel = {
    new Reporting(reporter) {
      if( empty(config.id) ) {
        error("Connector id must be specified")
      }
    }.result
  }


}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class Connector(val broker:Broker, val id:Long) extends BaseService {
  import Connector._

  override val dispatchQueue = broker.dispatchQueue

  var config:ConnectorDTO = defaultConfig
  var transportServer:TransportServer = _
  var protocol:Protocol = _

  val connections = HashMap[Long, BrokerConnection]()
  override def toString = "connector: "+config.id
  val accept_counter = new LongCounter

  object BrokerAcceptListener extends TransportAcceptListener {
    def onAcceptError(e: Exception): Unit = {
      log.warn(e, "Error occured while accepting client connection.")
    }

    def onAccept(transport: Transport): Unit = {
      if( protocol!=null ) {
        transport.setProtocolCodec(protocol.createProtocolCodec)
      }

      accept_counter.incrementAndGet
      var connection = new BrokerConnection(Connector.this, broker.connection_id_counter.incrementAndGet)
      connection.dispatchQueue.setLabel("connection %d to %s".format(connection.id, transport.getRemoteAddress))
      connection.protocolHandler = protocol.createProtocolHandler
      connection.transport = transport

      if( STICK_ON_THREAD_QUEUES ) {
        connection.dispatchQueue.setTargetQueue(Dispatch.getRandomThreadQueue)
      }

      // We release when it gets removed form the connections list.
      connection.dispatchQueue.retain
      connections.put(connection.id, connection)
      info("Client connected from: %s", connection.transport.getRemoteAddress)

      try {
        connection.start()
      } catch {
        case e1: Exception => {
          onAcceptError(e1)
        }
      }

      // We may need to stop acepting connections..
      if(at_connection_limit) {
        transportServer.suspend
      }
    }
  }

  def at_connection_limit = {
    connections.size >= config.connection_limit.getOrElse(Integer.MAX_VALUE)
  }

  /**
   * Validates and then applies the configuration.
   */
  def configure(config: ConnectorDTO, reporter:Reporter) = ^{
    if ( validate(config, reporter) < ERROR ) {
      this.config = config

      if( serviceState.isStarted ) {
        // TODO: apply changes while running
        reporter.report(WARN, "Updating connector configuration at runtime is not yet supported.  You must restart the broker for the change to take effect.")

      }
    }
  } |>>: dispatchQueue


  override def _start(onCompleted:Runnable) = {
    assert(config!=null, "Connector must be configured before it is started.")
    protocol = ProtocolFactory.get(config.protocol.getOrElse("multi")).get
    transportServer = TransportFactory.bind( config.bind )
    transportServer.setDispatchQueue(dispatchQueue)
    transportServer.setAcceptListener(BrokerAcceptListener)

    if( transportServer.isInstanceOf[KeyManagerAware] && broker.key_storage!=null ) {
      transportServer.asInstanceOf[KeyManagerAware].setKeyManagers(broker.key_storage.create_key_managers)
    }
    transportServer.start(^{
      info("Accepting connections at: "+config.bind)
      onCompleted.run
    })
  }


  override def _stop(onCompleted:Runnable): Unit = {
    transportServer.stop(^{
      info("Stopped connector at: "+config.bind)
      val tracker = new LoggingTracker(toString, dispatchQueue)
      connections.valuesIterator.foreach { connection=>
        tracker.stop(connection)
      }
      tracker.callback(onCompleted)
    })
  }

  /**
   * Connections callback into the connector when they are stopped so that we can
   * stop tracking them.
   */
  def stopped(connection:BrokerConnection) = dispatchQueue {
    val at_limit = at_connection_limit
    if( connections.remove(connection.id).isDefined ) {
      info("Client disconnected from: %s", connection.transport.getRemoteAddress)
      connection.dispatchQueue.release
      if( at_limit ) {
        transportServer.resume
      }
    }
  }

}
