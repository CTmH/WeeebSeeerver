package cn.edu.bit.web.server.common;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import cn.edu.bit.web.server.config.WebConfig;


/**
 * 打印日志系统
 */
public final class LogSystem {
	public static final int lineLength = 79;
	public static final String line;
	public static final String filename = "";
	public static final FileOutputStream logout;
		
	static {
		// init FileOutputStream
		if (WebConfig.printLogFile) {
			Calendar c = Calendar.getInstance();
			int y = c.get(Calendar.YEAR);
			int m = c.get(Calendar.MONTH)+1;
			int d = c.get(Calendar.DAY_OF_MONTH);
			String sd = y+""+(m<10?"0"+m:m)+""+(d<10?"0"+d:d)+".txt";
			
			File logf  = new File(WebConfig.logPath+File.separatorChar+sd);
			FileOutputStream t_logout = null;
			try {
				t_logout = new FileOutputStream(logf, true);
				t_logout.write( ("\r\n#\r\n#   "+
						"Start at :"+":"+getDate()+"\r\n#\r\n").getBytes() );
				
			} catch(Exception o) {
				System.out.println("写日志错误"+":"+logf);
				o.printStackTrace();
			}
			logout = t_logout;
		} else {
			logout = null;
		}
		// init line;
		String t = "";
		for (int i=0; i<lineLength; ++i) {
			t += '-';  
		}
		line = t;
	}
	
	/** 不允许取得实例 */
	private LogSystem() {}
	
	/**
	 * 打印正常消息
	 */
	public final static void message(Object o) {
		String s = getDate() + o;
		System.out.println("[Info] "+s);
		printtoFile("[Info] "+s);
	}
	
	/**
	 * 打印错误消息
	 */
	public final static void error(Object o) {
		String s = getDate() + o;
		System.out.println("[Err ] "+s);
		printtoFile("[Err ] "+s);
	}
	
	/**
	 * 打印Http请求的头
	 */
	public final static void httpHead(HttpHeadAnalyser o) {
		StringBuffer sb = new StringBuffer();
		sb.append(getDate()+"HTTP:\r\n");
		sb.append(line+"\r\n");
		sb.append(o.toString()+"\r\n");
		sb.append(new String(o.getMessageBody())+"\r\n");
		sb.append(line+"\r\n");
		
		System.out.println(sb);
		printtoFile(sb);
	}

	
	public static final String getDate() {
		return new Date().toLocaleString()+' ';
	}
	
	/**
	 * 写入日志文件,
	 * @param o - 写入的内容
	 */
	public static final void printtoFile(Object o) {
		if (WebConfig.printLogFile && logout!=null) {
			try {
				logout.write( (o.toString()+"\r\n").getBytes());
			} catch (IOException e) {
				System.out.println("写日志文件错误"+".");
				e.printStackTrace();
			}
		}
	}
}
