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
		// ��ʼ�������ļ�
		
		if (port>0) {
			WebConfig.serverPort = port;
		}
		this.port = WebConfig.serverPort;
		
		// ��ʼ��������״̬
		stop = true;
		socketlist = new LinkedBlockingQueue<Socket>();
//		listener = new Listener();
//		printWelcome();
	}
	
	public WebServer() {
		this(-1);
	}
	
	public final void printWelcome() {
		System.out.println("��ӭ������������ѧ��");
		System.out.println(LogSystem.line);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			serverSocket = new ServerSocket(port);
		} catch(IOException e) {
			System.out.println("["+"����"+"] "+port+" "+"�˿��ѱ�ռ��"+".\n\n");
			JOptionPane.showMessageDialog(	null, 
											port+" "+"�˿��ѱ�ռ��"+".", 
											"����", 
											JOptionPane.ERROR_MESSAGE, 
											null);
			// �������Ӧ�������˳�ϵͳ
			System.exit(1);
			return;
		}
		
		try {
			System.out.println(	"��������ַ"+":\t"+
					InetAddress.getLocalHost().getHostAddress()+"/" +
					InetAddress.getLocalHost().getHostName() );
		}catch(Exception e) {
			System.out.println(e);
		}
		System.out.println("�������˿�"+":\t"+port);
		System.out.println("����ʼʱ��"+":\t"+LogSystem.getDate());
		

//		// ��ʼ������ϵͳ
//		FilterSystem.init();
//		// ��ʼ��cgi������
//		Cgi_Manage.Init();
		// ��ʼ���ļ�������
		FileManager.Init();
//		// ��ʼ���ļ�����
//		MimeTypes.init();
		// ��ʼ��������������
		VirtualHostSystem.init();
		
		while (!stop) {
			while (socketlist.size()>=WebConfig.maxConnect) {
				try {
					socketlist.remove().close();  // ֮���߳��Լ��������
					Thread.sleep(1000);
				} catch (InterruptedException ie) {}
				catch (Exception e) {
					LogSystem.message("�������رվ����Ӵ���"+":["+e+"]");
				}
			}
			try {
				Socket socket = serverSocket.accept();
				LinkThread sl = new LinkThread(socket, threadPool);
				socketlist.add(socket);
				++totalLink;
			} catch (IOException e) {
				LogSystem.message("������������������"+":["+e+"]");
			}
		}
	}

	/** �ı�˿� */
	public void port(int p) {
		if (stop) {
			port = p;
		} else {
			throw new IllegalStateException("�޷��ı�˿�"+".");
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
