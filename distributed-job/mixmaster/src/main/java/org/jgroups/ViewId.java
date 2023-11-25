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

package org.jgroups;

import com.silong.foundation.dj.hook.clock.LogicalClock;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jgroups.util.Bits;
import org.jgroups.util.SizeStreamable;
import org.jgroups.util.Util;

/**
 * 实现按照逻辑时钟排序的ViewId
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-24 19:37
 */
@NoArgsConstructor
public class ViewId implements Comparable<ViewId>, SizeStreamable, Constructable<ViewId> {

  protected Address creator; // Address of the creator of this view

  protected long id; // Hybird Logical clock time of the view

  /**
   * Creates a ViewID with the coordinator address and a Lamport timestamp of 0.
   *
   * @param creator the address of the member that issued this view
   */
  public ViewId(@NonNull Address creator) {
    this.creator = creator;
  }

  /**
   * Creates a ViewID with the coordinator address and the given Lamport timestamp.
   *
   * @param creator - the address of the member that issued this view
   * @param id - the Lamport timestamp of the view
   */
  public ViewId(@NonNull Address creator, long id) {
    this.creator = creator;
    this.id = id;
  }

  public Supplier<? extends ViewId> create() {
    return ViewId::new;
  }

  /**
   * Returns the address of the member that issued this view
   *
   * @return the Address of the the creator
   */
  public Address getCreator() {
    return creator;
  }

  /**
   * returns the lamport time of the view
   *
   * @return the lamport time timestamp
   */
  public long getId() {
    return id;
  }

  @Override
  public String toString() {
    return "[" + creator + '|' + id + ']';
  }

  public ViewId copy() {
    return new ViewId(creator, id);
  }

  @Override
  public int compareTo(@NonNull ViewId other) {
    int ret;
    return (ret = compareToIDs(other)) == 0 ? creator.compareTo(other.creator) : ret;
  }

  public int compareToIDs(@NonNull ViewId other) {
    return LogicalClock.compare(id, other.id);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof ViewId v) {
      return id == v.id && Objects.equals(creator, v.creator);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, creator);
  }

  @Override
  public void writeTo(DataOutput out) throws IOException {
    Util.writeAddress(creator, out);
    Bits.writeLongCompressed(id, out);
  }

  @Override
  public void readFrom(DataInput in) throws IOException, ClassNotFoundException {
    creator = Util.readAddress(in);
    id = Bits.readLongCompressed(in);
  }

  @Override
  public int serializedSize() {
    return Bits.size(id) + Util.size(creator);
  }
}
