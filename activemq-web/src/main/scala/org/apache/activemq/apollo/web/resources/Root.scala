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
package org.apache.activemq.apollo.web.resources

import java.lang.String
import com.sun.jersey.api.NotFoundException
import javax.ws.rs._
import core.{Response, Context}
import org.fusesource.scalate.util.Logging
import reflect.{BeanProperty}
import com.sun.jersey.api.view.ImplicitProduces
import org.apache.activemq.apollo.jaxb.BrokerConfig
import com.google.inject.servlet.RequestScoped
import org.apache.activemq.apollo.{BrokerRegistry, ConfigStore}
import org.fusesource.hawtdispatch.Future
import Response._
import Response.Status._

/**
 * Defines the default representations to be used on resources
 */
@ImplicitProduces(Array("text/html;qs=5"))
@Produces(Array("application/json", "application/xml","text/xml"))
trait Resource extends Logging {

  def result[T](value:Status, message:Any=null):T = {
    val response = status(value);
    if( message!=null ) {
      response.entity(message)
    }
    throw new WebApplicationException(response.build)
  }

}

/**
 * Manages a collection of broeker resources.
 */
@Path("/")
class Root() extends Resource {

  @Context
  var configStore:ConfigStore = BrokerRegistry.configStore;

  @GET
  def get() = this

  case class BrokerRef(@BeanProperty id:String, @BeanProperty href:String)

  def getBrokers: Array[BrokerRef] = {
    Future[List[String]] { cb=>
      configStore.listBrokerConfigs(cb)
    }.map(x=> new BrokerRef(x, x)).toArray[BrokerRef]
  }

  @Path("{id}")
  def broker(@PathParam("id") id : String): Broker = new Broker(this, id)
}

/**
 * A broker resource is used to represent the configuration and runtime status of a broker.
 */
case class Broker(parent:Root, @BeanProperty id: String) extends Resource {

  @Context
  var configStore:ConfigStore = BrokerRegistry.configStore;

  case class BrokerSummary(@BeanProperty id:String, @BeanProperty config_rev:Int, @BeanProperty config_href:String, @BeanProperty status_href:String)

  @GET
  def get() = {
    val c = config()
    new BrokerSummary(id, c.rev, "config/"+c.rev, "status")
  }

  @GET @Path("config")
  def getConfig():BrokerConfig = {
    config()
  }

  private def config() = {
    Future[Option[BrokerConfig]] { cb=>
      configStore.getBrokerConfig(id, cb)
    }.getOrElse(result(NOT_FOUND))
  }

  @GET @Path("config/{rev}")
  def getConfig(@PathParam("rev") rev:Int):BrokerConfig = {
    // that rev may have gone away..
    var c = config()
    c.rev==rev || result(NOT_FOUND)
    c
  }

  @PUT @Path("config/{rev}")
  def put(@PathParam("rev") rev:Int, config:BrokerConfig) = {
    config.id = id;
    config.rev = rev
    Future[Boolean] { cb=>
      configStore.putBrokerConfig(config, cb)
    } || result(NOT_FOUND)
  }

  @DELETE @Path("config/{rev}")
  def delete(@PathParam("rev") rev:Int) = {
    Future[Boolean] { cb=>
      configStore.removeBrokerConfig(id, rev, cb)
    } || result(NOT_FOUND)
  }

  @Path("status")
  def status = BrokerStatus(this, id)
}
