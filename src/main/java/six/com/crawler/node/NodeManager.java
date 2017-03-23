package six.com.crawler.node;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import six.com.crawler.configure.SpiderConfigure;
import six.com.crawler.dao.RedisManager;
import six.com.crawler.email.QQEmailClient;
import six.com.crawler.entity.Node;
import six.com.crawler.entity.NodeType;
import six.com.crawler.rpc.NettyRpcServer;
import six.com.crawler.rpc.protocol.RpcRequest;
import six.com.crawler.rpc.protocol.RpcResponse;
import six.com.crawler.rpc.NettyRpcCilent;
import six.com.crawler.utils.JavaSerializeUtils;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月13日 下午1:41:16 集群节点管理类 所有集群服务最基本的依赖
 */
@Component
public class NodeManager implements InitializingBean {

	final static Logger log = LoggerFactory.getLogger(NodeManager.class);

	public final int executeCommandRetryCount = 3;

	@Autowired
	private SpiderConfigure configure;

	@Autowired
	private RedisManager redisManager;

	@Autowired
	private QQEmailClient emailClient;

	// 当前节点
	private Node currentNode;

	private CuratorFramework zKClient;

	private NettyRpcServer nettyRpcServer;

	private NettyRpcCilent nettyRpcCilent;

	/**
	 * 分布式锁保证每个节点都是有序的初始化注册
	 */
	public void afterPropertiesSet() {
		// 初始化当前节点信息
		initCurrentNode();
		String lockKey = "cluster_manager_init";
		try {
			// 判断是否是单机模式
			if (getCurrentNode().getType() != NodeType.SINGLE) {
				redisManager.lock(lockKey);
				// 初始化zKClient
				initZKClient();
				// 初始化注册当前节点
				initRegisterNode();
			}
		} catch (Exception e) {
			log.error("init clusterManager err", e);
			System.exit(1);
		} finally {
			redisManager.unlock(lockKey);
		}
		/**
		 * 初始化 节点server和client 通信
		 */
		String localHost = getCurrentNode().getHost();
		int trafficPort = getCurrentNode().getTrafficPort();
		nettyRpcServer = new NettyRpcServer(localHost, trafficPort);
		nettyRpcCilent = new NettyRpcCilent();
		register("getCurrentNode", new NodeCommand() {
			@Override
			public Object execute(Object param) {
				return getCurrentNode();
			}
		});
	}

	protected void initZKClient() {
		String connectString = getConfigure().getConfig("zookeeper.host", null);
		// 检查 nodeName是否有注册过。如果注册过那么检查 ip 和端口是相同
		if (StringUtils.isBlank(connectString)) {
			log.error("the zookeeper's host is blank");
			System.exit(1);
		}
		zKClient = CuratorFrameworkFactory.newClient(connectString, new RetryPolicy() {
			@Override
			public boolean allowRetry(int arg0, long arg1, RetrySleeper arg2) {
				return false;
			}
		});
		zKClient.start();
		try {
			zKClient.blockUntilConnected();
		} catch (InterruptedException e) {
			log.error("connect zooKeeper[" + connectString + "] err", e);
			System.exit(1);
		}
		try {
			Stat stat = zKClient.checkExists().forPath(ZooKeeperPathUtils.getRootPath());
			if (null == stat) {
				zKClient.create().withMode(CreateMode.PERSISTENT).forPath(ZooKeeperPathUtils.getRootPath());
			}
			stat = zKClient.checkExists().forPath(ZooKeeperPathUtils.getMasterNodesPath());
			if (null == stat) {
				zKClient.create().withMode(CreateMode.PERSISTENT).forPath(ZooKeeperPathUtils.getMasterNodesPath());
			}

			stat = zKClient.checkExists().forPath(ZooKeeperPathUtils.getMasterStandbyNodesPath());
			if (null == stat) {
				zKClient.create().withMode(CreateMode.PERSISTENT)
						.forPath(ZooKeeperPathUtils.getMasterStandbyNodesPath());
			}
		} catch (Exception e) {
			log.error("init zooKeeper's persistent path err", e);
			System.exit(1);
		}
	}

	protected void initRegisterNode() {
		NodeRegisterEvent event = NodeRegisterEventFactory.createNodeRegisterEvent(currentNode);
		if (!event.register(this, zKClient)) {
			log.error("register node[" + currentNode.getName() + "] to zooKeeper failed");
			System.exit(1);
		}
	}

	private void initCurrentNode() {
		// 获取节点配置
		String host = getConfigure().getHost();
		int port = getConfigure().getPort();
		int trafficPort = getConfigure().getConfig("node.trafficPort", 8180);
		int runningJobMaxSize = getConfigure().getConfig("worker.running.max.size", 20);
		String nodeName = getConfigure().getConfig("node.name", null);
		// 检查节点名字是否设置
		if (StringUtils.isBlank(nodeName)) {
			log.error("don't set node's name ,please set it ,then to start");
			System.exit(1);
		}
		// 检查节点类型
		NodeType nodeType = getConfigure().getNodeType();
		currentNode = new Node();
		currentNode.setType(nodeType);
		currentNode.setName(nodeName);
		currentNode.setHost(host);
		currentNode.setPort(port);
		currentNode.setTrafficPort(trafficPort);
		currentNode.setRunningJobMaxSize(runningJobMaxSize);
		log.info("the node[" + nodeName + "] type:" + nodeType);
	}

