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
package com.silong.foundation.plugins.maven.mysql;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.testcontainers.containers.MySQLContainer;

/**
 * 容器停止
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 15:17
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.NONE)
public class StopMysqlMojo extends AbstractMysqlMojo {

  @Override
  public void execute() {
    if (skip) {
      getLog().info("Etcd server had been skipped...");
      return;
    }
    MySQLContainer mySqlContainer = (MySQLContainer) getPluginContext().get(MYSQL_CONTEXT_KEY);
    mySqlContainer.stop();
    getLog().info("Stopping mysql...");
  }
}
