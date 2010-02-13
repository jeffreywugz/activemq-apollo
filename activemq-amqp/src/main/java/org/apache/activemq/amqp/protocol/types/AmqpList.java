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
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.activemq.amqp.protocol.marshaller.AmqpEncodingError;
import org.apache.activemq.amqp.protocol.marshaller.AmqpMarshaller;
import org.apache.activemq.amqp.protocol.marshaller.Encoded;
import org.apache.activemq.amqp.protocol.types.IAmqpList;
import org.apache.activemq.util.buffer.Buffer;

/**
 * Represents a a sequence of polymorphic values
 */
public interface AmqpList extends AmqpType<AmqpList.AmqpListBean, AmqpList.AmqpListBuffer>, IAmqpList<AmqpType<?, ?>> {

    /**
     * Represents a a sequence of polymorphic values
     */
    public void set(int index, AmqpType<?, ?> value);
    public AmqpType<?, ?> get(int index);
    public int getListCount();

    public static class AmqpListBean implements AmqpList{

        private AmqpListBuffer buffer;
        private AmqpListBean bean = this;
        private IAmqpList<AmqpType<?, ?>> value;

        AmqpListBean() {
            this.value = new IAmqpList.AmqpWrapperList(new ArrayList<AmqpType<?,?>>());
        }

        AmqpListBean(IAmqpList<AmqpType<?, ?>> value) {
            this.value = value;
        }

        AmqpListBean(AmqpList.AmqpListBean other) {
            this.bean = other;
        }

        public final AmqpListBean copy() {
            return new AmqpList.AmqpListBean(bean);
        }

        public final AmqpList.AmqpListBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{
            if(buffer == null) {
                buffer = new AmqpListBuffer(marshaller.encode(this));
            }
            return buffer;
        }

        public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{
            getBuffer(marshaller).marshal(out, marshaller);
        }


        public void set(int index, AmqpType<?, ?> value) {
            copyCheck();
            bean.value.set(index, value);
        }

        public AmqpType<?, ?> get(int index) {
            return bean.value.get(index);
        }

        public int getListCount() {
            return bean.value.getListCount();
        }

        public Iterator<AmqpType<?, ?>> iterator() {
            return new AmqpListIterator<AmqpType<?, ?>>(bean.value);
        }


        private final void copyCheck() {
            if(buffer != null) {;
                throw new IllegalStateException("unwriteable");
            }
            if(bean != this) {;
                copy(bean);
            }
        }

        private final void copy(AmqpList.AmqpListBean other) {
            this.value = other.value;
            bean = this;
        }

        public boolean equals(Object o){
            if(this == o) {
                return true;
            }

            if(o == null || !(o instanceof AmqpList)) {
                return false;
            }

            return equals((AmqpList) o);
        }

        public boolean equals(AmqpList b) {
            return AbstractAmqpList.checkEqual(this, b);
        }

        public int hashCode() {
            return AbstractAmqpList.hashCodeFor(this);
        }
    }

    public static class AmqpListBuffer implements AmqpList, AmqpBuffer< IAmqpList<AmqpType<?, ?>>> {

        private AmqpListBean bean;
        protected Encoded<IAmqpList<AmqpType<?, ?>>> encoded;

        protected AmqpListBuffer() {
        }

        protected AmqpListBuffer(Encoded<IAmqpList<AmqpType<?, ?>>> encoded) {
            this.encoded = encoded;
        }

        public final Encoded<IAmqpList<AmqpType<?, ?>>> getEncoded() throws AmqpEncodingError{
            return encoded;
        }

        public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{
            encoded.marshal(out);
        }

        public void set(int index, AmqpType<?, ?> value) {
            bean().set(index, value);
        }

        public AmqpType<?, ?> get(int index) {
            return bean().get(index);
        }

        public int getListCount() {
            return bean().getListCount();
        }

        public Iterator<AmqpType<?, ?>> iterator() {
            return bean().iterator();
        }

        public AmqpList.AmqpListBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{
            return this;
        }

        protected AmqpList bean() {
            if(bean == null) {
                bean = new AmqpList.AmqpListBean(encoded.getValue());
                bean.buffer = this;
            }
            return bean;
        }

        public boolean equals(Object o){
            return bean().equals(o);
        }

        public boolean equals(AmqpList o){
            return bean().equals(o);
        }

        public int hashCode() {
            return bean().hashCode();
        }

        public static AmqpList.AmqpListBuffer create(Encoded<IAmqpList<AmqpType<?, ?>>> encoded) {
            if(encoded.isNull()) {
                return null;
            }
            return new AmqpList.AmqpListBuffer(encoded);
        }

        public static AmqpList.AmqpListBuffer create(DataInput in, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError {
            return create(marshaller.unmarshalAmqpList(in));
        }

        public static AmqpList.AmqpListBuffer create(Buffer buffer, int offset, AmqpMarshaller marshaller) throws AmqpEncodingError {
            return create(marshaller.decodeAmqpList(buffer, offset));
        }
    }
}