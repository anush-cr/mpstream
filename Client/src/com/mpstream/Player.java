package com.mpstream;

import static com.mpstream.Player.timeToPlay;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import com.xuggle.xuggler.demos.VideoImage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import com.mpstream.datastructures.*;
import java.util.List;
import javax.swing.JFrame;
import com.mpstream.datastructures.ImageComponent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Player implements Runnable{
    //The window we'll draw the video on.
    private static VideoImage mScreen = null;
    //The audio line we'll output sound to; it'll be the default audio device on your system if available
    public static SourceDataLine mLine;
    private Buffer buffer;
    private Thread t;
    private String threadName;
    private static long startTime;
    private static long keyframeTimestamp;
     private static long videoStartTime;
    private JFrame mainFrame;
    private ImageComponent videoScreen;
    private final Client client;
    public int video = 0, rebuffer = 0;;
    public boolean bufferFlag = true;
    private Player player;
    
    private static void openJavaSound(IAudioSamples samples) throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat(samples.getSampleRate(),
            (int)IAudioSamples.findSampleBitDepth(samples.getFormat()),
            samples.getChannels(),
            true,
            false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        mLine = (SourceDataLine) AudioSystem.getLine(info);
        //if that succeeded, try opening the line.
        mLine.open(audioFormat);
        //And if that succeed, start the line.
        mLine.start();
    }

    private static void closeJavaSound() {
        if (mLine != null) {
            //Wait for the line to finish playing
            mLine.drain();
            //Close the line
            mLine.close();
            mLine=null;
        }
    }
    
    public static long timeToPlay(long timestamp) {
        long millisecondsToSleep = 0;
        long currentTime = System.currentTimeMillis();
        long clockTimeSinceStartofVideo = currentTime - startTime;   
        //System.out.println("clockTimeSinceStartofVideo:"+clockTimeSinceStartofVideo);
        // compute how long for this frame since the first frame in the stream.
        // remember that IVideoPicture and IAudioSamples timestamps are always in MICROSECONDS,
        // so we divide by 1000 to get milliseconds.
        long streamTimeSinceStartOfVideo = (timestamp - keyframeTimestamp)/1000;
        //System.out.println("streamTimeSinceStartOfVideo:"+streamTimeSinceStartOfVideo);
        final long tolerance = 50; // and we give ourselfs 50 ms of tolerance
        millisecondsToSleep = (streamTimeSinceStartOfVideo -
            (clockTimeSinceStartofVideo+tolerance));
        //System.out.println("sleep:"+millisecondsToSleep);
        if(millisecondsToSleep < 0)
            millisecondsToSleep = 0;
        return millisecondsToSleep;
    }
 
    public Player(Buffer b, Client c) {
        threadName = "Player main thread";
        this.buffer = b;
        this.client = c;
        this.mainFrame = client.mainFrame;
        this.videoScreen = client.videoScreen;
        this.player = this;
    }
    
    @Override
    public void run() {
        try {
            client.statusLabel.setText("Status bar:\nBuffering");
            t.sleep(2500);     
            IAudioSamples ia;
            synchronized(buffer.audioList) {
                while(buffer.audioList.indexOf(null) == 0) {}
                ia = (IAudioSamples)buffer.audioList.get(0);
                openJavaSound(ia);
            }
            synchronized(buffer.videoList) {
                while(buffer.videoList.indexOf(null) == 0) {}
                IVideoPicture vp = (IVideoPicture)buffer.videoList.get(0);
                keyframeTimestamp = vp.getTimeStamp();
            }
            audioPlayer ap = new audioPlayer(buffer,client,player);
            ap.start();
            for(;;) {
                if(client.exitFlag)
                    break;
                if(bufferFlag) {
                    t.sleep(3000);
                    client.statusLabel.setText("Status bar:\nPlaying Video");
                }
                IVideoPicture videoPic = null;
                synchronized(buffer.videoList) {
                    if(buffer.videoList.indexOf(null) <= video) {
                        client.statusLabel.setText("Status bar:\nBuffering");
                        bufferFlag = true;
                        rebuffer++;
                        continue;
                    } else {
                        videoPic = (IVideoPicture)buffer.videoList.get(video++);
                    }
                }
                if(videoPic != null) {
                    if(bufferFlag) {
                        bufferFlag = false;
                        startTime = System.currentTimeMillis();
                        videoStartTime = startTime;
                        keyframeTimestamp = videoPic.getTimeStamp();
                        videoScreen.setImage(Utils.videoPictureToImage(videoPic));
                        videoPic.delete();
                    } else {
                        t.sleep(timeToPlay(videoPic.getTimeStamp()));
                        videoScreen.setImage(Utils.videoPictureToImage(videoPic));
                        videoPic.delete();
                        mainFrame.validate();
                        mainFrame.repaint();
                        if(buffer.videoList.indexOf(null) == buffer.videoList.size()-1 && buffer.videoList.indexOf(null) ==  video) {
                            client.exitFlag = true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        closeJavaSound();
        client.exitFlag = true;
        client.statusLabel.setText("Status bar:\nFinished playing video");
        client.statusLabel.append("\nBuffer time: " + client.bufferTime);
        System.out.println("Buffer time: " + client.bufferTime);
        client.statusLabel.append("\nNo of rebuffer events: " +  rebuffer);
        System.out.println("No of rebuffer events: " +  rebuffer);
        System.out.println("Thread " +  threadName + " exiting.");
    }
    
    public void start() {
        System.out.println("Starting " +  threadName );
        if (t == null) {
            t = new Thread (this, threadName);
            t.start();
        }
    }
      
    class audioPlayer implements Runnable {
        private List audioList;
        private int lastPacket = 0;
        private Buffer buffer;
        private Client client;
        private Player player;
        private Thread t;
        private String threadName;
        private int audio = 0;
        
        public audioPlayer(Buffer b, Client c, Player p){ 
            this.threadName = "Audio Player thread";
            this.buffer = b;
            this.client = c;
            this.player = p;
        }

        @Override
        public void run() {
            try {
                for(;;) {
                    if(client.exitFlag)
                        break;
                    if(player.bufferFlag) {
                        t.sleep(3000);
                    }
                    IAudioSamples ia = null;
                    synchronized(buffer.audioList) {
                        if(buffer.audioList.indexOf(null) <= audio) {
                            client.statusLabel.setText("Status bar:\nAudio Buffering");
                            player.bufferFlag = true;
                            player.rebuffer++;
                            continue;
                        } else {
                            ia = (IAudioSamples)buffer.audioList.get(audio++);
                        }
                    }
                    if(ia != null) {
                        if(bufferFlag) {
                            byte[] rawBytes = ia.getData().getByteArray(0, ia.getSize());
                            player.mLine.write(rawBytes, 0, ia.getSize());
                        } else {
                            //t.sleep(timeToPlay(ia.getTimeStamp()));
                            //play audio
                            byte[] rawBytes = ia.getData().getByteArray(0, ia.getSize());
                            player.mLine.write(rawBytes, 0, ia.getSize());
                            ia.delete();
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.out.println("Thread " +  threadName + " exiting.");
        }
        public void start() {
            System.out.println("Starting " +  threadName );
            if (t == null) {
                t = new Thread (this, threadName);
                t.start();
            }
        }

    }
}