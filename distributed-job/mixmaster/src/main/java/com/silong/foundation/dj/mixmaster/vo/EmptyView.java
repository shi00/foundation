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

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.ViewId;

/**
 * Empty View
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-11 9:09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EmptyView extends View {
  public static final EmptyView EMPTY_VIEW = new EmptyView();

  private EmptyView() {}

  @Override
  public Supplier<? extends View> create() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ViewId getViewId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Address getCreator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Address getCoord() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Address> getMembers() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Address[] getMembersRaw() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsMember(Address mbr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsMembers(Address... mbrs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int compareTo(View o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeTo(DataOutput out) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void readFrom(DataInput in) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int serializedSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull
  public Iterator<Address> iterator() {
    throw new UnsupportedOperationException();
  }
}
