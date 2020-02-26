# EasyRpc
EasyRpc is a simple, high-performance, easy-to-use RPC framework based on Netty, ZooKeeper and ProtoStuff.

------

## 1 Architecture

### 1.1 System timing

![系统时序](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/系统时序.png)

### 1.2 Packet structure

![数据包结构](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/数据包结构.png)

## 2 Features

- Easy to use
- Low latency based on Netty 4
- Non-blocking asynchronous/synchronous call support
- Service registration and discovery based on ZooKeeper
- Fast serialization and deserialization based on Protostuff
- 4 load balancing strategies support: random, round, hash, lowest latency
- Timeout or Exception handling

## 3 Getting started

### 3.1 Implement IMsgHandler interface for the provider

```java
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
```

### 3.2 Start service provider

```java
public class DemoServer {

  public static void main(String[] args) throws Throwable {
    RpcServer server = new RpcServer(zkAddr, topic, localIp, port);
    server.register(new HelloMsgHandler());
    System.err.println("服务器启动成功！");
  }
}
```

### 3.3 Call remote service in consumer

#### 3.3.1 Sync

```java
  void testSync() throws Throwable {
    RpcClient client = new RpcClient(zkAddr, topic);
    HelloReq helloReq = new HelloReq();
    helloReq.msg = "ping";
    HelloResp resp = client.send(client.randomNode(), MsgId.HELLO, helloReq, 3000);
    Assertions.assertEquals(resp.msg, "pong");
  }
```

#### 3.3.2 Async

```java
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
```

## License

EasyRpc is under the Apache 2.0 license. See the [LICENSE](https://github.com/Matrix6677/EasyRpc/blob/master/LICENSE) file for details.