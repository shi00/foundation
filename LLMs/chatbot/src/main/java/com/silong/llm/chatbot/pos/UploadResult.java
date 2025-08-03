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

package com.silong.llm.chatbot.pos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File upload result")
public class UploadResult {

  /** 桶名 */
  @Schema(description = "The bucket name for uploading files to OSS.", example = "oss-bucket")
  private String bucket;

  /** 上传成功的对象名列表 */
  @Schema(description = "List of successfully uploaded file names.")
  private List<String> files;
}
