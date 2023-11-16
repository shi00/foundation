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

import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import lombok.NonNull;

/**
 * 多版本对象基类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-16 19:10
 */
abstract class MultipleVersionObj<T> implements Iterable<T>, Serializable {

  /** 历史版本记录上限 */
  final int recordLimit;

  /** 当前记录数 */
  int index;

  /** 移动记录头 */
  final VersionObj<T> head =
      new VersionObj<>() {

        @Override
        public void setPrev(VersionObj<T> prevRecord) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setValue(T value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public VersionObj<T> getPrev() {
          throw new UnsupportedOperationException();
        }

        @Override
        public T getValue() {
          throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
          return String.format("[head|next:%s]", next);
        }
      };

  /** 移动记录尾 */
  final VersionObj<T> tail =
      new VersionObj<>() {

        @Override
        public VersionObj<T> getNext() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setNext(VersionObj<T> nextRecord) {
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
          return String.format("[tail|prev:%s]", prev);
        }
      };

  /**
   * 构造方法
   *
   * @param recordLimit 记录上限
   */
  public MultipleVersionObj(int recordLimit) {
    if (recordLimit < 0) {
      throw new IllegalArgumentException("recordLimit must be greater than or equals to 0.");
    }
    this.recordLimit = recordLimit;
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

  /** 清空记录 */
  public void clear() {
    head.next = tail;
    tail.prev = head;
    index = 0;
  }

  /**
   * 当前记录数
   *
   * @return 记录数
   */
  public int size() {
    return index;
  }

  /**
   * 记录对象
   *
   * @param obj 对象
   */
  public void record(@NonNull T obj) {
    if (index < recordLimit) {
      push(obj);
      index++;
    } else {
      // 循环利用
      recycle(obj);
    }
  }

  private void recycle(T obj) {
    VersionObj<T> recycleObj = tail.prev;
    tail.prev = recycleObj.prev;
    recycleObj.prev.next = tail;

    recycleObj.prev = head;
    recycleObj.value = obj;
    recycleObj.next = head.next;

    head.next.prev = recycleObj;
    head.next = recycleObj;
  }

  private void push(T obj) {
    VersionObj<T> nextObj = head.next;
    VersionObj<T> obj1 = new VersionObj<>(head, obj, nextObj);
    head.next = obj1;
    nextObj.prev = obj1;
  }

  @Override
  @NonNull
  public Iterator<T> iterator() {
    return new Iterator<>() {

      private VersionObj<T> cur = head;

      @Override
      public boolean hasNext() {
        return cur.next != tail;
      }

      @Override
      public T next() {
        VersionObj<T> nextObj = cur.next;
        if (nextObj == tail) {
          throw new NoSuchElementException();
        }
        cur = nextObj;
        return nextObj.value;
      }
    };
  }
}
