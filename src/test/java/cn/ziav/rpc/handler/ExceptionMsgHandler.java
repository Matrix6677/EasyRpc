package cn.ziav.rpc.handler;

import cn.ziav.rpc.server.IMsgHandler;

/** @author Zavi */
public class ExceptionMsgHandler implements IMsgHandler<String, String> {

  @Override
  public String process(String s) throws Throwable {
    throw new IllegalStateException("fdsew");
  }

  @Override
  public int msgId() {
    return MsgId.EXCEPTION;
  }
}
