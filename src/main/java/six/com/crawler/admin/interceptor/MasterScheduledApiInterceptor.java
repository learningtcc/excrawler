package six.com.crawler.admin.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import six.com.crawler.node.Node;
import six.com.crawler.node.ClusterManager;
import six.com.crawler.node.NodeType;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月17日 下午4:16:02 控制页面 必须访问 master节点页面
 * 
 */
public class MasterScheduledApiInterceptor implements HandlerInterceptor {

	ClusterManager clusterManager;

	public MasterScheduledApiInterceptor(ClusterManager clusterManager) {
		this.clusterManager = clusterManager;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (clusterManager.getCurrentNode().getType() != NodeType.SINGLE
				&& clusterManager.getCurrentNode().getType() != NodeType.MASTER) {
			Node masterNode = clusterManager.getMasterNodeFromRegister();
			if (null != masterNode && masterNode.equals(clusterManager.getCurrentNode())) {
				return true;
			} else {
				response.sendRedirect(getMasterIndex(masterNode));
				return false;
			}
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
	}

	private static String getMasterIndex(Node masterNode) {
		String indexUrl = "http://" + masterNode.getHost() + ":" + masterNode.getPort() + "/crawler";
		return indexUrl;
	}

}
