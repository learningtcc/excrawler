package six.com.crawler.common.dao.provider;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.jdbc.SQL;

import six.com.crawler.common.dao.BaseDao;
import six.com.crawler.common.dao.JobSnapshotDao;
import six.com.crawler.common.entity.JobSnapshot;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2016年10月24日 下午3:40:34 
 */
public class JobSnapshotDaoProvider extends BaseProvider{

	public String query(String jobName) {
		SQL sql = new SQL();
		String columns = "id,"
						+ "`name`,"
						+ "tableName,"
						+ "UNIX_TIMESTAMP(startTime)*1000 startTime,"
						+ "UNIX_TIMESTAMP(endTime)*1000 endTime," 
						+ "`state`,"
						+ "totalProcessCount,"
						+ "totalResultCount,"
						+"totalProcessTime,"
						+ "avgProcessTime," 
						+ "maxProcessTime," 
						+ "minProcessTime," 
						+ "errCount";
		sql.SELECT(columns);
		sql.FROM(JobSnapshotDao.TABLE_NAME);
		sql.WHERE("name=#{jobName}");
		sql.ORDER_BY("startTime desc");
		return sql.toString();
	}

	public String save(JobSnapshot jobSnapshot) {
		String columns = "id,"
				+ "`name`,"
				+ "tableName,"
				+ "startTime,"
				+ "endTime,"
				+ "`state`,"
				+ "totalProcessCount,"
				+ "totalResultCount,"
				+ "totalProcessTime,"
				+ "avgProcessTime,"
				+ "maxProcessTime,"
				+ "minProcessTime,"
				+ "errCount";
		String values = "#{id}," 
				+ "#{name}," 
				+ "#{tableName}," 
				+ "#{startTime}," 
				+ "#{endTime}," 
				+ "#{state}," 
				+ "#{totalProcessCount},"
				+ "#{totalResultCount},"
				+ "#{totalProcessTime}," 
				+ "#{avgProcessTime}," 
				+ "#{maxProcessTime}," 
				+ "#{minProcessTime},"
				+ "#{errCount}";
		SQL sql = new SQL();
		sql.INSERT_INTO(JobSnapshotDao.TABLE_NAME);
		sql.VALUES(columns, values);
		return sql.toString();
	}

	@SuppressWarnings("unchecked") 
	public String batchSave(Map<String, Object> map) {
		List<JobSnapshot> jobSnapshots = (List<JobSnapshot>) map.get(BaseDao.BATCH_SAVE_PARAM);
		String columns="id,"
				+ "`name`,"
				+ "tableName,"
				+ "startTime,"
				+ "endTime,"
				+ "`state`,"
				+ "totalProcessCount,"
				+ "totalResultCount,"
				+ "totalProcessTime,"
				+ "avgProcessTime,"
				+ "maxProcessTime,"
				+ "minProcessTime,"
				+ "errCount";
		String values="(#{list["+INDEX_FLAG+"].id},"
				+ "#{list["+INDEX_FLAG+"].name},"
				+ "#{list["+INDEX_FLAG+"].tableName},"
				+ "#{list["+INDEX_FLAG+"].startTime},"
				+ "#{list["+INDEX_FLAG+"].endTime},"
				+ "#{list["+INDEX_FLAG+"].state},"
				+ "#{list["+INDEX_FLAG+"].totalProcessCount},"
				+ "#{list["+INDEX_FLAG+"].totalResultCount},"
				+ "#{list["+INDEX_FLAG+"].totalProcessTime},"
				+ "#{list["+INDEX_FLAG+"].avgProcessTime},"
				+ "#{list["+INDEX_FLAG+"].maxProcessTime},"
				+ "#{list["+INDEX_FLAG+"].minProcessTime},"
				+ "#{list["+INDEX_FLAG+"].errCount})";
		StringBuilder sbd = new StringBuilder();  
		sbd.append("insert into ").append(JobSnapshotDao.TABLE_NAME);  
		sbd.append("(").append(columns).append(") ");  
		sbd.append("values");  
		sbd.append(setBatchSaveSql(values, jobSnapshots));
		return sbd.toString();
	}

}
