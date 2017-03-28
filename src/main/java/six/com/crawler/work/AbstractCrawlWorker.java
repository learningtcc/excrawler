package six.com.crawler.work;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import six.com.crawler.constants.JobConTextConstants;
import six.com.crawler.entity.HttpProxyType;
import six.com.crawler.entity.JobSnapshot;
import six.com.crawler.entity.Page;
import six.com.crawler.entity.ResultContext;
import six.com.crawler.entity.Site;
import six.com.crawler.http.HttpProxyPool;
import six.com.crawler.utils.ThreadUtils;
import six.com.crawler.work.downer.Downer;
import six.com.crawler.work.downer.DownerManager;
import six.com.crawler.work.downer.DownerType;
import six.com.crawler.work.downer.exception.DownerException;
import six.com.crawler.work.extract.ExtractItem;
import six.com.crawler.work.extract.Extracter;
import six.com.crawler.work.extract.ExtracterFactory;
import six.com.crawler.work.extract.ExtracterType;
import six.com.crawler.work.store.Store;
import six.com.crawler.work.store.StoreFactory;
import six.com.crawler.work.store.StoreType;

/**
 * @author six
 * @date 2016年1月15日 下午6:45:26 爬虫抽象层
 * 
 *       当爬虫队列数据为null时 那么就会设置状态为finished
 */
public abstract class AbstractCrawlWorker extends AbstractWorker {

	final static Logger LOG = LoggerFactory.getLogger(AbstractCrawlWorker.class);
	// 上次处理数据时间
	protected int findElementTimeout = Constants.FIND_ELEMENT_TIMEOUT;
	// 默认HTTP PROXY最小休息时间 5 秒
	private int httpProxyMinResttime = Constants.DEFAULT_MIN_HTTPPROXY_RESTTIME;

	private Site site; // 站点

	private Downer downer;// 下载器
	// 解析处理程序
	private Extracter extracter;

	private Page doingPage;

	protected WorkQueue workQueue; // 队列
	// 存儲处理程序
	private Store store;

	private HttpProxyPool httpProxyPool;

	@Override
	protected final void initWorker(JobSnapshot jobSnapshot) {
		// 1.初始化 站点code
		String siteCode = getJob().getParam(JobConTextConstants.SITE_CODE);
		if (StringUtils.isBlank(siteCode)) {
			throw new NullPointerException("please set siteCode");
		}
		site = getManager().getSiteDao().query(siteCode);
		// 2.初始化工作队列
		String queueName = getJob().getQueueName();
		if (StringUtils.isBlank(queueName)) {
			throw new NullPointerException("please set queue's name");
		}
		workQueue = new RedisWorkQueue(getManager().getRedisManager(), queueName);
		// 4.初始化下载器
		int downerTypeInt = getJob().getParamInt(JobConTextConstants.DOWNER_TYPE, 1);
		DownerType downerType = DownerType.valueOf(downerTypeInt);
		downer = DownerManager.getInstance().buildDowner(downerType, this);

		int httpProxyTypeInt = getJob().getParamInt(JobConTextConstants.HTTP_PROXY_TYPE, 0);
		HttpProxyType httpProxyType = HttpProxyType.valueOf(httpProxyTypeInt);

		// 5.初始化http 代理
		int httpProxyRestTime = getJob().getParamInt(JobConTextConstants.HTTP_PROXY_REST_TIME, httpProxyMinResttime);
		httpProxyPool = new HttpProxyPool(getManager().getRedisManager(), siteCode, httpProxyType, httpProxyRestTime);
		downer.setHttpProxy(httpProxyPool.getHttpProxy());
		// 6.初始化内容抽取
		List<ExtractItem> extractItems = getManager().getExtractItemDao().query(getJob().getName());
		extracter = ExtracterFactory.newExtracter(this, extractItems, ExtracterType
				.valueOf(getJob().getParamInt(JobConTextConstants.EXTRACTER_TYPE, 0)));
		// 7.初始化数据存储
		List<String> outResultKey = new ArrayList<>();
		if (null != extractItems && !extractItems.isEmpty()) {
			int primaryKeyCount = 0;
			for (ExtractItem extractItem : extractItems) {
				if (extractItem.getOutputType() == 1) {
					outResultKey.add(extractItem.getOutputKey());
				}
				if (extractItem.getPrimary() == 1) {
					primaryKeyCount++;
				}
			}
			if (!outResultKey.isEmpty() && 0 == primaryKeyCount) {
				throw new RuntimeException("there is a primary's key at least");
			}
			outResultKey.add(0, Extracter.DEFAULT_RESULT_ID);
			outResultKey.add(Extracter.DEFAULT_RESULT_COLLECTION_DATE);
			outResultKey.add(Extracter.DEFAULT_RESULT_ORIGIN_URL);
		}
		int storeTypeInt = 0;
		// 兼容之前设置的store class模式
		String resultStoreClass = getJob().getParam(JobConTextConstants.RESULT_STORE_CLASS);
		if (StringUtils.equals("six.com.crawler.work.store.DataBaseStore", resultStoreClass)) {
			storeTypeInt = 1;
		} else {
			storeTypeInt = getJob().getParamInt(JobConTextConstants.RESULT_STORE_TYPE, 0);
		}
		this.store = StoreFactory.newStore(this, outResultKey, StoreType.valueOf(storeTypeInt));
		insideInit();
	}

