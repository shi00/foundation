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
package com.silong.foundation.ctask.utils;

import com.silong.foundation.ctask.xsd2java.ComplexTaskConfigList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 任务配置工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-03 08:18
 */
@SuppressFBWarnings(
    value = {"PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"},
    justification = "加载配置任务配置文件")
public class ComplexTaskListConfig {

  private final ComplexTaskConfigList complexTaskList;

  private ComplexTaskListConfig(ComplexTaskConfigList complexTaskList) {
    this.complexTaskList = complexTaskList;
  }

  private static URL toUrl(final String path) throws Exception {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path must not be null or empty.");
    }
    try {
      return new URL(path);
    } catch (MalformedURLException e) {
      URL url = ComplexTaskListConfig.class.getResource(path);
      if (url != null) {
        return url;
      }
      File file = new File(path);
      if (file.exists() && file.isFile()) {
        return file.toURI().toURL();
      }
    }
    throw new Exception("Failed to find file at " + path);
  }

  /**
   * 加载任务配置文件
   *
   * @param path 配置文件路径
   * @return 任务配置
   */
  public static ComplexTaskListConfig loadTasksConfig(String path) {
    try (InputStream inputStream = toUrl(path).openStream()) {
      JAXBContext jaxbContext = JAXBContext.newInstance(ComplexTaskConfigList.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      return new ComplexTaskListConfig(
          (ComplexTaskConfigList) jaxbUnmarshaller.unmarshal(inputStream));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

}
