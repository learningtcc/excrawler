package six.com.crawler.work.plugs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import six.com.crawler.common.entity.Page;
import six.com.crawler.common.entity.ResultContext;
import six.com.crawler.work.AbstractCrawlWorker;
import six.com.crawler.work.Constants;
import six.com.crawler.work.RedisWorkQueue;

/**
 * 抓取宁波住宅与房地产网(楼层单元状态的信息)
 * @author 38342
 * @version v1.0
 * date:20170301
 */
public class NbCnnbfdcRoomStateInfoWorker extends AbstractCrawlWorker{
	
	RedisWorkQueue roomInfoQueue;
	
	private Map<String,String> roomStates = new HashMap<String,String>();

	@Override
	protected void insideInit() {
		roomInfoQueue = new RedisWorkQueue(getManager().getRedisManager(), "nb_cnnbfdc_room_info");
	}

	@Override
	protected void beforeDown(Page doingPage) {
	}

	@Override
	protected void beforeExtract(Page doingPage) {
		//获取所有状态信息
		if(roomStates.isEmpty()){
			String allCssQuery = "table[width='600']>tbody>tr>td>font";
			Elements elements2 = doingPage.getDoc().select(allCssQuery);
			if(null==elements2){
				throw new RuntimeException("don't find state node:" + allCssQuery);
			}
			for(Element ets : elements2){
				String key = ets.attr("color");
				String value = ets.ownText();
				roomStates.put(key, value);
			}
		}
		String unitId = doingPage.getMeta("unitId").get(0);
		List<String> unitIds = new ArrayList<String>();
		List<String> roomNos = new ArrayList<String>();
		String styleCssQuery = "table[id]>tbody>tr>td>table";
		Elements styleElements = doingPage.getDoc().select(styleCssQuery);
		List<String> roomStateList = new ArrayList<String>();
		for (Element et : styleElements) {
			String bgColor = "";
			String style = et.attr("style");
			if(style!=null){
				String[] ss = style.split(";");
				for (String string : ss) {
					if(string.contains("background-color")){
						bgColor = string.split(":")[1];
						break;
					}
				}
			}
			String roomState = roomStates.get(bgColor);
			roomState = roomState.replace(": ", "");
			Elements elements = et.select("tbody>tr:eq(1)>td>a");
			if(elements==null || elements.size()==0){
				//此时匹配的是未备案的房间号
				elements = et.select("tbody>tr:eq(1)>td");
			}
			String s = elements.first().ownText();
			roomNos.add(s);
			roomStateList.add(roomState);
			unitIds.add(unitId);
		}
		doingPage.getMetaMap().put("roomState", roomStateList);
		doingPage.getMetaMap().put("unitId", unitIds);
		doingPage.getMetaMap().put("roomNo", roomNos);
	}

	@Override
	protected void afterExtract(Page doingPage, ResultContext resultContext) {
	}

	@Override
	protected void onComplete(Page doingPage, ResultContext resultContext) {
		String cssQuery = "table[id]";
		Elements roomStateElements = doingPage.getDoc().select(cssQuery);
		List<String> roomNos = resultContext.getExtractResult("roomNo");
		if(roomNos!=null){
			for(int i=0;i<roomNos.size();i++){
				String roomStateId = resultContext.getOutResults().get(i).get(Constants.DEFAULT_RESULT_ID);
				String id = roomStateElements.get(i).attr("id");
				id = id.replace("room", "");
				String pageUrl = "http://newhouse.cnnbfdc.com/openRoomData.aspx?roomId=" + id;
				Page roomPage = new Page(doingPage.getSiteCode(), 1, pageUrl, pageUrl);
				roomPage.getMetaMap().computeIfAbsent("roomStateId",mapKey->new ArrayList<>()).add(roomStateId);
				roomPage.setReferer(doingPage.getFinalUrl());
				roomInfoQueue.push(roomPage);
			}
		}
	}

	@Override
	protected void insideOnError(Exception e, Page doingPage) {
	}

}
