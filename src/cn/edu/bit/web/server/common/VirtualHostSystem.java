package cn.edu.bit.web.server.common;
import java.io.File;
import cn.edu.bit.web.server.config.WebConfig;


public class VirtualHostSystem {
	private VirtualHostSystem() {}
	
	private static vhost[] hosts = new vhost[0];
	
	private static final String PATH = WebConfig.webRootPath + File.separatorChar;
	private static final String defaultpath = PATH + WebConfig.defaultRootPath;
	
	public final static void init() {
		if (!WebConfig.vHostEnable) return;
		hosts = new vhost[WebConfig.hostsMap.size()];
		int hInx = 0;
		for (String hostName:WebConfig.hostsMap.keySet()) {
			String sFile = PATH+WebConfig.hostsMap.get(hostName);
			if (sFile!=null) {
				File f = new File(sFile);
				if (f.isDirectory()) {
					hosts[hInx] = new vhost();
					hosts[hInx].host = hostName;
					hosts[hInx].path = f;
					++hInx;
				}
			}
		}
	}
	
	/**
	 * 根据请求头域的host域获得本地文件夹的映射,一定会返回有效的路径
	 * @param hha - 请求头
	 * @return - 本地路径String;
	 */
	public final static String getVhost(HttpHeadAnalyser hha) {
		if (!WebConfig.vHostEnable) return defaultpath;
		String host = hha.getHost();
		if (host!=null) {
			for (int i=0; i<hosts.length; ++i) {
				if (hosts[i]==null) break;
				if (hosts[i].host.compareToIgnoreCase(host)==0) {
					return hosts[i].path.getPath();
				}
			}
		}
		return defaultpath;
	}
}

/** 虚拟主机数据封装 */
class vhost {
	public String host;
	public File path;
}
