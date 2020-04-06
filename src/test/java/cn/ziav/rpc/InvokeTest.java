package cn.ziav.rpc;

import static cn.ziav.rpc.Constant.localIp;
import static cn.ziav.rpc.Constant.port;
import static cn.ziav.rpc.Constant.topic;
import static cn.ziav.rpc.Constant.zkAddr;
import static cn.ziav.rpc.exception.ExceptionCode.TIME_OUT;

import cn.ziav.rpc.bean.HelloReq;
import cn.ziav.rpc.bean.HelloResp;
import cn.ziav.rpc.client.EasyRpcCallback;
import cn.ziav.rpc.client.RpcClient;
import cn.ziav.rpc.exception.RemotingException;
import cn.ziav.rpc.handler.ExceptionMsgHandler;
import cn.ziav.rpc.handler.HelloMsgHandler;
import cn.ziav.rpc.handler.MsgId;
import cn.ziav.rpc.handler.MultiThreadMsgHandler;
import cn.ziav.rpc.handler.TimeoutMsgHandler;
import cn.ziav.rpc.server.RpcServer;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InvokeTest {

  @BeforeAll
  static void initServer() throws Throwable {
    RpcServer server = new RpcServer(zkAddr, topic, localIp, port);
    server.register(new HelloMsgHandler());
    server.register(new TimeoutMsgHandler());
    server.register(new ExceptionMsgHandler());
    server.register(new MultiThreadMsgHandler());
  }

  @Test
  void testSync() throws Throwable {
    RpcClient client = new RpcClient(zkAddr, topic);
    HelloReq helloReq = new HelloReq();
    helloReq.msg = "ping";
    HelloResp resp = client.send(client.randomNode(), MsgId.HELLO, helloReq, 3000);
    Assertions.assertEquals(resp.msg, "pong");
  }

  @Test
  void testAsync() throws Throwable {
    RpcClient client = new RpcClient(zkAddr, topic);
    HelloReq helloReq = new HelloReq();
    helloReq.msg = "ping";
    CountDownLatch latch = new CountDownLatch(1);
    client.sendAsync(
        client.randomNode(),
        MsgId.HELLO,
        helloReq,
        3000,
        new EasyRpcCallback<HelloResp>() {
          @Override
          public void success(HelloResp result) {
            Assertions.assertEquals(result.msg, "pong");
            latch.countDown();
          }

          @Override
          public void fail(Throwable throwable) {}
        });
    latch.await();
  }

  @Test
  void testTimeout() throws Throwable {
    RpcClient client = new RpcClient(zkAddr, topic);
    RemotingException remotingException =
        Assertions.assertThrows(
            RemotingException.class,
            () -> client.send(client.randomNode(), MsgId.TIMEOUT, "ping", 50));
    Assertions.assertEquals(TIME_OUT, remotingException.code);
  }

  @Test
  void testException() throws Throwable {
    RpcClient client = new RpcClient(zkAddr, topic);
    CountDownLatch latch = new CountDownLatch(1);
    client.sendAsync(
        client.randomNode(),
        MsgId.EXCEPTION,
        "",
        3000,
        new EasyRpcCallback<HelloResp>() {
          @Override
          public void success(HelloResp result) {}

          @Override
          public void fail(Throwable throwable) {
            Assertions.assertTrue(throwable instanceof IllegalStateException);
            latch.countDown();
          }
        });
    latch.await();
  }

  @Test
  void multiThreadTest() throws Throwable {
    CountDownLatch latch = new CountDownLatch(5);
    RpcClient client = new RpcClient(zkAddr, topic);

    for (int i = 0; i < 5; i++) {
      int threadId = i;
      new Thread(
              () -> {
                try {
                  String result =
                      client.send(client.lowLatencyNode(), MsgId.MULTI_THREAD, threadId + "", 3000);
                  Assertions.assertEquals(result, "thread-" + threadId);
                  latch.countDown();
                } catch (Throwable throwable) {
                  System.err.println(throwable);
                }
              })
          .start();
    }
    latch.await();
  }
}
