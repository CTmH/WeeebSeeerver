package cn.edu.bit.web.server.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

import cn.edu.bit.web.server.interf.IResponse;
import cn.edu.bit.web.server.config.WebConfig;
import cn.edu.bit.web.server.common.file.FileManager;
import cn.edu.bit.web.server.config.RequestErrCode;

public class LinkThread extends Thread implements IResponse{
	private Socket socket;
	private volatile boolean stop = false;
	
	private InputStream in;
	private OutputStream out;
	private File sendfile;
	private SenderThread senderThread;
	
	public LinkThread(Socket s, ExecutorService threadPool) throws IOException {
		socket = s;
		socket.setSoTimeout(WebConfig.socketReadOuttime);
		in  = s.getInputStream();
		out = s.getOutputStream();
		senderThread = new SenderThread(this, threadPool);
		if(threadPool==null) {
			this.start();
		}else {
			threadPool.execute(this);
		}
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
		Logger.message(socket.getRemoteSocketAddress()+"Socket����.");
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
				Logger.error(socket.getRemoteSocketAddress()+
						"Socket ��ʱ"+','+"�ͻ��˼�������"+".");
				break;
			} catch (IOException e) {
				// e.printStackTrace();
				Logger.error(socket.getRemoteSocketAddress()+
						"�ͻ��˹ر�"+','+"�ͻ��˼�������"+".");
			}
			
			if (hha!=null) {
				if (!clientisClosed) clientisClosed = hha.isCloseConnect();
				
				hha.setBasePath( VirtualHostSystem.getVhost(hha) );
				
				
				if (hha.isGET()||hha.isHEAD()) {
					sendfile = hha.getRequestFile();
					if ( sendfile!=null ) {
						try {
							// ���ļ��ɷ�������֤�����ܷ��ʷ���403
							if(WebConfig.secretFiles.contains(hha.getRequestFile().getName())) {
								hha.error(RequestErrCode.E403, hha.getRequestURI());
								continue;
							}
							FileManager.get().request(hha, this);
							// ����Ĵ���������ɹ���ִ��
							++connect;
							// ����ɹ������������Խ�����֮�����Լ����������̹߳ر�socket
							break;
							
						} catch(Exception e) {
							Logger.error("�������"+":"+e);
							// ��ת������Ĵ���--Ӧ��һ��404����
						}
					}
					
					hha.error(RequestErrCode.E404, hha.getRequestURI());
					
					Logger.error(socket.getRemoteSocketAddress()+"�������"+","+
							"�Ҳ����ļ�"+":"+hha.getRequestURI());
					
					Logger.httpHead(hha);
					break;
					
				} else if (hha.isPOST()) {
					try {
						// CGI����
						CgiManager.get().request(hha, this);	
						++connect;
						break;
						
					} catch (Exception e) {
						Logger.error("�������"+":"+e);
						// ������δ֪�����Ӧ������Ĵ���--404����
					}
					hha.error(RequestErrCode.E404, hha.getRequestURI());
					Logger.httpHead(hha);
				} else {
					StringBuffer buf = new StringBuffer();
					StringBuffer bod = new StringBuffer();
					bod.append("<html><body><h1><font color=\"#FF0000\">Error "+
								400+".</font></h1><hr/>" +
								"<font size=\"+1\" color=\"#999999\">"+
								"�����ʽ���Ϸ���"+".</font><p></body></html>");
					
					buf.append("HTTP/1.0 "+400+" "+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
					buf.append("Connection:close"+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
					buf.append("Content-Length:"+bod.toString().getBytes().length+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
					buf.append(""+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);  // ��Ȼ�����ַ��ӷ�
					
					try {
						out.write(buf.toString().getBytes());
						out.write(bod.toString().getBytes());
					} catch (IOException e) {}
					Logger.error("δ֪����"+":"+socket.getRemoteSocketAddress());
				}
			} else {
				StringBuffer buf = new StringBuffer();
				StringBuffer bod = new StringBuffer();
				bod.append("<html><body><h1><font color=\"#FF0000\">Error "+
							400+".</font></h1><hr/>" +
							"<font size=\"+1\" color=\"#999999\">"+
							"�����ʽ���Ϸ���"+".</font><p></body></html>");
				
				buf.append("HTTP/1.0 "+400+" "+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
				buf.append("Connection:close"+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
				buf.append("Content-Length:"+bod.toString().getBytes().length+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
				buf.append(""+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);  // ��Ȼ�����ַ��ӷ�
				
				try {
					out.write(buf.toString().getBytes());
					out.write(bod.toString().getBytes());
				} catch (IOException e) {}
				Logger.error(socket.getRemoteSocketAddress()+"Httpͷ��ȡ����.");
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
	 * �����Ϣ������Ϊ��,�Ҵ��ڳ�ʱ״̬�����쳣�����Թر�����
	 * ͨ����������̵߳�����(connect����)���ж��Ƿ�Ӧ�ùر��׽���
	 */
	public void closeConnect() {
		--connect;
		if (connect<=0) {
			Logger.message(socket.getRemoteSocketAddress()+"Closed!");
			try {
				in.close();
				out.close();
				socket.close();
			}catch(Exception ee){}
			stop = true;
		}
	}
	/** 
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
			// e.printStackTrace();
			Logger.message("д�ļ���ʧ��");
		}
		return 0;
	}
}