package six.com.crawler.schedule;

import java.rmi.Remote;

import six.com.crawler.entity.Job;
/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月13日 上午11:45:58
 */
public abstract class MasterAbstractSchedulerManager extends AbstractSchedulerManager implements Remote {

	protected final void init() {
		doInit();
	}

	protected abstract void doInit();
	
	
	/**
	 * 开始执行 job's worker
	 * 
	 * @param jobName
	 * @param WorkName
	 */
	public abstract void startWorker(String jobName);

	/**
	 * 结束执行job's worker
	 * @param jobName
	 * @param WorkName
	 */
	public abstract void endWorer(String jobName);
	
	public abstract void repair();

	/**
	 * 向调度器 调度job
	 * 
	 * @param job
	 */
	public abstract void scheduled(Job job);

	/**
	 * 取消调度
	 * 
	 * @param job
	 */
	public abstract void cancelScheduled(String jobChainName);

}
