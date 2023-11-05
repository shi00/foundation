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
package com.silong.foundation.common.exception;

import com.silong.foundation.common.model.ErrorDetail;
import lombok.Getter;

/**
 * 服务异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-12 22:27
 */
public class ServiceException extends RuntimeException {
  /** 错误信息 */
  @Getter private final ErrorDetail errorDetail;

  /**
   * 构造方法
   *
   * @param errorDetail 错误详情
   */
  public ServiceException(ErrorDetail errorDetail) {
    super(errorDetail.toString());
    this.errorDetail = errorDetail;
  }
}
