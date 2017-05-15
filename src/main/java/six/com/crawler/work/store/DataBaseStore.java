package six.com.crawler.work.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSource;

import six.com.crawler.entity.JobParamKeys;
import six.com.crawler.entity.JobSnapshot;
import six.com.crawler.entity.JobSnapshotState;
import six.com.crawler.utils.DbHelper;
import six.com.crawler.utils.JobTableUtils;
import six.com.crawler.work.AbstractWorker;
import six.com.crawler.work.store.exception.StoreException;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2016年12月9日 上午11:15:34
 */
public class DataBaseStore extends AbstarctStore {

	final static String checkTableIsCreateSql = "select table_name  " + " from INFORMATION_SCHEMA.tables  "
			+ " where TABLE_NAME=?";
	final static Logger LOG = LoggerFactory.getLogger(DataBaseStore.class);
	private String insertSqlTemplate;
	private String insertSql;
	private String createTableSqlTemplate;
	private String tableName;
	private String dbDriverClassName;
	private String dbUrl;
	private String dbUser;
	private String dbPasswd;
	int batchSize = 1;
	private DruidDataSource datasource;
	// 处理的结果key
	private String[] fields;

	private final static String TABLE_KEY = "table";

	public DataBaseStore(AbstractWorker<?> worker) {
		super(worker);
		String everySendSizeStr = worker.getJob().getParam(JobParamKeys.BATCH_SIZE);
		if (null != everySendSizeStr) {
			try {
				batchSize = Integer.valueOf(everySendSizeStr);
			} catch (Exception e) {
				throw new RuntimeException("Integer.valueOf batchSize err:" + everySendSizeStr, e);
			}

		}
		dbUrl = worker.getJob().getParam(JobParamKeys.DB_URL);
		dbUser = worker.getJob().getParam(JobParamKeys.DB_USER);
		dbPasswd = worker.getJob().getParam(JobParamKeys.DB_PASSWD);
		dbDriverClassName = worker.getJob().getParam(JobParamKeys.DB_DRIVER_CLASS_NAME);
		datasource = new DruidDataSource();
		datasource.setUrl(dbUrl);
		datasource.setDriverClassName(dbDriverClassName);
		datasource.setUsername(dbUser);
		datasource.setPassword(dbPasswd);
		datasource.setMaxActive(1);
		
		
		JobSnapshot jobSnapshot = getWorker().getManager().getScheduleCache()
				.getJobSnapshot(getWorker().getJob().getName());
		tableName = jobSnapshot.getParam(TABLE_KEY);
		if (StringUtils.isBlank(tableName)) {
			String fixedTableName = getWorker().getJob().getParam(JobParamKeys.FIXED_TABLE_NAME);
			String isSnapshotTable = getWorker().getJob().getParam(JobParamKeys.IS_SNAPSHOT_TABLE);
			if ("1".equals(isSnapshotTable)) {
				JobSnapshot lastJobSnapshot = getWorker().getManager().getLastEnd(getWorker().getJob().getName(),jobSnapshot.getId());
				if (null != lastJobSnapshot && JobSnapshotState.FINISHED != lastJobSnapshot.getEnumStatus()) {
					tableName = lastJobSnapshot.getParam(TABLE_KEY);
				}
				if (StringUtils.isBlank(tableName)||!checkIsCreated(tableName)) {
					tableName = JobTableUtils.buildJobTableName(fixedTableName,
							getWorker().getWorkerSnapshot().getJobSnapshotId());
				}
			} else {
				tableName = fixedTableName;
			}
			jobSnapshot.putParam(TABLE_KEY, tableName);
			getWorker().getManager().updateJobSnapshot(jobSnapshot);
		}
		insertSqlTemplate = worker.getJob().getParam(JobParamKeys.INSERT_SQL_TEMPLATE);
		insertSql = JobTableUtils.buildInsertSql(insertSqlTemplate, tableName);
		String fieldsStr = StringUtils.substringBetween(insertSql, "(", ")");
		fields = StringUtils.split(fieldsStr, ",");
		String field = null;
		for (int i = 0; i < fields.length; i++) {
			field = fields[i];
			field = StringUtils.trim(field);
			field = StringUtils.remove(field, "`");
			fields[i] = StringUtils.trim(field);
		}
		createTableSqlTemplate = worker.getJob().getParam(JobParamKeys.CREATE_TABLE_SQL_TEMPLATE);
		initTable();
	}

	private void initTable() {
		if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(createTableSqlTemplate)) {
			// 检查table 是否
			if (!checkIsCreated(tableName)) {
				String createTableSql = JobTableUtils.buildCreateTableSql(createTableSqlTemplate, tableName);
				try {
					doSql(createTableSql, null);
				} catch (StoreException e) {
					throw new RuntimeException("create data temp table err", e);
				}
			}
		}
	}

	private boolean checkIsCreated(String table) {
		Connection connection = null;
		try {
			connection = datasource.getConnection();
			List<String> tables = DbHelper.queryTableNames(connection, table);
			if (tables.size() > 0) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			throw new RuntimeException("check[" + table + "] is created err:" + checkTableIsCreateSql, e);
		} finally {
			DbHelper.close(connection);
		}
	}

	@Override
	protected int insideStore(List<Map<String, String>> results) throws StoreException {
		int storeCount = 0;
		if (null != results) {
			List<Object> parameters = new ArrayList<>();
			for (Map<String, String> dataMap : results) {
				parameters.clear();
				for (String field : fields) {
					String param = dataMap.get(field);
					parameters.add(param);
				}
				storeCount += doSql(insertSql, parameters);
			}
		}
		return storeCount;
	}

	private int doSql(String sql, List<Object> parameter) throws StoreException {
		int storeCount = 0;
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			connection = datasource.getConnection();
			ps = connection.prepareStatement(sql);
			DbHelper.setPreparedStatement(ps, parameter);
			storeCount = ps.executeUpdate();
		} catch (SQLException e) {
			if (!e.getMessage().contains("Duplicate entry")) {
				throw new StoreException("execute sql err:" + sql, e);
			} else {
				LOG.info("duplicate entry:" + parameter);
			}
		} finally {
			DbHelper.close(connection);
		}
		return storeCount;

	}

	@Override
	public void close() {
		if (null != datasource) {
			datasource.close();
		}
	}

}
