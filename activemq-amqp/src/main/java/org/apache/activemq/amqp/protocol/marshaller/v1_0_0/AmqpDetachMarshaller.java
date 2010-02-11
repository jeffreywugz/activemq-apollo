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
package org.apache.activemq.amqp.protocol.marshaller.v1_0_0;

import java.io.DataInput;
import java.io.IOException;
import org.apache.activemq.amqp.protocol.marshaller.AmqpEncodingError;
import org.apache.activemq.amqp.protocol.marshaller.Encoded;
import org.apache.activemq.amqp.protocol.marshaller.UnexpectedTypeException;
import org.apache.activemq.amqp.protocol.marshaller.v1_0_0.Encoder;
import org.apache.activemq.amqp.protocol.marshaller.v1_0_0.Encoder.*;
import org.apache.activemq.amqp.protocol.types.AmqpBoolean;
import org.apache.activemq.amqp.protocol.types.AmqpDetach;
import org.apache.activemq.amqp.protocol.types.AmqpOptions;
import org.apache.activemq.amqp.protocol.types.AmqpSessionError;
import org.apache.activemq.amqp.protocol.types.AmqpSessionName;
import org.apache.activemq.amqp.protocol.types.AmqpSymbol;
import org.apache.activemq.amqp.protocol.types.AmqpType;
import org.apache.activemq.amqp.protocol.types.AmqpUlong;
import org.apache.activemq.amqp.protocol.types.IAmqpList;
import org.apache.activemq.util.buffer.Buffer;

public class AmqpDetachMarshaller implements DescribedTypeMarshaller<AmqpDetach>{

    static final AmqpDetachMarshaller SINGLETON = new AmqpDetachMarshaller();
    private static final Encoded<IAmqpList> NULL_ENCODED = new Encoder.NullEncoded<IAmqpList>();

    public static final String SYMBOLIC_ID = "amqp:detach:list";
    //Format code: 0x00000001:0x00000203:
    public static final long CATEGORY = 1;
    public static final long DESCRIPTOR_ID = 515;
    public static final long NUMERIC_ID = CATEGORY << 32 | DESCRIPTOR_ID; //(4294967811L)
    //Hard coded descriptor:
    public static final EncodedBuffer DESCRIPTOR = FormatCategory.createBuffer(new Buffer(new byte [] {
        (byte) 0x80,                                         // ulong descriptor encoding)
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,  // CATEGORY CODE
        (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x03   // DESCRIPTOR ID CODE
    }), 0);

    private static final ListDecoder DECODER = new ListDecoder() {
        public final AmqpType<?, ?> unmarshalType(int pos, DataInput in) throws IOException {
            switch(pos) {
            case 0: {
                return AmqpOptions.AmqpOptionsBuffer.create(AmqpMapMarshaller.createEncoded(in));
            }
            case 1: {
                return AmqpSessionName.AmqpSessionNameBuffer.create(AmqpBinaryMarshaller.createEncoded(in));
            }
            case 2: {
                return AmqpBoolean.AmqpBooleanBuffer.create(AmqpBooleanMarshaller.createEncoded(in));
            }
            case 3: {
                return AmqpSessionError.AmqpSessionErrorBuffer.create(AmqpSessionErrorMarshaller.createEncoded(in));
            }
            default: {
                return AmqpMarshaller.SINGLETON.unmarshalType(in);
            }
            }
        }

        public final AmqpType<?, ?> decodeType(int pos, EncodedBuffer buffer) throws AmqpEncodingError {
            switch(pos) {
            case 0: {
                return AmqpOptions.AmqpOptionsBuffer.create(AmqpMapMarshaller.createEncoded(buffer));
            }
            case 1: {
                return AmqpSessionName.AmqpSessionNameBuffer.create(AmqpBinaryMarshaller.createEncoded(buffer));
            }
            case 2: {
                return AmqpBoolean.AmqpBooleanBuffer.create(AmqpBooleanMarshaller.createEncoded(buffer));
            }
            case 3: {
                return AmqpSessionError.AmqpSessionErrorBuffer.create(AmqpSessionErrorMarshaller.createEncoded(buffer));
            }
            default: {
                return AmqpMarshaller.SINGLETON.decodeType(buffer);
            }
            }
        }
    };

    public static class AmqpDetachEncoded extends DescribedEncoded<IAmqpList> {

        public AmqpDetachEncoded(DescribedBuffer buffer) {
            super(buffer);
        }

        public AmqpDetachEncoded(AmqpDetach value) {
            super(AmqpListMarshaller.encode(value));
        }

        protected final String getSymbolicId() {
            return SYMBOLIC_ID;
        }

        protected final long getNumericId() {
            return NUMERIC_ID;
        }

        protected final Encoded<IAmqpList> decodeDescribed(EncodedBuffer encoded) throws AmqpEncodingError {
            return AmqpListMarshaller.createEncoded(encoded, DECODER);
        }

        protected final Encoded<IAmqpList> unmarshalDescribed(DataInput in) throws IOException {
            return AmqpListMarshaller.createEncoded(in, DECODER);
        }

        protected final EncodedBuffer getDescriptor() {
            return DESCRIPTOR;
        }
    }

    public static final Encoded<IAmqpList> encode(AmqpDetach value) throws AmqpEncodingError {
        return new AmqpDetachEncoded(value);
    }

    static final Encoded<IAmqpList> createEncoded(Buffer source, int offset) throws AmqpEncodingError {
        return createEncoded(FormatCategory.createBuffer(source, offset));
    }

    static final Encoded<IAmqpList> createEncoded(DataInput in) throws IOException, AmqpEncodingError {
        return createEncoded(FormatCategory.createBuffer(in.readByte(), in));
    }

    static final Encoded<IAmqpList> createEncoded(EncodedBuffer buffer) throws AmqpEncodingError {
        byte fc = buffer.getEncodingFormatCode();
        if (fc == Encoder.NULL_FORMAT_CODE) {
            return NULL_ENCODED;
        }

        DescribedBuffer db = buffer.asDescribed();
        AmqpType<?, ?> descriptor = AmqpMarshaller.SINGLETON.decodeType(db.getDescriptorBuffer());
        if(!(descriptor instanceof AmqpUlong && ((AmqpUlong)descriptor).getValue().longValue() == NUMERIC_ID ||
               descriptor instanceof AmqpSymbol && ((AmqpSymbol)descriptor).getValue().equals(SYMBOLIC_ID))) {
            throw new UnexpectedTypeException("descriptor mismatch: " + descriptor);
        }
        return new AmqpDetachEncoded(db);
    }

    public final AmqpDetach.AmqpDetachBuffer decodeDescribedType(AmqpType<?, ?> descriptor, DescribedBuffer encoded) throws AmqpEncodingError {
        return AmqpDetach.AmqpDetachBuffer.create(new AmqpDetachEncoded(encoded));
    }
}
