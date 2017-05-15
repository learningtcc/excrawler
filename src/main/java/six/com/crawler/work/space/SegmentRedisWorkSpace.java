package six.com.crawler.work.space;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import six.com.crawler.dao.RedisManager;
import six.com.crawler.node.lock.DistributedLock;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年5月12日 下午4:48:45
 */
public class SegmentRedisWorkSpace<T extends WorkSpaceData> implements WorkSpace<T> {

	final static Logger log = LoggerFactory.getLogger(RedisWorkSpace.class);

	/**
	 * 默认处理队列分片大小
	 */
	private final static int DEFAULT_DOING_SEGMENT_MAX_SIZE = 2000;

	/**
	 * 默认处理完队列分片大小
	 */
	private final static int DEFAULT_DONE_SEGMENT_MAX_SIZE = 100000;

	public final static String ERR_QUEUE_KEY_PRE = "workspace_err_queue_";

	public final static String SEGMENT_DOING_QUEUE_NAME_KEYS = "workspace_segment_doing_queue_keys_";
	public final static String SEGMENT_DOING_QUEUE_NAME = "workspace_segment_doing_queue_";

	public final static String SEGMENT_DOING_MAP_NAME_KEYS = "workspace_segment_doing_map_keys_";
	public final static String SEGMENT_DOING_MAP_NAME = "workspace_segment_doing_map_";

	public final static String SEGMENT_DONE_MAP_NAME_KEYS = "workspace_segment_done_map_keys_";
	public final static String SEGMENT_DONE_MAP_NAME = "workspace_segment_done_map_";

	public final static int batch = 10000;

	private final String workSpaceName;

	private final String errQueueKey;

	private final Class<T> clz;

	private RedisManager redisManager;

	private DistributedLock distributedLock;

	private SegmentQueue<Index> doingSegmentQueue;

	private SegmentMap<T> doingSegmentMap;

	private SegmentMap<String> doneSegmentMap;

	public SegmentRedisWorkSpace(RedisManager redisManager, DistributedLock distributedLock, String workSpaceName,
			Class<T> clz) {
		this(redisManager, distributedLock, workSpaceName, clz, DEFAULT_DOING_SEGMENT_MAX_SIZE,
				DEFAULT_DONE_SEGMENT_MAX_SIZE);
	}

	public SegmentRedisWorkSpace(RedisManager redisManager, DistributedLock distributedLock, String workSpaceName,
			Class<T> clz, int doingSemegtMaxSize, int doneSemegtMaxSize) {

		Objects.requireNonNull(redisManager, "redisManager must not be null");
		Objects.requireNonNull(workSpaceName, "workSpaceName must not be null");
		Objects.requireNonNull(clz, "clz must not be null");

		this.redisManager = redisManager;
		this.distributedLock = distributedLock;
		this.workSpaceName = workSpaceName;
		this.errQueueKey = ERR_QUEUE_KEY_PRE + workSpaceName;
		this.clz = clz;
		String segmentDoingQueueNames = SEGMENT_DOING_QUEUE_NAME_KEYS + workSpaceName;
		String segmentDoingQueueNamePre = SEGMENT_DOING_QUEUE_NAME + workSpaceName;
		this.doingSegmentQueue = new SegmentQueue<Index>(segmentDoingQueueNames, segmentDoingQueueNamePre, redisManager,
				doingSemegtMaxSize, Index.class);

		String segmentDoingMapNames = SEGMENT_DOING_MAP_NAME_KEYS + workSpaceName;
		String segmentDoingMapNamePre = SEGMENT_DOING_MAP_NAME + workSpaceName;
		this.doingSegmentMap = new SegmentMap<>(segmentDoingMapNames, segmentDoingMapNamePre, redisManager,
				doingSemegtMaxSize, clz);

		String segmentDoneMapNames = SEGMENT_DONE_MAP_NAME_KEYS + workSpaceName;
		String segmentDoneMapNamePre = SEGMENT_DONE_MAP_NAME + workSpaceName;
		this.doneSegmentMap = new SegmentMap<>(segmentDoneMapNames, segmentDoneMapNamePre, redisManager,
				doneSemegtMaxSize, String.class);

	}

	public String getName() {
		return workSpaceName;
	}

	@Override
	public boolean push(T data) {
		Objects.requireNonNull(data, "data must not be null");
		distributedLock.lock();
		try {
			String dataKey = data.getKey();
			String mapKey = doingSegmentMap.where(dataKey);
			if (null == mapKey) {
				Index index = doingSegmentMap.put(mapKey, dataKey, data);
				doingSegmentQueue.push(index);
				log.info("push workSpace[" + workSpaceName + "] data succeed:" + data.toString());
			} else {
				doingSegmentMap.put(mapKey, data.getKey(), data);
				log.info("update workSpace[" + workSpaceName + "] data succeed:" + data.toString());
			}
			return true;
		} catch (Exception e) {
			log.error("push workSpace[" + workSpaceName + "] data err:" + data.toString(), e);
			throw e;
		} finally {
			distributedLock.unLock();
		}
	}

