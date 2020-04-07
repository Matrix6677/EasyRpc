/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ziav.rpc.server;

import static cn.ziav.rpc.common.MsgResponse.SERVER_ERROR;
import static com.google.common.base.Preconditions.checkArgument;

import cn.ziav.rpc.common.MsgRequest;
import cn.ziav.rpc.common.MsgResponse;
import cn.ziav.rpc.exception.ExceptionCode;
import cn.ziav.rpc.exception.RemotingException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 消息分发器 */
@io.netty.channel.ChannelHandler.Sharable
public class RpcDispatcher extends ChannelDuplexHandler {
  private static final Logger logger = LoggerFactory.getLogger(RpcDispatcher.class);
  /** 消息处理器：<msgId,handler> */
  private final Map<Integer, IMsgHandler> msgHandlerMap = new HashMap<>();

  /** <ip:port,channel> */
  private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

  public void register(IMsgHandler handler) {
    IMsgHandler pre = msgHandlerMap.putIfAbsent(handler.msgId(), handler);
    checkArgument(pre == null, "重复的消息处理器，msgId=%s", handler.msgId());
    logger.info("消息处理器{}注册成功", handler.msgId());
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    String key = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
    channelMap.put(key, ctx.channel());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    String key = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
    channelMap.remove(key);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof MsgRequest) {
      MsgRequest msgReq = (MsgRequest) msg;
      // 根据mId获取Handler
      IMsgHandler msgHandler = msgHandlerMap.get(msgReq.mId);
      MsgResponse<Object> msgResponse = new MsgResponse<>(msgReq.id, msgReq.mId);
      if (msgHandler == null) {
        logger.warn("no msg handler found, mid={}", msgReq.mId);
        msgResponse.mStatus = SERVER_ERROR;
        msgResponse.mData =
            new RemotingException(ExceptionCode.NO_SUCH_HANDLER, "mid=" + msgReq.mId);
        ctx.writeAndFlush(msgResponse);
        return;
      }

      // 在Handler的线程池中，执行process方法
      msgHandler
          .threadPool()
          .submit(
              () -> {
                try {
                  msgResponse.mData = msgHandler.process(msgReq.mData);
                } catch (Throwable throwable) {
                  logger.error("", throwable);
                  msgResponse.mStatus = SERVER_ERROR;
                  msgResponse.mData = throwable;
                }
                // 写入缓冲区
                ctx.writeAndFlush(msgResponse);
              });
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
      String key = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
      Channel channel = channelMap.getOrDefault(key, ctx.channel());
      channel.close();
      channelMap.remove(key);
    }
    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.error("", cause);
  }

  public Map<String, Channel> getChannels() {
    return channelMap;
  }
}
