package cn.ziav.rpc;

import static cn.ziav.rpc.Constant.localIp;
import static cn.ziav.rpc.Constant.port;
import static cn.ziav.rpc.Constant.topic;
import static cn.ziav.rpc.Constant.zkAddr;

import cn.ziav.rpc.handler.CreateUserMsgHandler;
import cn.ziav.rpc.handler.ExistUserMsgHandler;
import cn.ziav.rpc.handler.GetUserMsgHandler;
import cn.ziav.rpc.handler.ListUserMsgHandler;
import cn.ziav.rpc.server.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zavi
 */
public class DemoServer {

  static Logger logger = LoggerFactory.getLogger(DemoServer.class);

  public static void main(String[] args) throws Throwable {
    RpcServer server = new RpcServer(zkAddr, topic, localIp, port);
    server.register(new ExistUserMsgHandler());
    server.register(new GetUserMsgHandler());
    server.register(new ListUserMsgHandler());
    server.register(new CreateUserMsgHandler());
    logger.info("服务器启动成功！");
  }
}
