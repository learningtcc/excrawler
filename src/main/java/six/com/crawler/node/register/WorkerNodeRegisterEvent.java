package six.com.crawler.node.register;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import six.com.crawler.node.Node;
import six.com.crawler.node.NodeManager;
import six.com.crawler.node.ZooKeeperPathUtils;
import six.com.crawler.utils.JavaSerializeUtils;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月16日 下午12:05:52
 */
public class WorkerNodeRegisterEvent extends NodeRegisterEvent {

	final static Logger log = LoggerFactory.getLogger(WorkerNodeRegisterEvent.class);

	public WorkerNodeRegisterEvent(Node currentNode) {
		super(currentNode);
	}

	@Override
	public boolean register(NodeManager clusterManager, CuratorFramework zKClient) {
		try {
			Node masterNode = clusterManager.getMasterNode();
			if (null != masterNode) {
				byte[] data = JavaSerializeUtils.serialize(getCurrentNode());
				zKClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
						.forPath(ZooKeeperPathUtils.getWorkerNodePath(getCurrentNode().getName()), data);
				zKClient.checkExists().usingWatcher(this).forPath(ZooKeeperPathUtils.getMasterNodesPath());
				return true;
			} else {
				log.error("please first start the masterNode");
				return false;
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return false;
	}

	@Override
	public void unRegister(NodeManager clusterManager, CuratorFramework zKClient) {
		try {
			zKClient.delete().forPath(ZooKeeperPathUtils.getWorkerNodePath(getCurrentNode().getName()));
		} catch (Exception e) {
			log.error("", e);
		}
	}
	
	@Override
	public void process(WatchedEvent arg0) {
		EventType type = arg0.getType();
		if (EventType.NodeDeleted == type || EventType.None == type) {
			log.error("missing master node");
		}
	}
}