	@Override
	public boolean errRetryPush(T data) {
		Objects.requireNonNull(data, "data must not be null");
		Index index = data.getIndex();
		if (null != index && null != index.getMapKey()) {
			distributedLock.lock();
			try {
				doingSegmentMap.put(index.getMapKey(), index.getDataKey(), data);
				return true;
			} catch (Exception e) {
				throw e;
			} finally {
				distributedLock.unLock();
			}
		}
		return false;
	}

	@Override
	public T pull() {
		T data = null;
		try {
			distributedLock.lock();
			Index index = doingSegmentQueue.poll();
			if (null != index) {
				data = doingSegmentMap.get(index);
				data.setIndex(index);
			}
		} catch (Exception e) {
			log.error("pull workSpace[" + workSpaceName + "] data err", e);
			throw e;
		} finally {
			distributedLock.unLock();
		}
		return data;
	}

	public void repair() {
		try {
			distributedLock.lock();
			List<String> mapKeys = doingSegmentMap.getSegments();
			if (null != mapKeys) {
				for (String mapKey : mapKeys) {
					List<T> datas = doingSegmentMap.getData(mapKey);
					for (T data : datas) {
						doingSegmentQueue.push(doingSegmentMap.getIndex(mapKey, data.getKey()));
					}
				}
			}
		} catch (Exception e) {
			log.error("repair workSpace[" + workSpaceName + "] err", e);
			throw e;
		} finally {
			distributedLock.unLock();
		}
	}

	public String batchGetDoingData(List<T> resutList, int segmentIndex, String cursorStr) {
		String segment = doingSegmentMap.getSegment(segmentIndex);
		cursorStr = doingSegmentMap.batchGet(segment, resutList, cursorStr);
		return cursorStr;
	}

	public String batchGetErrData(List<T> resutList, String cursorStr) {
		return batchGet(resutList, cursorStr, errQueueKey);
	}

	public String batchGetDoneData(List<String> resutList, int segmentIndex, String cursorStr) {
		String segment = doneSegmentMap.getSegment(segmentIndex);
		cursorStr = doneSegmentMap.batchGet(segment, resutList, cursorStr);
		return cursorStr;
	}

	private String batchGet(List<T> resutList, String cursorStr, String type) {
		Objects.requireNonNull(resutList, "resutList must not be null");
		if (StringUtils.isBlank(cursorStr)) {
			cursorStr = "0";
		}
		Map<String, T> map = new HashMap<>();
		cursorStr = redisManager.hscan(type, cursorStr, map, clz);
		resutList.addAll(map.values());
		return cursorStr;
	}

	public void addErr(T data) {
		redisManager.hset(errQueueKey, data.getKey(), data);
	}

	public void againDoErrQueue() {
		Map<String, T> map = new HashMap<>();
		String cursorStr = "0";
		do {
			cursorStr = redisManager.hscan(errQueueKey, cursorStr, map, clz);
			map.values().stream().forEach(data -> {
				push(data);
			});
			map.clear();
		} while (!"0".equals(cursorStr));
		redisManager.del(errQueueKey);
	}

	@Override
	public boolean isDone(String key) {
		boolean isduplicate = false;
		if (StringUtils.isNotBlank(key)) {
			distributedLock.lock();
			try {
				isduplicate = null != doneSegmentMap.where(key);
			} catch (Exception e) {
				throw e;
			} finally {
				distributedLock.unLock();
			}
		}
		return isduplicate;
	}

	@Override
	public void addDone(String key) {
		if (StringUtils.isNotBlank(key)) {
			distributedLock.lock();
			try {
				doneSegmentMap.put(null, key, "1");
			} catch (Exception e) {
				throw e;
			} finally {
				distributedLock.unLock();
			}
		}
	}

	public void ack(T data) {
		Objects.requireNonNull(data, "data must not be null");
		distributedLock.lock();
		try {
			doingSegmentMap.del(data.getIndex());
		} catch (Exception e) {
			throw e;
		} finally {
			distributedLock.unLock();
		}
	}

	@Override
	public int doingSegmentSize(){
		return doingSegmentMap.segmentSize();
	}
	
	@Override
	public boolean doingIsEmpty(){
		return 0==doingSegmentMap.size()&&0==doingSegmentQueue.size();
	}
	
	@Override
	public int doingSize() {
		return doingSegmentMap.size();
	}

	@Override
	public int errSize() {
		return redisManager.hllen(errQueueKey);
	}

	@Override
	public int doneSize() {
		return doneSegmentMap.size();
	}

	@Override
	public void clearDoing() {
		distributedLock.lock();
		try {
			doingSegmentQueue.clear();
			doingSegmentMap.clear();
		} catch (Exception e) {
			throw e;
		} finally {
			distributedLock.unLock();
		}
	}

	@Override
	public void clearErr() {
		distributedLock.lock();
		try {
			redisManager.del(errQueueKey);
		} catch (Exception e) {
			throw e;
		} finally {
			distributedLock.unLock();
		}
	}

	@Override
	public void clearDone() {
		distributedLock.lock();
		try {
			doneSegmentMap.clear();
		} catch (Exception e) {
			throw e;
		} finally {
			distributedLock.unLock();
		}
	}

	@Override
	public void close() {
		distributedLock.lock();
		try {
			doingSegmentMap.cleanUp();
		} catch (Exception e) {
			throw e;
		} finally {
			distributedLock.unLock();
		}
	}

}
