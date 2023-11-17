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

import static java.util.Spliterator.*;
import static java.util.stream.Collectors.joining;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import org.jgroups.View;
import org.jgroups.util.SizeStreamable;
import org.jgroups.util.Util;

/**
 * 集群视图
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-16 17:35
 */
@EqualsAndHashCode(callSuper = true)
public class ClusterView extends MultipleVersionObj<View> implements SizeStreamable {

  @Serial private static final long serialVersionUID = -240_752_712_356_040_731L;

  /** 默认构造方法 */
  public ClusterView() {
    this(0);
  }

  /**
   * 构造方法
   *
   * @param recordLimit 记录上限
   */
  public ClusterView(int recordLimit) {
    super(recordLimit);
    clear();
  }

  @Override
  public int serializedSize() {
    int totalSize = Long.BYTES;
    for (View view : this) {
      totalSize += view.serializedSize();
    }
    // 历史视图列表 + recordLimit + size
    return totalSize;
  }

  @Override
  public void writeTo(DataOutput out) throws IOException {
    out.write(recordLimit);
    out.write(index);
    for (View view : this) {
      view.writeTo(out);
    }
  }

  @Override
  public void readFrom(DataInput in) throws IOException, ClassNotFoundException {
    recordLimit = in.readInt();
    int length = in.readInt();
    for (int i = 0; i < length; i++) {
      View view = Util.readView(in);
      append(view);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "ClusterView{recordLimit:%d, size:%d, %s}",
        recordLimit, index, toStream(iterator()).map(View::toString).collect(joining(", ")));
  }

  /**
   * 合并集群视图
   *
   * @param cView 集群视图
   */
  public void merge(ClusterView cView) {
    List<View> list =
        Stream.concat(toStream(iterator()), toStream(cView.iterator()))
            .distinct()
            .sorted(View::compareTo)
            .toList();
    clear();
    list.forEach(this::record);
  }
}
