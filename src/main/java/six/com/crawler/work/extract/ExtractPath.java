package six.com.crawler.work.extract;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * @author sixliu E-mail:359852326@qq.com
 * @version 创建时间：2016年5月17日 下午7:48:04 类说明
 */ 
public class ExtractPath implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3396441928276937779L;

	private String name;

	private String siteCode;// 站点id

	private int ranking;

	private String path;// 要获取的值的属性名字

	private String filterPath;// 需要过滤的路径path

	private String extractAttName;// 要获取的值的属性名字

	private String substringStart; //截止字符开始字符，不设值 默认从头开始

	private String substringEnd;// 截取字符结尾字符,不设值 默认截取到尾部

	private String compareAttName;// 用来比较 的att name

	private String containKeyWord;// 包含的关键字

	private String replaceWord;// 要替换的词

	private String replaceValue;// 替换的值
	
	private String appendHead; // 在头部追加字符

	private String appendEnd;// 在尾部追加字符
	
	private int extractEmptyCount;// 抽取结果为空的计数

	private String describe;// 描述


	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute
	public String getSiteCode() {
		return siteCode;
	}

	public void setSiteCode(String siteCode) {
		this.siteCode = siteCode;
	}

	@XmlAttribute
	public int getRanking() {
		return ranking;
	}

	public void setRanking(int ranking) {
		this.ranking = ranking;
	}

	@XmlElement(name = "path")
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@XmlElement(name = "filterPath")
	public String getFilterPath() {
		return filterPath;
	}

	public void setFilterPath(String filterPath) {
		this.filterPath = filterPath;
	}

	@XmlAttribute
	public String getExtractAttName() {
		return extractAttName;
	}

	public void setExtractAttName(String extractAttName) {
		this.extractAttName = extractAttName;
	}

	@XmlAttribute
	public String getAppendHead() {
		return appendHead;
	}

	public void setAppendHead(String appendHead) {
		this.appendHead = appendHead;
	}

	@XmlAttribute
	public String getAppendEnd() {
		return appendEnd;
	}

	public void setAppendEnd(String appendEnd) {
		this.appendEnd = appendEnd;
	}

	@XmlElement(name = "substringStart")
	public String getSubstringStart() {
		return substringStart;
	}

	public void setSubstringStart(String substringStart) {
		this.substringStart = substringStart;
	}

	@XmlElement(name = "substringEnd")
	public String getSubstringEnd() {
		return substringEnd;
	}

	public void setSubstringEnd(String substringEnd) {
		this.substringEnd = substringEnd;
	}
	
	@XmlAttribute
	public String getCompareAttName() {
		return compareAttName;
	}

	public void setCompareAttName(String compareAttName) {
		this.compareAttName = compareAttName;
	}

	@XmlAttribute
	public String getContainKeyWord() {
		return containKeyWord;
	}

	public void setContainKeyWord(String containKeyWord) {
		this.containKeyWord = containKeyWord;
	}

	@XmlElement(name = "replaceWord")
	public String getReplaceWord() {
		return replaceWord;
	}

	public void setReplaceWord(String replaceWord) {
		this.replaceWord = replaceWord;
	}

	@XmlElement(name = "replaceValue")
	public String getReplaceValue() {
		return replaceValue;
	}

	public void setReplaceValue(String replaceValue) {
		this.replaceValue = replaceValue;
	}

	@XmlElement(name = "describe")
	public String getDescribe() {
		return describe;
	}

	public void setDescribe(String describe) {
		this.describe = describe;
	}

	public int getExtractEmptyCount() {
		return extractEmptyCount;
	}

	public void setExtractEmptyCount(int extractEmptyCount) {
		this.extractEmptyCount = extractEmptyCount;
	}

}
