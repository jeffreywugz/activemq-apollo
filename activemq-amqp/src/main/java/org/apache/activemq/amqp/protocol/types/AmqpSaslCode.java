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

import java.lang.Short;
import java.util.HashMap;
import org.apache.activemq.amqp.protocol.marshaller.AmqpEncodingError;
import org.apache.activemq.amqp.protocol.types.AmqpUbyte;

/**
 * Represents a codes to indicate the outcome of the sasl dialog
 */
public enum AmqpSaslCode {

    /**
     * <p>
     * Connection authentication succeeded.
     * </p>
     */
    OK(new Short("0")),
    /**
     * <p>
     * Connection authentication failed due to an unspecified problem with the supplied
     * credentials.
     * </p>
     */
    AUTH(new Short("1")),
    /**
     * <p>
     * Connection authentication failed due to a system error.
     * </p>
     */
    SYS(new Short("2")),
    /**
     * <p>
     * Connection authentication failed due to a system error that is unlikely to be corrected
     * without intervention.
     * </p>
     */
    SYS_PERM(new Short("3")),
    /**
     * <p>
     * Connection authentication failed due to a transient system error.
     * </p>
     */
    SYS_TEMP(new Short("4"));

    private static final HashMap<Short, AmqpSaslCode> LOOKUP = new HashMap<Short, AmqpSaslCode>(2);
    static {
        for (AmqpSaslCode saslCode : AmqpSaslCode.values()) {
            LOOKUP.put(saslCode.value.getValue(), saslCode);
        }
    }

    private final AmqpUbyte value;

    private AmqpSaslCode(Short value) {
        this.value = new AmqpUbyte.AmqpUbyteBean(value);
    }

    public final AmqpUbyte getValue() {
        return value;
    }

    public static final AmqpSaslCode get(AmqpUbyte value) throws AmqpEncodingError{
        AmqpSaslCode saslCode= LOOKUP.get(value.getValue());
        if (saslCode == null) {
            //TODO perhaps this should be an IllegalArgumentException?
            throw new AmqpEncodingError("Unknown saslCode: " + value + " expected one of " + LOOKUP.keySet());
        }
        return saslCode;
    }
}