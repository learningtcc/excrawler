package six.com.crawler.work.plugs;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import six.com.crawler.entity.Page;
import six.com.crawler.entity.PageType;
import six.com.crawler.entity.ResultContext;
import six.com.crawler.http.HttpMethod;
import six.com.crawler.utils.ArrayListUtils;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.space.WorkSpace;

public class DLDCGXProjectListWorker extends AbstractCrawlWorker {

	WorkSpace<Page> projectInfoQueue;

	String pageIndexTemplate = "<<pageIndex>>";

	int pageIndex = 1;

	int pageCount;

	String projectListUrlTemplate = "http://218.25.171.244/InfoLayOut_GX/Config/LoadProcToXML.aspx?pid=Arty_YSXX&csnum=2&cn1=pageindex&cv1="
			+ pageIndexTemplate + "&cn2=DefOrderfldName&cv2=xkzh";

	String refererUrl;

	private Page buildPage(int pageIndex, String refererUrl) {
		String url = StringUtils.replace(projectListUrlTemplate, pageIndexTemplate, String.valueOf(pageIndex));
		Page page = new Page(getSite().getCode(), 1, url, url);
		page.setReferer(refererUrl);
		page.setMethod(HttpMethod.POST);
		page.setType(PageType.XML.value());
		page.getParameters().put("pid", "Arty_YSXX");
		page.getParameters().put("cv1", pageIndex);
		page.getParameters().put("cv2", "xkzh");
		return page;
	}

	@Override
	protected void insideInit() {
		projectInfoQueue = getManager().getWorkSpaceManager().newWorkSpace("dldc_gx_project_info", Page.class);
		Page firstPage = buildPage(pageIndex, refererUrl);
		getWorkSpace().clearDoing();
		getWorkSpace().push(firstPage);
	}

	@Override
	protected void beforeDown(Page doingPage) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void beforeExtract(Page doingPage) {
		if (pageCount == -1) {
			pageCount = Integer.parseInt(doingPage.getDoc().select("NewDataSet>Table1>PageCount").first().ownText());
		}
	}

	@Override
	protected void afterExtract(Page doingPage, ResultContext resultContext) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onComplete(Page doingPage, ResultContext resultContext) {
		String BASE_URL = "http://218.25.171.244/InfoLayOut_GX/Config/LoadProcToXML.aspx?pid=Arty_YSXX&csnum=1&cn1=ysxkid&cv1=";
		List<String> projectIds = resultContext.getExtractResult("projectId");
		if (null != projectIds) {
			for (String projectId : projectIds) {
				String URL = BASE_URL + projectId;
				Page projectInfo = new Page(getSite().getCode(), 1, URL, URL);
				projectInfo.setReferer(doingPage.getFinalUrl());
				projectInfo.setMethod(HttpMethod.GET);
				projectInfo.setType(PageType.XML.value());

				projectInfo.getMetaMap().put("projectId", ArrayListUtils.asList(projectId));
				projectInfoQueue.push(projectInfo);
			}
		}
		pageIndex++;
		if (pageIndex <= pageCount) {
			Page page = buildPage(pageIndex, doingPage.getFinalUrl());
			getWorkSpace().push(page);
		}
	}

}
