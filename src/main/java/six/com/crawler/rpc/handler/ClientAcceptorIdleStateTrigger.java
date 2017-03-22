package six.com.crawler.rpc.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import six.com.crawler.rpc.protocol.HeartbeatMsg;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月22日 下午1:10:36
 */
@ChannelHandler.Sharable
public class ClientAcceptorIdleStateTrigger extends ChannelInboundHandlerAdapter {

	final static Logger log = LoggerFactory.getLogger(ClientAcceptorIdleStateTrigger.class);

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleState state = ((IdleStateEvent) evt).state();
			if (state == IdleState.WRITER_IDLE) {
				ctx.writeAndFlush(HeartbeatMsg.heartbeatMsg());
				String address = ctx.channel().remoteAddress().toString();
				log.info("send heartbeat msg to server[" + address + "]");
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

}
