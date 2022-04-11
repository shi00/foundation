package com.silong.foundation.devastator;

import java.io.Serializable;
import java.util.Collection;

/**
 * 集群
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 09:28
 */
public interface Cluster extends Serializable {

  /**
   * 集群唯一标识
   *
   * @param <T> uuid类型
   * @return uuid
   */
  <T extends Comparable<T>> T uuid();

  /**
   * 集群视图版本
   *
   * @return 视图版本
   */
  long viewVersion();

  /**
   * 获取指定版本集群视图
   *
   * @param version 版本
   * @return 集群视图
   */
  Collection<ClusterNode> view(long version);

  /**
   * 获取本地节点
   *
   * @return 本地节点
   */
  ClusterNode loadNode();
}
