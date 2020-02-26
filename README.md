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

## 4 References

[1]阮一峰.理解字节序[J/OL].阮一峰的网络日志,2016-11-22.

[2]猿码道.聊聊Linux 五种IO模型[J/OL].简书,2016.05.18.

[3]猿码道.聊聊同步、异步、阻塞与非阻塞[J/OL].简书,2016.05.18.

[4]科来网络.网络通讯协议[J/OL].科来网络,2019-01.01.

[5]此鱼不得水.Dubbo编码解码[J/OL].简书,2017-12-20.

[6]Newland.谈谈如何使用Netty开发实现高性能的RPC服务器[J/OL].博客园,2016-06-25.

[7]加多.谈谈Netty的线程模型[J/OL].并发编程网,2019-08-23.

[8]鲁小憨.怎样对 RPC 进行有效的性能测试[J/OL].简书,2018-02-02.

## License

EasyRpc is under the Apache 2.0 license. See the [LICENSE](https://github.com/Matrix6677/EasyRpc/blob/master/LICENSE) file for details.