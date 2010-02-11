/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with his work
 * for additional information regarding copyright ownership. The ASF licenses
 * this file to You under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.activemq.amqp.protocol.marshaller.v1_0_0;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

import org.apache.activemq.amqp.protocol.marshaller.v1_0_0.AmqpStringMarshaller.STRING_ENCODING;
import org.apache.activemq.amqp.protocol.marshaller.v1_0_0.AmqpSymbolMarshaller.SYMBOL_ENCODING;
import org.apache.activemq.amqp.protocol.BitUtils;
import org.apache.activemq.amqp.protocol.marshaller.AmqpEncodingError;
import org.apache.activemq.util.buffer.Buffer;

class BaseEncoder implements PrimitiveEncoder {
    public final Buffer decodeBinary(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return new Buffer(encoded.data, encoded.offset + offset, length);
    }

    public final Buffer decodeBinaryVbin32(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeBinary(encoded, encoded.offset + offset, length);
    }

    public final Buffer decodeBinaryVbin8(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeBinary(encoded, encoded.offset + offset, length);
    }

    public final Byte decodeByte(Buffer encoded, int offset) throws AmqpEncodingError {
        return encoded.get(encoded.offset + offset);
    }

    public final Integer decodeChar(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getInt(encoded.data, encoded.offset + offset);
    }

    public final Double decodeDouble(Buffer encoded, int offset) throws AmqpEncodingError {
        return Double.longBitsToDouble(decodeLong(encoded, encoded.offset + offset));
    }

    public final Float decodeFloat(Buffer encoded, int offset) throws AmqpEncodingError {
        return Float.intBitsToFloat(decodeInt(encoded, encoded.offset + offset));
    }

    public final Integer decodeInt(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getInt(encoded.data, encoded.offset + offset);
    }

    public final Long decodeLong(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getLong(encoded.data, encoded.offset + offset);
    }

    public final Short decodeShort(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getShort(encoded.data, encoded.offset + offset);
    }

    public final String decodeString(Buffer encoded, int offset, int length, String charset) throws AmqpEncodingError {
        try {
            return new String(encoded.data, encoded.offset + offset, length, charset);
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
    }

    public final String decodeStringStr32Utf16(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "utf-16");
    }

