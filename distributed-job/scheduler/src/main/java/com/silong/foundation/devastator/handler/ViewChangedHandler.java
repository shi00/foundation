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
package com.silong.foundation.devastator.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.silong.foundation.devastator.core.ClusterNodeUUID;
import com.silong.foundation.devastator.core.DefaultDistributedEngine;
import com.silong.foundation.devastator.event.ViewChangedEvent;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.MergeView;
import org.jgroups.View;

import java.io.Closeable;
import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.ThreadFactory;

import static com.lmax.disruptor.dsl.ProducerType.MULTI;
import static com.silong.foundation.devastator.core.DefaultObjectPartitionMapping.calculateMask;

/**
 * 集群视图变化事件处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-29 23:40
 */
@Slf4j
public class ViewChangedHandler implements EventHandler<ViewChangedEvent>, Closeable, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 视图变更事件处理器线程名 */
  public static final String VIEW_CHANGED_EVENT_PROCESSOR = "view-changed-processor";

  /** 分布式引擎 */
  private final DefaultDistributedEngine engine;

  private final Disruptor<ViewChangedEvent> disruptor;

  private final RingBuffer<ViewChangedEvent> ringBuffer;

  /**
   * 事件处理器
   *
   * @param engine 分布式引擎
   */
  public ViewChangedHandler(DefaultDistributedEngine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.engine = engine;
    this.disruptor = buildViewChangedEventDisruptor(engine.config().viewChangedEventQueueSize());
    this.ringBuffer = disruptor.start();
  }

  private Disruptor<ViewChangedEvent> buildViewChangedEventDisruptor(int queueSize) {
    if (calculateMask(queueSize) == -1) {
      throw new IllegalArgumentException("queueSize must be power of 2.");
    }
    Disruptor<ViewChangedEvent> disruptor =
        new Disruptor<>(
            ViewChangedEvent::new,
            queueSize,
            (ThreadFactory) r -> new Thread(r, VIEW_CHANGED_EVENT_PROCESSOR),
            MULTI,
            new LiteBlockingWaitStrategy());
    disruptor.handleEventsWith(this);
    return disruptor;
  }

  private ClusterNodeInfo getClusterNodeInfo(Address address) {
    return ((ClusterNodeUUID) address).clusterNodeInfo();
  }

  private boolean isCoordChanged(View oldView, View newView) {
    Address newCoord = newView.getMembers().get(0);
    ClusterNodeInfo newClusterNodeInfo = getClusterNodeInfo(newCoord);
    if (oldView == null) {
      log.info(
          "{} becomes the coordinator of {}.",
          nodeIdentity(newClusterNodeInfo),
          newClusterNodeInfo.getClusterName());
      return true;
    }

    Address oldCoord = oldView.getMembers().get(0);
    boolean isChanged = !newCoord.equals(oldCoord);
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
    long seq = ringBuffer.next();
    try {
      ringBuffer.get(seq).oldView(oldView).newview(newView);
    } finally {
      ringBuffer.publish(seq);
    }
  }

  private String nodeIdentity(ClusterNodeInfo clusterNodeInfo) {
    return String.format(
        "[%s:%s]", clusterNodeInfo.getHostName(), clusterNodeInfo.getInstanceName());
  }

  @Override
  public void onEvent(ViewChangedEvent event, long sequence, boolean endOfBatch) {
    View newView = null;
    View oldView = null;
    try (event) {
      newView = event.newview();
      oldView = event.oldView();
      if (isCoordChanged(oldView, newView)) {
        // 1.节点首次加入集群
        // 2.集群coord变更
        // 以上两情况同步集群状态
        engine.syncClusterState();
      }

      if (newView instanceof MergeView) {

      } else {

      }
    } catch (Exception e) {
      log.warn("Failed to process ViewChangedEvent:{oldView:{}, newView:{}}.", oldView, newView, e);
    }
  }

  @Override
  public void close() {
    disruptor.shutdown();
  }
}
