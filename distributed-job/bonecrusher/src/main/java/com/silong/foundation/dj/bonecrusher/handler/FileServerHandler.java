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

import static com.silong.foundation.dj.bonecrusher.message.ErrorCode.CLASS_NOT_FOUND;
import static com.silong.foundation.dj.bonecrusher.message.ErrorCode.LOADING_CLASS_SUCCESSFUL;
import static com.silong.foundation.dj.bonecrusher.message.Messages.Type.LOADING_CLASS_RESP;

import com.google.protobuf.ByteString;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
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
public class FileServerHandler extends SimpleChannelInboundHandler<Messages.Request> {

  /** 文件保存目录 */
  private final Path dataStorePath;

  /**
   * 构造方法
   *
   * @param dataStorePath 数据存储目录
   */
  public FileServerHandler(@NonNull Path dataStorePath) {
    super(false);
    this.dataStorePath = dataStorePath;
  }

  /** 导出分区数据 */
  private Path exportPartition2Files() {
    // TODO 分区数据导出待实现
    return SystemUtils.getJavaIoTmpDir().toPath();
  }

  private void handleSyncDataReq(ChannelHandlerContext ctx, Messages.SyncDataReq request) {

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
  public void channelRead0(ChannelHandlerContext ctx, Messages.Request request) throws Exception {
    switch (request.getType()) {
      case DATA_SYNC_REQ -> handleSyncDataReq(ctx, request.getSyncData());
      case LOADING_CLASS_REQ -> handleLoadingClassReq(ctx, request.getLoadingClass());
    }
  }

  private void handleLoadingClassReq(ChannelHandlerContext ctx, Messages.LoadingClassReq request)
      throws IOException {
    String classFqdn = request.getClassFqdn();
    try (InputStream inputStream = getClass().getResourceAsStream(fqdn2Path(classFqdn))) {
      if (inputStream == null) {
        ctx.writeAndFlush(
            Unpooled.wrappedBuffer(
                Messages.Response.newBuilder()
                    .setLoadingClass(
                        Messages.LoadingClassResp.newBuilder()
                            .setResult(
                                Messages.Result.newBuilder()
                                    .setCode(CLASS_NOT_FOUND.getCode())
                                    .setDesc(String.format(CLASS_NOT_FOUND.getDesc(), classFqdn))))
                    .setType(LOADING_CLASS_RESP)
                    .build()
                    .toByteArray()));
        return;
      }

      ctx.writeAndFlush(
          Unpooled.wrappedBuffer(
              Messages.Response.newBuilder()
                  .setLoadingClass(
                      Messages.LoadingClassResp.newBuilder()
                          .setDataBlock(
                              Messages.DataBlock.newBuilder()
                                  .setData(ByteString.readFrom(inputStream)))
                          .setResult(
                              Messages.Result.newBuilder()
                                  .setCode(LOADING_CLASS_SUCCESSFUL.getCode())
                                  .setDesc(
                                      String.format(
                                          LOADING_CLASS_SUCCESSFUL.getDesc(), classFqdn))))
                  .setType(LOADING_CLASS_RESP)
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
