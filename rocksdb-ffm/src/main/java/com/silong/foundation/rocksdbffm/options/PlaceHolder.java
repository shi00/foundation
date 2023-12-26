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

package com.silong.foundation.rocksdbffm.options;

import java.lang.foreign.MemoryLayout;

/**
 * 占位符，对于c++中存在的数据结构，在java侧没有对应的MemoryLayout表示，使用占位符进行简单表示，无法支持相应值的读写
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-26 10:14
 */
public sealed interface PlaceHolder permits StdFunctionPlaceHolder {

  /**
   * 内存布局
   *
   * @return 内存布局
   */
  MemoryLayout layout();

  /**
   * 占位符占用字节数
   *
   * @return 字节数
   */
  int size();
}
