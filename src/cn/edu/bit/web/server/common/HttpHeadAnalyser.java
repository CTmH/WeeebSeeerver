package cn.edu.bit.web.server.common;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cn.edu.bit.web.server.config.WebConfig;


public class HttpHeadAnalyser {
	
	public static final String[] HttpField = {
		"Host", "User-Agent", "Accept", "Referer", "Accept-Language",
		"Content-Type", "Content-Length", "Cache-Control",
		"Accept-Encoding", "UA-CPU",
	};
	public static final char cr = '\r';
	public static final char lf = '\n';
	
	/**
	 **  ָʾ���ڱ�Request-URIָ������Դ��ִ�еķ�������Сд���С�
	 */
	public final String GET 	= "GET";
	public final String HEAD	= "HEAD";
	public final String OPTIONS	= "OPTIONS";
	public final String POST	= "POST";
	public final String PUT		= "PUT";
	public final String DELETE	= "DELETE";
	public final String TRACE	= "TRACE";
	public final String CONNECT	= "CONNECT";
	
	private String httphead;
	private byte[] messBody = new byte[0];
	private OutputStream out;
	private boolean closed = false;
	private String basePath = "";
	private StringBuffer outMessageHead = new StringBuffer();
	
	private String requesturi = null;
	private File requestfile = null;
	private String host = null;
	private String referer = null;

	private Socket socket;
	
	public HttpHeadAnalyser(Socket clietSocket) 
	throws IOException, SocketTimeoutException
	{
		InputStream in  = clietSocket.getInputStream();
		OutputStream out= clietSocket.getOutputStream();
		socket = clietSocket;
		this.out = out;
		
		StringBuffer sb = new StringBuffer();
		String readline = readLine(in);
		// ����ǰ������ rfc2616-4.1
		while (readline.length()<=0) {
			readline = readLine(in);
		}
		// Ȼ��ʼ��ȡHttpͷ
		while (readline!=null && readline.length()>0) {
			sb.append(readline+"\n");
			readline = readLine(in);
		}
		httphead = sb.toString();
		
		// Content-Length ������Ϣ��
		String cl = get("Content-Length");
		if (cl!=null && cl.trim().length()>0) {
			int len = Integer.parseInt(cl.trim());
			byte[] body = new byte[len];
			if (len==in.read(body)) {
				messBody = body;
			}
		}
	}
	
	/** �Ѷ�Ŀ¼��������ת����Ϊ������Ŀ¼����ҳ������ */
	private File conversIndexfile(File f) {
		int circle = WebConfig.defaultIndexfile.length;
		String indexfile = f.getPath();
		File newfile = null;
		
		for (int i=0; i<circle; ++i) {
			newfile = new File(
					indexfile+File.separatorChar +
					WebConfig.defaultIndexfile[i]);
			if (newfile.isFile()) {
				return newfile;
			}
		}
		return f;
	}
	
	/** ����һ����sָ����Request����ȥ������β���ַ�, ʧ�ܷ���null */
	public String get(String s) {
		int start = httphead.indexOf(s);
		int end = httphead.indexOf('\n', start);

		if (start>=0 && end<=httphead.length() && end>0) {
			if (httphead.charAt(start+s.length())==':') {
				return httphead.substring(start+s.length()+1, end).trim();
			}
		}
		return null;
	}
	
	/**
	 * ���ʹ������,��������Ϣ��ӦHeader,�ر���ͻ��˵�����
	 * @param num - �������
	 * @param message - ������Ϣ
	 */
	public void error(int num, String message) {
		StringBuffer buf = new StringBuffer();
		StringBuffer bod = new StringBuffer();
		bod.append("<html><body><h1><font color=\"#FF0000\">Error "+
					num+".</font></h1><hr/>" +
					"<font size=\"+1\" color=\"#999999\">"+
					message+"�ķ����������ƺ���һ������"+".</font><p></body></html>");
		
		buf.append("HTTP/1.0 "+num+" "+cr+lf);
		buf.append("Connection:close"+cr+lf);
		buf.append("Content-Length:"+bod.toString().getBytes().length+cr+lf);
		buf.append(""+cr+lf);  // ��Ȼ�����ַ��ӷ�
		
		try {
			out.write(buf.toString().getBytes());
			out.write(bod.toString().getBytes());
		} catch (IOException e) {}
		closed = true;
	}
	
	/**
	 * uri��ƴ����?������ַ���
	 * @return null-û�в���
	 */
	public String getArguments() {
		String arg = getRequestURI();
		int begin = arg.indexOf('?');
		if ( begin<0 || begin>=(arg.length()-1) ) return null;
		arg = arg.substring(begin+1);
		return arg;
	}
	