    public final String decodeStringStr32Utf8(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "utf-8");
    }

    public final String decodeStringStr8Utf16(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "utf-16");
    }

    public final String decodeStringStr8Utf8(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "utf-8");
    }

    public final String decodeSymbolSym32(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "us-ascii");
    }

    public final String decodeSymbolSym8(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "us-ascii");
    }

    public final Date decodeTimestamp(Buffer encoded, int offset) throws AmqpEncodingError {
        return new Date(decodeInt(encoded, encoded.offset + offset));
    }

    public final Short decodeUbyte(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getUByte(encoded.data, encoded.offset + offset);
    }

    public Long decodeUint(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getUInt(encoded.data, encoded.offset + offset);
    }

    public BigInteger decodeUlong(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getULong(encoded.data, encoded.offset + offset);
    }

    public Integer decodeUshort(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getUShort(encoded.data, encoded.offset + offset);
    }

    public UUID decodeUuid(Buffer encoded, int offset) throws AmqpEncodingError {
        return new UUID(decodeLong(encoded, offset), decodeLong(encoded, offset + 8));
    }

    public void encodeBinaryVbin32(Buffer val, Buffer buf, int offset) throws AmqpEncodingError {
        System.arraycopy(val, val.offset, buf.data, buf.offset + offset, val.length);

    }

    public void encodeBinaryVbin8(Buffer val, Buffer buf, int offset) throws AmqpEncodingError {
        System.arraycopy(val, val.offset, buf.data, buf.offset + offset, val.length);
    }

    public void encodeByte(Byte val, Buffer buf, int offset) throws AmqpEncodingError {
        buf.data[buf.offset + offset] = val;

    }

    public void encodeChar(Integer val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setUInt(buf.data, buf.offset + offset, val);

    }

    public void encodeDouble(Double val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setLong(buf.data, buf.offset + offset, Double.doubleToLongBits(val));

    }

    public void encodeFloat(Float val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setInt(buf.data, buf.offset + offset, Float.floatToIntBits(val));

    }

    public void encodeInt(Integer val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setInt(buf.data, buf.offset + offset, val);
    }

    public void encodeLong(Long val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setLong(buf.data, buf.offset + offset, val);
    }

    public void encodeShort(Short val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setShort(buf.data, buf.offset + offset, val);
    }

    public void encodeStringStr32Utf16(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("utf-16");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);

    }

    public void encodeStringStr32Utf8(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);
    }

    public void encodeStringStr8Utf16(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("utf-16");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);

    }

    public void encodeStringStr8Utf8(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);

    }

    public void encodeSymbolSym32(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("us-ascii");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);

    }

    public void encodeSymbolSym8(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("us-ascii");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);
    }

    public void encodeTimestamp(Date val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setLong(buf.data, buf.offset + offset, val.getTime());
    }

    public void encodeUbyte(Short val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setUByte(buf.data, buf.offset + offset, val);
    }

    public void encodeUint(Long val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setUInt(buf.data, buf.offset + offset, val);
    }

    public void encodeUlong(BigInteger val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setULong(buf.data, buf.offset + offset, val);
    }

    public void encodeUshort(Integer val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setUShort(buf.data, buf.offset + offset, val);
    }

    public void encodeUuid(UUID val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setLong(buf.data, buf.offset + offset, val.getMostSignificantBits());
        BitUtils.setLong(buf.data, buf.offset + offset + 8, val.getLeastSignificantBits());
    }

    public final int getEncodedSizeOfString(String val, STRING_ENCODING encoding) throws AmqpEncodingError {
        try {
            switch (encoding) {
            case STR32_UTF16:
            case STR8_UTF16: {
                return val.getBytes("utf-16").length;
            }
            case STR32_UTF8:
            case STR8_UTF8: {
                return val.getBytes("utf-8").length;
            }
            default:
                throw new UnsupportedEncodingException(encoding.name());
            }
        } catch (UnsupportedEncodingException uee) {
            throw new AmqpEncodingError(uee.getMessage(), uee);
        }
    }

    public final int getEncodedSizeOfSymbol(String val, SYMBOL_ENCODING encoding) {
        return val.length();
    }

    public final byte[] readBinary(AmqpBinaryMarshaller.BINARY_ENCODING encoding, int length, int count, DataInput dis) throws IOException {
        byte[] rc = new byte[(int) length];
        dis.readFully(rc);
        return rc;
    }

    public Buffer readBinaryVbin32(int size, DataInput dis) throws IOException, AmqpEncodingError {
        return readBinaryVbin32(size, dis);
    }

    public Buffer readBinaryVbin8(int size, DataInput dis) throws IOException, AmqpEncodingError {
        return readBinaryVbin32(size, dis);
    }

    public final Byte readByte(DataInput dis) throws IOException {
        return (byte) dis.readByte();
    }

    public final Integer readChar(DataInput dis) throws IOException {
        return dis.readInt();
    }

    public final Double readDouble(DataInput dis) throws IOException {
        return dis.readDouble();
    }

    public final Float readFloat(DataInput dis) throws IOException {
        return dis.readFloat();
    }

    public final Integer readInt(DataInput dis) throws IOException {
        return dis.readInt();
    }

    public final Long readLong(DataInput dis) throws IOException {
        return dis.readLong();
    }

    public final Short readShort(DataInput dis) throws IOException {
        return dis.readShort();
    }

    public final String readString(AmqpStringMarshaller.STRING_ENCODING encoding, int size, int count, DataInput dis) throws IOException {
        byte[] str = new byte[size];
        dis.readFully(str);
        switch (encoding) {
        case STR32_UTF16:
        case STR8_UTF16:
            return new String(str, "utf-16");
        case STR32_UTF8:
        case STR8_UTF8:
            return new String(str, "utf-8");
        default:
            throw new UnsupportedEncodingException(encoding.name());
        }
    }

    public String readStringStr32Utf16(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "utf-16");
    }

    public String readStringStr32Utf8(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "utf-8");
    }

    public String readStringStr8Utf16(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "utf-16");
    }

    public String readStringStr8Utf8(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "utf-8");
    }

    public final String readSymbol(AmqpSymbolMarshaller.SYMBOL_ENCODING encoding, int size, int count, DataInput dis) throws IOException {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "us-ascii");
    }

    public String readSymbolSym32(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "us-ascii");
    }

    public String readSymbolSym8(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "us-ascii");
    }

    public final Date readTimestamp(DataInput dis) throws IOException {
        return new Date(dis.readLong());
    }

    public final Short readUbyte(DataInput dis) throws IOException {
        return (short) (0xFF & (short) dis.readByte());
    }

    public final Long readUint(DataInput dis) throws IOException {
        long rc = 0;
        rc = rc | (0xFFFFFFFFL & (((long) dis.readByte()) << 24));
        rc = rc | (0xFFFFFFFFL & (((long) dis.readByte()) << 16));
        rc = rc | (0xFFFFFFFFL & (((long) dis.readByte()) << 8));
        rc = rc | (0xFFFFFFFFL & (long) dis.readByte());

        return rc;
    }

    public final BigInteger readUlong(DataInput dis) throws IOException {
        byte[] rc = new byte[9];
        rc[0] = 0;
        dis.readFully(rc, 1, 8);
        return new BigInteger(rc);
    }

    public final Integer readUshort(DataInput dis) throws IOException {
        int rc = 0;
        rc = rc | ((int) 0xFFFF & (((int) dis.readByte()) << 8));
        rc = rc | ((int) 0xFFFF & (int) dis.readByte());

        return rc;
    }

    public final UUID readUuid(DataInput dis) throws IOException {
        return new UUID(dis.readLong(), dis.readLong());
    }

    public final void writeBinary(byte[] val, AmqpBinaryMarshaller.BINARY_ENCODING encoding, DataOutput dos) throws IOException {
        dos.write(val);
    }

    public void writeBinaryVbin32(Buffer val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.data, val.offset, val.length);
    }

    public void writeBinaryVbin8(Buffer val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.data, val.offset, val.length);

    }

    public final void writeByte(Byte val, DataOutput dos) throws IOException {
        dos.writeByte(val);
    }

    public final void writeChar(Integer val, DataOutput dos) throws IOException {
        dos.writeInt(val);

    }

    public final void writeDouble(Double val, DataOutput dos) throws IOException {
        dos.writeLong(Double.doubleToLongBits(val));
    }

    public final void writeFloat(Float val, DataOutput dos) throws IOException {
        dos.writeInt(Float.floatToIntBits(val));
    }

    public final void writeInt(Integer val, DataOutput dos) throws IOException {
        dos.writeInt(val);
    }

    public final void writeLong(Long val, DataOutput dos) throws IOException {
        dos.writeLong(val);
    }

    public final void writeShort(Short val, DataOutput dos) throws IOException {
        dos.writeShort(val);
    }

    public final void writeString(String val, AmqpStringMarshaller.STRING_ENCODING encoding, DataOutput dos) throws IOException {
        switch (encoding) {
        case STR32_UTF16:
        case STR8_UTF16: {
            dos.write(val.getBytes("utf-16"));
        }
        case STR32_UTF8:
        case STR8_UTF8: {
            dos.write(val.getBytes("utf-8"));
        }
        default:
            throw new UnsupportedEncodingException(encoding.name());
        }

    }

    public void writeStringStr32Utf16(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("utf-16"));

    }

    public void writeStringStr32Utf8(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("utf-8"));

    }

    public void writeStringStr8Utf16(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("utf-16"));

    }

    public void writeStringStr8Utf8(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("utf-8"));
    }

    public final void writeSymbol(String val, SYMBOL_ENCODING encoding, DataOutput dos) throws IOException {
        dos.write(val.getBytes("us-ascii"));
    }

    public void writeSymbolSym32(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("us-ascii"));
    }

    public void writeSymbolSym8(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("us-ascii"));
    }

    public final void writeTimestamp(Date val, DataOutput dos) throws IOException {
        dos.writeLong(val.getTime());
    }

    public final void writeUbyte(Short val, DataOutput dos) throws IOException {
        dos.write(val);
    }

    public final void writeUint(Long val, DataOutput dos) throws IOException {
        dos.writeInt((int) val.longValue());
    }

    public final void writeUlong(BigInteger val, DataOutput dos) throws IOException {
        byte[] b = val.toByteArray();
        if (b.length > 8) {
            for (int i = 0; i < b.length - 8; i++) {
                if (b[i] > 0) {
                    throw new UnsupportedEncodingException("Unsigned long too large");
                }
            }
        }
        dos.write(b, b.length - 8, 8);
    }

    public final void writeUshort(Integer val, DataOutput dos) throws IOException {
        dos.writeShort((short) val.intValue());
    }

    public final void writeUuid(UUID val, DataOutput dos) throws IOException {
        dos.writeLong(val.getMostSignificantBits());
        dos.writeLong(val.getLeastSignificantBits());
    }
}
