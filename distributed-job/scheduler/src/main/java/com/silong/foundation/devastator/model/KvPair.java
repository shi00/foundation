/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * key<--->value对
 *
 * @param <K> key type
 * @param <V> value type
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-18 22:48
 */
public record KvPair <K, V> (K key, V value) implements Serializable {

    @Serial
    private static final long serialVersionUID = -263188620675495564L;

    /**
     * key or value is null
     *
     * @return true or false
     */
    public boolean isKeyOrValueNull() {
        return key == null || value == null;
    }
}
