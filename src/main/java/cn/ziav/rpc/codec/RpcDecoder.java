package cn.ziav.rpc.codec;

import static cn.ziav.rpc.common.Constants.FLAG_REQUEST;
import static cn.ziav.rpc.common.Constants.FLAG_TWOWAY;
import static cn.ziav.rpc.common.Constants.HEADER_LENGTH;
import static cn.ziav.rpc.common.Constants.MAGIC_HIGH;
import static cn.ziav.rpc.common.Constants.MAGIC_LOW;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import cn.ziav.rpc.common.Constants;
import cn.ziav.rpc.common.MsgRequest;
import cn.ziav.rpc.common.MsgResponse;
import cn.ziav.rpc.utils.Bytes;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.protostuff.GraphIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * 协议解码器
 *
 * @author Zavi
 */
public class RpcDecoder extends LengthFieldBasedFrameDecoder {

  public RpcDecoder() {
    // 这里使用小端字节序，效率更高
    super(LITTLE_ENDIAN, Constants.DEFAULT_PAYLOAD, 0, 4, 0, 4, true);
  }

  @Override
  public Object decode(ChannelHandlerContext ctx, ByteBuf input) throws Exception {
    input = (ByteBuf) super.decode(ctx, input);
    if (input == null) {
      return null;
    }

    int readable = input.readableBytes();

    // 创建消息头字节数组
    byte[] header = new byte[Math.min(readable, HEADER_LENGTH)];
    // 读取消息头数据
    input.readBytes(header);

    // 检查魔数是否相等，不相等说明请求非法，直接断联
    if (readable > 0 && header[0] != MAGIC_HIGH || readable > 1 && header[1] != MAGIC_LOW) {
      ctx.disconnect();
      return null;
    }

    // 检测可读数据量是否少于消息头长度，若小于则立即返回
    if (readable < HEADER_LENGTH) {
      return null;
    }

    // 从消息头中获取消息体长度
    int len = Bytes.bytes2int(header, 16);
    int tt = len + HEADER_LENGTH;
    // 检查数据大小是否超限
    Preconditions.checkArgument(
        tt <= Constants.DEFAULT_PAYLOAD, "数据大小超过上限%sM", Constants.DEFAULT_PAYLOAD / 1024 / 1024);

    // 获取协议头的数据包类型、请求编号、业务编号
    byte flag = header[2];
    long id = Bytes.bytes2long(header, 4);
    int mId = Bytes.bytes2int(header, 12);
    // 通过逻辑与运算判断是否为请求消息
    if ((flag & FLAG_REQUEST) != 0) {
      // 创建MsgRequest对象
      MsgRequest req = new MsgRequest(id, mId);
      // 通过逻辑与运算得到通信方式，并设置到 Request 对象中
      req.mTwoWay = (flag & FLAG_TWOWAY) != 0;

      // 通过ProtoStuff反序列化请求体
      Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
      Wrapper wrapper = schema.newMessage();

      byte[] bytes = new byte[len];
      input.readBytes(bytes);
      GraphIOUtil.mergeFrom(bytes, wrapper, schema);
      req.mData = wrapper.getData();
      // 写入到ByteBuf缓冲区
      //      out.add(req);
      return req;
    } else {
      // 创建MsgResponse对象
      MsgResponse resp = new MsgResponse(id, mId);
      // 设置对象状态
      resp.mStatus = header[3];
      // 只要消息体长度>1就进行反序列化
      if (len > 0) {
        // 反序列化响应对象的具体内容
        Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
        byte[] bytes = new byte[len];
        input.readBytes(bytes);
        Wrapper wrapper = schema.newMessage();
        GraphIOUtil.mergeFrom(bytes, wrapper, schema);
        resp.mData = wrapper.getData();
      }
      // 写入到ByteBuf缓冲区
      //      out.add(resp);
      return resp;
    }
  }
}
