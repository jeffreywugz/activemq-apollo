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
import java.lang.String;
import org.apache.activemq.amqp.protocol.marshaller.AmqpEncodingError;
import org.apache.activemq.amqp.protocol.marshaller.AmqpMarshaller;
import org.apache.activemq.amqp.protocol.marshaller.Encoded;
import org.apache.activemq.util.buffer.Buffer;

/**
 * Represents a symbolic values from a constrained domain
 * <p>
 * Symbols are values from a constrained domain. Although the set of possible domains is
 * open-ended, typically the both number and size of symbols in use for any given application
 * will be small, e.g. small enough that it is reasonable to cache all the distinct values.
 * </p>
 */
public interface AmqpSymbol extends AmqpType<AmqpSymbol.AmqpSymbolBean, AmqpSymbol.AmqpSymbolBuffer> {


    public String getValue();

    public static class AmqpSymbolBean implements AmqpSymbol{

        private AmqpSymbolBuffer buffer;
        private AmqpSymbolBean bean = this;
        private String value;

        protected AmqpSymbolBean() {
        }

        public AmqpSymbolBean(String value) {
            this.value = value;
        }

        public AmqpSymbolBean(AmqpSymbol.AmqpSymbolBean other) {
            this.bean = other;
        }

        public final AmqpSymbolBean copy() {
            return bean;
        }

        public final AmqpSymbol.AmqpSymbolBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{
            if(buffer == null) {
                buffer = new AmqpSymbolBuffer(marshaller.encode(this));
            }
            return buffer;
        }

        public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{
            getBuffer(marshaller).marshal(out, marshaller);
        }


        public String getValue() {
            return bean.value;
        }


        public boolean equals(Object o){
            if(this == o) {
                return true;
            }

            if(o == null || !(o instanceof AmqpSymbol)) {
                return false;
            }

            return equivalent((AmqpSymbol) o);
        }

        public int hashCode() {
            if(getValue() == null) {
                return AmqpSymbol.AmqpSymbolBean.class.hashCode();
            }
            return getValue().hashCode();
        }

        public boolean equivalent(AmqpType<?,?> t){
            if(this == t) {
                return true;
            }

            if(t == null || !(t instanceof AmqpSymbol)) {
                return false;
            }

            return equivalent((AmqpSymbol) t);
        }

        public boolean equivalent(AmqpSymbol b) {
            if(b == null) {
                return false;
            }

            if(b.getValue() == null ^ getValue() == null) {
                return false;
            }

            return b.getValue() == null || b.getValue().equals(getValue());
        }
    }

    public static class AmqpSymbolBuffer implements AmqpSymbol, AmqpBuffer< String> {

        private AmqpSymbolBean bean;
        protected Encoded<String> encoded;

        protected AmqpSymbolBuffer() {
        }

        protected AmqpSymbolBuffer(Encoded<String> encoded) {
            this.encoded = encoded;
        }

        public final Encoded<String> getEncoded() throws AmqpEncodingError{
            return encoded;
        }

        public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{
            encoded.marshal(out);
        }

        public String getValue() {
            return bean().getValue();
        }

        public AmqpSymbol.AmqpSymbolBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{
            return this;
        }

        protected AmqpSymbol bean() {
            if(bean == null) {
                bean = new AmqpSymbol.AmqpSymbolBean(encoded.getValue());
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

        public static AmqpSymbol.AmqpSymbolBuffer create(Encoded<String> encoded) {
            if(encoded.isNull()) {
                return null;
            }
            return new AmqpSymbol.AmqpSymbolBuffer(encoded);
        }

        public static AmqpSymbol.AmqpSymbolBuffer create(DataInput in, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError {
            return create(marshaller.unmarshalAmqpSymbol(in));
        }

        public static AmqpSymbol.AmqpSymbolBuffer create(Buffer buffer, int offset, AmqpMarshaller marshaller) throws AmqpEncodingError {
            return create(marshaller.decodeAmqpSymbol(buffer, offset));
        }
    }
}