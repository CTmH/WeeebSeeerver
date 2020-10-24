package cn.edu.bit.web.server.config;

import java.util.HashMap;
import java.util.Map;

public class WebConfig {
	private WebConfig() {}
	
	public static String[] defaultIndexfile = {
			"index.htm", "index.html", 
	};
	
	/** 当前使用的语言 */
	public static String language = "Chinese.txt";
	
	/** 默认的主页文件 */
	public static String defaultRootPath = "default";
	
	/** 网站主页根文件夹 */
	public static String webRootPath = "website";
	
	/** 允许文件缓存使用的最大内存量 KB */
	public static long maxMemoryUse = 50*1024*1024;
	
	/** 服务器的最大连接数 */
	public static int maxConnect = 1000;
	
	/** 限制每个IP的最大链接数,为'0'不限制 实现原理参考LinkedIPArray类的说明 */
	public static int ipConnectLimit = 5;
	
	/** 一个文件被缓冲的最长时间 not use */
	public static int cacheFileOutTime = 10*1000;
	
	/** 流量限制 单位:KB/s 为'0'不限制流量 */
	public static int downSpeedLimit = 0;
	
	/** 发送文件缓冲区大小 */
	public static int writeBufferSize = 1024*5;
	
	/** 服务器接受连接后,等待客户发送信息的超时 毫秒 */
	public static int socketReadOuttime = 15000;
	
	/** 服务器默认端口,优先级低于命令参数 */
	public static int serverPort = 11440;
	
	/** 状态窗口刷新信息的间隔 毫秒 */
	public static int refurbishSpace = 5000;
	
	/** 如果处于GUI模式消息窗口显示的文本最多的行数 */
	public static int maxMessageLine = 5000;
	
	/** 小于这个大小的文件被缓存,否则直接访问硬盘 KB*/
	public static int maxCachedFileLength = 5*1024*1024;
	
	/** 每经过n小时,文件缓冲管理器清理不经常用到的文件 Hour*/
	public static int clearCacheTime = 3;
	
	/** 是否支持PHP CGI */
	public static boolean phpSupport = true;
	
	/** 是否开启文件扩展名过滤 */
	public static boolean filterEnable = false;
	
	/** 是否开启CGI系统 */
	public static boolean cgiEnable = false;
	
	/** 是否打印到日志文件 */
	public static boolean printLogFile = true;
	
	/** 是否启用虚拟主机 */
	public static boolean vHostEnable = false;
	
	// ----------------------路径配置-----------------------//
	
	/**  系统配置文件夹 */
	public final static String systemPath = "etc";
	
	/**  系统文件配置	路径 */
	public final static String systemFile = "system.conf";
	
	/** 语言包配置文件 路径 */
	public final static String languagePath = "language";
	
	/** 虚拟主机配置文件 */
	public static String virtualHost = "host.conf";
	public static Map<String, String> hostsMap = new HashMap<String, String>(){
		{
			put("testfile","text.txt");
		}
	};
	
	/** CGI 配置文件 */
	public static String cgiConf = "cgi.conf";
	
	/** 过滤器配置文件 */
	public static String exclude = "exclude.conf";
	
	/** 系统日志文件夹 */
	public static String logPath = "log";
	
	/** Mime 文件类型配置文件 */
	public static String miniTypeConf = "mime.types.conf";
	
}
