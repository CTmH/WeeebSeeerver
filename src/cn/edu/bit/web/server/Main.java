package cn.edu.bit.web.server;

import javax.swing.JOptionPane;

import cn.edu.bit.web.server.common.WebServer;
import cn.edu.bit.web.server.config.WebConfig;


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
			else if (args[i].startsWith("-t:")) {
				try {
					int threads = Integer.parseInt( args[i].substring(3) );
					WebConfig.maxThread=threads;  // �ڷ�����start֮ǰ����
				} catch(NumberFormatException e) {
					System.err.println(e);
					return;
				}
			} else {
				System.err.println("��������"+".");
				JOptionPane.showMessageDialog(	null, 
												"��������"+".", 
												"����", 
												JOptionPane.ERROR_MESSAGE, 
												null);
				return;
			}
		}
		sm.start();
	}
}
