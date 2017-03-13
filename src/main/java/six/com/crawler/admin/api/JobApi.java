package six.com.crawler.admin.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import six.com.crawler.common.entity.Job;
import six.com.crawler.common.entity.JobSnapshot;
import six.com.crawler.common.entity.PageQuery;
import six.com.crawler.common.service.JobService;

/**
 * @author six
 * @date 2016年5月31日 下午2:56:54 爬虫 Job 任务 api
 */
@Controller
public class JobApi extends BaseApi {

	@Autowired
	private JobService jobService;

	@RequestMapping(value = "/crawler/job/query/{pageIndex}/{pageSize}/{jobName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<PageQuery<Job>> queryJobs(@PathVariable("pageIndex") int pageIndex,
			@PathVariable("pageSize") int pageSize,
			@PathVariable("jobName") String jobName) {
		ResponseMsg<PageQuery<Job>> responseMsg = createResponseMsg();
		jobService.queryJobs(responseMsg, jobName, pageIndex, pageSize);
		return responseMsg;
	}


	@RequestMapping(value = "/crawler/job/save", method = RequestMethod.POST)
	@ResponseBody
	public ResponseMsg<Boolean> addJob(Job job) {
		return null;
	}

	@RequestMapping(value = "/crawler/job/queryjobinfo/{jobName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<Map<String, Object>> queryJobInfo(@PathVariable("jobName") String jobName) {
		ResponseMsg<Map<String, Object>> msg = new ResponseMsg<>();
		Map<String, Object> result = jobService.queryJobInfo(jobName);
		msg.setData(result);
		return msg;
	}

	@RequestMapping(value = "/crawler/job/getHistoryJobSnapshot/{jobName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<List<JobSnapshot>> getHistoryJobSnapshot(@PathVariable("jobName") String jobName) {
		ResponseMsg<List<JobSnapshot>> msg = new ResponseMsg<>();
		List<JobSnapshot> result = jobService.queryJobSnapshotsFromHistory(jobName);
		msg.setData(result);
		return msg;
	}

	/**
	 * 向前段推送 job的 活动信息数据
	 * 
	 * @param jobNameList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@MessageMapping("/jobSnapshot")
	@SendTo("/topic/job/jobSnapshot")
	public ResponseMsg<List<JobSnapshot>> jobSnapshot(List<Object> list) {
		ResponseMsg<List<JobSnapshot>> msg = new ResponseMsg<>();
		List<JobSnapshot> result = new ArrayList<>();
		if (list != null) {
			JobSnapshot jobSnapshot = null;
			for (Object ob : list) {
				if (ob instanceof Job) {
					Job job = (Job) ob;
					jobSnapshot = jobService.getJobSnapshotFromRegisterCenter(job.getLocalNode(), job.getName(),
							job.getQueueName());
				} else {
					Map<String, String> map = (Map<String, String>) ob;
					jobSnapshot = jobService.getJobSnapshotFromRegisterCenter(map.get("hostNode"), map.get("name"),
							map.get("queueName"));
				}
				if (null != jobSnapshot) {
					result.add(jobSnapshot);
				}
			}
		}
		msg.setData(result);
		return msg;
	}

	@RequestMapping(value = "/crawler/job/getLoclaAll", method = RequestMethod.GET)
	@ResponseBody
	public ResponseMsg<List<Job>> getLoclaAllJobs() {
		return null;
	}

	@RequestMapping(value = "/crawler/job/upload/profile", method = RequestMethod.POST)
	@ResponseBody
	public ResponseMsg<String> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
		ResponseMsg<String> responseMsg = createResponseMsg();
		String msg = uploadFile(jobService, multipartFile);
		responseMsg.setMsg(msg);
		return responseMsg;
	}

	@RequestMapping(value = "/crawler/job/download/profile/{jobName}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("jobName") String jobName) {
		return downloadFile(jobService, jobName);
	}

	public JobService getJobService() {
		return jobService;
	}

	public void setJobService(JobService jobService) {
		this.jobService = jobService;
	}
}
