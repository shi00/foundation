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

package com.silong.llm.chatbot.configure;

import com.silong.llm.chatbot.configure.properties.UploadFileProperties;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;

/**
 * 文件上传自动配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:21
 */
@Configuration
@EnableConfigurationProperties(UploadFileProperties.class)
public class WebfluxUploadAutoConfiguration {

  @Bean
  public MultipartHttpMessageReader multipartHttpMessageReader(
      UploadFileProperties uploadFileProperties) throws IOException {

    DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();

    // 设置最大单个文件大小为 10MB
    partReader.setMaxDiskUsagePerPart(uploadFileProperties.getMaxDiskUsagePerPart().toBytes());

    // 设置最大请求大小为 100MB
    partReader.setMaxInMemorySize((int) uploadFileProperties.getMaxInMemorySize().toBytes());

    partReader.setMaxParts(uploadFileProperties.getMaxParts());

    partReader.setFileStorageDirectory(Path.of(uploadFileProperties.getFileStorageDirectory()));

    return new MultipartHttpMessageReader(partReader);
  }
}