	public Node getMasterNode() {
		Node masterNode = null;
		if (getCurrentNode().getType() != NodeType.SINGLE) {
			try {
				List<String> masterNodePaths = zKClient.getChildren().forPath(ZooKeeperPathUtils.getMasterNodesPath());
				if (null != masterNodePaths & masterNodePaths.size() == 1) {
					String masterNodePath = ZooKeeperPathUtils.getMasterNodePath(masterNodePaths.get(0));
					byte[] data = zKClient.getData().forPath(masterNodePath);
					masterNode = JavaSerializeUtils.unSerialize(data, Node.class);
				}
			} catch (Exception e) {
				log.error("", e);
			}
		} else {
			masterNode = getCurrentNode();
		}
		return masterNode;
	}

	public List<Node> getWorkerNodes() {
		List<Node> allNodes = new ArrayList<>();
		if (getCurrentNode().getType() != NodeType.SINGLE) {
			try {
				String workerNodesPath = ZooKeeperPathUtils.getWorkerNodesPath();
				List<String> workerNodePaths = zKClient.getChildren().forPath(workerNodesPath);
				for (String workerNodePath : workerNodePaths) {
					byte[] data = zKClient.getData().forPath(workerNodesPath + "/" + workerNodePath);
					Node workerNode = JavaSerializeUtils.unSerialize(data, Node.class);
					allNodes.add(workerNode);
				}
			} catch (Exception e) {
				log.error("", e);
			}
		} else {
			allNodes.add(currentNode);
		}
		return allNodes;
	}

	public Node getWorkerNode(String nodeName) {
		Node workerNode = null;
		if (getCurrentNode().getType() != NodeType.SINGLE) {
			try {
				byte[] data = zKClient.getData().forPath(ZooKeeperPathUtils.getWorkerNodePath(nodeName));
				workerNode = JavaSerializeUtils.unSerialize(data, Node.class);
			} catch (Exception e) {
				log.error("", e);
			}
		} else {
			workerNode = currentNode;
		}
		return workerNode;
	}

	public List<Node> getFreeWorkerNodes(int needFresNodes) {
		List<Node> freeNodes = new ArrayList<>(needFresNodes);
		List<Node> allWorkerNodes = getWorkerNodes();
		for (Node workerNode : allWorkerNodes) {
			if (workerNode.getType() != NodeType.MASTER) {
				Node newestNode = null;
				if (!getCurrentNode().equals(workerNode)) {
					newestNode = (Node) execute(workerNode, "getCurrentNode", null);
				} else {
					newestNode = getCurrentNode();
				}
				if (newestNode.getRunningJobSize() < newestNode.getRunningJobMaxSize()) {
					freeNodes.add(workerNode);
					if (freeNodes.size() >= needFresNodes) {
						break;
					}
				}
			}
		}
		return freeNodes;
	}

	public Object execute(Node node, String commandName, Object param) {
		String id = getCurrentNode().getHost() + "@" + node.getHost() + ":" + node.getTrafficPort() + "/" + commandName
				+ "/" + System.currentTimeMillis();
		RpcRequest rpcRequest = new RpcRequest();
		rpcRequest.setId(id);
		rpcRequest.setCommand(commandName);
		rpcRequest.setCallHost(node.getHost());
		rpcRequest.setCallPort(node.getTrafficPort());
		rpcRequest.setOriginHost(getCurrentNode().getHost());
		rpcRequest.setParam(param);
		RpcResponse rpcResponse = nettyRpcCilent.execute(rpcRequest);
		return rpcResponse.getResult();
	}

	public void register(String commandName, NodeCommand nodeCommand) {
		nettyRpcServer.register(commandName, nodeCommand);
		log.info("register nodeCommand:" + commandName);
	}

	public void remove(String commandName) {
		nettyRpcServer.remove(commandName);
		log.info("remove nodeCommand:" + commandName);
	}

	@PreDestroy
	public void destroy() {
		if (null != nettyRpcCilent) {
			nettyRpcCilent.destroy();
		}

		if (null != nettyRpcServer) {
			nettyRpcServer.destroy();
		}
		if (null != zKClient) {
			zKClient.close();
		}
	}

	public SpiderConfigure getConfigure() {
		return configure;
	}

	public void setConfigure(SpiderConfigure configure) {
		this.configure = configure;
	}

	public Node getCurrentNode() {
		return currentNode;
	}

	public RedisManager getRedisManager() {
		return redisManager;
	}

	public void setRedisManager(RedisManager redisManager) {
		this.redisManager = redisManager;
	}

	public QQEmailClient getEmailClient() {
		return emailClient;
	}

	public void setEmailClient(QQEmailClient emailClient) {
		this.emailClient = emailClient;
	}
}
