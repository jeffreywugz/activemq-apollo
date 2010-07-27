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
import java.lang.Short;
import java.lang.String;
import java.util.Iterator;
import org.apache.activemq.amqp.protocol.marshaller.AmqpEncodingError;
import org.apache.activemq.amqp.protocol.marshaller.AmqpMarshaller;
import org.apache.activemq.amqp.protocol.marshaller.Encoded;
import org.apache.activemq.amqp.protocol.types.IAmqpList;
import org.fusesource.hawtbuf.Buffer;

/**
 * Represents a details of a Session error
 * <p>
 * This struct carries information on an exception which has occurred on the Session. The
 * command-id, when given, correlates the error to a specific command.
 * </p>
 */
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//
//!!!!!!!!THIS CLASS IS AUTOGENERATED DO NOT MODIFY DIRECTLY!!!!!!!!!!!!//
//!!!!!!Instead, modify the generator in activemq-amqp-generator!!!!!!!!//
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//
public interface AmqpSessionError extends AmqpList {



    /**
     * error code indicating the type of error
     */
    public void setErrorCode(AmqpSessionErrorCode errorCode);

    /**
     * error code indicating the type of error
     */
    public AmqpSessionErrorCode getErrorCode();

    /**
     * exceptional command
     * <p>
     * The command-id of the command which caused the exception. If the exception was not
     * caused by a specific command, this value is not set.
     * </p>
     * <p>
     * A sequence-no encodes a serial number as defined in RFC-1982. The arithmetic, and
     * operators for these numbers are defined by RFC-1982.
     * </p>
     */
    public void setCommandId(Long commandId);

    /**
     * exceptional command
     * <p>
     * The command-id of the command which caused the exception. If the exception was not
     * caused by a specific command, this value is not set.
     * </p>
     * <p>
     * A sequence-no encodes a serial number as defined in RFC-1982. The arithmetic, and
     * operators for these numbers are defined by RFC-1982.
     * </p>
     */
    public void setCommandId(long commandId);

    /**
     * exceptional command
     * <p>
     * The command-id of the command which caused the exception. If the exception was not
     * caused by a specific command, this value is not set.
     * </p>
     * <p>
     * A sequence-no encodes a serial number as defined in RFC-1982. The arithmetic, and
     * operators for these numbers are defined by RFC-1982.
     * </p>
     */
    public void setCommandId(AmqpSequenceNo commandId);

    /**
     * exceptional command
     * <p>
     * The command-id of the command which caused the exception. If the exception was not
     * caused by a specific command, this value is not set.
     * </p>
     * <p>
     * A sequence-no encodes a serial number as defined in RFC-1982. The arithmetic, and
     * operators for these numbers are defined by RFC-1982.
     * </p>
     */
    public AmqpSequenceNo getCommandId();

    /**
     * the class code of the command whose execution gave rise to the error (if appropriate)
     */
    public void setCommandCode(Short commandCode);

    /**
     * the class code of the command whose execution gave rise to the error (if appropriate)
     */
    public void setCommandCode(short commandCode);

    /**
     * the class code of the command whose execution gave rise to the error (if appropriate)
     */
    public void setCommandCode(AmqpUbyte commandCode);

    /**
     * the class code of the command whose execution gave rise to the error (if appropriate)
     */
    public Short getCommandCode();

    /**
     * index of the exceptional field
     * <p>
     * The zero based index of the exceptional field within the arguments to the exceptional
     * command. If the exception was not caused by a specific field, this value is not set.
     * </p>
     */
    public void setFieldIndex(Short fieldIndex);

    /**
     * index of the exceptional field
     * <p>
     * The zero based index of the exceptional field within the arguments to the exceptional
     * command. If the exception was not caused by a specific field, this value is not set.
     * </p>
     */
    public void setFieldIndex(short fieldIndex);

