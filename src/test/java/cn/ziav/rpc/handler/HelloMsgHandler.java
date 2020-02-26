package cn.ziav.rpc.handler;

import cn.ziav.rpc.bean.HelloReq;
import cn.ziav.rpc.bean.HelloResp;
import cn.ziav.rpc.server.IMsgHandler;

/** @author Zavi */
public class HelloMsgHandler implements IMsgHandler<HelloReq, HelloResp> {

  @Override
  public HelloResp process(HelloReq helloReq) throws InterruptedException {
    HelloResp helloResp = new HelloResp();
    helloResp.msg = "pong";
    return helloResp;
  }

  @Override
  public int msgId() {
    return MsgId.HELLO;
  }
}
