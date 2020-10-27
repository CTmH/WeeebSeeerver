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
				// 回调成功,立即返回
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
		out(socket.getRemoteSocketAddress()+"Socket连接.");
		HttpHeadAnalyser hha = null;
		/** 如果客户端发出关闭消息为true */
		boolean clientisClosed = false;
		
		// 进入循环前设置connect++
		++connect;
		do { // 消息循环的开始, 直到客户端发送关闭链接的头域,或没有其他的消息时,循环才退出.
			
			try {
				hha = null;
				hha = new HttpHeadAnalyser(socket);
			} catch (SocketTimeoutException e) {
				LogSystem.error(socket.getRemoteSocketAddress()+
						"Socket 超时"+','+"客户端监听结束"+".");
				break;
			} catch (IOException e) {
				LogSystem.error(socket.getRemoteSocketAddress()+
						"客户端关闭"+','+"客户端监听结束"+".");
				break;
			}
			
			if (hha!=null) {
				if (!clientisClosed) clientisClosed = hha.isCloseConnect();
				
				hha.setBasePath( VirtualHostSystem.getVhost(hha) );
				
				
				if (hha.isGET()) {
					sendfile = hha.getRequestFile();
					if ( sendfile!=null ) {
						try {
							// TODO:get解析
//							if (Cgi_Manage.get().isCgi(hha)) {
//								// 对脚本文件的请求
//								Cgi_Manage.get().request(hha, this);
//							} else {
//								// 对普通文件的请求,先过滤
//								FilterSystem.exclude(sendfile);
//								FileManager.get().request(hha, this);
//							}
//							// 下面的代码在请求成功后执行
//							++connect;
							// 请求成功继续循环并等待回调,
							continue;
							
						} catch(Exception e) {
							error("请求错误"+":"+e);
							// 会转向下面的代码--应答一个404错误
						}
					}
					
					hha.error(RequestErrCode.E404, hha.getRequestURI());
					
					error(socket.getRemoteSocketAddress()+"请求错误"+","+
							"找不到文件"+":"+hha.getRequestURI());
					
					LogSystem.httpHead(hha);
					break;
					
				} else if (hha.isPOST()) {
					try {
						// TODO:CGI配置
//						Cgi_Manage.get().request(hha, this);
//						++connect;
						continue;
						
					} catch (Exception e) {
						error("请求错误"+":"+e);
						// 其他的未知错误会应答下面的代码--404错误
					}
					hha.error(RequestErrCode.E404, hha.getRequestURI());
					LogSystem.httpHead(hha);
				} else {
					error("未知请求"+":"+socket.getRemoteSocketAddress());
				}
			} else {
				error(socket.getRemoteSocketAddress()+"Http头读取出错.");
			}
			// ----- 下面的代码在出错时执行,
			// 出错立即退出
			break;
			// -----
		} while( (!clientisClosed) );
		// 退出循环时设置 connect--;
		closeConnect();
	}
	
	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
	}
	
	/** 
	 * 尝试关闭连接,如果消息队列中为空,且处于超时状态<br>
	 * closeConnect()通过检测链接线程的数量(connect变量)
	 * 来判断是否应该关闭套接字
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
	 * 这是很关键的终结变量,小心的设置它!!! 
	 * 设置他的方法必须'成对'出现
	 * 
	 * 每当一个新的<b>链接线程</b>被建立,connect加一,
	 * 当<b>链接线程</b>退出,connect减一
	 */
	private volatile int connect = 0;
	
	/** 检查这次握手是否已经结束 */
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
			LogSystem.message("写文件流失败");
		}
		return 0;
	}
	
	/** 通过LogSystem打印信息 */
	private final void out(Object o) {
		LogSystem.message(o.toString());
	}
	/** 通过LogSystem打印错误信息 */
	private final void error(Object o) {
		LogSystem.error(o.toString());
	}
}
