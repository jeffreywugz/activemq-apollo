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

import _root_.org.fusesource.hawtbuf._
import org.fusesource.hawtdispatch._
import collection.JavaConversions
import org.apache.activemq.apollo.util._
import collection.mutable.HashMap
import org.apache.activemq.apollo.broker.store.QueueRecord
import Buffer._
import org.apache.activemq.apollo.util.path.{Path, Part, PathMap, PathParser}
import java.util.ArrayList
import org.apache.activemq.apollo.dto._
import security.SecurityContext
import java.util.concurrent.TimeUnit

trait DomainDestination {

  def id:Long
  def name:String

  def can_bind(destination:DestinationDTO, consumer:DeliveryConsumer, security:SecurityContext):Boolean
  def bind (destination:DestinationDTO, consumer:DeliveryConsumer)
  def unbind (consumer:DeliveryConsumer, persistent:Boolean)

  def can_connect(destination:DestinationDTO, producer:BindableDeliveryProducer, security:SecurityContext):Boolean
  def connect (destination:DestinationDTO, producer:BindableDeliveryProducer)
  def disconnect (producer:BindableDeliveryProducer)

}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object LocalRouter extends Log {
  val TOPIC_DOMAIN = ascii("topic");
  val QUEUE_DOMAIN = ascii("queue");
  val TEMP_TOPIC_DOMAIN = ascii("temp-topic");
  val TEMP_QUEUE_DOMAIN = ascii("temp-queue");

  val QUEUE_KIND = ascii("queue");
  val DEFAULT_QUEUE_PATH = ascii("default");

  class ConsumerContext(val destination:DestinationDTO, val consumer:DeliveryConsumer, val security:SecurityContext) {
    override def hashCode: Int = consumer.hashCode

    override def equals(obj: Any): Boolean = {
      obj match {
        case x:ConsumerContext=> x.consumer == consumer
        case _ => false
      }
    }
  }

  class ProducerContext(val destination:DestinationDTO, val producer:BindableDeliveryProducer, val security:SecurityContext) {
    override def hashCode: Int = producer.hashCode

    override def equals(obj: Any): Boolean = {
      obj match {
        case x:ProducerContext=> x.producer == producer
        case _ => false
      }
    }
  }
}


