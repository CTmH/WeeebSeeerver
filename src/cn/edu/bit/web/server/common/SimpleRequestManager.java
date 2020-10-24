package cn.edu.bit.web.server.common;

import java.io.IOException;
import java.io.InputStream;

import cn.edu.bit.web.server.config.Range;
import cn.edu.bit.web.server.interf.IRequest;
import cn.edu.bit.web.server.interf.IResponse;

public class SimpleRequestManager implements IRequest {

	@Override
	public void request(Object o, IResponse ir) throws Exception {
		// TODO Auto-generated method stub
		InputStream in	= null;
		try {
			HttpHeadAnalyser hha=(HttpHeadAnalyser)o;
			Range r 		= hha.getRange();
			
			hha.printEnd();
			
		} catch(IOException e) {
			// LogSystem.error(Language.clientClose+".");
			// donothing..
		} catch(Exception e) {
			LogSystem.error("Î´Öª´íÎó"+":"+e);
			
		} finally {
			ir.response( in );
		}
	}

}
