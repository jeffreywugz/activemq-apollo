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
package org.apache.activemq.apollo.broker;

import org.apache.activemq.apollo.broker.DeliveryTarget;
import org.apache.activemq.apollo.broker.Destination;
import org.apache.activemq.apollo.broker.MessageDelivery;
import org.apache.activemq.flow.ISourceController;
import org.apache.activemq.queue.IQueue;
import org.apache.activemq.queue.Subscription;

public class Queue implements DeliveryTarget {

    private Destination destination;
    private final IQueue<Long, MessageDelivery> queue;
    private VirtualHost virtualHost;

    Queue(IQueue<Long, MessageDelivery> queue) {
        this.queue = queue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.activemq.broker.DeliveryTarget#deliver(org.apache.activemq
     * .broker.MessageDelivery, org.apache.activemq.flow.ISourceController)
     */
    public void deliver(MessageDelivery message, ISourceController<?> source) {
        queue.add(message, source);
    }

    public final void addSubscription(final Subscription<MessageDelivery> sub) {
        queue.addSubscription(sub);
    }

    public boolean removeSubscription(final Subscription<MessageDelivery> sub) {
        return queue.removeSubscription(sub);
    }

    public void start() throws Exception {
        queue.start();
    }

    public void stop() throws Exception {
        if (queue != null) {
            queue.stop();
        }
    }

    public void shutdown(boolean sync) throws Exception {
        if (queue != null) {
            queue.shutdown(sync);
        }
    }

    public boolean hasSelector() {
        return false;
    }

    public boolean matches(MessageDelivery message) {
        return true;
    }

    public VirtualHost getBroker() {
        return virtualHost;
    }

    public void setVirtualHost(VirtualHost virtualHost) {
        this.virtualHost = virtualHost;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public final Destination getDestination() {
        return destination;
    }

    public boolean isDurable() {
        return true;
    }

    public static class QueueSubscription implements BrokerSubscription {
        Subscription<MessageDelivery> subscription;
        final Queue queue;

        public QueueSubscription(Queue queue) {
            this.queue = queue;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.apache.activemq.broker.BrokerSubscription#connect(org.apache.
         * activemq.broker.protocol.ProtocolHandler.ConsumerContext)
         */
        public void connect(Subscription<MessageDelivery> subscription) throws UserAlreadyConnectedException {
            this.subscription = subscription;
            queue.addSubscription(subscription);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.apache.activemq.broker.BrokerSubscription#disconnect(org.apache
         * .activemq.broker.protocol.ProtocolHandler.ConsumerContext)
         */
        public void disconnect(Subscription<MessageDelivery> context) {
            queue.removeSubscription(subscription);
        }
        
        /* (non-Javadoc)
         * @see org.apache.activemq.broker.BrokerSubscription#getDestination()
         */
        public Destination getDestination() {
            return queue.getDestination();
        }
    }

}