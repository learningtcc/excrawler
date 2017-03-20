package six.com.crawler.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import six.com.crawler.common.entity.Node;
import six.com.crawler.common.utils.JavaSerializeUtils;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月16日 下午12:19:52
 */
public class MasterAndWorkerNodeRegisterEvent extends NodeRegisterEvent {

	final static Logger log = LoggerFactory.getLogger(MasterAndWorkerNodeRegisterEvent.class);

	public MasterAndWorkerNodeRegisterEvent(Node currentNode) {
		super(currentNode);
	}

	@Override
	public boolean doRegister(ClusterManager clusterManager, CuratorFramework zKClient) {
		try {
			Node masterNode = clusterManager.getMasterNode();
			if (null == masterNode || masterNode.equals(getCurrentNode())) {
				byte[] data = JavaSerializeUtils.serialize(getCurrentNode());
				zKClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
						.forPath(ZooKeeperPathUtils.getMasterNodePath(getCurrentNode().getName()), data);
				zKClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
						.forPath(ZooKeeperPathUtils.getWorkerNodePath(getCurrentNode().getName()), data);
				return true;
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return false;
	}

	@Override
	public void process(WatchedEvent arg0) {
		EventType type = arg0.getType();
		if (EventType.NodeDeleted == type || EventType.None == type) {
			log.error("missing master node");
		}
	}
}
