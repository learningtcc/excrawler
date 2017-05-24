package six.com.crawler.admin.service;

import java.util.List;

import six.com.crawler.admin.api.ResponseMsg;
import six.com.crawler.entity.WorkerSnapshot;

/** 
* @author  作者 
* @E-mail: 359852326@qq.com 
* @date 创建时间：2017年2月24日 下午10:35:07 
*/
public interface MasterScheduledService {

	/**
	 * 执行job
	 * 
	 * @param jobName
	 * @return
	 */
	public ResponseMsg<String> execute(String jobName);
	
	
	/**
	 * 暂停执行job
	 * 
	 * @param jobName
	 * @return
	 */
	public ResponseMsg<String> suspend(String jobName);

	/**
	 * 继续执行job
	 * 
	 * @param jobName
	 * @return
	 */
	public ResponseMsg<String> goOn(String jobName);

	/**
	 * 终止执行job
	 * 
	 * @param jobName
	 * @return
	 */
	public ResponseMsg<String> stop(String jobName);
	

	public ResponseMsg<List<WorkerSnapshot>> getWorkerInfo(String jobName);
}
