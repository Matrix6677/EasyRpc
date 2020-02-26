package cn.ziav.rpc.handler;

import cn.ziav.rpc.server.IMsgHandler;

/** @author Zavi */
public class TimeoutMsgHandler implements IMsgHandler<String, String> {

  @Override
  public String process(String s) throws Throwable {
    Thread.sleep(2000L);
    return "pong";
  }

  @Override
  public int msgId() {
    return MsgId.TIMEOUT;
  }
}
