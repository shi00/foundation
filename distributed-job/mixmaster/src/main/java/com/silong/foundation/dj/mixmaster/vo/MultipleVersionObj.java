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

import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.StreamSupport;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 多版本对象基类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-16 19:10
 */
@NoArgsConstructor
abstract class MultipleVersionObj<T> implements Iterable<T>, Serializable {

  /** 移动记录头 */
  final Node<T> head =
      new Node<>() {

        @Override
        public void setPrev(Node<T> prevRecord) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setValue(T value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Node<T> getPrev() {
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
  final Node<T> tail =
      new Node<>() {

        @Override
        public Node<T> getNext() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setNext(Node<T> nextRecord) {
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

  /** 当前记录数 */
  int index;

  /** 历史版本记录上限 */
  int recordLimit;

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
   * 尾部追加节点
   *
   * @param obj 对象
   */
  void append(@NonNull T obj) {
    Node<T> prevNode = tail.prev;
    Node<T> newNode = new Node<>(prevNode, obj, tail);
    tail.prev = newNode;
    prevNode.next = newNode;
    index++;
  }

  /**
   * 记录对象
   *
   * @param obj 对象
   */
  public MultipleVersionObj<T> record(@NonNull T obj) {
    if (index < recordLimit) {
      push(obj);
      index++;
    } else {
      // 循环利用
      recycle(obj);
    }
    return this;
  }

  private void recycle(T obj) {
    Node<T> recycleObj = tail.prev;
    tail.prev = recycleObj.prev;
    recycleObj.prev.next = tail;

    recycleObj.prev = head;
    recycleObj.value = obj;
    recycleObj.next = head.next;

    head.next.prev = recycleObj;
    head.next = recycleObj;
  }

  private void push(T obj) {
    Node<T> nextObj = head.next;
    Node<T> obj1 = new Node<>(head, obj, nextObj);
    head.next = obj1;
    nextObj.prev = obj1;
  }

  @Override
  @NonNull
  public Iterator<T> iterator() {
    return new Iterator<>() {

      private Node<T> cur = head;

      @Override
      public boolean hasNext() {
        return cur.next != tail;
      }

      @Override
      public T next() {
        Node<T> nextObj = cur.next;
        if (nextObj == tail) {
          throw new NoSuchElementException();
        }
        cur = nextObj;
        return nextObj.value;
      }
    };
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof MultipleVersionObj<?> that) {
      return index == that.index
          && recordLimit == that.recordLimit
          && compare(iterator(), that.iterator());
    }
    return false;
  }

  boolean compare(Iterator<?> a, Iterator<?> b) {
    while (a.hasNext()) {
      if (!b.hasNext()) {
        return false;
      }
      if (!Objects.equals(a.next(), b.next())) {
        return false;
      }
    }
    return !b.hasNext();
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        StreamSupport.stream(
                Spliterators.spliterator(iterator(), index, ORDERED | SIZED | NONNULL), false)
            .map(Object::hashCode)
            .reduce(Objects::hash)
            .orElse(0),
        index,
        recordLimit);
  }
}
