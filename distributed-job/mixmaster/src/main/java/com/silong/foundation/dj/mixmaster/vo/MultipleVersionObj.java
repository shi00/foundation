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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.NotThreadSafe;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 多版本对象，其内保存的多版本值不能重复
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-16 19:10
 */
@NoArgsConstructor
@NotThreadSafe
abstract class MultipleVersionObj<T> implements Iterable<T>, Serializable {

  /** 链表头 */
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
        public String toString() {
          return String.format("[head|next:%s]", next);
        }
      };

  /** 链表尾 */
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
    head.next = tail;
    tail.prev = head;
  }

  /**
   * 获取当前生效的分区节点映射表
   *
   * @return 分区到节点的映射表
   */
  @Nullable
  public T current() {
    return head.next.value;
  }

  /** 清空记录 */
  public void clear() {
    head.next = tail;
    tail.prev = head;
    index = 0;
  }

  /**
   * 找到当前指定值后一个版本的值
   *
   * @param obj 版本值
   * @return 后一个版本值
   */
  @Nullable
  public T after(T obj) {
    Node<T> node = find(obj, head);
    if (node != null) {
      return node.prev.value;
    }
    return null;
  }

  /**
   * 找到当前指定值前一个版本的值
   *
   * @param obj 版本值
   * @return 前一个版本值
   */
  @Nullable
  public T before(T obj) {
    Node<T> node = find(obj, head);
    if (node != null) {
      return node.next.value;
    }
    return null;
  }

  private Node<T> find(T value, Node<T> node) {
    if (node == tail) {
      return null;
    }
    if (Objects.equals(value, node.value)) {
      return node;
    }
    return find(value, node.next);
  }

  /**
   * 是否包含给定对象
   *
   * @param obj 对象
   * @return true or false
   */
  public boolean contains(@NonNull T obj) {
    for (T o : this) {
      if (obj.equals(o)) {
        return true;
      }
    }
    return false;
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
   * 是否为空
   *
   * @return true or false
   */
  public boolean isEmpty() {
    return index == 0;
  }

  /**
   * 尾部追加节点
   *
   * @param obj 对象
   */
  public void append(@NonNull T obj) {
    Node<T> prevNode = tail.prev;
    Node<T> newNode = new Node<>(prevNode, obj, tail);
    tail.prev = newNode;
    prevNode.next = newNode;
    index++;
  }

  /**
   * 插入记录对象，队头
   *
   * @param obj 对象
   */
  public void insert(@NonNull T obj) {
    if (index < recordLimit) {
      push(obj);
      index++;
    } else {
      // 循环利用
      recycle(obj);
    }
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
        toStream(iterator()).map(Object::hashCode).reduce(Objects::hash).orElse(0),
        index,
        recordLimit);
  }

  Stream<T> toStream(Iterator<T> iterator) {
    return StreamSupport.stream(
        Spliterators.spliterator(iterator, index, ORDERED | SIZED | NONNULL), false);
  }
}
