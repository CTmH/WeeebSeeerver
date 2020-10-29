package cn.edu.bit.web.server.common.file;

import cn.edu.bit.web.server.common.Logger;
import cn.edu.bit.web.server.config.WebConfig;

import java.io.*;

public class Cache {
    /** �Ѿ������� */
    public final static int CACHED = 1;
    /** ���ڻ����� */
    public final static int ISCACHEING = 2;
    /** û�б����� */
    public final static int NOCACHE = 3;
    /** ����������Ѿ������� */
    public final static int BRUSHOFF = 0;
    /** ���»���ı�� */
    public final static int RECACHE = 4;

    /** ��ǰ��������ü���,���ü�����Ϊ��,�����ͷ��ڴ� */
    private int reference = 0;

    private File filename;
    private long lastModifiedTime;
    private long creattime;
    private int usecount;
    private byte[] buffer = null;
    /** ʵ���ļ��ĳ��� */
    private long fileLength;

    private volatile int state;

    public Cache(File name) throws FileNotFoundException {
        if (!name.isFile()) throw new FileNotFoundException();
        creattime = System.currentTimeMillis()/1000;
        filename = name;
        usecount = 0;
        lastModifiedTime = name.lastModified();
        fileLength = name.length();
        state = NOCACHE;
    }

    /**
     * ����ļ��Ƿ񱻵�ǰ�Ķ������ù�
     * @param f - Ҫ�����ļ�
     * @return - ���÷���true,���״̬ΪBRUSHOFF���Ƿ���false
     */
    public boolean isCreated(File f) {
        return (filename.equals(f)) && (state!=BRUSHOFF);
    }

    /**
     * ���ص�ǰ��״̬
     * @return CACHED, ISCACHEING, NOCACHE, BRUSHOFF �е�һ��
     */
    public int currentState() {
        return state;
    }

    /**
     * ����ļ����޸Ĺ�����true
     */
    private boolean isModified() {
        return lastModifiedTime!=filename.lastModified();
    }

    /**
     * �õ��ļ�<b>ָ����Χ</b>��������,�������������Ի���,Ҳ���������ļ�ϵͳ,��ȡ���ڵ�ǰ��״̬
     * @param range - ����ķ�Χ���,Ϊ��������ȫ�����ļ�
     * @return java.io.InputStream
     * @throws IOException - �ļ���������,�׳�����쳣
     */
    public InputStream getInputStream()
            throws Exception
    {
        if (isModified()) {
            Logger.error("�ļ������������»���:"+filename);
            if (reCacheFile()) {
                Logger.error("�ļ��������.");
            } else {
                Logger.error("�����ػ���,����һ���߳����ڷ����������.");
            }
        }
        ++usecount;
        if (state==CACHED) {
            return new CacheStream();
        }
        else if (state==NOCACHE) {
            try {
                return new FileStream(filename);
            } catch(FileNotFoundException e) {
                release();
                throw e;
            }
        }
        else {
            throw new IllegalStateException();
        }
    }

    /**
     * ��ʼ����������ļ�, �����ļ��Ƿ��Ѿ��������
     */
    public void beginCacheFile() {
        if (state==NOCACHE || state==RECACHE) {
            state = ISCACHEING;

            InputStream in = null;
            fileLength = filename.length();
            if (fileLength>= WebConfig.maxCachedFileLength) {
                buffer = new byte[0];
                state = NOCACHE;
                return;
            }
            try {
                buffer = new byte[(int)fileLength];
            } catch(Error e) {
                // ����ڴ����
                buffer = new byte[0];
                state = NOCACHE;
                return;
            }
            try {
                in = new FileInputStream(filename);
                in.read(buffer);

                state = CACHED;
            } catch(Exception e) {
                state = NOCACHE;
                buffer = new byte[0];
            } finally {
                try {
                    if (in!=null) in.close();
                } catch (IOException e) {}
            }
        }
    }

    /**
     * ���»��嵱ǰ�������õ��ļ�
     * �ɹ�����true
     */
    public boolean reCacheFile() {
        if ( (state==CACHED||state==NOCACHE) && reference==0) {
            state=RECACHE;
            lastModifiedTime = filename.lastModified();
            beginCacheFile();
            return true;
        }
        return false;
    }

    /**
     * �ͷ��ļ�������ռ�õ����пռ�,�ɹ�����true
     */
    public boolean release() {
        if (reference==0) {
            buffer = new byte[0];
            state = BRUSHOFF;
            System.gc();
            return true;
        }
        return false;
    }

    /**
     * ���ػ����ʱ��
     * @return - �����ʱ������Ϊ��λ
     */
    public int cacheTime() {
        long current  = System.currentTimeMillis()/1000;
        return (int)(current-creattime);
    }

    /**
     * ���ر�ʹ�õĴ���
     * @return ʹ�õĴ���
     */
    public int getUseCount() {
        return usecount;
    }

    /**
     * ������һСʱ��,��ʹ�õĴ���
     * @return ʹ�õĴ���
     */
    public int ontHourUseCount() {
        return getUseCount()/(cacheTime()/60/60);
    }

    /**
     * ����ռ�õ��ڴ��С
     * @return - ���ֽ�Ϊ��λ
     */
    public int useMemory() {
        return buffer.length;
    }

    /** ��������װ */
    private class CacheStream extends InputStream {
        private int readcount = 0;

        /** ���뱣֤Range����ȷ�� */
        public CacheStream() {
            ++reference;
        }

        public int read() throws IOException {
            if (readcount<buffer.length) {
                return toInt(buffer[readcount++]);
            } else {
                return -1;
            }
        }

        public void close() {
            --reference;
        }
    }

    private class FileStream extends FileInputStream {
        private long current;

        /** ���뱣֤Range����ȷ�� */
        public FileStream(File f) throws IOException {
            super(f);
            current = 0;
        }

        public int read() throws IOException {
            if (current<=fileLength-1) {
                return super.read();
            } else {
                return -1;
            }
        }
    }

    private final static int toInt(byte b){
        int r = 0;
        if (b<0) r = 256 + b;
        else r = (int)b;
        return r;
    }

    public long getFileLength() {
        return fileLength;
    }

    public String getFilename() {
        return filename.getPath();
    }

    public int referenceCount() {
        return reference;
    }

    public String state() {
        final String[] s = {"BRUSHOFF", "CACHED", "ISCACHEING", "NOCACHE", "RECACHE",};
        return s[state];
    }
}
