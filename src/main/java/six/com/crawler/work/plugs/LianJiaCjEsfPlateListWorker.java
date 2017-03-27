package six.com.crawler.work.plugs;

import java.util.Arrays;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import six.com.crawler.entity.Page;
import six.com.crawler.entity.ResultContext;
import six.com.crawler.utils.UrlUtils;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.RedisWorkQueue;
import six.com.crawler.work.WorkQueue;

/** 
* @author  作者 
* @E-mail: 359852326@qq.com 
* @date 创建时间：2017年3月24日 下午4:59:45 
*/
public class LianJiaCjEsfPlateListWorker extends AbstractCrawlWorker {


	WorkQueue nextWorkQueue;
	String houseUrlCss="div[class=list-wrap]>ul>li>div[class=info-panel]>h2>a";
	String nextPageUrlCss="div[class=page-box house-lst-page-box]>a:contains(下一页)";
	
	String districtCss="div[class=option-list gio_district]>a[class!=on]";
	String plateCss="div[class=option-list sub-option-list gio_plate]>a[class!=on]";
	String firstUrl="http://sh.lianjia.com/chengjiao/";
	
	boolean initDistrict;
	
	@Override
	protected void insideInit() {
		nextWorkQueue=new RedisWorkQueue(getManager().getRedisManager(), "lianjia_cjerf_list");
		Page firstPage=new Page(getSite().getCode(), 1, firstUrl, firstUrl);
		getDowner().down(firstPage);
		Elements districtElements=firstPage.getDoc().select(districtCss);
		getWorkQueue().clear();
		for(Element districtElement:districtElements){
			String district=districtElement.text();
			String districtUrl=districtElement.attr("href");
			districtUrl = UrlUtils.paserUrl(firstPage.getBaseUrl(), firstPage.getFinalUrl(),districtUrl);
			Page districtPage=new Page(getSite().getCode(), 1, districtUrl, districtUrl);
			districtPage.setReferer(firstPage.getFinalUrl());
			districtPage.getMetaMap().put("district",Arrays.asList(district));
			districtPage.getMetaMap().put("city",Arrays.asList("sh"));
			districtPage.getMetaMap().put("cityName",Arrays.asList("上海"));
			getWorkQueue().push(districtPage);
		}
	}

	@Override
	protected void beforeDown(Page doingPage) {
		
	}

	@Override
	protected void beforeExtract(Page doingPage) {
		Elements plateElements=doingPage.getDoc().select(plateCss);
		for(Element plateElement:plateElements){
			String section=plateElement.text();
			String plateUrl=plateElement.attr("href");
			plateUrl = UrlUtils.paserUrl(doingPage.getBaseUrl(), doingPage.getFinalUrl(),plateUrl);
			Page districtPage=new Page(getSite().getCode(), 1, plateUrl,plateUrl);
			districtPage.setReferer(doingPage.getFinalUrl());
			districtPage.getMetaMap().put("section",Arrays.asList(section));
			districtPage.getMetaMap().putAll(doingPage.getMetaMap());
			nextWorkQueue.push(districtPage);
		}
	}

	@Override
	protected void afterExtract(Page doingPage, ResultContext resultContext) {
		
	}

	@Override
	protected void onComplete(Page doingPage, ResultContext resultContext) {
		
	}

	@Override
	protected boolean insideOnError(Exception e, Page doingPage) {
		return false;
	}


}
