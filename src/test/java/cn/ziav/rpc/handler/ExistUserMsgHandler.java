package cn.ziav.rpc.handler;

import cn.ziav.rpc.server.IMsgHandler;

/** @author Zavi */
public class ExistUserMsgHandler implements IMsgHandler<String, Boolean> {

  @Override
  public Boolean process(String email) throws Throwable {
    if (email == null || email.isEmpty()) {
      return true;
    }

    if (email.charAt(email.length() - 1) < '5') {
      return false;
    }

    return true;
  }

  @Override
  public int msgId() {
    return MsgId.EXIST_USER;
  }
}
