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

import static com.silong.foundation.rocksdbffm.Utils.OS_ARCH;
import static com.silong.foundation.rocksdbffm.Utils.OS_NAME;
import static java.lang.foreign.MemoryLayout.sequenceLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.util.Map;

/**
 * c++ std::function占位符
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-26 9:59
 */
class StdFunctionPlaceHolder implements PlaceHolder {
  /** 不同平台针对std::function类型的字节占用差异 */
  private static final Map<String, Integer> STD_FUNCTION_SIZEOF_PLATFORM =
      Map.of("windows:x86_64", 64, "linux:x86_64", 32);

  @Override
  public MemoryLayout layout() {
    return sequenceLayout(size(), ValueLayout.JAVA_BYTE);
  }

  @Override
  public int size() {
    return STD_FUNCTION_SIZEOF_PLATFORM.get(String.format("%s:%s", OS_NAME, OS_ARCH));
  }

  @Override
  public String toString() {
    return String.format("(StdFunctionPlaceHolder:%d)", size());
  }
}
