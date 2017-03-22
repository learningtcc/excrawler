package six.com.crawler.rpc.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import six.com.crawler.utils.JavaSerializeUtils;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月20日 下午3:18:19
 */
public class RpcEncoder extends MessageToByteEncoder<RpcMsg> implements RpcProtocol {

	final static Logger log = LoggerFactory.getLogger(RpcEncoder.class);

	@Override
	protected void encode(ChannelHandlerContext ctx, RpcMsg msg, ByteBuf out) throws Exception {
		byte msgType = msg.getType();
		byte[] body = convertToBytes(msg); // 将对象转换为byte
		int dataLength = body.length; // 读取消息的长度
		out.writeByte(msgType);// 写入消息类型
		out.writeInt(dataLength); // 先将消息长度写入，也就是消息头
		out.writeBytes(body); // 消息体中包含我们要发送的数据
	}

	private byte[] convertToBytes(RpcMsg msg) {
		byte[] data = JavaSerializeUtils.serialize(msg);
		return data;
	}
}
