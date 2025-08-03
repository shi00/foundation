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

package com.silong.llm.chatbot.configure.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * 文件上传相关配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:24
 */
@Data
@Validated
@ConfigurationProperties(prefix = "upload.file")
public class UploadFileProperties {

  /** 文件上传obs的桶名 */
  @NotEmpty private String obsBucket = "data-store";

  /** Configure the maximum amount of disk space allowed for file parts. default: 10MB */
  @NotNull private DataSize maxDiskUsagePerPart = DataSize.ofMegabytes(10);

  /** Specify the maximum number of parts allowed in a given multipart request. default: 1000 */
  @Min(1000)
  @Max(2000)
  private int maxParts = 1000;

  /** 临时文件保存目录，默认：System.getProperty("java.io.tmpdir") */
  private String fileStorageDirectory = System.getProperty("java.io.tmpdir");

  /** Configure the maximum amount of memory allowed per part. default: 10MB */
  @NotNull private DataSize maxInMemorySize = DataSize.ofMegabytes(10);
}
