package six.com.crawler.work.plugs;

import six.com.crawler.entity.Page;
import six.com.crawler.entity.ResultContext;
import six.com.crawler.work.AbstractCrawlWorker;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年2月23日 上午9:08:57
 */
public class TmsfHouseInfoWorker extends AbstractCrawlWorker {

	@Override
	protected void insideInit() {}

	protected void beforeDown(Page doingPage) {

	}

	@Override
	protected void beforeExtract(Page doingPage) {}

	@Override
	protected void afterExtract(Page doingPage, ResultContext result) {

	}

	@Override
	protected void onComplete(Page doingPage, ResultContext resultContext) {

	}

	@Override
	public boolean insideOnError(Exception t, Page doingPage) {
		return false;
	}

}
