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
package com.silong.foundation.djs.scheduler.cluster.core;

import com.silong.foundation.djs.scheduler.cluster.ClusterNode;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.HmacUtils;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Receiver;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256;
import static org.apache.commons.lang3.SystemUtils.getHostName;
import static org.jgroups.Version.printVersion;

/**
 * 基于JGroups的集群节点实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-07 22:49
 */
public class JgroupsClusterNode implements ClusterNode, ChannelListener, Receiver, Closeable {

  private JChannel jChannel;

  /** 节点名 */
  private final String hostName;

  /** 节点uuid */
  private final String uuid;

  /** 属性集合 */
  private final Map<String, Object> attributes = new TreeMap<>();

  /** ip地址列表 */
  private final Collection<String> addresses;

  /** 节点版本 */
  private final String version;

  /** 构造方法 */
  public JgroupsClusterNode() {
    this.version = printVersion();
    this.hostName = getHostName();
    this.addresses = getNodeAddresses();
    this.uuid =
        new HmacUtils(HMAC_SHA_256, version)
            .hmacHex(hostName + addresses.stream().collect(joining(",", "[", "]")));
    this.attributes.putAll(System.getenv());
//    this.jChannel = new JChannel("");
  }

  @SneakyThrows
  private Collection<String> getNodeAddresses() {
    return NetworkInterface.networkInterfaces()
        .flatMap(NetworkInterface::inetAddresses)
        .map(InetAddress::getHostAddress)
        .sorted(String::compareTo)
        .toList();
  }

  @Override
  public Collection<Role> roles() {
    return null;
  }

  @Override
  public String version() {
    return version;
  }

  @Override
  public String hostName() {
    return hostName;
  }

  @Override
  public Collection<String> addresses() {
    return addresses;
  }

  @Override
  public boolean isLocalNode() {
    return false;
  }

  @Override
  public Comparable<String> uuid() {
    return uuid;
  }

  @Override
  public <T> T attribute(String attributeName) {
    return (T) attributes.get(attributeName);
  }

  @Override
  public Map<String, Object> attributes() {
    return attributes;
  }

  @Override
  public void close() {
    if (jChannel != null) {
      jChannel.close();
    }
  }
}
