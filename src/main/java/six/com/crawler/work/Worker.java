package six.com.crawler.work;

import six.com.crawler.common.entity.Job;
import six.com.crawler.common.entity.JobSnapshot;
import six.com.crawler.common.entity.WorkerSnapshot;
import six.com.crawler.schedule.WorkerAbstractSchedulerManager;

/**
 * @author six
 * @date 2016年1月15日 下午6:20:00
 */
public interface Worker extends WorkerLifecycle {

	void bindWorkerSnapshot(WorkerSnapshot workerSnapshot);
	
	void bindManager(WorkerAbstractSchedulerManager manager);
	
	void bindJobSnapshot(JobSnapshot jobSnapshot);

	void bindJob(Job job);

	/**
	 * 初始化
	 */
	public void init();

	/**
	 * 获取 manager
	 * 
	 * @return
	 */
	WorkerAbstractSchedulerManager getManager();

	/**
	 * 获取 worker name
	 * 
	 * @return
	 */
	String getName();

	/**
	 * 获取worker 快照
	 * 
	 * @return
	 */
	WorkerSnapshot getWorkerSnapshot();

	/**
	 * 获取工作频率
	 * 
	 * @return
	 */
	long getWorkFrequency();

	/**
	 * 获取最后一次活动时间
	 * 
	 * @return
	 */
	long getLastActivityTime();

	JobSnapshot getJobSnapshot();
	/**
	 * 获取work Job
	 * 
	 * @return
	 */
	Job getJob();
}
