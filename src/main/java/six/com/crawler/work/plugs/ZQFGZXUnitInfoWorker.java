package six.com.crawler.work.plugs;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import six.com.crawler.entity.Page;
import six.com.crawler.entity.ResultContext;
import six.com.crawler.utils.ArrayListUtils;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.downer.HttpMethod;
import six.com.crawler.work.exception.ProcessWorkerCrawlerException;
import six.com.crawler.work.space.WorkSpace;

/**
 * 
 * @author weijiyong@tospur.com
 *
 */
public class ZQFGZXUnitInfoWorker extends AbstractCrawlWorker{

	WorkSpace<Page> roomStateInfoQueue;
	
	@Override
	protected void insideInit() {
		roomStateInfoQueue = getManager().getWorkSpaceManager().newWorkSpace("zqfgzx_room_state_info", Page.class);
	}

	@Override
	protected void beforeDown(Page doingPage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void beforeExtract(Page doingPage) {
		String attrCss="dl[class=dl-horizontal buildinginfo]:eq(0)>dd";
		Elements attrs=doingPage.getDoc().select(attrCss);
		if(attrs.size()<2){
			throw new ProcessWorkerCrawlerException("don't find state node:" + attrCss+",pageSrc is :"+doingPage.getPageSrc());
		}else{
			Element developer=attrs.get(2);
			doingPage.getMetaMap().put("developer", ArrayListUtils.asList(developer.ownText()));
		}
	}

	@Override
	protected void afterExtract(Page doingPage, ResultContext resultContext) {
		
	}

	@Override
	protected void onComplete(Page doingPage, ResultContext resultContext) {
		Page roomStatePage=new Page(doingPage.getSiteCode(), 1, doingPage.getFinalUrl(),  doingPage.getFinalUrl());
		roomStatePage.setReferer(doingPage.getReferer());
		roomStatePage.setMethod(HttpMethod.GET);
		roomStatePage.getMetaMap().put("unitId", doingPage.getMeta("unitId"));
		
		roomStateInfoQueue.push(roomStatePage);
	}
}