    /**
     * index of the exceptional field
     * <p>
     * The zero based index of the exceptional field within the arguments to the exceptional
     * command. If the exception was not caused by a specific field, this value is not set.
     * </p>
     */
    public void setFieldIndex(AmqpUbyte fieldIndex);

    /**
     * index of the exceptional field
     * <p>
     * The zero based index of the exceptional field within the arguments to the exceptional
     * command. If the exception was not caused by a specific field, this value is not set.
     * </p>
     */
    public Short getFieldIndex();

    /**
     * descriptive text about the exception
     * <p>
     * The description provided is implementation defined, but MUST be in the language
     * appropriate for the selected locale. The intention is that this description is suitable
     * for logging or alerting output.
     * </p>
     */
    public void setDescription(String description);

    /**
     * descriptive text about the exception
     * <p>
     * The description provided is implementation defined, but MUST be in the language
     * appropriate for the selected locale. The intention is that this description is suitable
     * for logging or alerting output.
     * </p>
     */
    public void setDescription(AmqpString description);

    /**
     * descriptive text about the exception
     * <p>
     * The description provided is implementation defined, but MUST be in the language
     * appropriate for the selected locale. The intention is that this description is suitable
     * for logging or alerting output.
     * </p>
     */
    public String getDescription();

    /**
     * map to carry additional information about the error
     */
    public void setErrorInfo(AmqpMap errorInfo);

    /**
     * map to carry additional information about the error
     */
    public IAmqpMap<AmqpType<?, ?>, AmqpType<?, ?>> getErrorInfo();

    public static class AmqpSessionErrorBean implements AmqpSessionError{

        private AmqpSessionErrorBuffer buffer;
        private AmqpSessionErrorBean bean = this;
        private AmqpSessionErrorCode errorCode;
        private AmqpSequenceNo commandId;
        private AmqpUbyte commandCode;
        private AmqpUbyte fieldIndex;
        private AmqpString description;
        private AmqpMap errorInfo;

        AmqpSessionErrorBean() {
        }

        AmqpSessionErrorBean(IAmqpList<AmqpType<?, ?>> value) {

            for(int i = 0; i < value.getListCount(); i++) {
                set(i, value.get(i));
            }
        }

        AmqpSessionErrorBean(AmqpSessionError.AmqpSessionErrorBean other) {
            this.bean = other;
        }

        public final AmqpSessionErrorBean copy() {
            return new AmqpSessionError.AmqpSessionErrorBean(bean);
        }

        public final AmqpSessionError.AmqpSessionErrorBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{
            if(buffer == null) {
                buffer = new AmqpSessionErrorBuffer(marshaller.encode(this));
            }
            return buffer;
        }

        public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{
            getBuffer(marshaller).marshal(out, marshaller);
        }


        public final void setErrorCode(AmqpSessionErrorCode errorCode) {
            copyCheck();
            bean.errorCode = errorCode;
        }

        public final AmqpSessionErrorCode getErrorCode() {
            return bean.errorCode;
        }

        public void setCommandId(Long commandId) {
            setCommandId(TypeFactory.createAmqpSequenceNo(commandId));
        }


        public void setCommandId(long commandId) {
            setCommandId(TypeFactory.createAmqpSequenceNo(commandId));
        }


        public final void setCommandId(AmqpSequenceNo commandId) {
            copyCheck();
            bean.commandId = commandId;
        }

        public final AmqpSequenceNo getCommandId() {
            return bean.commandId;
        }

        public void setCommandCode(Short commandCode) {
            setCommandCode(TypeFactory.createAmqpUbyte(commandCode));
        }


        public void setCommandCode(short commandCode) {
            setCommandCode(TypeFactory.createAmqpUbyte(commandCode));
        }


        public final void setCommandCode(AmqpUbyte commandCode) {
            copyCheck();
            bean.commandCode = commandCode;
        }

        public final Short getCommandCode() {
            return bean.commandCode.getValue();
        }

        public void setFieldIndex(Short fieldIndex) {
            setFieldIndex(TypeFactory.createAmqpUbyte(fieldIndex));
        }


