package cn.ziav.rpc.handler;

import cn.ziav.rpc.server.IMsgHandler;

/** @author Zavi */
public class MultiThreadMsgHandler implements IMsgHandler<String, String> {

  @Override
  public String process(String o) throws Throwable {
    return "thread-" + o;
  }

  @Override
  public int msgId() {
    return MsgId.MULTI_THREAD;
  }
}
