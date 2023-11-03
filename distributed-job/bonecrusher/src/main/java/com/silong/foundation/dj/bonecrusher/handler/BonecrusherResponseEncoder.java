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

package com.silong.foundation.dj.bonecrusher.handler;

import static com.silong.foundation.dj.bonecrusher.enu.MessageMagic.RESPONSE;
import static io.netty.buffer.Unpooled.EMPTY_BUFFER;

import com.silong.foundation.dj.bonecrusher.message.Messages.ResponseHeader;
import com.silong.foundation.lambda.Tuple2;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * 响应消息编码器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-02 23:33
 */
@Sharable
public class BonecrusherResponseEncoder extends ChannelOutboundHandlerAdapter {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (msg instanceof Tuple2) {
      @SuppressWarnings("unchecked")
      Tuple2<ResponseHeader, ByteBuf> tuple2 = (Tuple2<ResponseHeader, ByteBuf>) msg;
      ResponseHeader responseHeader = tuple2.t1();
      ByteBuf data = tuple2.t2();
      switch (responseHeader.getType()) {
        case DATA_SYNC_RESP, LOADING_CLASS_RESP, AUTHENTICATION_FAILED_RESP -> msg =
            (data != null && data != EMPTY_BUFFER)
                ? ctx.alloc()
                    .compositeBuffer(3)
                    .addComponents(
                        true,
                        RESPONSE.getMagicBuf(), // 响应魔数
                        Unpooled.wrappedBuffer(responseHeader.toByteArray()), // 响应头
                        data)
                : ctx.alloc()
                    .compositeBuffer(2)
                    .addComponents(
                        true,
                        RESPONSE.getMagicBuf(), // 响应魔数
                        Unpooled.wrappedBuffer(responseHeader.toByteArray()));
        default -> throw new IllegalArgumentException(
            "Unknown Message Type: " + responseHeader.getType());
      }
    }
    ctx.write(msg, promise);
  }
}
