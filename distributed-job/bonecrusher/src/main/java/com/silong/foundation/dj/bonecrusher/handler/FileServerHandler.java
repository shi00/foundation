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

import static com.silong.foundation.dj.bonecrusher.message.ErrorCode.*;
import static com.silong.foundation.dj.bonecrusher.message.Messages.Type.LOADING_CLASS_RESP;

import com.google.protobuf.ByteString;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.dj.bonecrusher.message.Messages.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.udt.nio.NioUdtProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * 文件数据保存处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-20 22:13
 */
@Slf4j
@Sharable
public class FileServerHandler extends ChannelInboundHandlerAdapter {

  /** 文件保存目录 */
  private final Path dataStorePath;

  /**
   * 构造方法
   *
   * @param dataStorePath 数据存储目录
   */
  public FileServerHandler(@NonNull Path dataStorePath) {
    this.dataStorePath = dataStorePath;
  }

  /** 导出分区数据 */
  private Path exportPartition2Files() {
    // TODO 分区数据导出待实现
    return SystemUtils.getJavaIoTmpDir().toPath();
  }

  private void handleSyncDataReq(ChannelHandlerContext ctx, SyncDataReq request) {

    //    RandomAccessFile raf = null;
    //    long length = -1;
    //    try {
    //      raf = new RandomAccessFile(msg, "r");
    //      length = raf.length();
    //    } catch (Exception e) {
    //      ctx.writeAndFlush("ERR: " + e.getClass().getSimpleName() + ": " + e.getMessage() +
    // '\n');
    //      return;
    //    } finally {
    //      if (length < 0 && raf != null) {
    //        raf.close();
    //      }
    //    }
    //
    //    ctx.write("OK: " + raf.length() + '\n');
    //    if (ctx.pipeline().get(SslHandler.class) == null) {
    //      // 传输文件使用了 DefaultFileRegion 进行写入到 NioSocketChannel 中
    //      ctx.write(new DefaultFileRegion(raf.getChannel(), 0, length));
    //    } else {
    //      // SSL enabled - cannot use zero-copy file transfer.
    //      ctx.write(new ChunkedFile(raf));
    //    }
    //    ctx.writeAndFlush("\n");
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Request request = (Request) msg;
    switch (request.getType()) {
      case DATA_SYNC_REQ -> handleSyncDataReq(ctx, request.getSyncData());
      case LOADING_CLASS_REQ -> handleLoadingClassReq(ctx, request.getLoadingClass());
    }
  }

  private void handleLoadingClassReq(ChannelHandlerContext ctx, LoadingClassReq request)
      throws IOException {
    String classFqdn = request.getClassFqdn();
    try (InputStream inputStream = getClass().getResourceAsStream(fqdn2Path(classFqdn))) {
      if (inputStream == null) {
        ctx.writeAndFlush(
            Unpooled.wrappedBuffer(
                Response.newBuilder()
                    .setType(LOADING_CLASS_RESP)
                    .setResult(
                        Result.newBuilder()
                            .setCode(CLASS_NOT_FOUND.getCode())
                            .setDesc(CLASS_NOT_FOUND.getDesc()))
                    .build()
                    .toByteArray()));
        return;
      }

      ctx.writeAndFlush(
          Unpooled.wrappedBuffer(
              Response.newBuilder()
                  .setType(LOADING_CLASS_RESP)
                  .setResult(Result.newBuilder().setCode(SUCCESS.getCode()))
                  .setDataBlockArray(
                      DataBlockArray.newBuilder()
                          .addDataBlock(
                              Messages.DataBlock.newBuilder()
                                  .setData(ByteString.readFrom(inputStream))))
                  .build()
                  .toByteArray()));

      log.info("transfer class: {}", classFqdn);
    }
  }

  private String fqdn2Path(String classFqdn) {
    return "/" + StringUtils.replaceChars(classFqdn, '.', '/') + ".class";
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, Throwable cause) {
    log.error(
        "An exception occurs that causes the channel to be closed. {}",
        NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
    ctx.close();
  }
}
