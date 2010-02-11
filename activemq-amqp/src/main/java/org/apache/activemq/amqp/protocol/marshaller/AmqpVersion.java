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
package org.apache.activemq.amqp.protocol.marshaller;

import org.apache.activemq.amqp.protocol.BitUtils;

public class AmqpVersion {

    private static final short PROTOCOL_ID = 0;
    public static final byte[] MAGIC = new byte[] { 'A', 'M', 'Q', 'P', PROTOCOL_ID };

    private final short major;
    private final short minor;
    private final short revision;

    private final int hashCode;
    
    public AmqpVersion(short major, short minor, short revision) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.hashCode = BitUtils.getInt(new byte[] { PROTOCOL_ID, (byte) (major & 0xFF), (byte) (minor & 0xFF), (byte) (revision & 0xFF) }, 0);
    }

    public short getProtocolId() {
        return PROTOCOL_ID;
    }

    public short getMajor() {
        return major;
    }

    public short getMinor() {
        return minor;
    }

    public short getRevision() {
        return revision;
    }
    
    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object o) {
        if (o.hashCode() != hashCode) {
            return false;
        } else {
            return o instanceof AmqpVersion;
        }
    }

    public boolean equals(AmqpVersion version) {
        return version.hashCode == hashCode;
    }
}
