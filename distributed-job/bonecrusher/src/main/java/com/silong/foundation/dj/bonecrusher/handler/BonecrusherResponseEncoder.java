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

import com.silong.foundation.dj.bonecrusher.message.Messages.ResponseHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import java.io.IOException;

/**
 * 响应消息编码器，magic->totalbytes->headerBytes->header->datablock
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-02 23:33
 */
@Sharable
public class BonecrusherResponseEncoder extends ChannelOutboundHandlerAdapter {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    switch (msg) {
      case ResponseHeader header -> {
        ByteBuf headerBuf = ctx.alloc().buffer(header.getSerializedSize());
        try (ByteBufOutputStream outputStream = new ByteBufOutputStream(headerBuf)) {
          header.writeTo(outputStream);
        } catch (IOException e) {
          throw new EncoderException(e);
        }

        ByteBuf prefixBuf = ctx.alloc().buffer(Integer.BYTES * 3);
        prefixBuf
            .writeInt(RESPONSE.getMagic()) // 魔数
            .writeInt(prefixBuf.capacity() + headerBuf.readableBytes()) // 响应数据总长，单位：字节
            .writeInt(headerBuf.readableBytes()); // 响应头长度
        msg = ctx.alloc().compositeBuffer(2).addComponents(true, prefixBuf, headerBuf);
      }
      case CompositeByteBuf buf -> {
        ByteBuf prefixBuf = ctx.alloc().buffer(Integer.BYTES * 2);
        prefixBuf
            .writeInt(RESPONSE.getMagic()) // 魔数
            .writeInt(prefixBuf.capacity() + buf.readableBytes()); // 响应数据总长，单位：字节
        msg = buf.addComponent(true, 0, prefixBuf);
      }
      case null -> throw new IllegalArgumentException("msg must not be null or empty.");
      default -> {}
    }
    ctx.write(msg, promise);
  }
}
