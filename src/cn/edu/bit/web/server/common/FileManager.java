package cn.edu.bit.web.server.common;

import cn.edu.bit.web.server.config.Range;
import cn.edu.bit.web.server.config.WebConfig;
import cn.edu.bit.web.server.interf.IRequest;
import cn.edu.bit.web.server.interf.IResponse;

import javax.xml.ws.http.HTTPException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * �ļ�����������ӳ�������ʱ,�ͻ�һֱ����,ֱ���������
 * �����ʵ��ֻ��һ��,ͨ��ȫ�ַ������Ի�����ʵ��
 */
public class FileManager implements IRequest {
    private static FileManager instance = new FileManager();

    /** ����FileCacheManage���Ψһʵ�� */
    public static IRequest get() {
        return instance;
    }
    /** ��FileCacheManage����׼�� */
    public static void Init() {}

    class Memory{
        public String getName() {
            return "�����ڴ�";
        }
        private Object getVolume() {
            Runtime r  = Runtime.getRuntime();
            usedMemory = r.totalMemory()-r.freeMemory();
            freeRatio  = r.freeMemory()*100/r.totalMemory();
            return freeRatio+ "%   (�ڴ�����:" +(usedMemory)/1024+"KB)";
        }
    }

    private Memory memory = new Memory();

    public volatile long usedMemory = 0;
    public volatile long freeRatio  = 100;

    /** �����ļ��� */
    private List list;
    /** ������� */
    private List requestQueue;

    private FileManager() {
        list = new ArrayList();
        requestQueue = new LinkedList();
        new RequestQueueProcessor().start();


        // ����ò��û�ж�Ӧ�ķ���
//        LogSystem.addToState(this);
//        LogSystem.addToState(memory);
//        LogSystem.showCacheMonitor(this);
    }

    /* ���е��������ͨ��ir�ص�!�������ֿյ��߳� */
    public synchronized void request(Object o, IResponse ir)
            throws FileNotFoundException
    {
        requestQueue.add( new RequestData(o,ir) );
    }

    /**
     * ����һ���ļ��Ļ������,����ļ�û�л���,�ͽ�����
     * @param f - ��Ч���ļ���,
     * @return һ���᷵��һ��FileCache
     * @throws FileNotFoundException -
     * 			cached�ڲ��Ὠ��FileCache����ļ��Ƿ�,���׳�����쳣
     */
    private Cache cached(File f) throws FileNotFoundException  {
        int cont = list.size();
        Cache fc;
        for (int i=0; i<cont; ++i) {
            fc = (Cache)list.get(i);
            if (fc.isCreated(f)) {
                if (fc.currentState()!= Cache.BRUSHOFF) {
                    return fc;
                } else {
                    list.remove(fc);
                }
            }
        }
        fc = new Cache(f);
        fc.beginCacheFile();
        list.add(fc);
        return fc;
    }

    /**
     * �Ƴ����ڵ��ļ�����, ����:
     * 1.���ڴ治��ʱ - ���õ��ڴ������ڴ��10%
     *   �Ƴ�ռ���ڴ�����5���ļ�����
     *
     * 2.�ڴ��㹻,ʱ��ﵽ2Сʱʱ;
     *   �Ƴ� 1Сʱ���ļ�ʹ�õĴ�������1���ļ�
     */
    private void removeCachedFile() {
        // �����ڴ�����
        memory.getVolume();
        if (usedMemory> WebConfig.maxMemoryUse || freeRatio<10) {
            out("�ļ���������:"+list.size()+".�ڴ治���ͷ��ڴ�");
            int[] max = {0,0,0,0,0};
            int index = 0;
            for (int x=0; x<list.size(); ++x) {
                int currt = ((Cache)list.get(x)).useMemory();
                if ( max[index]<currt ) {
                    max[index++] = x;
                    if (index>=max.length) {
                        index = 0;
                    }
                }
            }
            removesFile(max, max.length);
        } else {
            if (lapseTime()>=WebConfig.clearCacheTime) {
                out(WebConfig.clearCacheTime+"Сʱ,������..");
                resetTime();
                int[] tolong = new int[list.size()];
                int index = 0;
                for (int i=0; i<list.size(); ++i) {
                    int time = ((Cache)list.get(i)).ontHourUseCount();
                    if (time<WebConfig.clearCacheTime) {
                        tolong[index++] = i;
                        if (index>=tolong.length) break;
                    }
                }
                removesFile(tolong, index);
            }
        }
    }

