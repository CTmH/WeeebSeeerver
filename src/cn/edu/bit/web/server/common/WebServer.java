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
		// ��ʼ�������ļ�
		
		if (port>0) {
			WebConfig.serverPort = port;
		}
		this.port = WebConfig.serverPort;
		
		// ��ʼ��������״̬
		stop = true;
		socketlist = new LinkedList<LinkThread>();
//		listener = new Listener();
//		printWelcome();
	}
	
	public WebServer() {
		this(-1);
	}
	
	public final void printWelcome() {
		System.out.println("��ӭ��������������ѧ��");
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
		CgiResponse.Init();
//		// ��ʼ���ļ�����
//		MimeTypes.init();
		// ��ʼ��������������
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
		new Thread(new Runnable() {
			// �������ӵ��߳�
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
