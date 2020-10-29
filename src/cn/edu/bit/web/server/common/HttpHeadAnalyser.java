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
	 **  指示了在被Request-URI指定的资源上执行的方法。大小写敏感。
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
		// 忽略前导空行 rfc2616-4.1
		while (readline.length()<=0) {
			readline = readLine(in);
		}
		// 然后开始读取Http头
		while (readline!=null && readline.length()>0) {
			sb.append(readline+"\n");
			readline = readLine(in);
		}
		httphead = sb.toString();
		
		// Content-Length 设置消息体
		String cl = get("Content-Length");
		if (cl!=null && cl.trim().length()>0) {
			int len = Integer.parseInt(cl.trim());
			byte[] body = new byte[len];
			if (len==in.read(body)) {
				messBody = body;
			}
		}
	}
	
	/** 把对目录的请求尝试转换成为对请求目录的主页的请求 */
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
	
	/** 返回一个由s指定的Request域并且去掉了首尾空字符, 失败返回null */
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
	 * 发送错误代码,并结束消息响应Header,关闭与客户端的链接
	 * @param num - 错误代码
	 * @param message - 错误消息
	 */
	public void error(int num, String message) {
		StringBuffer buf = new StringBuffer();
		StringBuffer bod = new StringBuffer();
		bod.append("<html><body><h1><font color=\"#FF0000\">Error "+
					num+".</font></h1><hr/>" +
					"<font size=\"+1\" color=\"#999999\">"+
					message+"的服务器处理似乎有一点问题"+".</font><p></body></html>");
		
		buf.append("HTTP/1.0 "+num+" "+cr+lf);
		buf.append("Connection:close"+cr+lf);
		buf.append("Content-Length:"+bod.toString().getBytes().length+cr+lf);
		buf.append(""+cr+lf);  // 不然会变成字符加法
		
		try {
			out.write(buf.toString().getBytes());
			out.write(bod.toString().getBytes());
		} catch (IOException e) {}
		closed = true;
	}
	
	/**
	 * uri中拼接在?后面的字符串
	 * @return null-没有参数
	 */
	public String getArguments() {
		String arg = getRequestURI();
		int begin = arg.indexOf('?');
		if ( begin<0 || begin>=(arg.length()-1) ) return null;
		arg = arg.substring(begin+1);
		return arg;
	}
	
	/** 
	 * 返回 Host 头域中,主机地址部分
	 * Rfc 2616-14.23 Host
	 * @return - Host头域值,如果不存在返回null;
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
	 * 返回消息体的字节码
	 * @return 不会返回null, byte数组长度在0~MAXINT
	 */
	public byte[] getMessageBody() {
		return messBody;
	}
	
	/**
	 * 返回http中Referer域的值(并且格式化了)
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
	 * 把请求中的URI映射为本地文件,如果映射的是本地的一个目录(使用基地址),会把目录自动转换为对默认
	 * 主页文件的请求,参看 CommonInfo.defaultIndexfile;
	 * @return - 找到相对应的本地文件,否则返回空
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
	 *	Request-URI的OPTIONS依赖于请求的性质。星号"*"意味着请求不能应用于一个特定的资源，
	 *	但是能应用于服务器，并且只能被允许当使用的方法不能应用于资源的时候。
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
	 * 解码URI.<br> 
	 * 使用指定的编码机制对 application/x-www-form-urlencoded 字符串解码。<br>
	 * 给定的编码用于确定任何 "%xy" 格式的连续序列表示的字符。 
	 * @param uri - 欲解码的uri
	 * @return 解码后的uri并不解码'?'后面的字符,如果解码失败,返回原始字符串
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
			System.err.println("Url解析错误"+":"+uri);
		}
		return uri;
	}
	
	/**
	 * 返回客户端的ip地址
	 * @return - InetAddress,不存在返回null;
	 */
	public InetAddress getRemoteAddress() {
		return socket.getInetAddress();
	}
	
	/**
	 * 寻找指定文件的Mini类型
	 * @return - Mini类型字符串,找不到返回null
	 */
	public String getMimeName() {
		return null;
	}
	
	/**
	 * 返回HTTP请求的方法(GET,POST,PUT...)
	 * @return 找不到返回null;
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
	 * [14.10] Connection - Connection：close
	 * 检测"Connection"头域是否是"close"
	 * @return boolean - 是返回true; 消息中不包含"Connection"头域,
	 * 					 或者不为"close",返回false;
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
	 * 结束消息头的写入,下面的数据为应答的实体
	 * @throws IOException - 已经调用过printEnd()
	 */
	public void printEnd() throws IOException {
		testState();
		closed = true;
		out.write(outMessageHead.toString().getBytes());
		out.write(cr);
		out.write(lf);
	}
	
	/**
	 * 向响应中写入一条消息头域,并将它缓存,直到printEnd()被调用缓存的数据才会发送出去<br>
	 * 在一行中不允许有换行和回车,自动在结尾添加CRLF
	 * @throws IOException - 已经调用过printEnd()或者message中有Cr,lf 
	 */
	public void println(String message) throws IOException {
		testState();
		if (message.indexOf(cr)!=-1 && message.indexOf(lf)!=-1) {
			throw new IOException("发送的消息中不能有CR,LF");
		}
		outMessageHead.append(message+cr+lf);
	}
	
	/** 
	 * 从inputStream读取一行字符串,以cr&lf&crlf结尾 
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
		throw new IOException("当前行不以CRLF结尾"+".");
	}

	/**
	 * 设置基地址,用于虚拟多服务器
	 */
	public void setBasePath(String basepath) {
		basePath = basepath;
	}
	
	/** 
	 * 设置消息体长度 
	 * Content-Length - Content-Length = “Content-Length” “:” 1*DIGIT 
	 */
	public final void setContentLength(long l) throws IOException {
		println("Content-Length:"+l);
	}
	
	/** 设置消息体的数据类型,参数为空不执行任何操作 */
	public final void setMimeType(String mime) throws IOException {
		if (mime!=null) {
			println("Content-Type:"+mime);
		}
	}
	
	/**
	 * 设置消息体的从属范围,参数的正确性由调用者负责
		Content-Range = "Content-Range" ":" content-range-spec
		content-range-spec = byte-content-range-spec
		byte-content-range-spec = bytes-unit SP byte-range-resp-spec "/"( instance-length | "*" )
		byte-range-resp-spec = (first-byte-pos "-" last-byte-pos) | "*"
		instance-length = 1*DIGIT
		</pre>
		@param first - 起始字节在文件中的位置
		@param last - 结束字节在文件中的位置
		@param length - 文件的总长度
		@param IOException - 参看println();
	 */
	public final void setContentRange(long first, long last, long length) 
	throws IOException 
	{
		final char SP = ' ';
		println("bytes"+SP+first+"-"+last+"/"+length);
	}
	
	/**
	 * 隐含的测试closed是否为true,如果为true抛出异常--说明不能再向头域中写入数据
	 */
	private void testState() throws IOException {
		if (closed) {
			throw new IOException("消息体已经发送,不能继续写入消息头"+".");
		}
	}
	
	/**
	 * http请求头的String形式
	 */
	public String toString() {
		return httphead;
	}
}
