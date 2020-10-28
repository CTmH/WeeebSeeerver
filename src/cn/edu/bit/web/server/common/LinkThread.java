package cn.edu.bit.web.server.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import cn.edu.bit.web.server.interf.IResponse;
import cn.edu.bit.web.server.config.WebConfig;
import cn.edu.bit.web.server.config.RequestErrCode;

public class LinkThread extends Thread implements IResponse{
	private Socket socket;
	private volatile boolean stop = false;
	
	private InputStream in;
	private OutputStream out;
	private File sendfile;
	private SenderThread senderThread;
	
	public LinkThread(Socket s) throws IOException {
		socket = s;
		socket.setSoTimeout(WebConfig.socketReadOuttime);
		in  = s.getInputStream();
		out = s.getOutputStream();
		senderThread = new SenderThread(this);
		
		this.start();
	}

	@Override
	public void response(Object o) {
		// TODO Auto-generated method stub
		if ( (o!=null) && (!stop) ) {
			if (o instanceof InputStream) {
				senderThread.send( (InputStream)o );
				// �ص��ɹ�,��������
				return;
			} else {
				throw new IllegalArgumentException("responsion unsupport class:"+o);
			}
		}
		closeConnect();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		out(socket.getRemoteSocketAddress()+"Socket����.");
		HttpHeadAnalyser hha = null;
		/** ����ͻ��˷����ر���ϢΪtrue */
		boolean clientisClosed = false;
		
		// ����ѭ��ǰ����connect++
		++connect;
		do { // ��Ϣѭ���Ŀ�ʼ, ֱ���ͻ��˷��͹ر����ӵ�ͷ��,��û����������Ϣʱ,ѭ�����˳�.
			
			try {
				hha = null;
				hha = new HttpHeadAnalyser(socket);
			} catch (SocketTimeoutException e) {
				LogSystem.error(socket.getRemoteSocketAddress()+
						"Socket ��ʱ"+','+"�ͻ��˼�������"+".");
				break;
			} catch (IOException e) {
				LogSystem.error(socket.getRemoteSocketAddress()+
						"�ͻ��˹ر�"+','+"�ͻ��˼�������"+".");
				break;
			}
			
			if (hha!=null) {
				if (!clientisClosed) clientisClosed = hha.isCloseConnect();
				
				hha.setBasePath( VirtualHostSystem.getVhost(hha) );
				
				
				if (hha.isGET()) {
					sendfile = hha.getRequestFile();
					if ( sendfile!=null ) {
						try {
							// TODO:get����
//							if (Cgi_Manage.get().isCgi(hha)) {
//								// �Խű��ļ�������
//								Cgi_Manage.get().request(hha, this);
//							} else {
//								// ����ͨ�ļ�������,�ȹ���
//								FilterSystem.exclude(sendfile);
//								FileManager.get().request(hha, this);
//							}
//							// ����Ĵ���������ɹ���ִ��
//							++connect;
							// ����ɹ�����ѭ�����ȴ��ص�,
							continue;
							
						} catch(Exception e) {
							error("�������"+":"+e);
							// ��ת������Ĵ���--Ӧ��һ��404����
						}
					}
					
					hha.error(RequestErrCode.E404, hha.getRequestURI());
					
					error(socket.getRemoteSocketAddress()+"�������"+","+
							"�Ҳ����ļ�"+":"+hha.getRequestURI());
					
					LogSystem.httpHead(hha);
					break;
					
				} else if (hha.isPOST()) {
					try {
						// TODO:CGI����
//						Cgi_Manage.get().request(hha, this);
//						++connect;
						CgiResponse.get().request(hha, this);
						++connect;
						continue;
						
					} catch (Exception e) {
						error("�������"+":"+e);
						// ������δ֪�����Ӧ������Ĵ���--404����
					}
					hha.error(RequestErrCode.E404, hha.getRequestURI());
					LogSystem.httpHead(hha);
				} else {
					error("δ֪����"+":"+socket.getRemoteSocketAddress());
				}
			} else {
				error(socket.getRemoteSocketAddress()+"Httpͷ��ȡ����.");
			}
			// ----- ����Ĵ����ڳ���ʱִ��,
			// ���������˳�
			break;
			// -----
		} while( (!clientisClosed) );
		// �˳�ѭ��ʱ���� connect--;
		closeConnect();
	}
	
	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
	}
	
	/** 
	 * ���Թر�����,�����Ϣ������Ϊ��,�Ҵ��ڳ�ʱ״̬<br>
	 * closeConnect()ͨ����������̵߳�����(connect����)
	 * ���ж��Ƿ�Ӧ�ùر��׽���
	 */
	public void closeConnect() {
		--connect;
		if (connect<=0) {
			out(socket.getRemoteSocketAddress()+"Closed!");
			try {
				in.close();
				out.close();
				socket.close();
			}catch(Exception ee){}
			stop = true;
		}
	}
	/** 
	 * ���Ǻܹؼ����ս����,С�ĵ�������!!! 
	 * �������ķ�������'�ɶ�'����
	 * 
	 * ÿ��һ���µ�<b>�����߳�</b>������,connect��һ,
	 * ��<b>�����߳�</b>�˳�,connect��һ
	 */
	private volatile int connect = 0;
	
	/** �����������Ƿ��Ѿ����� */
	public boolean isDisconnect() {
		return stop;
	}
	
	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}
	
	public SocketAddress getRemoteSocketAddress() {
		return socket.getRemoteSocketAddress();
	}
	
	public File getFile() {
		return sendfile;
	}
	
	public int writeOutput(byte[] bytes, int offset, int length) {
		try {
			out.write(bytes, offset, length);
			return 1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogSystem.message("д�ļ���ʧ��");
		}
		return 0;
	}
	
	/** ͨ��LogSystem��ӡ��Ϣ */
	private final void out(Object o) {
		LogSystem.message(o.toString());
	}
	/** ͨ��LogSystem��ӡ������Ϣ */
	private final void error(Object o) {
		LogSystem.error(o.toString());
	}
}
