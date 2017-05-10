package six.com.crawler.rpc;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import six.com.crawler.rpc.protocol.RpcMsg;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月21日 上午9:08:36
 */
public abstract class NettyConnection extends SimpleChannelInboundHandler<RpcMsg> {

	final static Logger log = LoggerFactory.getLogger(NettyConnection.class);

	private String host;

	private int port;

	private volatile long lastActivityTime;

	private volatile ChannelHandlerContext ctx;
	// netty Channel
	private volatile Channel channel;

	private volatile AtomicInteger isHold = new AtomicInteger(0);

	NettyConnection(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public ChannelHandlerContext getContext() {
		return ctx;
	}

	public Channel getChannel() {
		return channel;
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
		this.channel = ctx.channel();
	}

	protected ChannelFuture writeAndFlush(RpcMsg t) {
		lastActivityTime = System.currentTimeMillis();
		return channel.writeAndFlush(t);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		doConnect();
	}

	protected abstract void doConnect();

	/**
	 * 是否可用
	 * 
	 * @return
	 */
	public boolean available() {
		return null != channel && channel.isActive();
	}

	public long getLastActivityTime() {
		return lastActivityTime;
	}
	
	public boolean isHold() {
		return isHold.get()==1;
	}

	public void hold() {
		isHold.set(1);
	}

	public void release() {
		isHold.set(0);
	}

	public void close() {
		if (null != ctx) {
			ctx.close();
		}
	}

	public void disconnect() {
		if (null != ctx) {
			ctx.disconnect();
		}
	}

	public String getConnectionKey() {
		return getNewConnectionKey(host, port);
	}

	public static String getNewConnectionKey(String host, int port) {
		String findKey = host + ":" + port;
		return findKey;
	}

}
