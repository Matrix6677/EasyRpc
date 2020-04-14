# EasyRpc

[![Build Status](https://github.com/Matrix6677/EasyRpc/workflows/Java%20CI/badge.svg)](https://github.com/Matrix6677/EasyRpc/actions) ![license](https://img.shields.io/github/license/alibaba/dubbo.svg)

EasyRpc is a simple, high-performance, easy-to-use RPC framework based on Netty, ZooKeeper and ProtoStuff.

------

## 1 Features

- Easy and simple to use
- Low latency based on Netty 4
- Non-blocking asynchronous/synchronous call support
- Service registration and discovery based on ZooKeeper
- Fast serialization and deserialization based on Protostuff
- 4 load balancing strategies support: random, round, hash, lowest latency
- Timeout or Exception handling

## 2 Overall Design

### 2.1 Architecture

![系统架构](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/%E7%B3%BB%E7%BB%9F%E6%9E%B6%E6%9E%84.png)

### 2.2 System Timing

![系统时序](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/系统时序.png)

### 2.3 Main Flow

<img src="https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/%E4%B8%BB%E6%B5%81%E7%A8%8B.png" alt="主流程" style="zoom:67%;" />

### 2.4 Packet structure

![数据包结构](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/数据包结构.png)

## 3 Performance Test

![吞吐量](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/%E5%90%9E%E5%90%90%E9%87%8F.png)

![平均耗时&随机取样](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/%E5%B9%B3%E5%9D%87%E8%80%97%E6%97%B6%26%E9%9A%8F%E6%9C%BA%E5%8F%96%E6%A0%B7.png)

## 4 Getting started

### 4.1 Implement IMsgHandler interface for the provider

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

### 4.2 Start service provider

```java
public class DemoServer {

  public static void main(String[] args) throws Throwable {
    RpcServer server = new RpcServer(zkAddr, topic, localIp, port);
    server.register(new HelloMsgHandler());
    System.err.println("服务器启动成功！");
  }
}
```

### 4.3 Call remote service in consumer

#### 4.3.1 Sync

```java
  void testSync() throws Throwable {
    RpcClient client = new RpcClient(zkAddr, topic);
    HelloReq helloReq = new HelloReq();
    helloReq.msg = "ping";
    HelloResp resp = client.send(client.randomNode(), MsgId.HELLO, helloReq, 3000);
    Assertions.assertEquals(resp.msg, "pong");
  }
```

#### 4.3.2 Async

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

## 5 License

EasyRpc is under the Apache 2.0 license. See the [LICENSE](https://github.com/Matrix6677/EasyRpc/blob/master/LICENSE) file for details.