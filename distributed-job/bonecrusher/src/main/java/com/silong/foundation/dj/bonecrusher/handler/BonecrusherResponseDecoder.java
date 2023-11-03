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

import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.lambda.Tuple2;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCountUtil;

/**
 * 响应消息解码器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-02 23:33
 */
@Sharable
public class BonecrusherResponseDecoder extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    switch (msg) {
      case ByteBuf byteBuf -> {
        // 检测消息魔数，如果不是响应消息则转给后续的handler处理
        int magic = byteBuf.markReaderIndex().readInt();
        if (magic != RESPONSE.getMagic()) {
          ctx.fireChannelRead(byteBuf.resetReaderIndex());
          break;
        }

        boolean release = true;

        try {
          int totalSize = byteBuf.readInt();
          int headerSize = byteBuf.readInt();

          byte[] array;
          int offset;
          if (byteBuf.hasArray()) {
            array = byteBuf.array();
            offset = byteBuf.arrayOffset() + byteBuf.readerIndex();
          } else {
            array = ByteBufUtil.getBytes(byteBuf, byteBuf.readerIndex(), headerSize, false);
            offset = 0;
          }

          Messages.ResponseHeader responseHeader =
              Messages.ResponseHeader.parser().parseFrom(array, offset, headerSize);
          ByteBuf dataBlock = EMPTY_BUFFER;
          byteBuf.skipBytes(headerSize);

          // 如果响应携带数据块则提取数据块
          if (!responseHeader.hasResult()) {
            checkDataBlockSize(totalSize, headerSize, byteBuf);
            release = false;
            dataBlock = byteBuf.slice();
          }

          msg =
              Tuple2.<Messages.ResponseHeader, ByteBuf>builder()
                  .t1(responseHeader)
                  .t2(dataBlock)
                  .build();
        } finally {
          if (release) {
            ReferenceCountUtil.release(byteBuf);
          }
        }
      }
      case null -> throw new IllegalArgumentException("msg must not be null or empty.");
      default -> {}
    }
    ctx.fireChannelRead(msg);
  }

  private void checkDataBlockSize(int totalSize, int headerSize, ByteBuf byteBuf) {
    int dataBlockSize = totalSize - headerSize - Integer.BYTES * 3;
    if (dataBlockSize != byteBuf.readableBytes()) {
      throw new DecoderException(
          String.format(
              "The received data is incomplete and cannot be parsed normally. TotalSize:%d, HeaderSize:%d, DataBlockSize:%d.",
              totalSize, headerSize, byteBuf.readableBytes()));
    }
  }
}
