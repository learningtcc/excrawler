package six.com.crawler.admin.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import six.com.crawler.admin.api.ResponseMsg;
import six.com.crawler.admin.service.BaseService;
import six.com.crawler.admin.service.WorkSpaceService;
import six.com.crawler.dao.RedisManager;
import six.com.crawler.entity.DoneInfo;
import six.com.crawler.entity.Page;
import six.com.crawler.entity.WorkSpaceInfo;
import six.com.crawler.work.space.RedisWorkSpace;
import six.com.crawler.work.space.WorkSpace;
import six.com.crawler.work.space.WorkSpaceData;
import six.com.crawler.work.space.WorkSpaceManager;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月8日 下午3:27:27
 */

@Service
public class WorkSpaceServiceImpl extends BaseService implements WorkSpaceService {

	final static Logger log = LoggerFactory.getLogger(WorkSpaceServiceImpl.class);

	@Autowired
	private RedisManager redisManager;

	@Autowired
	private WorkSpaceManager workSpaceManager;

	public ResponseMsg<List<WorkSpaceInfo>> getWorkSpaces() {
		ResponseMsg<List<WorkSpaceInfo>> responseMsg = createResponseMsg();
		List<WorkSpaceInfo> workSpaceInfos = new ArrayList<>();
		List<WorkSpace<WorkSpaceData>> allWorkSpaces = workSpaceManager.getAllWorkSpaces();
		WorkSpaceInfo workSpaceInfo = null;
		for (WorkSpace<WorkSpaceData> workSpace : allWorkSpaces) {
			workSpaceInfo = new WorkSpaceInfo();
			workSpaceInfo.setWorkSpaceName(workSpace.getName());
			workSpaceInfo.setDoingSize(workSpace.doingSize());
			workSpaceInfo.setErrSize(workSpace.errSize());
			workSpaceInfo.setDoneSize(workSpace.doneSize());
			workSpaceInfos.add(workSpaceInfo);
		}
		responseMsg.setIsOk(1);
		responseMsg.setData(workSpaceInfos);
		return responseMsg;
	}

	public WorkSpaceInfo getWorkSpaceInfo(String workSpaceName) {
		WorkSpaceInfo workSpaceInfo = new WorkSpaceInfo();
		if (StringUtils.isNotBlank(workSpaceName)) {
			WorkSpace<WorkSpaceData> workSpace = workSpaceManager.newWorkSpace(workSpaceName, WorkSpaceData.class);
			workSpaceInfo.setWorkSpaceName(workSpaceName);
			workSpaceInfo.setDoingSize(workSpace.doingSize());
			workSpaceInfo.setErrSize(workSpace.errSize());
			workSpaceInfo.setDoneSize(workSpace.doneSize());
		}
		return workSpaceInfo;
	}

	public Map<String, Object> getWorkSpaceDoingData(String workSpaceName, String cursor) {
		Map<String, Object> resultMap = new HashMap<>();
		String queueKey = RedisWorkSpace.WORK_QUEUE_KEY_PRE + workSpaceName;
		List<Page> list = new ArrayList<>();
		cursor = redisManager.hscan(queueKey, cursor, list, Page.class);
		resultMap.put("cursor", cursor);
		resultMap.put("list", list);
		return resultMap;
	}

	/**
	 * 默认查10条数据
	 */
	public List<Page> getWorkSpaceErrData(String workSpaceName, String cursor) {
		List<Page> list = new ArrayList<>();
		workSpaceManager.newWorkSpace(workSpaceName, Page.class).batchGetErrData(list, cursor);
		return list;
	}

	public ResponseMsg<String> clearDoing(String workSpaceName) {
		ResponseMsg<String> responseMsg = createResponseMsg();
		new RedisWorkSpace<>(getRedisManager(), workSpaceName, WorkSpaceData.class).clearDoing();
		String msg = "clean workSpace[" + workSpaceName + "]  doing data succeed";
		responseMsg.setMsg(msg);
		responseMsg.setIsOk(1);
		log.info(msg);
		return responseMsg;
	}

	public ResponseMsg<String> clearErr(String workSpaceName) {
		ResponseMsg<String> responseMsg = createResponseMsg();
		new RedisWorkSpace<>(getRedisManager(), workSpaceName, WorkSpaceData.class).clearErr();
		String msg = "clean workSpace[" + workSpaceName + "]  err data succeed";
		responseMsg.setMsg(msg);
		responseMsg.setIsOk(1);
		log.info(msg);
		return responseMsg;
	}

	public ResponseMsg<String> clearDone(String workSpaceName) {
		ResponseMsg<String> responseMsg = createResponseMsg();
		new RedisWorkSpace<>(getRedisManager(), workSpaceName, WorkSpaceData.class).clearDone();
		String msg = "clean workSpace[" + workSpaceName + "]  done data succeed";
		responseMsg.setMsg(msg);
		responseMsg.setIsOk(1);
		log.info(msg);
		return responseMsg;
	}

	@Override
	public List<DoneInfo> getQueueDones() {
		Set<String> proxyQueuekeys = redisManager.keys(RedisWorkSpace.WORK_DONE_QUEUE_KEY_PRE + "*");
		List<DoneInfo> doneInfos = new ArrayList<>();
		DoneInfo tempDoneInfo = null;
		for (String tempKey : proxyQueuekeys) {
			int size = redisManager.hllen(tempKey);
			String queueName = StringUtils.remove(tempKey, RedisWorkSpace.WORK_DONE_QUEUE_KEY_PRE);
			tempDoneInfo = new DoneInfo();
			tempDoneInfo.setQueueName(queueName);
			tempDoneInfo.setSize(size);
			doneInfos.add(tempDoneInfo);
		}
		return doneInfos;
	}

	@Override
	public String againDoErrQueue(String queueName) {
		new RedisWorkSpace<Page>(redisManager, queueName, Page.class).againDoErrQueue();
		return "again do errQueue[" + queueName + "] succeed";
	}

	public RedisManager getRedisManager() {
		return redisManager;
	}

	public void setRedisManager(RedisManager redisManager) {
		this.redisManager = redisManager;
	}

	public WorkSpaceManager getWorkSpaceManager() {
		return workSpaceManager;
	}

	public void setWorkSpaceManager(WorkSpaceManager workSpaceManager) {
		this.workSpaceManager = workSpaceManager;
	}
}
