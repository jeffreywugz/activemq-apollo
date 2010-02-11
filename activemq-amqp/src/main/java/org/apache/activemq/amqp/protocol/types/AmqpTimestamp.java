/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * his work for additional information regarding copyright ownership.
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
package org.apache.activemq.amqp.protocol.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import org.apache.activemq.amqp.protocol.marshaller.AmqpEncodingError;
import org.apache.activemq.amqp.protocol.marshaller.AmqpMarshaller;
import org.apache.activemq.amqp.protocol.marshaller.Encoded;
import org.apache.activemq.util.buffer.Buffer;

/**
 * Represents a an absolute point in time
 */
public interface AmqpTimestamp extends AmqpType<AmqpTimestamp.AmqpTimestampBean, AmqpTimestamp.AmqpTimestampBuffer> {


    public Date getValue();

    public static class AmqpTimestampBean implements AmqpTimestamp{

        private AmqpTimestampBuffer buffer;
        private AmqpTimestampBean bean = this;
        private Date value;

        protected AmqpTimestampBean() {
        }

        public AmqpTimestampBean(Date value) {
            this.value = value;
        }

        public AmqpTimestampBean(AmqpTimestamp.AmqpTimestampBean other) {
            this.bean = other;
        }

        public final AmqpTimestampBean copy() {
            return bean;
        }

        public final AmqpTimestamp.AmqpTimestampBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{
            if(buffer == null) {
                buffer = new AmqpTimestampBuffer(marshaller.encode(this));
            }
            return buffer;
        }

        public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{
            getBuffer(marshaller).marshal(out, marshaller);
        }


        public Date getValue() {
            return bean.value;
        }


        public boolean equals(Object o){
            if(this == o) {
                return true;
            }

            if(o == null || !(o instanceof AmqpTimestamp)) {
                return false;
            }

            return equivalent((AmqpTimestamp) o);
        }

        public int hashCode() {
            if(getValue() == null) {
                return AmqpTimestamp.AmqpTimestampBean.class.hashCode();
            }
            return getValue().hashCode();
        }

        public boolean equivalent(AmqpType<?,?> t){
            if(this == t) {
                return true;
            }

            if(t == null || !(t instanceof AmqpTimestamp)) {
                return false;
            }

            return equivalent((AmqpTimestamp) t);
        }

        public boolean equivalent(AmqpTimestamp b) {
            if(b == null) {
                return false;
            }

            if(b.getValue() == null ^ getValue() == null) {
                return false;
            }

            return b.getValue() == null || b.getValue().equals(getValue());
        }
    }

    public static class AmqpTimestampBuffer implements AmqpTimestamp, AmqpBuffer< Date> {

        private AmqpTimestampBean bean;
        protected Encoded<Date> encoded;

        protected AmqpTimestampBuffer() {
        }

        protected AmqpTimestampBuffer(Encoded<Date> encoded) {
            this.encoded = encoded;
        }

        public final Encoded<Date> getEncoded() throws AmqpEncodingError{
            return encoded;
        }

        public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{
            encoded.marshal(out);
        }

        public Date getValue() {
            return bean().getValue();
        }

        public AmqpTimestamp.AmqpTimestampBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{
            return this;
        }

        protected AmqpTimestamp bean() {
            if(bean == null) {
                bean = new AmqpTimestamp.AmqpTimestampBean(encoded.getValue());
                bean.buffer = this;
            }
            return bean;
        }

        public boolean equals(Object o){
            return bean().equals(o);
        }

        public int hashCode() {
            return bean().hashCode();
        }

        public boolean equivalent(AmqpType<?, ?> t) {
            return bean().equivalent(t);
        }

        public static AmqpTimestamp.AmqpTimestampBuffer create(Encoded<Date> encoded) {
            if(encoded.isNull()) {
                return null;
            }
            return new AmqpTimestamp.AmqpTimestampBuffer(encoded);
        }

        public static AmqpTimestamp.AmqpTimestampBuffer create(DataInput in, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError {
            return create(marshaller.unmarshalAmqpTimestamp(in));
        }

        public static AmqpTimestamp.AmqpTimestampBuffer create(Buffer buffer, int offset, AmqpMarshaller marshaller) throws AmqpEncodingError {
            return create(marshaller.decodeAmqpTimestamp(buffer, offset));
        }
    }
}