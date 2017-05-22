package six.com.crawler.work.downer.impl;


import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Request;
import six.com.crawler.entity.HttpProxy;
import six.com.crawler.entity.Page;
import six.com.crawler.entity.PageType;
import six.com.crawler.utils.UrlUtils;
import six.com.crawler.utils.AutoCharsetDetectorUtils.ContentType;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.downer.AbstractDowner;
import six.com.crawler.work.downer.HttpClient;
import six.com.crawler.work.downer.HttpConstant;
import six.com.crawler.work.downer.HttpMethod;
import six.com.crawler.work.downer.HttpResult;
import six.com.crawler.work.downer.PostContentType;
import six.com.crawler.work.downer.cache.DownerCache;
import six.com.crawler.work.downer.exception.DownerException;
import six.com.crawler.work.downer.exception.ManyRedirectDownException;

/**
 * @author six
 * @date 2016年6月15日 下午5:12:48
 */
public class OkHttpDowner extends AbstractDowner {

	protected final static Logger LOG = LoggerFactory.getLogger(OkHttpDowner.class);

	private HttpClient httpClient;

	public OkHttpDowner(AbstractCrawlWorker worker, boolean openDownCache, boolean useDownCache,
			DownerCache downerCache) {
		super(worker, openDownCache, useDownCache, downerCache);
		httpClient = worker.getManager().getHttpClient();
	}

	protected HttpResult insideDown(Page page) throws DownerException {
		HttpResult result = executeDown(page);
		String html = httpClient.getHtml(result, page.getType() == PageType.XML ? ContentType.XML : ContentType.HTML);
		String charset = result.getCharset();
		page.setPageSrc(html);
		page.setCharset(charset);
		return result;
	}

	private HttpResult executeDown(Page page) throws DownerException {
		String requestUrl = page.getOriginalUrl();
		String requesReferer = page.getReferer();
		HttpMethod httpMethod = page.getMethod();
		PostContentType postContentType = page.getPostContentType();
		Map<String, Object> parameters = page.getParameters();
		HttpResult result = null;
		int redirectTime = 0;
		HttpProxy httpProxy = null;
		do {
			httpProxy = getHttpProxy();
			Request request = httpClient.buildRequest(requestUrl, requesReferer, httpMethod, HttpConstant.headMap,
					postContentType, parameters, httpProxy);
			LOG.info("execute request[" + requestUrl + "] by proxy["
					+ (null != httpProxy ? httpProxy.toString() : "noproxy") + "]");
			result = httpClient.executeRequest(request);
			if (StringUtils.isNotBlank(result.getRedirectedUrl())) {
				requestUrl = result.getRedirectedUrl();
				requesReferer = result.getReferer();
				httpMethod = HttpMethod.GET;
				postContentType = null;
				parameters = null;
				redirectTime++;
				if (redirectTime > HttpConstant.REDIRECT_TIMES) {
					throw new ManyRedirectDownException(
							"execute request[" + request.url() + "] redirectTime is too many");
				}
			}
		} while (StringUtils.isNotBlank(result.getRedirectedUrl()));
		return result;
	}

	public byte[] downBytes(Page page) throws DownerException {
		HttpResult result = executeDown(page);
		return result.getData();
	}

	@Override
	protected void insideColose() {
		String domain = UrlUtils.getDomain(getHtmlCommonWorker().getSite().getMainUrl());
		httpClient.getCookiesStore().close(domain);
	}
}