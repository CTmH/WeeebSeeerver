package cn.edu.bit.web.server.common;

import cn.edu.bit.web.server.config.WebConfig;
import cn.edu.bit.web.server.interf.IRequest;
import cn.edu.bit.web.server.interf.IResponse;

import java.io.*;
import java.util.concurrent.TimeUnit;

public final class CgiManager implements IRequest{

    private final static CgiManager me = new CgiManager();

    public static void Init() {}

    public static final CgiManager get(){
        return me;
    }

    private int count = 0;
    private BuildInCgi[] cgis=new BuildInCgi[2];

    private CgiManager(){
        //��ʼ��
        cgis[0] = new BuildInCgi(
            "mycalc.exe",
            ".\\website\\default\\cgi-bin\\mycalc.exe",  // url�Ͷ�Ӧ������
            true
        );
        cgis[1] = new BuildInCgi(
            "dataset.py",
            ".\\website\\default\\cgi-bin\\dataset.py",
            true
        );
    }

    public void request(Object o, IResponse ir)
            throws Exception
    {
        ++count;
        if (o instanceof HttpHeadAnalyser) {
            HttpHeadAnalyser hha = (HttpHeadAnalyser)o;
            for (int i=0; i<2; ++i) {
                if (cgis[i].canDisposal(hha.getRequestFile())) {
                    cgis[i].request(o, ir);
                    return;
                }
            }
        }
        throw new Exception();
    }

    private class BuildInCgi implements IRequest{

        /** �ű���ִ�г��������·�� */
        protected String cgiPath;
        /** �Ƿ�����֧�� */
        protected boolean support;
        /** �ű��ı�ʶ���� */
        protected String name;

        private int count = 0;

        public BuildInCgi(String cginame,
                          String path,
                          boolean enable ){
            cgiPath = path;
            support = enable;
            name = cginame;
        }

        public final boolean canDisposal(File f) {
            if (support) {
                if (f.isFile()) {
                    String _name = f.getName();
                    if(name.equals(_name)) return true;
                }
            }
            return false;
        }

        public final void request(Object o, IResponse ir) throws Exception{
            //request�ӿڵ�ʵ��
            HttpHeadAnalyser hha = (HttpHeadAnalyser)o;
            File f = hha.getRequestFile();
            if(f!=null){
                count ++;
                try{
                    File runPath = new File(cgiPath);
                    String cmd=cgiPath;
                    if(name.endsWith(".py")) {
                    	cmd="python"+" "+cgiPath;
                    	//env=null;  // ���env��getENV()�Ľ�������CGI���Ϊ�գ���֪��Ϊʲô
                    }
                    //System.out.println("Command:"+cmd);
                    //System.out.println(runPath.getParentFile());
                    Process cgi = Runtime.getRuntime().exec(cmd);
                    writePostMessage(cgi.getOutputStream(), hha);
                    StringBuffer data = readData(cgi.getInputStream());
                    cgi.waitFor(10, TimeUnit.SECONDS);
                    hha.println("HTTP/1.1 200 OK");

                    hha.setContentLength(data.length());//֮ǰĪ�����ŵ�
                    hha.printEnd();
                    //System.out.println("CGI:"+data.toString());
                    ir.response(new ByteArrayInputStream(data.toString().getBytes()));
                    return;
                }catch (Exception e) {
                    throw e;
                }
            }
        }

        private void writePostMessage(OutputStream out, HttpHeadAnalyser hha)
                throws IOException
        {
            out.write(hha.getMessageBody());
            out.flush();
            out.close();
        }

        /** ��ȡ���ݵ��ַ��� */
        private StringBuffer readData(InputStream in){
            StringBuffer outputStringBuffer = new StringBuffer();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                String str = null;
                while ((str = bufferedReader.readLine()) != null) {
                    outputStringBuffer.append(str);
                    outputStringBuffer.append("\r\n");
                }
                bufferedReader.close();
                return outputStringBuffer;
            } catch (Exception e) {
                e.printStackTrace();     //�������, ��ӡ������Ϣ
                return null;
            }
        }
    }
}
