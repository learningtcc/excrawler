package six.com.crawler.work.plugs;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import six.com.crawler.entity.Page;
import six.com.crawler.entity.PageType;
import six.com.crawler.entity.ResultContext;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.downer.HttpMethod;

/**
 * 
 * @author weijiyong@tospur.com
 *
 */
public class ZQFGZXProjectInfoWorker  extends AbstractCrawlWorker{

	String pageIndexTemplate = "<<pageIndex>>";
	String pageCountCss = "a[class=linktwo]";
	int pageIndex = 1;
	int pageCount = -1;
	String projectListUrlTemplate = "http://www.zqfgzx.org.cn/index.php?s=/Property/search/page/" + pageIndexTemplate+".html";

	
	private Page buildPage(int pageIndex, String refererUrl) {
		String pageUrl = StringUtils.replace(projectListUrlTemplate, pageIndexTemplate, String.valueOf(pageIndex));
		Page page = new Page(getSite().getCode(), 1, pageUrl, pageUrl);
		page.setReferer(refererUrl);
		page.setMethod(HttpMethod.GET);
		page.setType(PageType.LISTING.value());
		return page;
	}
	
	
	@Override
	protected void insideInit() {
		String firstRefUrl="http://www.zqfgzx.org.cn/index.php?s=/Property/search.html";
		int pageIndex = 1;
		Page firstPage = buildPage(pageIndex,firstRefUrl);// 初始化第一页
		getDowner().down(firstPage);
		
		Element pageCountElement = firstPage.getDoc().select(pageCountCss).last();
		String pageCountStr = StringUtils.substringBetween(pageCountElement.attr("href"), "/index.php?s=/Property/search/page/",".html");
		int pageCount = Integer.valueOf(pageCountStr);
		
		getWorkSpace().clearDoing();
		
		getWorkSpace().push(firstPage);
		
		Page lastPage = firstPage;
		while (pageIndex <= pageCount) {
			Page nextPage = buildPage(pageIndex, lastPage.getFinalUrl());// 初始化第一页
			getWorkSpace().push(nextPage);
			lastPage = nextPage;
			pageIndex++;
		}
	}

	@Override
	protected void beforeDown(Page doingPage) {
		
	}

	@Override
	protected void beforeExtract(Page doingPage) {
		
	}

	@Override
	protected void afterExtract(Page doingPage, ResultContext resultContext) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onComplete(Page doingPage, ResultContext resultContext) {
		// TODO Auto-generated method stub
		
	}

}
