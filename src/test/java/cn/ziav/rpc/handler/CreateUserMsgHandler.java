package cn.ziav.rpc.handler;

import cn.ziav.rpc.bean.User;
import cn.ziav.rpc.server.IMsgHandler;

/** @author Zavi */
public class CreateUserMsgHandler implements IMsgHandler<User, Boolean> {

  @Override
  public Boolean process(User user) throws Throwable {
    if (user == null) {
      return false;
    }

    return true;
  }

  @Override
  public int msgId() {
    return MsgId.CREATE_USER;
  }
}
