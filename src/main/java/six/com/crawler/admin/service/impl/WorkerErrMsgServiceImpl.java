package six.com.crawler.admin.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import six.com.crawler.admin.api.ResponseMsg;
import six.com.crawler.admin.service.BaseService;
import six.com.crawler.admin.service.WorkerErrMsgService;
import six.com.crawler.dao.WorkerErrMsgDao;
import six.com.crawler.entity.PageQuery;
import six.com.crawler.entity.WorkerErrMsg;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月17日 下午1:21:10
 */

@Service
public class WorkerErrMsgServiceImpl extends BaseService implements WorkerErrMsgService {

	@Autowired
	private WorkerErrMsgDao workerErrMsgDao;

	@Override
	public ResponseMsg<PageQuery<WorkerErrMsg>> query(String jobName, String jobSnapshotId, int pageIndex,
			int pageSize) {
		ResponseMsg<PageQuery<WorkerErrMsg>> responseMsg = createResponseMsg();
		pageSize = pageSize <= 0 || pageSize > 50 ? 15 : pageSize;
		PageQuery<WorkerErrMsg> pageQuery = new PageQuery<>();
		int totalSize = 0;
		pageIndex = pageIndex * pageSize;
		List<WorkerErrMsg> workerErrMsgs = workerErrMsgDao.pageQuery(jobName, jobSnapshotId, pageIndex, pageSize);
		if (null != workerErrMsgs && workerErrMsgs.size() > 0) {
			totalSize = workerErrMsgs.get(0).getTotalSize();
			pageQuery.setTotalSize(totalSize);
			pageQuery.setTotalPage(totalSize % pageSize == 0 ? totalSize / pageSize : totalSize / pageSize + 1);
			pageQuery.setList(workerErrMsgs);
			pageQuery.setPageIndex(pageIndex);
			pageQuery.setPageSize(pageSize);
			responseMsg.setData(pageQuery);
		}
		responseMsg.setIsOk(1);
		return responseMsg;
	}

	public WorkerErrMsgDao getWorkerErrMsgDao() {
		return workerErrMsgDao;
	}

	public void setWorkerErrMsgDao(WorkerErrMsgDao workerErrMsgDao) {
		this.workerErrMsgDao = workerErrMsgDao;
	}

}