/**
 * Provides a non-blocking concurrent producer to consumer
 * routing implementation.
 *
 * DeliveryProducers create a route object for each destination
 * they will be producing to.  Once the route is
 * connected to the router, the producer can use
 * the route.targets list without synchronization to
 * get the current set of consumers that are bound
 * to the destination. 
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class LocalRouter(val host:VirtualHost) extends BaseService with Router {
  import LocalRouter._

  protected def dispatch_queue:DispatchQueue = host.dispatch_queue

  def auto_create_destinations = {
    import OptionSupport._
    host.config.auto_create_destinations.getOrElse(true)
  }

  private val ALL = new Path({
    val rc = new ArrayList[Part](1)
    rc.add(Part.ANY_DESCENDANT)
    rc
  })

  trait Domain[D <: DomainDestination] {

    // holds all the destinations in the domain by id
    var destination_by_id = HashMap[Long, D]()
    // holds all the destinations in the domain by path
    var destination_by_path = new PathMap[D]()
    // Can store consumers on wild cards paths

    val consumers_by_path = new PathMap[ConsumerContext]()
    val producers_by_path = new PathMap[ProducerContext]()

    def destinations:Iterable[D] = JavaConversions.asScalaIterable(destination_by_path.get(ALL))

    def get_destination_matches(path:Path) = {
      import JavaConversions._
      asScalaIterable(destination_by_path.get( path ))
    }

    def create_destination(path:Path, security:SecurityContext):Result[D,String]

    def get_or_create_destination(path:Path, security:SecurityContext):Result[D,String] = {
      Option(destination_by_path.chooseValue(path)).
      map(Success(_)).
      getOrElse( create_destination(path, security))
    }

    def add_destination(path:Path, dest:D) = {
      destination_by_path.put(path, dest)
      destination_by_id.put(dest.id, dest)

      // binds any matching wild card subs and producers...
      import JavaConversions._
      consumers_by_path.get( path ).foreach { x=>
        if( dest.can_bind(x.destination, x.consumer, x.security) ) {
          dest.bind(x.destination, x.consumer)
        }
      }
      producers_by_path.get( path ).foreach { x=>
        if( dest.can_connect(x.destination, x.producer, x.security) ) {
          dest.connect(x.destination, x.producer)
        }
      }
    }

    def remove_destination(path:Path, dest:D) = {
      destination_by_path.remove(path, dest)
      destination_by_id.remove(dest.id)
    }

    def can_bind(path:Path, destination:DestinationDTO, consumer:DeliveryConsumer, security:SecurityContext):Result[Zilch, String] = {

      val wildcard = PathParser.containsWildCards(path)
      var matches = get_destination_matches(path)

      // Should we attempt to auto create the destination?
      if( !wildcard ) {
        if ( matches.isEmpty && auto_create_destinations ) {
          val rc = create_destination(path, security)
          if( rc.failed ) {
            return rc.map_success(_=> Zilch);
          }
          matches = get_destination_matches(path)
        }
        if( matches.isEmpty ) {
          return Failure("The destination does not exist.")
        }

        matches.foreach { dest =>
          if( !dest.can_bind(destination, consumer, security) ) {
            return Failure("Not authorized to reveive from the destination.")
          }
        }
      }
      Success(Zilch)
    }

    def bind(path:Path, destination:DestinationDTO, consumer:DeliveryConsumer, security:SecurityContext):Unit = {
      var matches = get_destination_matches(path)
      matches.foreach { dest=>
        if( dest.can_bind(destination, consumer, security) ) {
          dest.bind(destination, consumer)
        }
      }
      consumer.retain
      consumers_by_path.put(path, new ConsumerContext(destination, consumer, security))
    }

    def unbind(destination:DestinationDTO, consumer:DeliveryConsumer, persistent:Boolean) = {
      val path = DestinationParser.decode_path(destination.name)
      if( consumers_by_path.remove(path, new ConsumerContext(destination, consumer, null) ) ) {
        get_destination_matches(path).foreach{ dest=>
          dest.unbind(consumer, persistent)
        }
        consumer.release
      }

//      if( persistent ) {
//          destroy_queue(consumer.binding, security_context).failure_option.foreach{ reason=>
//            async_die(reason)
//          }
//      }

    }

    def can_connect(path:Path, destination:DestinationDTO, producer:BindableDeliveryProducer, security:SecurityContext):Result[Zilch, String] = {

      val wildcard = PathParser.containsWildCards(path)
      var matches = get_destination_matches(path)

      // Should we attempt to auto create the destination?
      if( !wildcard ) {
        if ( matches.isEmpty && auto_create_destinations ) {
          val rc = create_destination(path, security)
          if( rc.failed ) {
            return rc.map_success(_=> Zilch);
          }
          matches = get_destination_matches(path)
        }
        if( matches.isEmpty ) {
          return Failure("The destination does not exist.")
        }

        matches.foreach { dest =>
          if( !dest.can_connect(destination, producer, security) ) {
            return Failure("Not authorized to send to the destination.")
          }
        }
      }
      Success(Zilch)

    }

    def connect(path:Path, destination:DestinationDTO, producer:BindableDeliveryProducer, security:SecurityContext):Unit = {
      var matches = get_destination_matches(path)
      matches.foreach { dest=>
        if( dest.can_connect(destination, producer, security) ) {
          dest.connect(destination, producer)
        }
      }
      producers_by_path.put(path, new ProducerContext(destination, producer, security))
    }

    def disconnect(destination:DestinationDTO, producer:BindableDeliveryProducer) = {
      val path = DestinationParser.decode_path(destination.name)
      get_destination_matches(path).foreach { dest=>
        dest.disconnect(producer)
      }
      producer.release
    }

  }

  object topic_domain extends Domain[Topic] {

    val topic_id_counter = new LongCounter

    // Stores durable subscription queues.
    val durable_subscriptions_by_path = new PathMap[Queue]()
    val durable_subscriptions_by_id = HashMap[(String,String), Queue]()


    override def can_bind(path:Path, destination:DestinationDTO, consumer:DeliveryConsumer, security:SecurityContext):Result[Zilch, String] = {
      var rc = super.can_bind(path, destination, consumer, security)
      if( !rc.failed ) {
        destination match {
          case destination:DurableSubscriptionDestinationDTO=>
            // So the user can subscribe to the topic.. but can he create durable sub??
            val qc = ds_config(destination)
            if( !can_create_ds(qc, security) ) {
               return Failure("Not authorized to create the durable subscription.")
            }
          case _ =>
        }
      }
      rc
    }

    def get_or_create_durable_subscription(destination:DurableSubscriptionDestinationDTO):Queue = {
      durable_subscriptions_by_id.get( (destination.client_id, destination.subscription_id) ).getOrElse {
        val binding = QueueBinding.create(destination)
        val qc = ds_config(destination)
        _create_queue(-1, binding, qc)
      }
    }

    def destroy_durable_subscription(queue:Queue):Unit = {
      val destination = queue.binding.binding_dto.asInstanceOf[DurableSubscriptionDestinationDTO]
      if( durable_subscriptions_by_id.remove( (destination.client_id, destination.subscription_id) ).isDefined ) {
        val path = queue.binding.destination
        durable_subscriptions_by_path.remove(path, queue)
        var matches = get_destination_matches(path)
        matches.foreach( _.unbind_durable_subscription(destination, queue) )
        _destroy_queue(queue.id, null)
      }
    }

    def topic_config(name:Path):TopicDTO = {
      import collection.JavaConversions._
      import DestinationParser.default._
      import AsciiBuffer._
      host.config.topics.find( x=> parseFilter(ascii(x.name)).matches(name) ).getOrElse(new TopicDTO)
    }

    def can_create_ds(config:DurableSubscriptionDTO, security:SecurityContext) = {
      if( host.authorizer==null || security==null) {
        true
      } else {
        host.authorizer.can_create(security, host, config)
      }
    }

    def ds_config(destination:DurableSubscriptionDestinationDTO):DurableSubscriptionDTO = {
      import collection.JavaConversions._
      import DestinationParser.default._
      import AsciiBuffer._

      val name = DestinationParser.decode_path(destination.name)
      def matches(x:DurableSubscriptionDTO):Boolean = {
        if( x.name != null && !parseFilter(ascii(x.name)).matches(name)) {
          return false
        }
        if( x.client_id != null && x.client_id!=x.client_id ) {
          return false
        }
        if( x.subscription_id != null && x.subscription_id!=x.subscription_id ) {
          return false
        }
        true
      }
      host.config.durable_subscriptions.find(matches _).getOrElse(new DurableSubscriptionDTO)
    }

    def bind(queue:Queue) = {

      val destination = queue.binding.binding_dto.asInstanceOf[DurableSubscriptionDestinationDTO]
      val path = queue.binding.destination
      val wildcard = PathParser.containsWildCards(path)
      var matches = get_destination_matches(path)

      // We may need to create the topic...
      if( !wildcard && matches.isEmpty ) {
        create_destination(path, null)
        matches = get_destination_matches(path)
      }

      durable_subscriptions_by_path.put(path, queue)
      durable_subscriptions_by_id.put((destination.client_id, destination.subscription_id), queue)

      matches.foreach( _.bind_durable_subscription(destination, queue) )
    }

    def unbind(queue:Queue) = {
      val path = queue.binding.destination
      durable_subscriptions_by_path.remove(path, queue)
    }

    def create_destination(path:Path, security:SecurityContext):Result[Topic,String] = {

      // We can't create a wild card destination.. only wild card subscriptions.
      assert( !PathParser.containsWildCards(path) )

      // A new destination is being created...
      val dto = topic_config(path)

      if(  host.authorizer!=null && security!=null && !host.authorizer.can_create(security, host, dto)) {
        return new Failure("Not authorized to create the destination")
      }

      val id = topic_id_counter.incrementAndGet
      val topic = new Topic(LocalRouter.this, DestinationParser.encode_path(path), dto, id)
      add_destination(path, topic)
      Success(topic)
    }

  }

  object queue_domain extends Domain[Queue] {

    def config(binding:QueueBinding):QueueDTO = {
      import collection.JavaConversions._
      import DestinationParser.default._

      def matches(x:QueueDTO):Boolean = {
        if( x.name != null && !parseFilter(ascii(x.name)).matches(binding.destination)) {
          return false
        }
        true
      }
      host.config.queues.find(matches _).getOrElse(new QueueDTO)
    }

    def can_create_queue(config:QueueDTO, security:SecurityContext) = {
      if( host.authorizer==null || security==null) {
        true
      } else {
        host.authorizer.can_create(security, host, config)
      }
    }

    def bind(queue:Queue) = {
      val path = queue.binding.destination
      assert( !PathParser.containsWildCards(path) )
      add_destination(path, queue)
    }

    def unbind(queue:Queue) = {
      val path = queue.binding.destination
      remove_destination(path, queue)
    }

    def create_destination(path: Path, security: SecurityContext) = {
      val dto = new QueueDestinationDTO
      dto.name = DestinationParser.encode_path(path)

      val binding = QueueDomainQueueBinding.create(dto)
      val qc = config(binding)
      if( can_create_queue(qc, security) ) {
        val queue = _create_queue(-1, binding, qc)
        import OptionSupport._
        if( qc.unified.getOrElse(false) ) {
          // hook up the queue to be a subscriber of the topic.
          val topic = topic_domain.get_or_create_destination(path, null).success
          topic.bind(null, queue)
        }
        Success(queue)
      } else {
        Failure("Not authorized to create the queue")
      }

    }
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // life cycle methods.
  //
  /////////////////////////////////////////////////////////////////////////////

  protected def _start(on_completed: Runnable) = {
    val tracker = new LoggingTracker("router startup", dispatch_queue)
    if( host.store!=null ) {
      val task = tracker.task("list_queues")
      host.store.list_queues { queue_keys =>
        for( queue_key <- queue_keys) {
          val task = tracker.task("load queue: "+queue_key)
          // Use a global queue to so we concurrently restore
          // the queues.
          globalQueue {
            host.store.get_queue(queue_key) { x =>
              x match {
                case Some(record)=>
                  dispatch_queue {
                    _create_queue(record.key, QueueBinding.create(record.binding_kind, record.binding_data), null)
                    task.run
                  }
                case _ => task.run
              }
            }
          }
        }
        task.run
      }
    }

    import OptionSupport._
    if(host.config.regroup_connections.getOrElse(false)) {
      schedule_connection_regroup
    }

    tracker.callback(on_completed)
  }

  protected def _stop(on_completed: Runnable) = {
    val tracker = new LoggingTracker("router shutdown", dispatch_queue)
    queues_by_id.valuesIterator.foreach { queue=>
      tracker.stop(queue)
    }
    tracker.callback(on_completed)
  }


  // Try to periodically re-balance connections so that consumers/producers
  // are grouped onto the same thread.
  def schedule_connection_regroup:Unit = dispatch_queue.after(1, TimeUnit.SECONDS) {
    if(service_state.is_started) {
      connection_regroup
      schedule_connection_regroup
    }
  }

  def connection_regroup = {
    // this should really be much more fancy.  It should look at the messaging
    // rates between producers and consumers, look for natural data flow partitions
    // and then try to equally divide the load over the available processing
    // threads/cores.



    // For the topics, just collocate the producers onto the first consumer's thread.
    topic_domain.destinations.foreach { node =>

      node.consumers.headOption.foreach{ consumer =>
        node.producers.foreach { r=>
          r.collocate(consumer.dispatch_queue)
        }
      }
    }


    queue_domain.destinations.foreach { queue=>
      queue.dispatch_queue {

        // Collocate the queue's with the first consumer
        // TODO: change this so it collocates with the fastest consumer.

        queue.all_subscriptions.headOption.map( _._1 ).foreach { consumer=>
          queue.collocate( consumer.dispatch_queue )
        }

        // Collocate all the producers with the queue..

        queue.inbound_sessions.foreach { session =>
          session.producer.collocate( queue.dispatch_queue )
        }
      }

    }
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // destination/domain management methods.
  //
  /////////////////////////////////////////////////////////////////////////////

  def domain(destination: DestinationDTO) = destination match {
    case x:TopicDestinationDTO => topic_domain
    case x:DurableSubscriptionDestinationDTO => topic_domain
    case x:QueueDestinationDTO => queue_domain
    case _ => throw new RuntimeException("Unknown domain type: "+destination.getClass)
  }

  def bind(destination: Array[DestinationDTO], consumer: DeliveryConsumer, security: SecurityContext) = {
    consumer.retain
    val paths = destination.map(x=> (DestinationParser.decode_path(x.name), x) )
    dispatch_queue ! {
      val failures = paths.map(x=> domain(x._2).can_bind(x._1, x._2, consumer, security) ).flatMap( _.failure_option )
      val rc = if( !failures.isEmpty ) {
        Failure(failures.mkString("; "))
      } else {
        paths.foreach { x=>
          domain(x._2).bind(x._1, x._2, consumer, security)
        }
        Success(Zilch)
      }
      consumer.release
      rc
    }
  }

  def unbind(destinations: Array[DestinationDTO], consumer: DeliveryConsumer, persistent:Boolean=false) = {
    consumer.retain
    dispatch_queue {
      destinations.foreach { destination=>
        domain(destination).unbind(destination, consumer, persistent)
      }
      consumer.release
    }
  }

  def connect(destinations: Array[DestinationDTO], producer: BindableDeliveryProducer, security: SecurityContext) = {
    producer.retain
    val paths = destinations.map(x=> (DestinationParser.decode_path(x.name), x) )
    dispatch_queue ! {

      val failures = paths.map(x=> domain(x._2).can_connect(x._1, x._2, producer, security) ).flatMap( _.failure_option )

      if( !failures.isEmpty ) {
        producer.release
        Failure(failures.mkString("; "))
      } else {
        paths.foreach { x=>
          domain(x._2).connect(x._1, x._2, producer, security)
        }
        producer.connected()
        Success(Zilch)
      }
    }
  }

  def disconnect(destinations:Array[DestinationDTO], producer:BindableDeliveryProducer) = {
    dispatch_queue {
      destinations.foreach { destination=>
        domain(destination).disconnect(destination, producer)
      }
      producer.disconnected()
      producer.release()
    }
  }

  def get_or_create_destination(id: DestinationDTO, security: SecurityContext) = dispatch_queue ! {
    _get_or_create_destination(id, security)
  }

  /**
   * Returns the previously created queue if it already existed.
   */
  def _get_or_create_destination(dto: DestinationDTO, security:SecurityContext): Result[DomainDestination, String] = {
    val path = DestinationParser.decode_path(dto.name)
    domain(dto).get_or_create_destination(path, security)
  }


  /////////////////////////////////////////////////////////////////////////////
  //
  // Queue management methods.  Queues are multi-purpose and get used by both
  // the queue domain and topic domain.
  //
  /////////////////////////////////////////////////////////////////////////////

  var queues_by_binding = HashMap[QueueBinding, Queue]()
  var queues_by_id = HashMap[Long, Queue]()

  /**
   * Gets an existing queue.
   */
  def get_queue(dto:DestinationDTO) = dispatch_queue ! {
    queues_by_binding.get(QueueBinding.create(dto))
  }

  /**
   * Gets an existing queue.
   */
  def get_queue(id:Long) = dispatch_queue ! {
    queues_by_id.get(id)
  }

  def _create_queue(id:Long, binding:QueueBinding, config:QueueDTO):Queue = {

    var qid = id
    if( qid == -1 ) {
      qid = host.queue_id_counter.incrementAndGet
    }

    val queue = new Queue(this, qid, binding, config)
    if( queue.tune_persistent && id == -1 ) {

      val record = new QueueRecord
      record.key = qid
      record.binding_data = binding.binding_data
      record.binding_kind = binding.binding_kind

      host.store.add_queue(record) { rc => Unit }

    }

    queue.start
    queues_by_binding.put(binding, queue)
    queues_by_id.put(queue.id, queue)

    // this causes the queue to get registered in the right location in
    // the router.
    binding.bind(this, queue)
    queue
  }

  /**
   * Returns true if the queue no longer exists.
   */
  def destroy_queue(id:Long, security:SecurityContext) = dispatch_queue ! { _destroy_queue(id,security) }

  def _destroy_queue(id:Long, security:SecurityContext):Result[Zilch, String] = {
    queues_by_id.get(id) match {
      case Some(queue) =>
        _destroy_queue(queue,security)
      case None =>
        Failure("Does not exist")
    }
  }

  /**
   * Returns true if the queue no longer exists.
   */
  def destroy_queue(dto:DestinationDTO, security:SecurityContext) = dispatch_queue ! { _destroy_queue(dto, security) }

  def _destroy_queue(dto:DestinationDTO, security:SecurityContext):Result[Zilch, String] = {
    queues_by_binding.get(QueueBinding.create(dto)) match {
      case Some(queue) =>
        _destroy_queue(queue, security)
      case None =>
        Failure("Does not exist")
    }
  }

  def _destroy_queue(queue:Queue, security:SecurityContext):Result[Zilch, String] = {

    if( security!=null && queue.config.acl!=null ) {
      if( !host.authorizer.can_destroy(security, host, queue.config) ) {
        return Failure("Not authorized to destroy")
      }
    }

    queue.binding.unbind(this, queue)
    queues_by_binding.remove(queue.binding)
    queues_by_id.remove(queue.id)
    queue.stop
    if( queue.tune_persistent ) {
      queue.dispatch_queue ^ {
        host.store.remove_queue(queue.id){x=> Unit}
      }
    }
    Success(Zilch)
  }

}
