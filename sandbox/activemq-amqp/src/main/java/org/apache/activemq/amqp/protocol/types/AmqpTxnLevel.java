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
 * Represents a transaction level
 */
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//
//!!!!!!!!THIS CLASS IS AUTOGENERATED DO NOT MODIFY DIRECTLY!!!!!!!!!!!!//
//!!!!!!Instead, modify the generator in activemq-amqp-generator!!!!!!!!//
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//
public enum AmqpTxnLevel {

    LOCAL(new Short("1")),
    DISTRIBUTED(new Short("2")),
    PROMOTABLE(new Short("3"));

    private static final HashMap<Short, AmqpTxnLevel> LOOKUP = new HashMap<Short, AmqpTxnLevel>(2);
    static {
        for (AmqpTxnLevel txnLevel : AmqpTxnLevel.values()) {
            LOOKUP.put(txnLevel.value.getValue(), txnLevel);
        }
    }

    private final AmqpUbyte value;

    private AmqpTxnLevel(Short value) {
        this.value = new AmqpUbyte.AmqpUbyteBean(value);
    }

    public final AmqpUbyte getValue() {
        return value;
    }

    public static final AmqpTxnLevel get(AmqpUbyte value) throws AmqpEncodingError{
        AmqpTxnLevel txnLevel= LOOKUP.get(value.getValue());
        if (txnLevel == null) {
            //TODO perhaps this should be an IllegalArgumentException?
            throw new AmqpEncodingError("Unknown txnLevel: " + value + " expected one of " + LOOKUP.keySet());
        }
        return txnLevel;
    }
}