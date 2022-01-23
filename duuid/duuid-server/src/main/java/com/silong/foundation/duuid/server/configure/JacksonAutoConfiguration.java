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
package com.silong.foundation.duuid.server.configure;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jackson定制自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 10:48
 */
@Configuration
public class JacksonAutoConfiguration {

  /**
   * 加载性能优化模块<br>
   * <pre>
   * For serialization (POJOs to JSON):
   *    Accessors for "getting" values (field access, calling getter method) are inlined using generated code instead of reflection
   *    Serializers for small number of 'primitive' types (int, long, String) are replaced with direct calls, instead of getting delegated to JsonSerializers
   *
   * For deserialization (JSON to POJOs)
   *    Calls to default (no-argument) constructors are byte-generated instead of using reflection
   *    Mutators for "setting" values (field access, calling setter method) are inlined using generated code instead of reflection
   *    Deserializers for small number of 'primitive' types (int, long, String) are replaced with direct calls, instead of getting delegated to JsonDeserializers
   * <pre>
   * @return afterburner
   */
  @Bean
  com.fasterxml.jackson.databind.Module afterburnerModule() {
    return new AfterburnerModule();
  }
}