        public void setFieldIndex(short fieldIndex) {
            setFieldIndex(TypeFactory.createAmqpUbyte(fieldIndex));
        }


        public final void setFieldIndex(AmqpUbyte fieldIndex) {
            copyCheck();
            bean.fieldIndex = fieldIndex;
        }

        public final Short getFieldIndex() {
            return bean.fieldIndex.getValue();
        }

        public void setDescription(String description) {
            setDescription(TypeFactory.createAmqpString(description));
        }


        public final void setDescription(AmqpString description) {
            copyCheck();
            bean.description = description;
        }

        public final String getDescription() {
            return bean.description.getValue();
        }

        public final void setErrorInfo(AmqpMap errorInfo) {
            copyCheck();
            bean.errorInfo = errorInfo;
        }

        public final IAmqpMap<AmqpType<?, ?>, AmqpType<?, ?>> getErrorInfo() {
            return bean.errorInfo;
        }

        public void set(int index, AmqpType<?, ?> value) {
            switch(index) {
            case 0: {
                setErrorCode(AmqpSessionErrorCode.get((AmqpUshort)value));
                break;
            }
            case 1: {
                setCommandId((AmqpSequenceNo) value);
                break;
            }
            case 2: {
                setCommandCode((AmqpUbyte) value);
                break;
            }
            case 3: {
                setFieldIndex((AmqpUbyte) value);
                break;
            }
            case 4: {
                setDescription((AmqpString) value);
                break;
            }
            case 5: {
                setErrorInfo((AmqpMap) value);
                break;
            }
            default : {
                throw new IndexOutOfBoundsException(String.valueOf(index));
            }
            }
        }

        public AmqpType<?, ?> get(int index) {
            switch(index) {
            case 0: {
                if(errorCode == null) {
                    return null;
                }
                return errorCode.getValue();
            }
            case 1: {
                return bean.commandId;
            }
            case 2: {
                return bean.commandCode;
            }
            case 3: {
                return bean.fieldIndex;
            }
            case 4: {
                return bean.description;
            }
            case 5: {
                return bean.errorInfo;
            }
            default : {
                throw new IndexOutOfBoundsException(String.valueOf(index));
            }
            }
        }

        public int getListCount() {
            return 6;
        }

        public IAmqpList<AmqpType<?, ?>> getValue() {
            return bean;
        }

        public Iterator<AmqpType<?, ?>> iterator() {
            return new AmqpListIterator<AmqpType<?, ?>>(bean);
        }


        private final void copyCheck() {
            if(buffer != null) {;
                throw new IllegalStateException("unwriteable");
            }
            if(bean != this) {;
                copy(bean);
            }
        }

        private final void copy(AmqpSessionError.AmqpSessionErrorBean other) {
            bean = this;
        }

        public boolean equals(Object o){
            if(this == o) {
                return true;
            }

            if(o == null || !(o instanceof AmqpSessionError)) {
                return false;
            }

            return equals((AmqpSessionError) o);
        }

        public boolean equals(AmqpSessionError b) {

            if(b.getErrorCode() == null ^ getErrorCode() == null) {
                return false;
            }
            if(b.getErrorCode() != null && !b.getErrorCode().equals(getErrorCode())){ 
                return false;
            }

            if(b.getCommandId() == null ^ getCommandId() == null) {
                return false;
            }
            if(b.getCommandId() != null && !b.getCommandId().equals(getCommandId())){ 
                return false;
            }

            if(b.getCommandCode() == null ^ getCommandCode() == null) {
                return false;
            }
            if(b.getCommandCode() != null && !b.getCommandCode().equals(getCommandCode())){ 
                return false;
            }

            if(b.getFieldIndex() == null ^ getFieldIndex() == null) {
                return false;
            }
            if(b.getFieldIndex() != null && !b.getFieldIndex().equals(getFieldIndex())){ 
                return false;
            }

            if(b.getDescription() == null ^ getDescription() == null) {
                return false;
            }
            if(b.getDescription() != null && !b.getDescription().equals(getDescription())){ 
                return false;
            }

            if(b.getErrorInfo() == null ^ getErrorInfo() == null) {
                return false;
            }
            if(b.getErrorInfo() != null && !b.getErrorInfo().equals(getErrorInfo())){ 
                return false;
            }
            return true;
        }

