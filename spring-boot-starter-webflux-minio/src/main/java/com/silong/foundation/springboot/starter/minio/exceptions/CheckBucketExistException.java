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

package com.silong.foundation.springboot.starter.minio.exceptions;

import lombok.Getter;
import lombok.NonNull;

/**
 * 查询桶存在异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 09:20
 */
@Getter
public class CheckBucketExistException extends RuntimeException {

  @java.io.Serial private static final long serialVersionUID = -1069532319816030970L;

  private final String bucket;

  /**
   * 构造方法
   *
   * @param bucket 桶
   * @param cause 异常
   */
  public CheckBucketExistException(@NonNull String bucket, @NonNull Throwable cause) {
    super(cause);
    this.bucket = bucket;
  }
}
