package cn.edu.bit.web.server.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.bit.web.server.config.WebConfig;

public class SenderThread extends Thread {
	private LinkThread link;
	private List inQueue;

	public SenderThread(LinkThread link, ExecutorService threadPool) {
		this.link = link;
		inQueue = new LinkedList();
		if(threadPool==null) {
			this.start();
		}else {
			threadPool.execute(this);
		}
	}
	
	public void send(InputStream in) {
		inQueue.add(in);
	}
	
	public boolean isEmpty() {
		return inQueue.isEmpty();
	}
	
	public void run() {
		// 流量控制需要的变量
		final boolean SPEEDLIMIT = !(WebConfig.downSpeedLimit<=0);
		final int waitTime = (int)(SPEEDLIMIT ?
			(float)WebConfig.writeBufferSize/
			(WebConfig.downSpeedLimit*1024)*1000 : 0);
		long useTime = 0;
		
		while (!link.isDisconnect()) {
			while (inQueue.isEmpty()) {
				try {
					if (link.isDisconnect()) return;
					sleep(50);
				} catch (InterruptedException e) {}
			}
			InputStream fin = (InputStream)inQueue.get(0);
			inQueue.remove(0); // 模仿队列

			byte[] buffer = new byte[WebConfig.writeBufferSize];
			try {
				System.out.println(link.getRemoteSocketAddress()+"发送文件"+":"+link.getFile());
				int len = fin.read(buffer);
				while (len>0) {
					link.writeOutput(buffer, 0, len);
					len = fin.read(buffer);
					
					if (SPEEDLIMIT) {
						useTime = System.currentTimeMillis() - useTime;
						if (useTime<waitTime) {
							try {
								Thread.sleep(waitTime-useTime);
							} catch (InterruptedException e) {}
						}
						useTime = System.currentTimeMillis();
					}
				}
				
			} catch (IOException e) {
				LogSystem.error(link.getRemoteSocketAddress()+"文件发送错误"+":"+
						link.getFile()+" "+"客户端关闭链接"+".");
			//	error("详细信息:"+e.getLocalizedMessage());
			} finally {
				try {
					fin.close();
					link.closeConnect();
				} catch (IOException e) {}
			}
		}
		return;
	}
}
