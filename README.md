# EasyRpc

[![Build Status](https://github.com/Matrix6677/EasyRpc/workflows/Java%20CI/badge.svg)](https://github.com/Matrix6677/EasyRpc/actions) ![license](https://img.shields.io/github/license/alibaba/dubbo.svg)

EasyRpc是基于Netty、ZooKeeper和ProtoStuff开发的一个简单易用，便于学习的RPC框架。

------

## 1 特性

- 简单易用；
- 注释完善，方便学习；
- 低延迟，基于Netty 4；
- 解决TCP粘包/拆包问题；
- 支持非阻塞的同步/异步调用；
- 基于ProtoStuff的对象序列化；
- 完整的单元测试和JMH性能压测；
- 基于ZooKeeper实现的服务注册和发现；
- 仿Dubbo数据包结构，优化协议头仅20字节；
- 支持4种负载均衡策略：随机、轮询、哈希、最佳响应；

## 2 总体设计

### 2.1 架构图

![系统架构](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/doc/%E7%B3%BB%E7%BB%9F%E6%9E%B6%E6%9E%84.png)

### 2.2 系统时序

![系统时序](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/doc/%E7%B3%BB%E7%BB%9F%E6%97%B6%E5%BA%8F.png)

### 2.3 主流程图

<img src="https://github.com/Matrix6677/EasyRpc/blob/master/doc/%E4%B8%BB%E6%B5%81%E7%A8%8B.png?raw=true" alt="主流程" style="zoom:67%;" />

### 2.4 数据包结构

![数据包结构](https://raw.githubusercontent.com/Matrix6677/EasyRpc/master/doc/%E6%95%B0%E6%8D%AE%E5%8C%85%E7%BB%93%E6%9E%84.png)

## 3 性能测试

![吞吐量](https://github.com/Matrix6677/EasyRpc/blob/master/doc/%E5%90%9E%E5%90%90%E9%87%8F.png?raw=true)

![平均耗时&随机取样](https://github.com/Matrix6677/EasyRpc/blob/master/doc/%E5%B9%B3%E5%9D%87%E8%80%97%E6%97%B6&%E9%9A%8F%E6%9C%BA%E5%8F%96%E6%A0%B7.png?raw=true)

## 4 参考文献

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