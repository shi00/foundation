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
package com.silong.foundation.devastator.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 集群节点附加信息keys
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-11 22:35
 */
public interface ExtendedUUIDKeys {

  /** 主机名 */
  byte[] HOSTNAME_KEY = "hostname".getBytes(UTF_8);
}
