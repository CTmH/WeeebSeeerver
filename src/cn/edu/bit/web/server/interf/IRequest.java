package cn.edu.bit.web.server.interf;

/** 
 * ����ӿ�.<br>
 * ʵ��IRequest�ӿڵ���<b>����ͨ��</b>"request"������"IResponsion"�����ص����������.<br>
 * �������Ķ���û������,Ҫô������ʱ�׳��쳣,Ҫô�ڻص�ʱ,����Ϊnull;
 */
public interface IRequest {
	/** 
	 * ���������ʵ������ӿ�,������������ɹ����ú�
	 * ���������������,���ȴ����ص�--ͨ��"IResponsion"�ӿ�
	 */
	public void request(Object o, IResponse ir) throws Exception;
}
