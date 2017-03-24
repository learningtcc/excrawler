package six.com.crawler.work.plugs;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import six.com.crawler.entity.Page;
import six.com.crawler.entity.PageType;
import six.com.crawler.entity.ResultContext;
import six.com.crawler.exception.BaseException;
import six.com.crawler.http.HttpMethod;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.Constants;
import six.com.crawler.work.RedisWorkQueue;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年2月21日 下午5:46:17
 */
public class TmsfProjectInfoWorker extends AbstractCrawlWorker {

	final static Logger LOG = LoggerFactory.getLogger(TmsfProjectInfoWorker.class);
	int longitudeMax = 135;
	int longitudeMin = 73;
	int latitudeMax = 53;
	int latitudeMix = 4;
	RedisWorkQueue projectInfo1Queue;
	RedisWorkQueue presellUrlQueue;
	String longitude_latitude_div_css = "div[id=boxid1]>div[class=border3 positionr]";
	String mapDivCss = "div[id=boxid1]>div>div";
	String projectNameCss = "div[class=lpxqtop]>div>div>span[class=buidname colordg]";
	String brandNameCss = "div[class=lpxqtop]>div>div>span[class=extension famwei ft14 mgr10]>ul>li:eq(1)";

	@Override
	protected void insideInit() {
		presellUrlQueue = new RedisWorkQueue(getManager().getRedisManager(), "tmsf_presell_url");
		projectInfo1Queue = new RedisWorkQueue(getManager().getRedisManager(), "tmsf_project_info_1");
	}

	@Override
	protected void beforeDown(Page doingPage) {

	}

	class DifferentPages extends BaseException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8769450490734642014L;

		public DifferentPages(String message) {
			super(message);
		}
	}

	@Override
	protected void beforeExtract(Page doingPage) {
		String projectNameCss = "div[id=head]>ul>li";
		Elements projectNameElements = doingPage.getDoc().select(projectNameCss);
		if (null != projectNameElements && !projectNameElements.isEmpty()) {
			throw new DifferentPages("different pages");
		} else {
			Elements mapDivs = doingPage.getDoc().select(mapDivCss);
			if (null != mapDivs && !mapDivs.isEmpty()) {
				Element longitude_latitude_div = doingPage.getDoc().select(longitude_latitude_div_css).first();
				String text = longitude_latitude_div.html();
				String start = "var point = new BMap.Point(";
				String end = ")";
				String[] result = StringUtils.substringsBetween(text, start, end);
				if (null == result || result.length != 1) {
					throw new RuntimeException("find longitude and latitude err");
				}
				String longitudeLatitudeStr = result[0];
				longitudeLatitudeStr = StringUtils.replace(longitudeLatitudeStr, "'", "");
				String[] longitudeAndLatitude = StringUtils.split(longitudeLatitudeStr, ",");
				if (longitudeAndLatitude.length != 2) {
					throw new RuntimeException("find longitude and latitude err");
				}
				double longitude = 0;
				double latitude = 0;
				for (String numStr : longitudeAndLatitude) {
					double num = Double.valueOf(numStr);
					if (num > longitudeMin && num < longitudeMax) {
						longitude = num;
					} else {
						latitude = num;
					}
				}
				doingPage.getMetaMap().put("latitude", Arrays.asList(String.valueOf(latitude)));
				doingPage.getMetaMap().put("longitude", Arrays.asList(String.valueOf(longitude)));
			}
		}
	}

	@Override
	protected void afterExtract(Page doingPage, ResultContext resultContext) {
	}

	@Override
	protected boolean insideOnError(Exception t, Page doingPage) {
		if (t instanceof DifferentPages) {
			projectInfo1Queue.push(doingPage);
			getWorkQueue().finish(doingPage);
			return true;
		}
		return false;
	}

	@Override
	protected void onComplete(Page doingPage, ResultContext resultContext) {
		List<String> presellUrls = resultContext.getExtractResult("presellUrl");
		if (null != presellUrls && presellUrls.size() > 0) {
			String presaleUrl = presellUrls.get(0);
			String sid = resultContext.getExtractResult("sid").get(0);
			String projectId = resultContext.getOutResults().get(0).get(Constants.DEFAULT_RESULT_ID);
			Page presellPage = new Page(doingPage.getSiteCode(), 1, presaleUrl, presaleUrl);
			presellPage.setReferer(doingPage.getFinalUrl());
			presellPage.setMethod(HttpMethod.GET);
			presellPage.setType(PageType.DATA.value());
			presellPage.getMetaMap().put("sid", Arrays.asList(sid));
			presellPage.getMetaMap().put("projectId", Arrays.asList(projectId));
			presellUrlQueue.push(presellPage);
		}
	}

}