    private void removesFile(int[] index, int count) {
        Object[] o = new Object[count];
        for (int i=0; i<o.length; ++i) {
            o[i] = list.get(index[i]);
        }
        for (int i=0; i<o.length; ++i) {
            if ( ((Cache)o[i]).release() ) {
                list.remove(o[i]);
            }
        }
    }

    private long starttime = System.currentTimeMillis();
    /**
     * �����ϴε���resetTime()������Сʱ��
     * @return - ����һ��int��Сʱ��
     */
    private long lapseTime() {
        long currenttime = System.currentTimeMillis();
        int passtime = (int)((currenttime-starttime)/1000/60/60);
        return passtime;
    }
    /**
     * ����ʱ��,��lapseTime()���ʹ��
     */
    private void resetTime() {
        starttime = System.currentTimeMillis();
    }

    /** ��Ϣ���д��� */
    private class RequestQueueProcessor extends Thread {
        /** û��"Range"�� */
        final Object NORANGE = null;
        public void run() {
            int delayremoveaction = 0;
            while (true) {
                if (requestQueue.size()>0) {
                    RequestData rd = (RequestData)requestQueue.get(0);
                    //--------
                    HttpHeadAnalyser hha = rd.hha;
                    InputStream in	= null;
                    try {
                        Cache fc	= cached(rd.o);
                        Range r 		= hha.getRange();

                        if (r==NORANGE) {
                            hha.println("HTTP/1.1 200 OK");
                            in = fc.getInputStream();
                            hha.setContentLength(fc.getFileLength());
                        } else {
                            hha.println("HTTP/1.1 206");
                            r.setLastPos(fc.getFileLength()-1);
                            in = cached(rd.o).getInputStream(r);
                            hha.setContentRange(
                                    r.getFirstPos(),
                                    r.getLastPos(),
                                    fc.getFileLength() );
                            hha.setContentLength(r.getLastPos()-r.getFirstPos()+1);
                        }
                        // TODO ��̫��զд
//                        hha.setMimeType( MimeTypes.getMimeName(hha) );
                        hha.printEnd();

                    } catch(HTTPException e) {
//                        hha.error(e.getHttpErrorCode(), e.toString());

                    } catch(IOException e) {
                        // LogSystem.error(Language.clientClose+".");
                        // donothing..
                    } catch(Exception e) {
                        LogSystem.error("δ֪�Ĵ���:"+e);

                    } finally {
                        rd.ir.response( in );
                        requestQueue.remove(0);
                    }
                } else {
                    if (++delayremoveaction>100) {
                        delayremoveaction = 0;
                        removeCachedFile();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                }
            }
        }
    }

    /** �����װ�� */
    private class RequestData {
        File o;
        IResponse ir;
        HttpHeadAnalyser hha;

        private RequestData(Object obj, IResponse ir) throws FileNotFoundException {
            hha = (HttpHeadAnalyser)obj;
            o = hha.getRequestFile();
            if (!o.isFile()) throw new FileNotFoundException();
            this.ir= ir;
        }
    }

    private void out(Object o) {
        LogSystem.message(o);
    }

    public String getName() {
        return "�ļ���������";
    }

    public Object getVolume() {
        return list.size();
    }

    public Object[] getFileList() {
        return list.toArray();
    }
}
