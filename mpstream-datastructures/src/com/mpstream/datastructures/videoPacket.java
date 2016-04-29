package com.mpstream.datastructures;

import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.IPixelFormat.Type;
import com.xuggle.ferry.IBuffer;
import java.io.*;
import java.util.zip.*;
import java.nio.ByteBuffer;

public class videoPacket implements Serializable {
    public int id;
    public ByteBuffer data;
    public byte[] buf;
    public IPixelFormat.Type p;
    public long pts;
    
    public videoPacket(int i, IVideoPicture iv) throws IOException{
        this.id = i;
        this.p = iv.getPixelType();
        this.pts = iv.getPts();
        //long startTime = System.nanoTime();
        try {        
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
            objectOut.writeObject(iv.getData().getByteArray(0, iv.getSize()));
            objectOut.close();
            buf = baos.toByteArray();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        iv.delete();
        //long endTime = System.nanoTime();
        //long duration = (endTime - startTime)/1000000;
        //System.out.println("Time to compress video packet: "+duration);
    }
    
    public IVideoPicture getvideoPic() {
        //long startTime = System.nanoTime();
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
            buf = (byte[]) objectIn.readObject();
            objectIn.close();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        int packetLength = buf.length; 
        ByteBuffer  byBuffer = ByteBuffer.allocateDirect (packetLength); 
        byBuffer.put(buf, 0, packetLength); 
        buf = null;
        IBuffer InBuffer = IBuffer.make(null, byBuffer, 0, packetLength); 
        IVideoPicture videoPic = IVideoPicture.make(InBuffer,p,640,360); 
        InBuffer.delete();
        videoPic.setComplete(true, p, 640, 360, pts);
        //long endTime = System.nanoTime();
        //long duration = (endTime - startTime)/1000000;
        //System.out.println("Time to decompress video packet: "+duration);
        return videoPic;
    }
}
