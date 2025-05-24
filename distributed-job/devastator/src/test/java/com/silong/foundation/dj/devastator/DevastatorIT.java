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

package com.silong.foundation.dj.devastator;

import com.silong.foundation.crypto.RootKey;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import java.util.Base64;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

/**
 * 集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-24 9:45
 */
public class DevastatorIT {

  @Test
  public void test1() {
    RootKey.initialize();
    byte[] src = RandomUtils.nextBytes(32);
    String s = Base64.getEncoder().encodeToString(src);
    System.out.println(
        AesGcmToolkit.encrypt(
            s,
            "security:Y/GooShaUv5kOyBKWO6KLkiz0xcBhO4Jq7N9PK2nDpQ/Y3OBIKWowfcJzrYX8lUYiSKg2BiLtpLpR7ag"));
  }
}
