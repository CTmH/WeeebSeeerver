package cn.edu.bit.web.server.common;

import cn.edu.bit.web.server.config.Range;
import cn.edu.bit.web.server.config.WebConfig;

import java.io.*;

public class Cache {
    /** 已经被缓存 */
    public final static int CACHED = 1;
    /** 正在缓存中 */
    public final static int ISCACHEING = 2;
    /** 没有被缓存 */
    public final static int NOCACHE = 3;
    /** 这个缓存类已经被丢弃 */
    public final static int BRUSHOFF = 0;
    /** 重新缓存的标记 */
    public final static int RECACHE = 4;

    /** 当前缓冲的引用计数,引用计数不为零,不能释放内存 */
    private int reference = 0;

    private File filename;
    private long lastModifiedTime;
    private long creattime;
    private int usecount;
    private byte[] buffer = null;
    /** 实际文件的长度 */
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
     * 检查文件是否被当前的对象引用过
     * @param f - 要检查的文件
     * @return - 引用返回true,如果状态为BRUSHOFF总是返回false
     */
    public boolean isCreated(File f) {
        return (filename.equals(f)) && (state!=BRUSHOFF);
    }

    /**
     * 返回当前的状态
     * @return CACHED, ISCACHEING, NOCACHE, BRUSHOFF 中的一个
     */
    public int currentState() {
        return state;
    }

    /**
     * 如果文件被修改过返回true
     */
    private boolean isModified() {
        return lastModifiedTime!=filename.lastModified();
    }

    /**
     * 得到文件<b>指定范围</b>的输入流,输入流可能来自缓冲,也可能来自文件系统,这取决于当前的状态
     * @param range - 请求的范围如果,为空则请求全部的文件
     * @return java.io.InputStream
     * @throws IOException - 文件操作错误,抛出这个异常
     */
    public InputStream getInputStream(Range range)
            throws Exception
    {
        boolean recached = false;
        if (isModified()) {
            LogSystem.error("文件被更改需重新缓冲:"+filename);
            if (reCacheFile()) {
                recached = true;
                LogSystem.error("文件缓冲完成.");
            } else {
                LogSystem.error("不能重缓冲,有另一个线程正在访问这个缓冲.");
            }
        }
        if (range==null) {
            range = new Range(0, fileLength-1);
        } else {
            if (recached) {
                throw new Exception("文件被更改,范围失效,请求需重新递交.");
            }
            boolean n1 = range.getFirstPos()<0;
            boolean n2 = range.getLastPos()>fileLength-1;
            boolean n3 = range.getFirstPos()>=range.getLastPos();
            if (n1 || n2 || n3) {
                throw new Exception("范围参数不合法.");
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
     * 得到文件<b>全部范围</b>的输入流,输入流可能来自缓冲,也可能来自文件系统,这取决于当前的状态
     * @return java.io.InputStream
     * @throws Exception - 文件操作错误或当Range参数的请求超过文件的实际大小,抛出这个异常
     */
    public InputStream getInputStream()
            throws Exception
    {
        return getInputStream(null);
    }

    /**
     * 开始缓冲载入的文件, 会检查文件是否已经被缓冲过
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
                // 如果内存溢出
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
     * 重新缓冲当前对像引用的文件
     * 成功返回true
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
     * 释放文件缓冲所占用的所有空间,成功返回true
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
     * 返回缓冲的时间
     * @return - 缓冲的时间以秒为单位
     */
    public int cacheTime() {
        long current  = System.currentTimeMillis()/1000;
        return (int)(current-creattime);
    }

    /**
     * 返回被使用的次数
     * @return 使用的次数
     */
    public int getUseCount() {
        return usecount;
    }

    /**
     * 返回在一小时中,被使用的次数
     * @return 使用的次数
     */
    public int ontHourUseCount() {
        return getUseCount()/(cacheTime()/60/60);
    }

    /**
     * 返回占用的内存大小
     * @return - 以字节为单位
     */
    public int useMemory() {
        return buffer.length;
    }

    /** 缓冲流包装 */
    private class CacheStream extends InputStream {
        private int readcount = 0;
        private Range range;

        /** 必须保证Range的正确性 */
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

        /** 必须保证Range的正确性 */
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
