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

package com.silong.foundation.dj.bonecrusher.enu;

/**
 * 服务器状态
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-02 9:23
 */
public enum ServerState {
  /** 新创建 */
  NEW,

  /** 初始化完毕 */
  INITIALIZED,

  /** 运行中 */
  RUNNING,

  /** 异常状态 */
  ABNORMAL,

  /** 已关闭 */
  SHUTDOWN
}
