/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.plugins.maven.wprops.tests;

import com.silong.foundation.plugins.maven.wprops.PropertiesFileWriterMojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 19:52
 */
public class PropertiesFileWriterMojoTest extends AbstractMojoTestCase {

  /** 测试pom */
  public static final String FORKED_POM_FILE = "src/test/resources/unit/pom.xml";

  private static final Map PLUGIN_CONTEXT = new ConcurrentHashMap();

  public void test() throws Exception {

    PropertiesFileWriterMojo mojo = (PropertiesFileWriterMojo) lookupMojo("write-properties-file", FORKED_POM_FILE);
    mojo.setPluginContext(PLUGIN_CONTEXT);
//    assertNotNull(mojo);
    mojo.execute();


  }
}
