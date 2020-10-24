package cn.edu.bit.web.server.config;

/**
 * HTTP 错误应答类
 */
public final class RequestErrCode {
	private RequestErrCode() {}
	
    /**"400"   [   10.4.1节] 坏请求			*/
	public final static  int E400 = 400;
	
    /**"401"   [   10.4.2节] 未授权的			*/
	public final static  int E401 = 401;
	
    /**"402"   [   10.4.3节] 必要的支付		*/
	public final static  int E402 = 402;
	
    /**"403"   [   10.4.4节] 禁用				*/
	public final static  int E403 = 403;
	
    /**"404"   [   10.4.5节] 没找到			*/
	public final static  int E404 = 404;
	
    /**"405"   [   10.4.6节] 不允许的方式		*/
	public final static  int E405 = 405;
	
    /**"406"   [   10.4.7节] 不接受			*/
	public final static  int E406 = 406;
	
    /**"407"   [   10.4.8节] 需要代理验证		*/
	public final static  int E407 = 407;
	
    /**"408"   [   10.4.9节] 请求超时			*/
	public final static  int E408 = 408;
	
    /**"409"   [   10.4.10节] 冲突 			*/
	public final static  int E409 = 409;
	
    /**"410"   [   10.4.11节] 停止			*/
	public final static  int E410 = 410;
	
    /**"411"   [   10.4.12节] 需要的长度		*/
	public final static  int E411 = 411;
	
    /**"412"   [ 10.4.13节] 预处理失败			*/
	public final static  int E412 = 412;
	
    /**"413"   [   10.4.14节] 请求实体太大		*/
	public final static  int E413 = 413;
	
    /**"414"   [   10.4.15节] 请求-URI太大	*/
	public final static  int E414 = 414;
	
    /**"415"   [   10.4.16节] 不支持的媒体类型	*/
	public final static  int E415 = 415;
	
    /**"416"   [ 10.4.17节] 请求的范围不满足	*/
	public final static  int E416 = 416;
	
    /**"417"   [   10.4.18节] 期望失败		*/
	public final static  int E417 = 417;
	
    /**"500"   [   10.5.1节]   服务器内部错误	*/
	public final static  int E500 = 500;
	
    /**"501"   [   10.5.2节]   不能实现		*/
	public final static  int E501 = 501;
	
    /**"502"   [   10.5.3节]   坏网关			*/
	public final static  int E502 = 502;
	
    /**"503"   [   10.5.4节]   服务不能实现	*/
	public final static  int E503 = 503;
	
    /**"504"   [   10.5.5节]   网关超时		*/
	public final static  int E504 = 504;
	
    /**"505"   [   10.5.6节]   HTTP版本不支持	*/
	public final static  int E505 = 505;
}
