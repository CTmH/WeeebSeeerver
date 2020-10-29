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
		Logger.message(socket.getRemoteSocketAddress()+"Socket连接.");
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
				Logger.error(socket.getRemoteSocketAddress()+
						"Socket 超时"+','+"客户端监听结束"+".");
				break;
			} catch (IOException e) {
				// e.printStackTrace();
				Logger.error(socket.getRemoteSocketAddress()+
						"客户端关闭"+','+"客户端监听结束"+".");
			}
			
			if (hha!=null) {
				if (!clientisClosed) clientisClosed = hha.isCloseConnect();
				
				hha.setBasePath( VirtualHostSystem.getVhost(hha) );
				
				
				if (hha.isGET()||hha.isHEAD()) {
					sendfile = hha.getRequestFile();
					if ( sendfile!=null ) {
						try {
							// 对文件可访问性验证，不能访问返回403
							if(WebConfig.secretFiles.contains(hha.getRequestFile().getName())) {
								hha.error(RequestErrCode.E403, hha.getRequestURI());
								continue;
							}
							FileManager.get().request(hha, this);
							// 下面的代码在请求成功后执行
							++connect;
							// 请求成功，这个处理可以结束，之后由自己或者其他线程关闭socket
							break;
							
						} catch(Exception e) {
							Logger.error("请求错误"+":"+e);
							// 会转向下面的代码--应答一个404错误
						}
					}
					
					hha.error(RequestErrCode.E404, hha.getRequestURI());
					
					Logger.error(socket.getRemoteSocketAddress()+"请求错误"+","+
							"找不到文件"+":"+hha.getRequestURI());
					
					Logger.httpHead(hha);
					break;
					
				} else if (hha.isPOST()) {
					try {
						// CGI配置
						CgiManager.get().request(hha, this);	
						++connect;
						break;
						
					} catch (Exception e) {
						Logger.error("请求错误"+":"+e);
						// 其他的未知错误会应答下面的代码--404错误
					}
					hha.error(RequestErrCode.E404, hha.getRequestURI());
					Logger.httpHead(hha);
				} else {
					StringBuffer buf = new StringBuffer();
					StringBuffer bod = new StringBuffer();
					bod.append("<html><body><h1><font color=\"#FF0000\">Error "+
								400+".</font></h1><hr/>" +
								"<font size=\"+1\" color=\"#999999\">"+
								"请求格式不合法！"+".</font><p></body></html>");
					
					buf.append("HTTP/1.0 "+400+" "+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
					buf.append("Connection:close"+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
					buf.append("Content-Length:"+bod.toString().getBytes().length+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
					buf.append(""+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);  // 不然会变成字符加法
					
					try {
						out.write(buf.toString().getBytes());
						out.write(bod.toString().getBytes());
					} catch (IOException e) {}
					Logger.error("未知请求"+":"+socket.getRemoteSocketAddress());
				}
			} else {
				StringBuffer buf = new StringBuffer();
				StringBuffer bod = new StringBuffer();
				bod.append("<html><body><h1><font color=\"#FF0000\">Error "+
							400+".</font></h1><hr/>" +
							"<font size=\"+1\" color=\"#999999\">"+
							"请求格式不合法！"+".</font><p></body></html>");
				
				buf.append("HTTP/1.0 "+400+" "+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
				buf.append("Connection:close"+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
				buf.append("Content-Length:"+bod.toString().getBytes().length+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);
				buf.append(""+HttpHeadAnalyser.cr+HttpHeadAnalyser.lf);  // 不然会变成字符加法
				
				try {
					out.write(buf.toString().getBytes());
					out.write(bod.toString().getBytes());
				} catch (IOException e) {}
				Logger.error(socket.getRemoteSocketAddress()+"Http头读取出错.");
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
	 * 如果消息队列中为空,且处于超时状态或发生异常，尝试关闭连接
	 * 通过检测链接线程的数量(connect变量)来判断是否应该关闭套接字
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
			// e.printStackTrace();
			Logger.message("写文件流失败");
		}
		return 0;
	}
}