	@Override
	protected void insideWork() throws Exception {
		long startTime = System.currentTimeMillis();
		doingPage = workQueue.pull();
		long endTime = System.currentTimeMillis();
		LOG.debug("workQueue pull time:" + (endTime - startTime));
		if (null != doingPage) {
			try {
				LOG.info("processor page:" + doingPage.getOriginalUrl());
				// 1.设置下载器代理
				downer.setHttpProxy(httpProxyPool.getHttpProxy());
				startTime = System.currentTimeMillis();
				// 2. 下载数据
				downer.down(doingPage);
				endTime = System.currentTimeMillis();
				LOG.debug("downer down time:" + (endTime - startTime));
				startTime = System.currentTimeMillis();
				// 3. 抽取前操作
				beforeExtract(doingPage);
				// 4.抽取结果
				ResultContext resultContext = extracter.extract(doingPage);
				// 5.抽取后操作
				afterExtract(doingPage, resultContext);
				endTime = System.currentTimeMillis();
				LOG.debug("extracter extract time:" + (endTime - startTime));
				startTime = System.currentTimeMillis();
				// 7.存储数据
				int storeCount = store.store(resultContext);
				getWorkerSnapshot().setTotalResultCount(getWorkerSnapshot().getTotalResultCount() + storeCount);
				endTime = System.currentTimeMillis();
				LOG.debug("store time:" + (endTime - startTime));
				// 8.记录操作数据
				workQueue.finish(doingPage);// 完成page处理
				// 9.完成操作
				onComplete(doingPage, resultContext);
				LOG.info("finished processor page:" + doingPage.getOriginalUrl());
			} catch (Exception e) {
				throw new RuntimeException("process page err:" + doingPage.getOriginalUrl(), e);
			}
		} else {
			// 没有处理数据时 设置 state == WorkerLifecycleState.FINISHED
			compareAndSetState(WorkerLifecycleState.STARTED, WorkerLifecycleState.FINISHED);
		}
	}

	/**
	 * 内部初始化
	 */
	protected abstract void insideInit();

	/**
	 * 下载前处理
	 * 
	 * @param doingPage
	 */
	protected abstract void beforeDown(Page doingPage);

	/**
	 * 抽取数据前
	 * 
	 * @param doingPage
	 */
	protected abstract void beforeExtract(Page doingPage);

	/**
	 * 抽取数据后
	 * 
	 * @param doingPage
	 * @param resultContext
	 */
	protected abstract void afterExtract(Page doingPage, ResultContext resultContext);

	/**
	 * 完成操作
	 * 
	 * @param doingPage
	 */
	protected abstract void onComplete(Page doingPage, ResultContext resultContext);

	/**
	 * 内部异常处理，如果成功处理返回true 否则返回false;
	 * 
	 * @param e
	 * @param doingPage
	 * @return
	 */
	protected abstract boolean insideOnError(Exception e, Page doingPage);

	protected void onError(Exception e) {
		if (null != doingPage) {
			if (e instanceof DownerException) {
				long restTime = 1000 * 5;
				LOG.info("perhaps server is too busy,it's time for having a rest(" + restTime + ")");
				ThreadUtils.sleep(restTime);
			}
			Exception insideException = null;
			boolean insideExceptionResult = false;
			try {
				insideExceptionResult = insideOnError(e, doingPage);
			} catch (Exception e1) {
				insideException = e1;
				LOG.error("insideOnError err page:" + doingPage.getFinalUrl(), e1);
			}
			// 判断内部处理是否可处理,如果不可处理那么这里默认处理
			if (!insideExceptionResult) {
				String msg = null;
				if (null == insideException
						&& doingPage.getRetryProcess() < Constants.WOKER_PROCESS_PAGE_MAX_RETRY_COUNT) {
					doingPage.setRetryProcess(doingPage.getRetryProcess() + 1);
					workQueue.retryPush(doingPage);
					msg = "retry processor[" + doingPage.getRetryProcess() + "] page:" + doingPage.getFinalUrl();
				} else {
					workQueue.pushErr(doingPage);
					workQueue.finish(doingPage);
					msg = "retry process count[" + doingPage.getRetryProcess() + "]>="
							+ Constants.WOKER_PROCESS_PAGE_MAX_RETRY_COUNT + " and push to err queue:"
							+ doingPage.getFinalUrl();
				}
				LOG.error(msg, e);
			}
		}
	}

	public WorkQueue getWorkQueue() {
		return workQueue;
	}

	public Downer getDowner() {
		return downer;
	}

	public Extracter getExtracter() {
		return extracter;
	}

	public Store getStore() {
		return this.store;
	}

	public long getFindElementTimeout() {
		return findElementTimeout;
	}

	public Site getSite() {
		return site;
	}

	protected void insideDestroy() {
		if (null != downer) {
			downer.close();
		}
		if (null != httpProxyPool) {
			httpProxyPool.destroy();
		}
	}
}
