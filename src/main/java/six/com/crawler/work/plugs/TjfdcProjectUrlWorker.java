package six.com.crawler.work.plugs;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import six.com.crawler.common.entity.Job;
import six.com.crawler.common.entity.Page;
import six.com.crawler.common.entity.PageType;
import six.com.crawler.common.entity.ResultContext;
import six.com.crawler.common.entity.Site;
import six.com.crawler.common.utils.UrlUtils;
import six.com.crawler.common.utils.WebDriverUtils;
import six.com.crawler.schedule.AbstractSchedulerManager;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.RedisWorkQueue;
import six.com.crawler.work.WorkQueue;
import six.com.crawler.work.WorkerLifecycleState;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年1月11日 上午10:48:39
 */
public class TjfdcProjectUrlWorker extends AbstractCrawlWorker {

	protected final static Logger LOG = LoggerFactory.getLogger(TjfdcProjectUrlWorker.class);
	private RedisWorkQueue tjfdcProjecInfoQueue;
	private String projectUrlXpath = "//div[@id='divContent']/ul/li/div/div/h3/a";
	private String nextPageXpath = "//a[@id='SplitPageModule1_lbnNextPage']";

	public TjfdcProjectUrlWorker(String name, AbstractSchedulerManager manager, Job job, Site site, WorkQueue stored) {
		super(name, manager, job, site, stored);
	}

	@Override
	protected void insideInit() {
		tjfdcProjecInfoQueue = new RedisWorkQueue(getManager().getRedisManager(), "tjfdc_projec_info");
	}

	protected void beforeDown(Page doingPage) {

	}

	@Override
	protected void beforeExtract(Page doingPage) {
		WebDriver webDriver = getDowner().getWebDriver();
		List<WebElement> projectUlrElements = WebDriverUtils.findElements(webDriver, projectUrlXpath,
				findElementTimeout);
		if (null != projectUlrElements) {
			for (WebElement projectUlrElement : projectUlrElements) {
				String tempUrl = projectUlrElement.getAttribute("href");
				tempUrl = UrlUtils.paserUrl(doingPage.getBaseUrl(), doingPage.getFinalUrl(), tempUrl);
				Page newPage = new Page(doingPage.getSiteCode(), 1, tempUrl, tempUrl);
				newPage.setReferer(doingPage.getFinalUrl());
				newPage.setType(PageType.DATA.value());
				tjfdcProjecInfoQueue.push(newPage);
			}
		}
		WebElement nextPageElement = WebDriverUtils.findElement(webDriver, nextPageXpath, findElementTimeout);
		String disabled = nextPageElement.getAttribute("disabled");
		if ("disabled".equalsIgnoreCase(disabled)) {
			// 没有处理数据时 设置 state == WorkerLifecycleState.STOPED
			compareAndSetState(WorkerLifecycleState.STARTED, WorkerLifecycleState.STOPED);
		} else {
			WebDriverUtils.click(webDriver, nextPageElement, nextPageXpath, findElementTimeout);
		}

	}

	@Override
	protected void afterExtract(Page doingPage, ResultContext result) {

	}

	@Override
	protected void insideOnError(Exception t, Page doingPage) {

	}

	@Override
	protected void onComplete(Page doingPage,ResultContext resultContext) {

	}

}
