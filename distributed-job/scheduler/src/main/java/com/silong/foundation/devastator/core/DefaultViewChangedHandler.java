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
package com.silong.foundation.devastator.core;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.silong.foundation.devastator.event.ViewChangedEvent;
import com.silong.foundation.devastator.model.ClusterNodeUUID;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.MergeView;
import org.jgroups.View;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.ThreadFactory;

import static com.lmax.disruptor.dsl.ProducerType.MULTI;

/**
 * 集群视图变化事件处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-29 23:40
 */
@Slf4j
class DefaultViewChangedHandler
    implements EventHandler<ViewChangedEvent>, AutoCloseable, Serializable {

  @Serial private static final long serialVersionUID = 7601624347424402665L;

  /**
   * The maximum capacity, used if a higher value is implicitly specified by either of the
   * constructors with arguments. MUST be a power of two <= 1<<30.
   */
  static final int MAXIMUM_CAPACITY = 1 << 30;

  /** 视图变更事件处理器线程名 */
  public static final String VIEW_CHANGED_EVENT_PROCESSOR = "view-changed-processor";

  /** 分布式引擎 */
  private DefaultDistributedEngine engine;

  /** 事件处理器 */
  private Disruptor<ViewChangedEvent> disruptor;

  /** 环状队列 */
  private RingBuffer<ViewChangedEvent> ringBuffer;

  /**
   * 事件处理器
   *
   * @param engine 分布式引擎
   */
  public DefaultViewChangedHandler(DefaultDistributedEngine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.engine = engine;
    this.disruptor = buildViewChangedEventDisruptor(engine.config().viewChangedEventQueueSize());
    this.ringBuffer = disruptor.start();
  }

  private Disruptor<ViewChangedEvent> buildViewChangedEventDisruptor(int queueSize) {
    Disruptor<ViewChangedEvent> disruptor =
        new Disruptor<>(
            ViewChangedEvent::new,
            powerOf2(queueSize),
            (ThreadFactory) r -> new Thread(r, VIEW_CHANGED_EVENT_PROCESSOR),
            MULTI,
            new BusySpinWaitStrategy());
    disruptor.handleEventsWith(this);
    return disruptor;
  }

  private ClusterNodeInfo getClusterNodeInfo(Address address) {
    return ((ClusterNodeUUID) address).getClusterNodeInfo();
  }

  /**
   * 比较两个集群视图是否发生了集群coordinator变化
   *
   * @param oldView old view of cluster
   * @param newView new view of cluster
   * @return true or false
   */
  private boolean isCoordinatorChanged(View oldView, View newView) {
    Address newCoord = newView.getCoord();
    ClusterNodeInfo newClusterNodeInfo = getClusterNodeInfo(newCoord);
    if (oldView == null) {
      log.info(
          "{} becomes the coordinator of {}.",
          nodeIdentity(newClusterNodeInfo),
          newClusterNodeInfo.getClusterName());
      return true;
    }

    Address oldCoord = oldView.getCoord();
    boolean isChanged = !oldCoord.equals(newCoord);
    if (isChanged) {
      ClusterNodeInfo oldClusterNodeInfo = getClusterNodeInfo(oldCoord);
      String clusterName = newClusterNodeInfo.getClusterName();
      log.info(
          "The coordinator for {} has been changed from {} to {}.",
          clusterName,
          nodeIdentity(oldClusterNodeInfo),
          nodeIdentity(newClusterNodeInfo));
    }
    return isChanged;
  }

  /**
   * 处理集群视图变化
   *
   * @param oldView 旧视图
   * @param newView 新视图
   */
  public void handle(View oldView, View newView) {
    if (oldView == null && newView == null) {
      log.error("oldView and newView cannot be null at the same time.");
      return;
    }
    long sequence = ringBuffer.next();
    try {
      ViewChangedEvent event = ringBuffer.get(sequence).oldView(oldView).newView(newView);
      if (log.isDebugEnabled()) {
        log.debug("Enqueue {} with sequence:{}.", event, sequence);
      }
    } finally {
      ringBuffer.publish(sequence);
    }
  }

  private String nodeIdentity(ClusterNodeInfo clusterNodeInfo) {
    return String.format(
        "[%s:%s]", clusterNodeInfo.getHostName(), clusterNodeInfo.getInstanceName());
  }

  @Override
  public void onEvent(ViewChangedEvent event, long sequence, boolean endOfBatch) {
    if (log.isDebugEnabled()) {
      log.debug(
          "Start processing {} with sequence:{} and endOfBatch:{}.", event, sequence, endOfBatch);
    }

    View newView = null;
    View oldView = null;
    try (event) {
      newView = event.newView();
      oldView = event.oldView();
      if (isCoordinatorChanged(oldView, newView)) {
        // 1.节点首次加入集群
        // 2.集群coord变更
        // 以上两情况同步集群状态
        engine.syncClusterState();
      }

      // 脑裂恢复
      if (newView instanceof MergeView) {}

      engine.repartition(oldView, newView);
    } catch (Exception e) {
      log.warn("Failed to process ViewChangedEvent:{oldView:{}, newView:{}}.", oldView, newView, e);
    } finally {
      if (log.isDebugEnabled()) {
        log.debug(
            "End processing {} with sequence:{} and endOfBatch:{}.", event, sequence, endOfBatch);
      }
    }
  }

  static int powerOf2(int size) {
    int n = -1 >>> Integer.numberOfLeadingZeros(size - 1);
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
  }

  @Override
  public void close() {
    if (this.disruptor != null) {
      this.disruptor.shutdown();
      this.disruptor = null;
    }
    this.ringBuffer = null;
    this.engine = null;
  }
}
