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
import static com.silong.foundation.dj.mixmaster.message.Messages.ClusterView.newBuilder;
import static java.util.stream.Collectors.joining;

import com.google.protobuf.ByteString;
import com.silong.foundation.dj.mixmaster.message.Messages;
import com.silong.foundation.dj.mixmaster.message.Messages.ViewList;
import java.io.*;
import java.util.List;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.View;
import org.jgroups.util.ByteArrayDataInputStream;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.Util;

/**
 * 集群视图
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-16 17:35
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ClusterView extends MultipleVersionObj<View> {

  @Serial private static final long serialVersionUID = -240_752_712_356_040_731L;

  /**
   * 构造方法
   *
   * @param recordLimit 记录上限
   */
  public ClusterView(int recordLimit) {
    super(recordLimit);
    clear();
  }

  /**
   * 序列化
   *
   * @param out 输出流
   * @throws IOException 异常
   */
  public void writeTo(@NonNull OutputStream out) throws IOException {
    Messages.ClusterView.Builder clusterViewBuilder = newBuilder().setRecordLimit(recordLimit);
    if (size() > 0) {
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
    clusterViewBuilder.build().writeTo(out);
  }

  /**
   * 反序列化
   *
   * @param in 输入流
   * @throws IOException 异常
   * @throws ClassNotFoundException 异常
   */
  public void readFrom(@NonNull InputStream in) throws IOException, ClassNotFoundException {
    Messages.ClusterView clusterView = Messages.ClusterView.parseFrom(in);
    recordLimit = clusterView.getRecordLimit();
    clear();

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
        append(view);
      }
    }
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
