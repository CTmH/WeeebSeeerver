package cn.edu.bit.web.server.config;
// CatfoOD 2008-3-30

/**
 * GET ������"Range"��İ�װ��
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
	 * Range��������ַ����й���ͷ��Ľ���
	 * @param r - �ַ������� "byte-ranges-specifier";
	 * @throws Exception - ������ַ������ǺϷ���"byte-ranges-specifier";
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
	 * ֱ������һ����Χ
	 * @param first - ���ֽ�Ϊ��λ�ļ�����ʼλ��
	 * @param last - ���ֽ�Ϊ��λ�ļ��Ľ���λ��,���һ���ļ���λ��=�ļ��ĳ���-1
	 */
	public Range(long first, long last) {
		firstPos = first;
		lastPos = last;
	}
	
	/**
	 * �����ʼ��ʱ������δָ����Χ�Ľ���λ��,������newLastΪ��Χ�Ľ���λ��,����ʲô������.
	 * <li> ���һ���ļ���λ��=�ļ��ĳ���-1
	 * @param newLast - �����µĽ���λ��,�����ǰ�Ľ���λ����δ�����
	 */
	public void setLastPos(long newLast) {
		if (lastPos==-1) {
			lastPos = newLast;
		}
	}
	
	/** �����ʼ���ֽ� */
	public long getFirstPos() {
		return firstPos;
	}
	
	/** ���ؽ��������ֽ�,�������-1˵������λ�����ļ���ĩβ,(�ļ�����-1) */
	public long getLastPos() {
		return lastPos;
	}
}
