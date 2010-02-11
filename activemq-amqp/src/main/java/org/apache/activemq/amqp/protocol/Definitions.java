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
package org.apache.activemq.amqp.protocol;

public interface Definitions {

    /**
     * the IANA assigned port number for AMQP
     * <p>
     * The standard AMQP port number that has been assigned by IANA for TCP, UDP, and SCTP.
     * </p>
     * <p>
     * There is currently no UDP mapping defined for AMQP. The UDP port number is reserved for
     * future transport mappings.
     * </p>
     */
    public static final String PORT = "5672";

    /**
     * major protocol version
     */
    public static final String MAJOR = "1";

    /**
     * minor protocol version
     */
    public static final String MINOR = "0";

    /**
     * protocol revision
     */
    public static final String REVISION = "0";

    /**
     * the minimum size (in bytes) of the maximum frame size
     * <p>
     * During the initial Connection negotiation, the two peers must agree upon a maximum frame
     * size. This constant defines the minimum value to which the maximum frame size can be set.
     * By defining this value, the peers can guarantee that they can send frames of up to this
     * size until they have agreed a definitive maximum frame size for that Connection.
     * </p>
     */
    public static final String MIN_MAX_FRAME_SIZE = "4096";
}