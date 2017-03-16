package six.com.crawler.common.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @author sixliu E-mail:359852326@qq.com
 * @version 创建时间：2016年5月19日 下午8:35:28 类说明 爬虫job
 */
public class Job extends PageQueryEntity implements Serializable {

	private static final long serialVersionUID = 781122651512874550L;

	private String name;// job 名字
	
	private String nextJobName;// 下一个执行任务

	private int level;// 任务级别
	
	private String designatedNodeName;//指定节点运行
	
	private int needNodes;//工作需要的节点数
	
	private int isScheduled;//是否开启定时
	
	private String cronTrigger;//cronTrigger 定时
	
	private long workFrequency = 1000;// 每次处理时间的阈值 默认1000毫秒

	private String workerClass;// worker class

	private String queueName;

	private String describe;// 任务描述

	private String user = "admin";// 任务 所属用户
	
	private int version;//版本号

	private List<JobParam> paramList;//任务参数
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNextJobName() {
		return nextJobName;
	}

	public void setNextJobName(String nextJobName) {
		this.nextJobName = nextJobName;
	}

	public String getDesignatedNodeName() {
		return designatedNodeName;
	}
	public void setDesignatedNodeName(String designatedNodeName) {
		this.designatedNodeName = designatedNodeName;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public long getWorkFrequency() {
		return workFrequency;
	}

	public void setWorkFrequency(long workFrequency) {
		this.workFrequency = workFrequency;
	}

	public int getIsScheduled() {
		return isScheduled;
	}

	public void setIsScheduled(int isScheduled) {
		this.isScheduled = isScheduled;
	}

	public int getNeedNodes() {
		return needNodes;
	}

	public void setNeedNodes(int needNodes) {
		this.needNodes = needNodes;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getCronTrigger() {
		return cronTrigger;
	}

	public void setCronTrigger(String cronTrigger) {
		this.cronTrigger = cronTrigger;
	}

	public String getWorkerClass() {
		return workerClass;
	}

	public void setWorkerClass(String workerClass) {
		this.workerClass = workerClass;
	}

	public String getDescribe() {
		return describe;
	}

	public void setDescribe(String describe) {
		this.describe = describe;
	}

	public List<JobParam> getParamList() {
		return paramList;
	}

	public void setParamList(List<JobParam> paramList) {
		this.paramList = paramList;
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getParam(String paramKey) {
		if (StringUtils.isBlank(paramKey)) {
			throw new NullPointerException("paramKey mustn't be blank");
		}
		String param = null;
		if (null != paramList) {
			for (int i = 0; i < paramList.size(); i++) {
				JobParam jobParam = paramList.get(i);
				if (paramKey.equals(jobParam.getName())) {
					param = jobParam.getValue();
					break;
				}
			}
		}
		return StringUtils.trim(param);
	}

	public List<String> getParams(String paramKey) {
		if (StringUtils.isBlank(paramKey)) {
			throw new NullPointerException("paramKey mustn't be blank");
		}
		List<String> resultParams = new ArrayList<>();
		if (null != paramList) {
			for (int i = 0; i < paramList.size(); i++) {
				JobParam jobParam = paramList.get(i);
				if (paramKey.equals(jobParam.getName())) {
					resultParams.add(StringUtils.trim(jobParam.getValue()));
				}
			}
		}
		return resultParams;
	}

	public int hashCode() {
		if (null != name) {
			return name.hashCode();
		}
		return 0;
	}

	public boolean equals(Object o) {
		if (null != o) {
			if (o instanceof Job) {
				Job targetJob = (Job) o;
				if (null != getName() && getName().equals(targetJob.getName())) {
					return true;
				}
			}
		}
		return false;
	}

}
