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

package com.silong.foundation.dj.mixmaster.vo;

import static com.google.protobuf.UnsafeByteOperations.unsafeWrap;
import static com.silong.foundation.dj.mixmaster.generated.Messages.ClusterView.newBuilder;
import static java.util.stream.Collectors.joining;

import com.google.protobuf.ByteString;
import com.silong.foundation.dj.mixmaster.generated.Messages;
import com.silong.foundation.dj.mixmaster.generated.Messages.ViewList;
import jakarta.annotation.Nullable;
import java.io.*;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.concurrent.ThreadSafe;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.View;
import org.jgroups.util.ByteArrayDataInputStream;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.Util;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

/**
 * 集群视图，包含连续变化的历史视图，包含的历史视图数量可指定，默认：5
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-16 17:35
 */
@Slf4j
@ThreadSafe
@EqualsAndHashCode(callSuper = true)
public class ClusterView extends MultipleVersionObj<View> {

  @Serial private static final long serialVersionUID = 2_816_357_969_167_034_367L;

  private final StampedLock lock = new StampedLock();

  /**
   * 构造方法
   *
   * @param recordLimit 记录上限
   */
  public ClusterView(int recordLimit) {
    super(recordLimit);
  }

  /** 清空视图，加锁同步 */
  @Override
  public void clear() {
    doWithWriteLock(super::clear);
  }

  /**
   * 序列化
   *
   * @param out ByteArrayOutputStream输出流
   * @throws IOException 异常
   */
  public void writeTo(@NonNull OutputStream out) throws IOException {
    try (SnappyOutputStream outputStream = new SnappyOutputStream(out)) {
      tryOptimisticRead(this::buildClusterView).writeTo(outputStream);
    }
  }

  @SneakyThrows
  private Messages.ClusterView buildClusterView() {
    Messages.ClusterView.Builder clusterViewBuilder = newBuilder().setRecordLimit(recordLimit);
    if (index > 0) {
      ViewList.Builder viewListBuilder = ViewList.newBuilder();
      for (View view : this) {
        ByteArrayDataOutputStream bout = new ByteArrayDataOutputStream(Util.size(view));
        Util.writeView(view, bout);
        viewListBuilder.addViewBytes(unsafeWrap(bout.buffer()));
        if (log.isDebugEnabled()) {
          log.debug("writeTo: {}", view);
        }
      }
      clusterViewBuilder.setViewList(viewListBuilder);
    }
    return clusterViewBuilder.build();
  }

  /**
   * 反序列化
   *
   * @param in ByteArrayInputStream输入流
   */
  public void readFrom(@NonNull InputStream in) {
    doWithWriteLock(() -> readClusterView(in));
  }

  @SneakyThrows
  private void readClusterView(InputStream in) {
    try (SnappyInputStream snappyInputStream = new SnappyInputStream(in)) {
      Messages.ClusterView clusterView = Messages.ClusterView.parseFrom(snappyInputStream);
      recordLimit = Math.max(clusterView.getRecordLimit(), recordLimit); // 考虑到配置值变化的情况，此处选用最大值
      super.clear();

      if (clusterView.hasViewList()) {
        ViewList viewList = clusterView.getViewList();
        int views = viewList.getViewBytesCount();
        for (int i = 0; i < views; i++) {
          ByteString viewBytes = viewList.getViewBytes(i);
          ByteArrayDataInputStream ins = new ByteArrayDataInputStream(viewBytes.toByteArray());
          View view = Util.readView(ins);
          if (log.isDebugEnabled()) {
            log.debug("readFrom: {}", view);
          }
          super.append(view);
        }
      }
    }
  }

  @Override
  public boolean contains(@NonNull View obj) {
    return tryOptimisticRead(() -> super.contains(obj));
  }

  /**
   * 追加视图，追加的view不能与当前保存的view重复，并且当前队尾的view的id必须大于给定view
   *
   * @param view 追加视图
   */
  @Override
  public void append(@NonNull View view) {
    doWithWriteLock(
        () -> {
          if (super.isEmpty()
              || (!super.contains(view)
                  && tail.prev.value.getViewId().compareTo(view.getViewId()) > 0)) {
            super.append(view);
          } else {
            log.warn("skip view: {}", view);
          }
        });
  }

  /**
   * 记录视图，记录的view不能与当前保存的view重复，并且当前队头的view的id必须小于给定view
   *
   * @param view 被记录视图
   */
  @Override
  public void insert(@NonNull View view) {
    doWithWriteLock(
        () -> {
          if (super.isEmpty()
              || (!super.contains(view)
                  && head.next.value.getViewId().compareTo(view.getViewId()) < 0)) {
            super.insert(view);
          } else {
            log.warn("skip view: {}", view);
          }
        });
  }

  @Override
  public boolean isEmpty() {
    return tryOptimisticRead(super::isEmpty);
  }

  @Nullable
  @Override
  public View after(View View) {
    return tryOptimisticRead(() -> super.after(View));
  }

  @Nullable
  @Override
  public View before(View View) {
    return tryOptimisticRead(() -> super.before(View));
  }

  /**
   * 合并集群视图，根据viewId进行排序，从大到小
   *
   * @param cView 集群视图
   */
  public void merge(@NonNull ClusterView cView) {
    doWithWriteLock(() -> doMerge(cView));
  }

  private void doMerge(ClusterView cView) {
    List<View> list =
        Stream.concat(toStream(iterator()), toStream(cView.iterator()))
            .distinct()
            .sorted(View::compareTo)
            .toList();
    super.clear();
    list.forEach(super::insert);
  }

  @Override
  public String toString() {
    return String.format(
        "ClusterView{recordLimit:%d, size:%d, %s}",
        recordLimit,
        index,
        toStream(iterator()).map(view -> "{" + view.toString() + "}").collect(joining(", ")));
  }

  /**
   * 返回集群视图列表
   *
   * @return 集群视图列表
   */
  public List<View> toList() {
    return toStream(iterator()).toList();
  }

  @Override
  public int size() {
    return tryOptimisticRead(super::size);
  }

  @Nullable
  @Override
  public View current() {
    return tryOptimisticRead(super::current);
  }

  private void doWithWriteLock(Runnable runnable) {
    long stamp = lock.writeLock();
    try {
      runnable.run();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  private <T> T tryOptimisticRead(Supplier<T> supplier) {
    long stamp = lock.tryOptimisticRead();
    T result = supplier.get();
    if (!lock.validate(stamp)) {
      stamp = lock.readLock();
      try {
        result = supplier.get();
      } finally {
        lock.unlockRead(stamp);
      }
    }
    return result;
  }
}
