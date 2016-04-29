package com.mpstream;

import com.mpstream.datastructures.Buffer;
import com.mpstream.datastructures.ImageComponent;
import com.mpstream.datastructures.audioPacket;
import com.mpstream.datastructures.configPacket;
import com.mpstream.datastructures.videoPacket;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.Utils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author anush_cr
 */
class PlanManager implements Runnable {
    public Thread t;
    private String threadName;
    long start = 0;
    long end = 0;
    Buffer buffer;
    ObjectInputStream input;
    Socket server;
    JFrame mainFrame;
    ImageComponent videoScreen;
    final Client client;
    
    PlanManager(Socket s, ObjectInputStream in, JFrame mf, ImageComponent ic, Buffer b, Client c) {
        this.threadName = "Plan Manager thread";
        buffer = b;
        mainFrame = mf;
        videoScreen = ic;
        input = in;
        server = s;
        this.client = c;
    }

    @Override
    @SuppressWarnings(value = "deprecation")
    public void run() {
        start = System.nanoTime();
        for (;;) {
            if(client.exitFlag)
                break;
            try {
                Object obj = input.readObject();
                if (obj instanceof videoPacket) {
                    videoPacket vp = (videoPacket) obj;
                    obj = null;
                    IVideoPicture videoPic = vp.getvideoPic();
                    //System.out.println("Received Video "+vp.id);
                    synchronized (buffer.videoList) {
                        if(buffer.videoList != null) {
                            //System.out.println("Buffer size " + buffer.videoList.indexOf(null));
                            buffer.videoList.set(vp.id, videoPic);
                        }
                    }
                    vp.buf = null;
                    
                //System.out.println("trying to play: "+buffer.videoList.get(vp.id).toString());
                    //videoScreen.setImage(Utils.videoPictureToImage((IVideoPicture)buffer.videoList.get(vp.id)));
                    //videoPic.delete();
                    vp = null;
                } else if (obj instanceof audioPacket) {
                    audioPacket ap = (audioPacket) obj;
                    obj = null;
                    IAudioSamples audioSample = ap.getAudioSample();
                    //System.out.println("Received Audio "+ap.id);
                    synchronized (buffer.audioList) {
                        if(buffer.audioList != null)
                            buffer.audioList.set(ap.id, audioSample);
                    }
                    ap.buf = null;
                    //audioSample.delete();
                    ap = null;
                } else if (obj instanceof configPacket) {
                    configPacket cp = (configPacket) obj;
                    obj = null;
                    if (cp.cmd == configPacket.CMD.DONE) {
                        //client.exitFlag = true;
                        cp = null;
                        break;
                    }
                }
                mainFrame.validate();
                mainFrame.repaint();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                break;
            }
        }
        try {
            server.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        end = System.nanoTime();
        long duration = (end - start) / 1000000;
        System.out.println("Buffer time: " + duration);
        System.out.println("Thread " + threadName + " exiting.");
    }

    public void start() {
        System.out.println("Starting " + threadName);
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }
    
}
