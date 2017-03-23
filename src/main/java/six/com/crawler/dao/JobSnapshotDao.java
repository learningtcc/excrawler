package six.com.crawler.dao;

import java.util.List;

import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import six.com.crawler.dao.provider.JobSnapshotDaoProvider;
import six.com.crawler.entity.JobSnapshot;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2016年10月24日 下午3:36:26
 */
public interface JobSnapshotDao extends BaseDao{

	String TABLE_NAME="ex_crawler_platform_job_snapshot";
	
	@SelectProvider(type = JobSnapshotDaoProvider.class, method = "query")
	public List<JobSnapshot> query(String jobName);
	
	@SelectProvider(type = JobSnapshotDaoProvider.class, method = "queryLast")
	public JobSnapshot queryLast(@Param("jobName")String jobName);
	
	@InsertProvider(type = JobSnapshotDaoProvider.class, method = "save")
	public int save(JobSnapshot jobSnapshot);
	
	
	@InsertProvider(type = JobSnapshotDaoProvider.class, method = "update")
	public int update(JobSnapshot jobSnapshot);
		
}
