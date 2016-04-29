package com.mpstream.datastructures;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IAudioSamples.Format;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class audioPacket implements Serializable {
    public int id;
    public ByteBuffer data;
    public byte[] buf;
    private int channels;
    public long pts;
    public long numSamples;
    public int sampleRate;
    public IAudioSamples.Format format;
    public audioPacket(int i, IAudioSamples ia) {
        this.id = i;
        this.channels = ia.getChannels();
        this.format = ia.getFormat();
        this.pts = ia.getPts();
        this.numSamples = ia.getNumSamples();
        this.sampleRate = ia.getSampleRate();
        try {        
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
            objectOut.writeObject(ia.getData().getByteArray(0, ia.getSize()));
            objectOut.close();
            buf = baos.toByteArray();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        ia.delete();
    }
    public IAudioSamples getAudioSample() {
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
        this.buf = null;
        IBuffer InBuffer = IBuffer.make(null, byBuffer, 0, packetLength);
        IAudioSamples audioSample = IAudioSamples.make(InBuffer, channels, format);
        InBuffer.delete();
        audioSample.setComplete(true, numSamples, sampleRate, channels, format, pts);
        return audioSample;
    }
}