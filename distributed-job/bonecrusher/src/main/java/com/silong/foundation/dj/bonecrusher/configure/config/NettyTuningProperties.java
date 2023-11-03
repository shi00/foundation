/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.dj.bonecrusher.configure.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.util.unit.DataSize;

/**
 * Bonecrusher Netty调优参数
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-04 0:12
 */
@Data
public class NettyTuningProperties {
  /** SO_REUSEADDR，默认：true */
  private boolean SO_REUSEADDR = true;

  /** SO_LINGER，默认：0 */
  private int SO_LINGER;

  /** SO_RCVBUF，默认：128K */
  @NotNull private DataSize SO_RCVBUF = DataSize.ofKilobytes(128);

  /** SO_SNDBUF，默认：128K */
  @NotNull private DataSize SO_SNDBUF = DataSize.ofKilobytes(128);

  /** PROTOCOL_RECEIVE_BUFFER_SIZE，默认：10M */
  @NotNull private DataSize PROTOCOL_RECEIVE_BUFFER_SIZE = DataSize.ofMegabytes(10);

  /** PROTOCOL_SEND_BUFFER_SIZE，默认：10M */
  @NotNull private DataSize PROTOCOL_SEND_BUFFER_SIZE = DataSize.ofMegabytes(10);

  /** SYSTEM_RECEIVE_BUFFER_SIZE，默认：1M */
  @NotNull private DataSize SYSTEM_RECEIVE_BUFFER_SIZE = DataSize.ofMegabytes(1);

  /** SYSTEM_SEND_BUFFER_SIZE，默认：1M */
  @NotNull private DataSize SYSTEM_SEND_BUFFER_SIZE = DataSize.ofMegabytes(1);
}
