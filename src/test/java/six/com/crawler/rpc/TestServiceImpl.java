package six.com.crawler.rpc;

/**
 * @author 作者
 * @E-mail: 359852326@qq.com
 * @date 创建时间：2017年4月6日 下午6:21:53
 */
public class TestServiceImpl implements TestService {

	@RpcService(name="say")
	@Override
	public String say(String name) {
		return "你好:" + name;
	}

}
