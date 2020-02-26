package cn.ziav.rpc.codec;

import static cn.ziav.rpc.common.Constants.FLAG_REQUEST;
import static cn.ziav.rpc.common.Constants.FLAG_TWOWAY;
import static cn.ziav.rpc.common.Constants.HEADER_LENGTH;
import static cn.ziav.rpc.common.Constants.MAGIC;
import static com.google.common.base.Preconditions.checkArgument;

import cn.ziav.rpc.common.Constants;
import cn.ziav.rpc.common.MsgRequest;
import cn.ziav.rpc.common.MsgResponse;
import cn.ziav.rpc.utils.Bytes;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.protostuff.GraphIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * 协议编码
 *
 * @author Zavi
 */
@Sharable
public class RpcEncoder extends MessageToByteEncoder {

  @Override
  protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
    if (msg instanceof MsgRequest) {
      // 创建消息头字节数组，长度20个字节
      byte[] header = new byte[HEADER_LENGTH];
      // 设置魔数
      Bytes.short2bytes(MAGIC, header);

      // 设置数据包类型（Request/Response）
      header[2] = FLAG_REQUEST;

      // 设置通信方式(单向/双向)
      if (((MsgRequest) msg).mTwoWay) {
        header[2] |= FLAG_TWOWAY;
      }

      // 设置请求编号，8个字节，从第4个字节开始设置
      Bytes.long2bytes(((MsgRequest) msg).id, header, 4);
      // 设置业务消息编号，4个字节，从第12个字节开始设置
      Bytes.int2bytes(((MsgRequest) msg).mId, header, 12);

      // 分配序列化所需的Buffer，默认大小512
      LinkedBuffer buffer = LinkedBuffer.allocate();
      Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
      // 将请求体用Wrapper包装
      Wrapper<?> wrapper = new Wrapper<>(((MsgRequest) msg).mData);
      try {
        // 通过ProtoStuff序列化wrapper对象
        byte[] bytes = GraphIOUtil.toByteArray(wrapper, schema, buffer);
        // 检查最终发起请求的数据包大小是否超过上限
        int tt = bytes.length + HEADER_LENGTH;
        Preconditions.checkArgument(
            tt <= Constants.DEFAULT_PAYLOAD,
            "数据大小超过上限%sM",
            Constants.DEFAULT_PAYLOAD / 1024 / 1024);
        // 设置请求体长度，4个字节，从第16个字节开始设置
        Bytes.int2bytes(bytes.length, header, 16);

        // 写入数据缓冲区
        out.writeIntLE(tt);
        out.writeBytes(header);
        out.writeBytes(bytes);
      } finally {
        // 清除序列化buffer
        buffer.clear();
      }
    }

    if (msg instanceof MsgResponse) {
      // 创建消息头字节数组，长度20个字节
      byte[] header = new byte[HEADER_LENGTH];
      // 设置魔数
      Bytes.short2bytes(MAGIC, header);
      // 设置响应状态码，长度1个字节，从第3个字节开始设置
      byte status = ((MsgResponse) msg).mStatus;
      header[3] = status;
      // 设置请求编号，8个字节，从第4个字节开始设置
      Bytes.long2bytes(((MsgResponse) msg).id, header, 4);
      // 设置业务消息编号，4个字节，从第12个字节开始设置
      Bytes.int2bytes(((MsgResponse) msg).mId, header, 12);

      // 分配序列化所需的Buffer，默认大小512
      LinkedBuffer buffer = LinkedBuffer.allocate();
      Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
      // 将请求体用Wrapper包装
      Wrapper<?> wrapper = new Wrapper<>(((MsgResponse) msg).mData);
      try {
        // 通过ProtoStuff序列化wrapper对象
        byte[] bytes = GraphIOUtil.toByteArray(wrapper, schema, buffer);
        // 检查最终发起请求的数据包大小是否超过上限
        int tt = bytes.length + HEADER_LENGTH;
        checkArgument(
            tt <= Constants.DEFAULT_PAYLOAD,
            "数据大小超过上限%sM",
            Constants.DEFAULT_PAYLOAD / 1024 / 1024);
        // 设置请求体长度，4个字节，从第16个字节开始设置
        Bytes.int2bytes(bytes.length, header, 16);
        // 写入数据缓冲区
        out.writeIntLE(tt);
        out.writeBytes(header);
        out.writeBytes(bytes);
      } finally {
        // 清除序列化buffer
        buffer.clear();
      }
    }
  }
}
