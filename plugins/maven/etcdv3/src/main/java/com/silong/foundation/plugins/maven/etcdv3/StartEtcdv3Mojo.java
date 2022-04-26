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
package com.silong.foundation.plugins.maven.etcdv3;

import io.etcd.jetcd.launcher.EtcdContainer;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.testcontainers.shaded.org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * 容器启动
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 15:15
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.NONE)
public class StartEtcdv3Mojo extends AbstractEtcdv3Mojo {

  @Override
  public void execute() {
    if (skip) {
      getLog().info("Skipping Etcd server...");
      return;
    }
    Properties properties = null;
    if (project != null) {
      properties = project.getProperties();
    }
    EtcdContainer etcdContainer =
        new EtcdContainer(image, isEmpty(nodeName) ? randomAlphabetic(32) : nodeName, emptyList());
    etcdContainer.start();
    Map pluginContext = getPluginContext();
    pluginContext.put(CONTAINER_CONTEXT_KEY, etcdContainer);
    String endpointStr = etcdContainer.clientEndpoint().toString();
    pluginContext.put(ENDPOINT_CONTEXT_KEY, endpointStr);
    if (properties != null) {
      properties.setProperty(endpoint, endpointStr);
      getLog().info(String.format("${%s}=%s", endpoint, endpointStr));
    }
    getLog().info("Start single node etcdv3 successfully.");
  }
}
