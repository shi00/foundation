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

import lombok.Getter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * mysql插件抽象类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 15:11
 */
public abstract class AbstractMysqlMojo extends AbstractMojo {

  /** 上下文key */
  public static final String MYSQL_CONTEXT_KEY = "mysql-container";

  /** mysql镜像 */
  @Getter
  @Parameter(property = "mysql.image", required = true)
  protected String image;

  /** 数据库名 */
  @Parameter(property = "mysql.database", required = true)
  protected String database;

  /** 是否忽略执行 */
  @Parameter(property = "mysql.skip", defaultValue = "false")
  protected boolean skip;

  /** 用户名 */
  @Getter
  @Parameter(property = "mysql.user", required = true)
  protected String userName;

  /** 密码 */
  @Getter
  @Parameter(property = "mysql.password", required = true)
  protected String password;

  /** jdbcUrl */
  @Parameter(property = "mysql.jdbc-url", required = true)
  protected String jdbcUrl;

  /** jdbc驱动 */
  @Getter
  @Parameter(property = "mysql.jdbc-driver", required = true)
  protected String jdbcDriver;

  /** @since 1.2 */
  @Parameter(readonly = true, defaultValue = "${project}")
  protected MavenProject project;
}