	/** 
	 * ���� Host ͷ����,������ַ����
	 * Rfc 2616-14.23 Host
	 * @return - Hostͷ��ֵ,��������ڷ���null;
	 */
	public String getHost() {
		if (host==null) {
			host = get("Host");
			int portbegin = host.indexOf(':');
			if (portbegin>0) {
				host = host.substring(0, portbegin);
			}
		}
		return host;
	}
	
	/**
	 * ������Ϣ����ֽ���
	 * @return ���᷵��null, byte���鳤����0~MAXINT
	 */
	public byte[] getMessageBody() {
		return messBody;
	}
	
	/**
	 * ����http��Referer���ֵ(���Ҹ�ʽ����)
	 */
	public String getRef() {
		if (referer==null) {
			referer = get("Referer");
			final String http = "http://";
			
			if (referer!=null) {
				referer = decodeURI(referer);
				int index = referer.indexOf(http);
				if (index>=0) {
					index = referer.indexOf('/', index+http.length());
					if (index>=0) {
						referer = referer.substring(index);
					}
				}
			}
		}
		return referer;
	}
	
	/**
	 * �������е�URIӳ��Ϊ�����ļ�,���ӳ����Ǳ��ص�һ��Ŀ¼(ʹ�û���ַ),���Ŀ¼�Զ�ת��Ϊ��Ĭ��
	 * ��ҳ�ļ�������,�ο� CommonInfo.defaultIndexfile;
	 * @return - �ҵ����Ӧ�ı����ļ�,���򷵻ؿ�
	 */
	public File getRequestFile() {
		if (requestfile==null) {
			String name = getRequestURI();
			int sp = name.indexOf("?");
			if (sp>=0) {
				name = name.substring(0, sp);
			}
			
			File file = new File(basePath+name);
			File reff = new File(basePath+getRef());

			if (file.isDirectory()) {
				file = conversIndexfile(file);
			}
	
			if (!file.isFile()) {
				if (reff.isFile()) {
					file = new File(reff.getParent()+File.separatorChar+name);
				} else if (reff.isDirectory()) {
					file = new File(reff.getPath()+File.separatorChar+name);
				}
			}
			if (file.isFile()) {
				requestfile = file;
			}
		}
		return requestfile;
	}
	
	/** 
	 *	Request-URI   ="*" | absoluteURI | abs_path | authotity 
	 *	Request-URI��OPTIONS��������������ʡ��Ǻ�"*"��ζ��������Ӧ����һ���ض�����Դ��
	 *	������Ӧ���ڷ�����������ֻ�ܱ�����ʹ�õķ�������Ӧ������Դ��ʱ��
	 */
	public String getRequestURI() {
		if (requesturi==null) {
			int start = httphead.indexOf(' ');
			int end = httphead.indexOf(' ', start+1);
			if (start>=0 && end>=0) {
				requesturi = httphead.substring(start+1, end);
				if (requesturi!=null) {
					requesturi = decodeURI(requesturi);
				}
			}
		}
		return requesturi;
	}
	
	/**
	 * ����URI.<br> 
	 * ʹ��ָ���ı�����ƶ� application/x-www-form-urlencoded �ַ������롣<br>
	 * �����ı�������ȷ���κ� "%xy" ��ʽ���������б�ʾ���ַ��� 
	 * @param uri - �������uri
	 * @return ������uri��������'?'������ַ�,�������ʧ��,����ԭʼ�ַ���
	 */
	private final String decodeURI(String uri) {
		int fen = uri.indexOf('?');
		String url_s = null;
		String url_e = null;
		if (fen>=0 && fen<uri.length()-1) {
			url_s = uri.substring(0, fen);
			url_e = uri.substring(fen+1);
		} else {
			url_s = uri;
			url_e = null;
		}
		try {
			url_s = URLDecoder.decode(url_s, "UTF-8");
			uri = url_s+ (url_e==null? "": "?"+url_e);
		} catch(Exception e) {
			System.err.println("Url��������"+":"+uri);
		}
		return uri;
	}
	
	/**
	 * ���ؿͻ��˵�ip��ַ
	 * @return - InetAddress,�����ڷ���null;
	 */
	public InetAddress getRemoteAddress() {
		return socket.getInetAddress();
	}
	
	/**
	 * Ѱ��ָ���ļ���Mini����
	 * @return - Mini�����ַ���,�Ҳ�������null
	 */
	public String getMimeName() {
		return null;
	}
	
