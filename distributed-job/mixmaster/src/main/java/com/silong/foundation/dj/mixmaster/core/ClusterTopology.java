/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.dj.mixmaster.core;

import static com.silong.foundation.dj.hook.utils.StampedLocks.tryOptimisticRead;
import static com.silong.foundation.dj.hook.utils.StampedLocks.writeLock;
import static com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties.CLUSTER_TOPOLOGY_COLUMN_FAMILY;

import com.silong.foundation.dj.hook.clock.LogicalClock;
import com.silong.foundation.dj.hook.clock.LogicalClock.Timestamp;
import com.silong.foundation.dj.longhaul.RocksDbPersistStorage;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.View;
import org.jgroups.ViewId;
import org.jgroups.util.*;
import org.jgroups.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 集群拓扑图
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-23 20:59
 */
@Slf4j
@Component
@ToString
@NoArgsConstructor
class ClusterTopology implements Serializable, SizeStreamable {

  @Serial private static final long serialVersionUID = -8_703_813_222_462_062_393L;

  /** 同步锁 */
  @ToString.Exclude private final StampedLock lock = new StampedLock();

  /** 保存集群拓扑图，根据view自带的混合逻辑时钟排序，由近及远 */
  private final TreeMap<ViewId, View> topologies =
      new TreeMap<>(Collections.reverseOrder(ViewId::compareToIDs));

  /** 持久化存储 */
  @ToString.Exclude private RocksDbPersistStorage persistStorage;

  /** 恢复持久化数据 */
  @PostConstruct
  void resume() {
    // 加载持久化的数据
    persistStorage.iterate(
        CLUSTER_TOPOLOGY_COLUMN_FAMILY,
        (key, value) -> {
          try {
            topologies.put(toViewId(key), toView(value));
          } catch (IOException | ClassNotFoundException e) {
            log.error(
                "Failed to resume views from local storage. {} : {}",
                HexFormat.of().formatHex(key),
                HexFormat.of().formatHex(value),
                e);
          }
        });
  }

  private byte[] toBytes(ViewId viewId) throws IOException {
    ByteArrayDataOutputStream out = new ByteArrayDataOutputStream(Util.size(viewId));
    Util.writeViewId(viewId, out);
    return out.buffer();
  }

  private ViewId toViewId(byte[] bytes) throws IOException, ClassNotFoundException {
    ByteArrayDataInputStream in = new ByteArrayDataInputStream(bytes);
    return Util.readViewId(in);
  }

  private View toView(byte[] bytes) throws IOException, ClassNotFoundException {
    ByteArrayDataInputStream in = new ByteArrayDataInputStream(bytes);
    return Util.readView(in);
  }

  private byte[] toBytes(View view) throws IOException {
    ByteArrayDataOutputStream out = new ByteArrayDataOutputStream(Util.size(view));
    Util.writeView(view, out);
    return out.buffer();
  }

  /**
   * 当前集群拓扑图是否为空
   *
   * @return true or false
   */
  public boolean isEmpty() {
    return tryOptimisticRead(lock, topologies::isEmpty);
  }

  /**
   * 最新的集群视图
   *
   * @return 视图
   */
  @Nullable
  public View lastView() {
    return tryOptimisticRead(lock, this::firstEntry);
  }

  private View firstEntry() {
    Map.Entry<ViewId, View> entry = topologies.firstEntry();
    return entry != null ? entry.getValue() : null;
  }

  /**
   * 获取大于等于给定时间且最小的集群视图
   *
   * @param time UTC时间
   * @return 视图
   */
  @Nullable
  public View cellingView(long time) {
    Map.Entry<ViewId, View> entry =
        topologies.ceilingEntry(
            new ViewId(UUID.randomUUID(), LogicalClock.to(new Timestamp(time, 0))));
    return entry != null ? entry.getValue() : null;
  }

  /**
   * 获取小于等于给定时间且最大的集群视图
   *
   * @param time UTC时间
   * @return 视图
   */
  @Nullable
  public View floorView(long time) {
    Map.Entry<ViewId, View> entry =
        topologies.floorEntry(
            new ViewId(UUID.randomUUID(), LogicalClock.to(new Timestamp(time, 0))));
    return entry != null ? entry.getValue() : null;
  }

  /**
   * 保存集群视图
   *
   * @param view 集群视图
   * @exception Exception 异常
   */
  public void save(@NonNull View view) throws Exception {
    ViewId viewId = view.getViewId();
    byte[] key = toBytes(viewId);
    byte[] value = toBytes(view);
    writeLock(
        lock,
        () -> {
          persistStorage.put(CLUSTER_TOPOLOGY_COLUMN_FAMILY, key, value);
          topologies.put(viewId, view);
        });
  }

  @Override
  public int serializedSize() {
    return tryOptimisticRead(
        lock,
        () ->
            Integer.BYTES
                + topologies.entrySet().stream()
                    .mapToInt(e -> Util.size(e.getKey()) + Util.size(e.getValue()))
                    .sum());
  }

  @Override
  public void writeTo(DataOutput output) {
    tryOptimisticRead(
        lock,
        (Supplier<Optional<?>>)
            () -> {
              try {
                output.writeInt(topologies.size());
              } catch (IOException e) {
                throw new IllegalStateException(e);
              }
              topologies.forEach(
                  (k, v) -> {
                    try {
                      Util.writeViewId(k, output);
                      Util.writeView(v, output);
                    } catch (IOException e) {
                      throw new IllegalStateException(e);
                    }
                  });
              return Optional.empty();
            });
  }

  @Override
  public void readFrom(DataInput input) {
    writeLock(
        lock,
        () -> {
          try {
            int size = input.readInt();
            for (int i = 0; i < size; i++) {
              topologies.put(Util.readViewId(input), Util.readView(input));
            }
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }

  @Autowired
  public void setPersistStorage(RocksDbPersistStorage persistStorage) {
    this.persistStorage = persistStorage;
  }
}
