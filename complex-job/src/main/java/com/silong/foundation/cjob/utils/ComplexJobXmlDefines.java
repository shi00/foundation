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
package com.silong.foundation.cjob.utils;

import com.silong.foundation.cjob.xsd2java.ComplexJobConfigList;
import com.silong.foundation.cjob.xsd2java.ComplexJobConfigList.ComplexJobConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * xml配置解析工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-03 08:18
 */
@SuppressFBWarnings(
    value = {"PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"},
    justification = "加载配置任务配置文件")
public class ComplexJobXmlDefines {

  private static final ComplexJobXmlDefines INSTANCE = new ComplexJobXmlDefines();

  private static final Map<String, ComplexJobConfigList> COMPLEX_JOB_CONFIG_LIST_MAP =
      new ConcurrentHashMap<>();

  /** 私有构造，通过静态方法初始化 */
  private ComplexJobXmlDefines() {}

  /**
   * 根据工作实现类权限定名查找工作定义
   *
   * @param jobImplFqdn 工作实现类权限定名
   * @return 工作定义
   */
  public ComplexJobConfig find(String jobImplFqdn) {
    if (jobImplFqdn == null || jobImplFqdn.isEmpty()) {
      throw new IllegalArgumentException("jobImplFqdn must not be null or empty.");
    }
    return COMPLEX_JOB_CONFIG_LIST_MAP.values().stream()
        .flatMap(complexJobConfigList -> complexJobConfigList.getComplexJobConfig().stream())
        .filter(complexJobConfig -> complexJobConfig.getImplementation().equals(jobImplFqdn))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    String.format(
                        "Failed to find %s from the list of jobs definition.", jobImplFqdn)));
  }

  private static URL toUrl(final String path) throws Exception {
    try {
      return new URL(path);
    } catch (MalformedURLException e) {
      URL url = ComplexJobXmlDefines.class.getResource(path);
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
   * 加载job定义配置文件
   *
   * @param path 配置文件路径
   * @return 任务配置
   */
  public static ComplexJobXmlDefines loadJobDefineXml(String path) {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path must not be null or empty.");
    }
    COMPLEX_JOB_CONFIG_LIST_MAP.computeIfAbsent(
        path,
        key -> {
          try (InputStream inputStream = toUrl(key).openStream()) {
            JAXBContext jaxbContext = JAXBContext.newInstance(ComplexJobConfigList.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (ComplexJobConfigList) jaxbUnmarshaller.unmarshal(inputStream);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
    return INSTANCE;
  }
}
