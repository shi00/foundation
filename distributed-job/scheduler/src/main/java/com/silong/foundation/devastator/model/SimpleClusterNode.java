/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator.model;

import com.silong.foundation.devastator.ObjectIdentity;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jgroups.Address;

import java.io.Serial;

/**
 * 简单节点
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-03 00:15
 */
public record SimpleClusterNode(Address address) implements ObjectIdentity<Address> {

    @Serial
    private static final long serialVersionUID = 655289082556457884L;

    @Override
    @NonNull
    public Address uuid() {
        return address;
    }

    @Override
    public long objectVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean verify(@NonNull ObjectIdentity<Address> obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close()  {

    }
}