	/**
	 * ����HTTP����ķ���(GET,POST,PUT...)
	 * @return �Ҳ�������null;
	 */
	public String getMethod() {
		int end = httphead.indexOf(' ');
		return httphead.substring(0, end);
	}
	
	public boolean isGET() {
		return httphead.startsWith(GET);
	}
	public boolean isPOST() {
		return httphead.startsWith(POST);
	}
	public boolean isHEAD() {
		return httphead.startsWith(HEAD);
	}
	
	/**
	 * [14.10] Connection - Connection��close
	 * ���"Connection"ͷ���Ƿ���"close"
	 * @return boolean - �Ƿ���true; ��Ϣ�в�����"Connection"ͷ��,
	 * 					 ���߲�Ϊ"close",����false;
	 */
	public boolean isCloseConnect() {
		String cl = get("Connection");
		if (cl!=null) {
			if (cl.compareToIgnoreCase("close")==0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * ������Ϣͷ��д��,���������ΪӦ���ʵ��
	 * @throws IOException - �Ѿ����ù�printEnd()
	 */
	public void printEnd() throws IOException {
		testState();
		closed = true;
		out.write(outMessageHead.toString().getBytes());
		out.write(cr);
		out.write(lf);
	}
	
	/**
	 * ����Ӧ��д��һ����Ϣͷ��,����������,ֱ��printEnd()�����û�������ݲŻᷢ�ͳ�ȥ<br>
	 * ��һ���в������л��кͻس�,�Զ��ڽ�β���CRLF
	 * @throws IOException - �Ѿ����ù�printEnd()����message����Cr,lf 
	 */
	public void println(String message) throws IOException {
		testState();
		if (message.indexOf(cr)!=-1 && message.indexOf(lf)!=-1) {
			throw new IOException("���͵���Ϣ�в�����CR,LF");
		}
		outMessageHead.append(message+cr+lf);
	}
	
	/** 
	 * ��inputStream��ȡһ���ַ���,��cr&lf&crlf��β 
	 * @throws IOException 
	 */
	public static String readLine(InputStream in) throws IOException {
		List<Byte> buff = new ArrayList<Byte>();
		int read = in.read();
		while (read>=0 && read!=cr && read!=lf) {
			buff.add((byte)read);
			read = in.read();
		}
		if (read==cr) {
			read = in.read();
			if (read==lf) {
				byte[] bytes=new byte[buff.size()];
				for(int i=0;i<buff.size();++i) {
					bytes[i]=buff.get(i).byteValue();
				}
				return new String(bytes);
			}
		}else if (read==-1) {
			return "";
		}
		throw new IOException("��ǰ�в���CRLF��β"+".");
	}

	/**
	 * ���û���ַ,��������������
	 */
	public void setBasePath(String basepath) {
		basePath = basepath;
	}
	
	/** 
	 * ������Ϣ�峤�� 
	 * Content-Length - Content-Length = ��Content-Length�� ��:�� 1*DIGIT 
	 */
	public final void setContentLength(long l) throws IOException {
		println("Content-Length:"+l);
	}
	
	/** ������Ϣ�����������,����Ϊ�ղ�ִ���κβ��� */
	public final void setMimeType(String mime) throws IOException {
		if (mime!=null) {
			println("Content-Type:"+mime);
		}
	}
	
	/**
	 * ������Ϣ��Ĵ�����Χ,��������ȷ���ɵ����߸���
		Content-Range = "Content-Range" ":" content-range-spec
		content-range-spec = byte-content-range-spec
		byte-content-range-spec = bytes-unit SP byte-range-resp-spec "/"( instance-length | "*" )
		byte-range-resp-spec = (first-byte-pos "-" last-byte-pos) | "*"
		instance-length = 1*DIGIT
		</pre>
		@param first - ��ʼ�ֽ����ļ��е�λ��
		@param last - �����ֽ����ļ��е�λ��
		@param length - �ļ����ܳ���
		@param IOException - �ο�println();
	 */
	public final void setContentRange(long first, long last, long length) 
	throws IOException 
	{
		final char SP = ' ';
		println("bytes"+SP+first+"-"+last+"/"+length);
	}
	
	/**
	 * �����Ĳ���closed�Ƿ�Ϊtrue,���Ϊtrue�׳��쳣--˵����������ͷ����д������
	 */
	private void testState() throws IOException {
		if (closed) {
			throw new IOException("��Ϣ���Ѿ�����,���ܼ���д����Ϣͷ"+".");
		}
	}
	
	/**
	 * http����ͷ��String��ʽ
	 */
	public String toString() {
		return httphead;
	}
}
