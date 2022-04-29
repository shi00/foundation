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
import com.silong.foundation.devastator.ClusterNode;
import com.silong.foundation.devastator.core.DefaultDistributedEngine;
import com.silong.foundation.devastator.event.ViewChangedEvent;
import org.jgroups.MergeView;
import org.jgroups.View;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * 集群视图变化事件处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-29 23:40
 */
public class ViewChangedEventHandler implements EventHandler<ViewChangedEvent>, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 分布式引擎 */
  private final DefaultDistributedEngine engine;

  /**
   * 事件处理器
   *
   * @param engine 分布式引擎
   */
  public ViewChangedEventHandler(DefaultDistributedEngine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.engine = engine;
  }

  @Override
  public void onEvent(ViewChangedEvent event, long sequence, boolean endOfBatch) throws Exception {
    // 同步集群状态
    engine.syncClusterState();

    try {
      View newView = event.newview();
      View oldView = event.oldView();
      if (newView instanceof MergeView) {

      } else {
        // 获取集群节点列表
        Collection<ClusterNode> clusterNodes = engine.getClusterNodes(newView);
        // 根据集群节点调整分区分布
        IntStream.range(0, engine.getPartitionCount())
            .parallel()
            .forEach(
                partitionNo ->
                    engine
                        .getPartition2ClusterNodes()
                        .put(
                            partitionNo,
                            engine
                                .getAllocator()
                                .allocatePartition(
                                    partitionNo, engine.getBackupNums(), clusterNodes, null)));
      }
    } finally {
      // 处理完毕置空，避免影响GC及时收集
      event.close();
    }
  }
}
