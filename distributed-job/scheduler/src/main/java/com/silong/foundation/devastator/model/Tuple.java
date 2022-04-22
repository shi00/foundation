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
 * 二元组
 *
 * @param <T1> 类型
 * @param <T2> 类型
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-18 22:48
 */
public record Tuple<T1, T2> (T1 t1, T2 t2) implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
}
