package cn.edu.bit.web.server.common;

import cn.edu.bit.web.server.config.WebConfig;
import cn.edu.bit.web.server.interf.IRequest;
import cn.edu.bit.web.server.interf.IResponse;

import java.io.*;

public final class CgiResponse implements IRequest{

    private final static CgiResponse me = new CgiResponse();

    public static void Init() {}

    public static final CgiResponse get(){
        return me;
    }

    private int count = 0;
    private BuildInCgi[] cgis=new BuildInCgi[2];

    private CgiResponse(){
        //��ʼ��
        cgis[0] = new BuildInCgi(
            "mycalc.exe",
            ".\\website\\default\\cgi-bin\\mycalc.exe",     //·��·��·��·��·��·��
            true
        );
        cgis[1] = new BuildInCgi(
            "mydatabase.exe",
            ".\\website\\default\\cgi-bin\\mydatabase.exe",     //·��·��·��
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
                String []env = getENV(hha);
                try{
                    File runPath = new File(cgiPath);
                    Process cgi = Runtime.getRuntime().exec(cgiPath, env, runPath.getParentFile());
                    writePostMessage(cgi.getOutputStream(), hha);
                    StringBuffer data = readData(cgi.getInputStream());
                    hha.println("HTTP/1.1 200 OK");

                    hha.setContentLength(data.length());//֮ǰĪ�����ŵ�
                    hha.printEnd();
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

        private String[] getENV(HttpHeadAnalyser hha) {
            String[] env = new String[22];
            String scrname = sf(hha.getRequestFile().getName());
            String abspath = sf(hha.getRequestFile().getAbsolutePath());

            env[0] = "CONTENT_TYPE="	+	sf(hha.get("Content-Type"));
            env[1] = "PATH_TRANSLATED="	+	abspath;
            env[6] = "SCRIPT_NAME="		+	scrname;
            env[12]= "PATH_INFO="		+
                    sf( abspath.substring(0, abspath.length() - scrname.length()) );

            env[2] = "QUERY_STRING="	+	sf(hha.getArguments());
            env[3] = "REMOTE_ADDR="		+	sf(hha.getRemoteAddress().getHostAddress());
            env[4] = "REMOTE_HOST="		+	sf(hha.getRemoteAddress().getHostName());
            env[5] = "REQUEST_METHOD="	+	sf(hha.getMethod());
            env[7] = "SERVER_NAME="		+	sf(hha.getHost());
            env[8] = "SERVER_PORT="		+	WebConfig.serverPort;
            env[9] = "SERVER_SOFTWARE="	+	"v0.471 WebDrome";//VersionControl.programName+' '+
                    //VersionControl.version;

            env[10]= "SERVER_PROTOCOL=HTTP/1.1";
            env[11]= "GATEWAY_INTERFACE=CGI/1.1";
            env[13]= "REMOTE_IDENT=";
            env[14]= "REMOTE_USER=";
            env[15]= "AUTH_TYPE=";
            env[16]= "CONTENT_LENGTH="	+	(hha.getMessageBody().length>0 ?
                    hha.getMessageBody().length : "");

            env[17]= "ACCEPT=" 			+	sf(hha.get("Accept"));
            env[18]= "ACCEPT_ENCODING="	+	sf(hha.get("Accept-Encoding"));
            env[19]= "ACCEPT_LANGUAGE="	+	sf(hha.get("Accept-Language"));
            env[20]= "REFFERER="		+	sf(hha.get("Referer"));
            env[21]= "USER_AGENT="		+	sf(hha.get("User-Agent"));

            return env;
        }

        private final String sf(String s) {
            return s!=null ? s : "" ;
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
