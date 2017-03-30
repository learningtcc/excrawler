package six.com.crawler.work.plugs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import six.com.crawler.entity.Page;
import six.com.crawler.entity.PageType;
import six.com.crawler.entity.ResultContext;
import six.com.crawler.http.HttpMethod;
import six.com.crawler.utils.JsonUtils;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.extract.Extracter;
import six.com.crawler.work.space.RedisWorkSpace;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年2月22日 下午1:10:41
 */
public class TmsfPresellInfoWorker extends AbstractCrawlWorker {

	Map<String, String> jsonKeyMap;
	RedisWorkSpace<Page> houseUrlQueue;
	String sidTemplate = "<<sid>>";
	String propertyidTemplate = "<<propertyid>>";
	String presellIdTemplate = "<<presellId>>";
	String houseUrlTemplate = "http://www.tmsf.com/newhouse/" + "property_" + sidTemplate + "_" + propertyidTemplate
			+ "_control.htm?" + "presellid=" + presellIdTemplate
			+ "&buildingid=&area=&allprice=&housestate=&housetype=&isopen=";

	@Override
	protected void insideInit() {
		houseUrlQueue = new RedisWorkSpace<Page>(getManager().getRedisManager(), "tmsf_house_url",Page.class);
		jsonKeyMap = new HashMap<>();
		jsonKeyMap.put("propertyid", "property.propertyid");
		jsonKeyMap.put("presellid", "presell.presellid");
		jsonKeyMap.put("projectName", "property.propertyname");
		jsonKeyMap.put("presellName", "presell.presellname");
		jsonKeyMap.put("presellCode", "presell.persellno");
		jsonKeyMap.put("presellIssueDate", "presell.applydate");
		jsonKeyMap.put("presellApplyCompany", "presell.applycorp");
		jsonKeyMap.put("issueCompany", "presell.sendcorp");
		jsonKeyMap.put("superviseBank", "presell.bank");
		jsonKeyMap.put("superviseBankAccount", "presell.bankaccno");
		jsonKeyMap.put("presellApprovalProjectName", "presell.projname");
		jsonKeyMap.put("developer", "property.chname");
		jsonKeyMap.put("buildingAddress", "property.located");
		jsonKeyMap.put("openingDate", "presell.openingdate");
	}

	@Override
	protected void beforeDown(Page doingPage) {

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void beforeExtract(Page doingPage) {
		String presaleJson = doingPage.getPageSrc();
		Map<String, Object> map = JsonUtils.toObject(presaleJson, Map.class);
		for (String field : jsonKeyMap.keySet()) {
			String jsonKey = jsonKeyMap.get(field);
			String[] jsonKeys = StringUtils.split(jsonKey, ".");
			Map<String, Object> tempJsonMap = (Map<String, Object>) map.get(jsonKeys[0]);
			Object jsonValue = tempJsonMap.get(jsonKeys[1]);
			doingPage.getMetaMap().computeIfAbsent(field, mapKey -> new ArrayList<>())
					.add(null != jsonValue ? jsonValue.toString() : "");
		}
	}

	@Override
	protected void afterExtract(Page doingPage, ResultContext result) {

	}

	@Override
	protected void onComplete(Page doingPage, ResultContext resultContext) {
		String sid = resultContext.getExtractResult("sid").get(0);
		String propertyid = resultContext.getExtractResult("propertyid").get(0);
		String presellid = resultContext.getExtractResult("presellid").get(0);
		String systemPresellId = resultContext.getOutResults().get(0).get(Extracter.DEFAULT_RESULT_ID);
		String houseUrl = StringUtils.replace(houseUrlTemplate, sidTemplate, sid);
		houseUrl = StringUtils.replace(houseUrl, propertyidTemplate, propertyid);
		houseUrl = StringUtils.replace(houseUrl, presellIdTemplate, presellid);
		Page presalePage = new Page(doingPage.getSiteCode(), 1, houseUrl, houseUrl);
		presalePage.setReferer(doingPage.getFinalUrl());
		presalePage.setMethod(HttpMethod.GET);
		presalePage.setType(PageType.DATA.value());
		presalePage.getMetaMap().put("presellId_org", Arrays.asList(presellid));
		presalePage.getMetaMap().put("presellId", Arrays.asList(systemPresellId));
		houseUrlQueue.push(presalePage);
	}

	@Override
	public boolean insideOnError(Exception t, Page doingPage) {
		return false;
	}

}
