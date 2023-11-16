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

import static com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties.MAX_PARTITIONS_COUNT;

import jakarta.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import lombok.*;

/**
 * 数据分区对象
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-13 10:41
 * @param <T> 节点类型
 */
@Data
public class Partition<T> implements Iterable<T>, Serializable {

  @Serial private static final long serialVersionUID = -1_761_846_080_989_577_337L;

  /** 分区编号 */
  private final int partitionNo;

  /** 数据分区在节点间移动的记录上限 */
  private final int shiftRecords;

  /** 长度 */
  private int curSize;

  /** 移动记录头 */
  private final VersionNode<T> head =
      new VersionNode<>() {

        @Override
        public void setPrev(VersionNode<T> prevRecord) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setValue(T value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public VersionNode<T> getPrev() {
          throw new UnsupportedOperationException();
        }

        @Override
        public T getValue() {
          throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
          return String.format("[head|next:%s]", getNext());
        }
      };

  /** 移动记录尾 */
  private final VersionNode<T> tail =
      new VersionNode<>() {

        @Override
        public VersionNode<T> getNext() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setNext(VersionNode<T> nextRecord) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setValue(T value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public T getValue() {
          throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
          return String.format("[tail|prev:%s]", getPrev());
        }
      };

  /**
   * 构造方法
   *
   * @param partitionNo 分区编号
   * @param shiftRecords 移动记录上限
   */
  public Partition(int partitionNo, int shiftRecords) {
    if (partitionNo < 0 || partitionNo > MAX_PARTITIONS_COUNT) {
      throw new IllegalArgumentException(
          String.format(
              "partitionNo(%d) must be greater than or equals to 0 and less than or equals to %d.",
              partitionNo, MAX_PARTITIONS_COUNT));
    }
    if (shiftRecords < 0) {
      throw new IllegalArgumentException("shiftRecords must be greater than or equals to 0.");
    }
    this.partitionNo = partitionNo;
    this.shiftRecords = shiftRecords;
    clear();
  }

  /**
   * 当前记录数
   *
   * @return 记录数
   */
  public int records() {
    return curSize;
  }

  /** 清空记录 */
  public void clear() {
    head.next = tail;
    tail.prev = head;
  }

  /**
   * 获取当前生效的分区节点映射表
   *
   * @return 分区到节点的映射表
   */
  @Nullable
  public T currentRecord() {
    return head.next.value;
  }

  /**
   * 记录数据分区归属的节点
   *
   * @param node 节点
   */
  public void record(@NonNull T node) {
    if (curSize < shiftRecords) {
      push(node);
      curSize++;
    } else {
      // 循环利用
      recycle(node);
    }
  }

  private void recycle(T node) {
    VersionNode<T> recycleRecord = tail.prev;
    tail.prev = recycleRecord.prev;
    recycleRecord.prev.next = tail;

    recycleRecord.prev = head;
    recycleRecord.value = node;
    recycleRecord.next = head.next;

    head.next.prev = recycleRecord;
    head.next = recycleRecord;
  }

  private void push(T node) {
    VersionNode<T> nextNode = head.next;
    VersionNode<T> shiftRecord = new VersionNode<>(head, node, nextNode);
    head.next = shiftRecord;
    nextNode.prev = shiftRecord;
  }

  @Override
  @NonNull
  public Iterator<T> iterator() {
    return new Iterator<>() {

      private VersionNode<T> cur = head;

      @Override
      public boolean hasNext() {
        return cur.next != tail;
      }

      @Override
      public T next() {
        VersionNode<T> nextNode = cur.next;
        if (nextNode == tail) {
          throw new NoSuchElementException();
        }
        cur = nextNode;
        return nextNode.value;
      }
    };
  }
}
