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

package com.silong.foundation.utilities.nlloader;

import static com.silong.foundation.utilities.nlloader.NativeLibLoader.*;

import com.silong.foundation.utilities.xxhash.XxHashGenerator;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-13 23:03
 */
public class JarUtilsTests {
  @Test
  void test1() throws Exception {
    Path path2 = JarUtils.byGetResource(StringUtils.class);
    Path path1 = JarUtils.byGetProtectionDomain(StringUtils.class);
    Assertions.assertEquals(path1, path2);
  }

  @Test
  void test2() throws Exception {
    Path path2 = JarUtils.locateJarFile(org.apache.commons.lang3.StringUtils.class);
    Path path1 = JarUtils.byGetProtectionDomain(org.apache.commons.lang3.StringUtils.class);
    Assertions.assertEquals(path1, path2);
  }

  @Test
  void test3() throws Exception {
    Path path2 = JarUtils.locateJarFile(org.apache.commons.lang3.StringUtils.class);
    Path path1 = JarUtils.byGetResource(org.apache.commons.lang3.StringUtils.class);
    Assertions.assertEquals(path1, path2);
  }

  @Test
  void test4() {
    Path path = JarUtils.locateJarFile(XxHashGenerator.class);
    String dir = UUID.randomUUID().toString();
    Collection<String> paths = JarUtils.extractNativeLibs(path, TEMP_DIR.resolve(dir));
    Assertions.assertEquals(1, paths.size());
    Assertions.assertTrue(new File(paths.stream().findFirst().orElseThrow()).exists());
  }
}
