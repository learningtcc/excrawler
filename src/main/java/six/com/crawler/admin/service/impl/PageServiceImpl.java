package six.com.crawler.admin.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import six.com.crawler.admin.service.PageService;
import six.com.crawler.dao.PageDao;
import six.com.crawler.entity.Page;

/** 
* @author  作者 
* @E-mail: 359852326@qq.com 
* @date 创建时间：2016年9月9日 下午12:49:23 
*/
@Service
public class PageServiceImpl implements PageService{

	@Autowired 
	private PageDao pageDao;
	
	@Override
	public List<Page> query(String sitecode,List<String> urlMd5s) {
		List<Page> result=pageDao.queryByPageKeys(sitecode, urlMd5s);
		return result;
	}

	@Override
	public void save(List<Page> page) {
		
	}
	
	public PageDao getPageDao() {
		return pageDao;
	}
	public void setPageDao(PageDao pageDao) {
		this.pageDao = pageDao;
	}

}
