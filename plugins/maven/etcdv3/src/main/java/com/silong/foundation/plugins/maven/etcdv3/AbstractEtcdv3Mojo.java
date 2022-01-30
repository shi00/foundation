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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * etcd插件抽象类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 15:11
 */
public abstract class AbstractEtcdv3Mojo extends AbstractMojo {

  /** 上下文key */
  public static final String CONTAINER_CONTEXT_KEY = "etcdv3-container";

  /** 上下文key */
  public static final String ENDPOINT_CONTEXT_KEY = "etcdv3-endpoint";

  /** etcd镜像 */
  @Parameter(property = "etcd.server.image", required = true)
  protected String image;

  /** etcd容器名 */
  @Parameter(property = "etcd.server.node")
  protected String nodeName;

  /** 是否忽略执行 */
  @Parameter(property = "etcd.server.skip", defaultValue = "false")
  protected boolean skip;

  /** 访问端点 */
  @Parameter(property = "etcd.server.endpoint", required = true)
  protected String endpoint;

  /** @since 1.2 */
  @Parameter(readonly = true, defaultValue = "${project}")
  protected MavenProject project;
}
