package cn.edu.bit.web.server.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JOptionPane;

import cn.edu.bit.web.server.common.file.FileManager;
import cn.edu.bit.web.server.config.WebConfig;

public class WebServer implements Runnable{
	
	private ServerSocket serverSocket;
	private boolean stop=true;
	private Queue<Socket> socketlist;
	private int port;
	private long totalLink = 0;
	
	private ExecutorService threadPool=Executors.newFixedThreadPool(WebConfig.maxThread);

	
	public WebServer(int port) {
		// 初始化配置文件
		
		if (port>0) {
			WebConfig.serverPort = port;
		}
		this.port = WebConfig.serverPort;
		
		// 初始化服务器状态
		stop = true;
		socketlist = new LinkedBlockingQueue<Socket>();
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
					socketlist.remove().close();  // 之后线程自己报错结束
					Thread.sleep(1000);
				} catch (InterruptedException ie) {}
				catch (Exception e) {
					LogSystem.message("服务器关闭旧连接错误"+":["+e+"]");
				}
			}
			try {
				Socket socket = serverSocket.accept();
				LinkThread sl = new LinkThread(socket, threadPool);
				socketlist.add(socket);
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
		stop = false;
	}
	
	public void stop() {
		stop = true;
	}
}