        public int hashCode() {
            return AbstractAmqpList.hashCodeFor(this);
        }
    }

    public static class AmqpSessionErrorBuffer extends AmqpList.AmqpListBuffer implements AmqpSessionError{

        private AmqpSessionErrorBean bean;

        protected AmqpSessionErrorBuffer(Encoded<IAmqpList<AmqpType<?, ?>>> encoded) {
            super(encoded);
        }

        public final void setErrorCode(AmqpSessionErrorCode errorCode) {
            bean().setErrorCode(errorCode);
        }

        public final AmqpSessionErrorCode getErrorCode() {
            return bean().getErrorCode();
        }

        public void setCommandId(Long commandId) {
            bean().setCommandId(commandId);
        }

        public void setCommandId(long commandId) {
            bean().setCommandId(commandId);
        }


        public final void setCommandId(AmqpSequenceNo commandId) {
            bean().setCommandId(commandId);
        }

        public final AmqpSequenceNo getCommandId() {
            return bean().getCommandId();
        }

        public void setCommandCode(Short commandCode) {
            bean().setCommandCode(commandCode);
        }

        public void setCommandCode(short commandCode) {
            bean().setCommandCode(commandCode);
        }


        public final void setCommandCode(AmqpUbyte commandCode) {
            bean().setCommandCode(commandCode);
        }

        public final Short getCommandCode() {
            return bean().getCommandCode();
        }

        public void setFieldIndex(Short fieldIndex) {
            bean().setFieldIndex(fieldIndex);
        }

        public void setFieldIndex(short fieldIndex) {
            bean().setFieldIndex(fieldIndex);
        }


        public final void setFieldIndex(AmqpUbyte fieldIndex) {
            bean().setFieldIndex(fieldIndex);
        }

        public final Short getFieldIndex() {
            return bean().getFieldIndex();
        }

        public void setDescription(String description) {
            bean().setDescription(description);
        }

        public final void setDescription(AmqpString description) {
            bean().setDescription(description);
        }

        public final String getDescription() {
            return bean().getDescription();
        }

        public final void setErrorInfo(AmqpMap errorInfo) {
            bean().setErrorInfo(errorInfo);
        }

        public final IAmqpMap<AmqpType<?, ?>, AmqpType<?, ?>> getErrorInfo() {
            return bean().getErrorInfo();
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

        public AmqpSessionError.AmqpSessionErrorBuffer getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{
            return this;
        }

        protected AmqpSessionError bean() {
            if(bean == null) {
                bean = new AmqpSessionError.AmqpSessionErrorBean(encoded.getValue());
                bean.buffer = this;
            }
            return bean;
        }

        public boolean equals(Object o){
            return bean().equals(o);
        }

        public boolean equals(AmqpSessionError o){
            return bean().equals(o);
        }

        public int hashCode() {
            return bean().hashCode();
        }

        public static AmqpSessionError.AmqpSessionErrorBuffer create(Encoded<IAmqpList<AmqpType<?, ?>>> encoded) {
            if(encoded.isNull()) {
                return null;
            }
            return new AmqpSessionError.AmqpSessionErrorBuffer(encoded);
        }

        public static AmqpSessionError.AmqpSessionErrorBuffer create(DataInput in, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError {
            return create(marshaller.unmarshalAmqpSessionError(in));
        }

        public static AmqpSessionError.AmqpSessionErrorBuffer create(Buffer buffer, int offset, AmqpMarshaller marshaller) throws AmqpEncodingError {
            return create(marshaller.decodeAmqpSessionError(buffer, offset));
        }
    }
}
