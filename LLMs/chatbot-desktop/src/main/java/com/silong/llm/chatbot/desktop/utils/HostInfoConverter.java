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

package com.silong.llm.chatbot.desktop.utils;

import javafx.util.StringConverter;
import lombok.NonNull;

/**
 * 对象转换工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
public class HostInfoConverter extends StringConverter<HostInfoConverter.HostInfo> {

  private static final HostInfoConverter INSTANCE = new HostInfoConverter();

  private HostInfoConverter() {}

  /**
   * 获取实例
   *
   * @return 实例
   */
  public static HostInfoConverter getInstance() {
    return INSTANCE;
  }

  @Override
  public String toString(HostInfo object) {
    return object == null ? null : String.format("%s:%d", object.host, object.port);
  }

  @Override
  public HostInfo fromString(String hostInfo) {
    if (hostInfo == null || hostInfo.isEmpty()) {
      return null;
    }

    String[] split = hostInfo.split(":", 2);
    String host = split[0].trim();
    int port = Integer.parseInt(split[1].trim());
    return new HostInfo(host, port);
  }

  public record HostInfo(@NonNull String host, @NonNull Integer port)
      implements Comparable<HostInfo> {
    @Override
    public int compareTo(@NonNull HostInfo o) {
      int compare = host.compareTo(o.host);
      return compare == 0 ? port.compareTo(o.port) : compare;
    }
  }
}
