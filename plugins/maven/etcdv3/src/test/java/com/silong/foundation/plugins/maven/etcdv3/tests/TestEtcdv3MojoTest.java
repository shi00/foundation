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
package com.silong.foundation.plugins.maven.etcdv3.tests;

import com.silong.foundation.plugins.maven.etcdv3.StartEtcdv3Mojo;
import com.silong.foundation.plugins.maven.etcdv3.StopEtcdv3Mojo;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.silong.foundation.plugins.maven.etcdv3.AbstractEtcdv3Mojo.ENDPOINT_CONTEXT_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 19:52
 */
public class TestEtcdv3MojoTest extends AbstractMojoTestCase {

  /** 测试pom */
  public static final String FORKED_POM_FILE = "src/test/resources/unit/pom.xml";

  /** 超时时间 */
  public static final Duration TIMEOUT = Duration.of(10, SECONDS);

  private static final ByteSequence KEY = ByteSequence.from("test/key".getBytes(UTF_8));

  private static final ByteSequence VALUE = ByteSequence.from("test/value".getBytes(UTF_8));

  private static final Map PLUGIN_CONTEXT = new ConcurrentHashMap();

  public void test() throws Exception {
    StartEtcdv3Mojo startEtcdv3Mojo = (StartEtcdv3Mojo) lookupMojo("start", FORKED_POM_FILE);
    startEtcdv3Mojo.setPluginContext(PLUGIN_CONTEXT);
    assertNotNull(startEtcdv3Mojo);
    startEtcdv3Mojo.execute();

    String endpoint = (String) startEtcdv3Mojo.getPluginContext().get(ENDPOINT_CONTEXT_KEY);

    try (Client client = getClient(endpoint)) {
      client.getKVClient().put(KEY, VALUE).get(TIMEOUT.getSeconds(), TimeUnit.SECONDS);
      ByteSequence value =
          client
              .getKVClient()
              .get(KEY)
              .get(TIMEOUT.getSeconds(), TimeUnit.SECONDS)
              .getKvs()
              .get(0)
              .getValue();
      assertEquals(VALUE, value);
    }

    StopEtcdv3Mojo stopEtcdv3Mojo = (StopEtcdv3Mojo) lookupMojo("stop", FORKED_POM_FILE);
    stopEtcdv3Mojo.setPluginContext(PLUGIN_CONTEXT);
    stopEtcdv3Mojo.execute();

    try (Client client = getClient(endpoint)) {
      client.getKVClient().get(KEY).get(TIMEOUT.getSeconds(), TimeUnit.SECONDS);
    } catch (Exception e) {
      System.out.println("Stopped etcdv3 completed.");
    }
  }

  @NonNull
  private Client getClient(String endpoint) {
    return Client.builder()
        .endpoints(endpoint)
        .connectTimeout(TIMEOUT)
        .keepaliveTimeout(TIMEOUT)
        .build();
  }
}
