package cn.edu.bit.web.server.common;

import cn.edu.bit.web.server.config.Range;
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
    public InputStream getInputStream(Range range)
            throws Exception
    {
        boolean recached = false;
        if (isModified()) {
            LogSystem.error("�ļ������������»���:"+filename);
            if (reCacheFile()) {
                recached = true;
                LogSystem.error("�ļ��������.");
            } else {
                LogSystem.error("�����ػ���,����һ���߳����ڷ����������.");
            }
        }
        if (range==null) {
            range = new Range(0, fileLength-1);
        } else {
            if (recached) {
                throw new Exception("�ļ�������,��ΧʧЧ,���������µݽ�.");
            }
            boolean n1 = range.getFirstPos()<0;
            boolean n2 = range.getLastPos()>fileLength-1;
            boolean n3 = range.getFirstPos()>=range.getLastPos();
            if (n1 || n2 || n3) {
                throw new Exception("��Χ�������Ϸ�.");
            }
        }
        ++usecount;
        if (state==CACHED) {
            return new CacheStream(range);
        }
        else if (state==NOCACHE) {
            try {
                return new FileStream(filename, range);
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
     * �õ��ļ�<b>ȫ����Χ</b>��������,�������������Ի���,Ҳ���������ļ�ϵͳ,��ȡ���ڵ�ǰ��״̬
     * @return java.io.InputStream
     * @throws Exception - �ļ����������Range���������󳬹��ļ���ʵ�ʴ�С,�׳�����쳣
     */
    public InputStream getInputStream()
            throws Exception
    {
        return getInputStream(null);
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
        private Range range;

        /** ���뱣֤Range����ȷ�� */
        public CacheStream(Range r) {
            ++reference;
            range = r;
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
        private Range range;
        private long current;

        /** ���뱣֤Range����ȷ�� */
        public FileStream(File f, Range r) throws IOException {
            super(f);
            range = r;
            this.skip(r.getFirstPos());
            current = r.getFirstPos();
        }

        public int read() throws IOException {
            if (current<=range.getLastPos()) {
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
