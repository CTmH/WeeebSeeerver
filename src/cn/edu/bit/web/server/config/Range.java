package cn.edu.bit.web.server.config;
// CatfoOD 2008-3-30

/**
 * GET 请求中"Range"域的包装类
 * <pre>
 * HTTP/1.1 
 * 
 * [14.35.1]
 * ranges-specifier = byte-ranges-specifier
 * byte-ranges-specifier = bytes-unit "=" byte-range-set
 * byte-range-set  = 1#( byte-range-spec | suffix-byte-range-spec )
 * byte-range-spec = first-byte-pos "-" [last-byte-pos]
 * first-byte-pos  = 1*DIGIT
 * last-byte-pos   = 1*DIGIT
 * 
 * [3.12]
 * range-unit = bytes-unit | other-range-unit
 * bytes-unit = "bytes"
 * other-range-unit = token
 * </pre>
 */
public class Range {
	private long firstPos = 0;
	private long lastPos = 0;
	
	/**
	 * Range负责解析字符串中关于头域的解释
	 * @param r - 字符串中是 "byte-ranges-specifier";
	 * @throws Exception - 传入的字符串不是合法的"byte-ranges-specifier";
	 */
	public Range(String r) throws Exception {
		final String BYTESUNIT = "bytes";
		final String Dividing = "-";
		if (r.toLowerCase().startsWith(BYTESUNIT)) {
			int begin = r.indexOf('=');
			if (begin>=BYTESUNIT.length()) {
				String byte_range_spec = r.substring(begin+1).trim();
				int div = byte_range_spec.indexOf(Dividing);
				try {
					firstPos = Long.parseLong( byte_range_spec.substring(0, div) );
					String last_byte_pos = byte_range_spec.substring(div+1);
					if (last_byte_pos.length()>0) {
						lastPos = Long.parseLong( last_byte_pos );
					} else {
						lastPos = -1; 
					}
				//System.out.println(firstPos+" "+lastPos);
					return;
				} catch(Exception e){}
			}
		}
		throw new Exception();
	}
	
	/**
	 * 直接设置一个范围
	 * @param first - 以字节为单位文件的起始位置
	 * @param last - 以字节为单位文件的结束位置,最后一个文件的位置=文件的长度-1
	 */
	public Range(long first, long last) {
		firstPos = first;
		lastPos = last;
	}
	
	/**
	 * 如果初始化时参数中未指定范围的结束位置,则设置newLast为范围的结束位置,否则什么都不做.
	 * <li> 最后一个文件的位置=文件的长度-1
	 * @param newLast - 设置新的结束位置,如果当前的结束位置是未定义的
	 */
	public void setLastPos(long newLast) {
		if (lastPos==-1) {
			lastPos = newLast;
		}
	}
	
	/** 返回最开始的字节 */
	public long getFirstPos() {
		return firstPos;
	}
	
	/** 返回结束处的字节,如果返回-1说明最后的位置在文件的末尾,(文件长度-1) */
	public long getLastPos() {
		return lastPos;
	}
}
