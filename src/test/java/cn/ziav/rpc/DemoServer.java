package cn.ziav.rpc;

import static cn.ziav.rpc.BenchmarkTest.localIp;
import static cn.ziav.rpc.BenchmarkTest.port;
import static cn.ziav.rpc.BenchmarkTest.topic;
import static cn.ziav.rpc.BenchmarkTest.zkAddr;

import cn.ziav.rpc.handler.CreateUserMsgHandler;
import cn.ziav.rpc.handler.ExistUserMsgHandler;
import cn.ziav.rpc.handler.GetUserMsgHandler;
import cn.ziav.rpc.handler.ListUserMsgHandler;
import cn.ziav.rpc.server.RpcServer;

/** @author Zavi */
public class DemoServer {

  public static void main(String[] args) throws Throwable {
    RpcServer server = new RpcServer(zkAddr, topic, localIp, port);
    server.register(new ExistUserMsgHandler());
    server.register(new GetUserMsgHandler());
    server.register(new ListUserMsgHandler());
    server.register(new CreateUserMsgHandler());
    System.err.println("服务器启动成功！");
  }
}
