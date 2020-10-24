package cn.edu.bit.web.server;

import javax.swing.JOptionPane;

import cn.edu.bit.web.server.common.WebServer;


public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		WebServer sm = new WebServer();
		for (int i=0; i<args.length; ++i) {
			if (args[i].startsWith("-p:")) {
				try {
					int port = Integer.parseInt( args[i].substring(3) );
					sm.port(port);
				} catch(NumberFormatException e) {
					System.err.println(e);
					return;
				}
			}
			else {
				System.err.println("参数错误"+".");
				JOptionPane.showMessageDialog(	null, 
												"参数错误"+".", 
												"错误", 
												JOptionPane.ERROR_MESSAGE, 
												null);
				return;
			}
		}
		sm.start();
	}
}
