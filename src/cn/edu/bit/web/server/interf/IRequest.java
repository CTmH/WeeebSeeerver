package cn.edu.bit.web.server.interf;

/** 
 * 请求接口.<br>
 * 实现IRequest接口的类<b>必须通过</b>"request"方法中"IResponsion"参数回调到请求的类.<br>
 * 如果请求的对象没有意义,要么在请求时抛出异常,要么在回调时,参数为null;
 */
public interface IRequest {
	/** 
	 * 被请求的类实现这个接口,当这个方法被成功调用后
	 * 调用这个方法的类,正等待被回调--通过"IResponsion"接口
	 */
	public void request(Object o, IResponse ir) throws Exception;
}
