package cn.edu.bit.web.server.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import cn.edu.bit.web.server.config.WebConfig;

public class WebServer implements Runnable{
	
	private ServerSocket serverSocket;
	private boolean stop=true;
	private List<LinkThread> socketlist;
	private int port;
	private long totalLink = 0;

	
	public WebServer(int port) {
		// 初始化配置文件
		
		if (port>0) {
			WebConfig.serverPort = port;
		}
		this.port = WebConfig.serverPort;
		
		// 初始化服务器状态
		stop = true;
		socketlist = new LinkedList<LinkThread>();
//		listener = new Listener();
//		printWelcome();
	}
	
	public WebServer() {
		this(-1);
	}
	
	public final void printWelcome() {
		System.out.println("欢迎报考北京理工大学！");
		System.out.println(LogSystem.line);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			serverSocket = new ServerSocket(port);
		} catch(IOException e) {
			System.out.println("["+"错误"+"] "+port+" "+"端口已被占用"+".\n\n");
			JOptionPane.showMessageDialog(	null, 
											port+" "+"端口已被占用"+".", 
											"错误", 
											JOptionPane.ERROR_MESSAGE, 
											null);
			// 如果出错应该立即退出系统
			System.exit(1);
			return;
		}
		
		try {
			System.out.println(	"服务器地址"+":\t"+
					InetAddress.getLocalHost().getHostAddress()+"/" +
					InetAddress.getLocalHost().getHostName() );
		}catch(Exception e) {
			System.out.println(e);
		}
		System.out.println("服务器端口"+":\t"+port);
		System.out.println("服务开始时间"+":\t"+LogSystem.getDate());
		

//		// 初始化过滤系统
//		FilterSystem.init();
//		// 初始化cgi管理器
//		Cgi_Manage.Init();
		// 初始化文件管理器
		FileManager.Init();
//		// 初始化文件类型
//		MimeTypes.init();
		// 初始化虚拟主机配置
		VirtualHostSystem.init();
		
		while (!stop) {
			while (socketlist.size()>=WebConfig.maxConnect) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
			}
			try {
				Socket soket = serverSocket.accept();
				LinkThread sl = new LinkThread(soket);
				socketlist.add(sl);
				++totalLink;
			} catch (IOException e) {
				LogSystem.message("服务器连接侦听错误"+":["+e+"]");
			}
		}
	}

	/** 改变端口 */
	public void port(int p) {
		if (stop) {
			port = p;
		} else {
			throw new IllegalStateException("无法改变端口"+".");
		}
	}
	
	public void start() {
		new Thread(this).start();
		new Thread(new Runnable() {
			// 监听连接的线程
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (!stop || socketlist.size()!=0) {
					
					for (int i=0; i<socketlist.size(); ++i) {
						LinkThread is = (LinkThread)socketlist.get(i);
						if (is.isDisconnect()) {
							socketlist.remove(is);
						}
					}
					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
				}
			}
		}).start();
		stop = false;
	}
	
	public void stop() {
		stop = true;
	}
}
