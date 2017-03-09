package six.com.crawler.admin.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import six.com.crawler.common.entity.DoneInfo;
import six.com.crawler.common.entity.Page;
import six.com.crawler.common.service.WorkQueueService;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年3月8日 下午3:24:18
 */

@Controller
public class QueueApi {

	@Autowired
	private WorkQueueService workQueueService;

	@RequestMapping(value = "/crawler/queue/getQueueInfo/{queueName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<List<Page>> getQueueInfo(@PathVariable("queueName") String queueName) {
		ResponseMsg<List<Page>> msg = new ResponseMsg<>();
		List<Page> result = workQueueService.getQueueInfo(queueName);
		msg.setData(result);
		return msg;
	}

	@RequestMapping(value = "/crawler/queue/getErrQueueInfo/{queueName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<List<Page>> getErrQueueInfo(@PathVariable("queueName") String queueName) {
		ResponseMsg<List<Page>> msg = new ResponseMsg<>();
		List<Page> result = workQueueService.getErrQueueInfo(queueName);
		msg.setData(result);
		return msg;
	}
	
	
	@RequestMapping(value = "/crawler/queue/getQueueDones", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<List<DoneInfo>> getQueueDones() {
		ResponseMsg<List<DoneInfo>> msg = new ResponseMsg<>();
		List<DoneInfo> result = workQueueService.getQueueDones();
		msg.setData(result);
		return msg;
	}

	@RequestMapping(value = "/crawler/queue/cleanQueueDone/{queueName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<String> cleanQueueDone(@PathVariable("queueName") String queueName) {
		ResponseMsg<String> msg = new ResponseMsg<>();
		String result = workQueueService.cleanQueueDones(queueName);
		msg.setMsg(result);
		return msg;
	}


	@RequestMapping(value = "/crawler/queue/cleanQueue/{queueName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<String> cleanQueue(@PathVariable("queueName") String queueName) {
		ResponseMsg<String> msg = new ResponseMsg<>();
		String result = workQueueService.cleanQueue(queueName);
		msg.setMsg(result);
		return msg;
	}

	@RequestMapping(value = "/crawler/queue/repairQueue/{queueName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<String> repairQueue(@PathVariable("queueName") String queueName) {
		ResponseMsg<String> msg = new ResponseMsg<>();
		String result = workQueueService.repairQueue(queueName);
		msg.setMsg(result);
		return msg;
	}

}
