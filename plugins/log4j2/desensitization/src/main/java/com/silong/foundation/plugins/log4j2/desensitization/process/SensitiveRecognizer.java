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
package com.silong.foundation.plugins.log4j2.desensitization.process;

/**
 * 敏感信息识别接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-28 14:28
 */
public interface SensitiveRecognizer {
  /**
   * 对文本中包含的敏感信息进行替换
   *
   * @param text 文本
   * @return 敏感信息替换后的文本
   */
  String replace(String text);

  /**
   * 获取用于替换敏感信息的掩码
   *
   * @return 掩码
   */
  default String getMasker() {
    return "******";
  }
}
