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
package com.silong.foundation.devastator.core;

import com.silong.foundation.devastator.model.Devastator.HardwareInfo;
import com.silong.foundation.devastator.utils.TypeConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * 序列化测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-23 19:22
 */
public class ProtobufTests {

  @Test
  @DisplayName("protobuf-HardwareInfo")
  void test1() throws IOException {
    TypeConverter<HardwareInfo, byte[]> converter =
        TypeConverter.getProtobufTypeConverter(HardwareInfo.class);
    HardwareInfo hardwareInfo =
        HardwareInfo.newBuilder().setTotalMemory(1).setAvailableProcessors(1).build();
    byte[] to = converter.to(hardwareInfo);
    Assertions.assertEquals(hardwareInfo, converter.from(to));
  }
}
