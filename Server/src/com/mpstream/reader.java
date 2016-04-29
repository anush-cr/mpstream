package com.mpstream;

import com.mpstream.datastructures.audioPacket;
import com.mpstream.datastructures.videoPacket;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class reader {
    String file;
    private IContainer container;
    IStream stream;
    IStreamCoder coder;
    int numStreams;
    int videoStreamId = -1;
    int audioStreamId = -1;
    IStreamCoder videoCoder = null;
    IStreamCoder audioCoder = null;
    //String filename = "clip7.flv";
    int v=0,a=0,v2=0;
    @SuppressWarnings("deprecation")
    public reader(String f) {   
        this.file = f;
        // Create a Xuggler container object and open it
        container = IContainer.make();
        if (container.open(file, IContainer.Type.READ, null) < 0)
            throw new IllegalArgumentException("could not open file: " + this.file);
        // query how many streams the call to open found
        numStreams = container.getNumStreams();
        // and iterate through the streams to find the first audio stream
        for(int i = 0; i < numStreams; i++) {
            // Find the stream object
            stream = container.getStream(i);
            // Get the pre-configured decoder that can decode this stream;
            coder = stream.getStreamCoder();
            if (videoStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
            } else if (audioStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamId = i;
                audioCoder = coder;
            }
        }
        if (videoStreamId == -1 && audioStreamId == -1)
            throw new RuntimeException("could not find audio or video stream in container: "+ file);
        //Check if we have a video stream in this file.  If so let's open up our decoder so it can do work.
        if (videoCoder != null) {
            if(videoCoder.open() < 0)
                throw new RuntimeException("could not open audio decoder for container: "+ file);
        }
        if (audioCoder != null) {
            if (audioCoder.open() < 0)
                throw new RuntimeException("could not open audio decoder for container: "+ file);
        }
    }
    
    public Object readPacket() {
        //Now, we start walking through the container and get each packet.
        IPacket packet = IPacket.make();
        if(container.readNextPacket(packet) >= 0) {
            //Now we have a packet, let's see if it belongs to our video stream
            if (packet.getStreamIndex() == videoStreamId) {
                if(v2 == 0 || v2%3 == 0) {
                    //We allocate a new picture to get the data out of Xuggler
                    IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                    //Now, we decode the video, checking for any errors.
                    int bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);
                    if (bytesDecoded < 0)
                        throw new RuntimeException("got error decoding audio in: " + file);
                    //check if picture packet from decoder is complete
                    if (picture.isComplete()) {
                        videoPacket vp = null;
                        try {
                            vp = new videoPacket(v++,picture);
                            System.out.println("Read video frame "+v);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        packet.delete();
                        picture.delete();
                        return vp;
                    }
                } else {
                    //return null;
                }
            } else if (packet.getStreamIndex() == audioStreamId) {
                //We allocate a set of samples with the same number of channels as the coder tells us is in this buffer.
                //We also pass in a buffer size
                IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
                /*A packet can actually contain multiple frames of audio.  So, we may need to decode audio multiple
                 * times at different offsets in the packet's data.
                 */
                int offset = 0;
                // Keep going until we've processed all data
                while(offset < packet.getSize()) {
                    int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
                    if (bytesDecoded < 0)
                        throw new RuntimeException("got error decoding audio in: " + file);
                    offset += bytesDecoded;
                    //check fro complete set of samples
                    if (samples.isComplete()) {
                        audioPacket ap = new audioPacket(a++,samples);
                        packet.delete();
                        samples.delete();
                        return ap;
                    }
                }
            } else {
                //This packet isn't part of our video stream, so we just silently drop it.
                do {} while(false);
                packet.delete();
            }
        } else {
            return -1;
        }
        packet.delete();
        return null;
    }
    
    public void close() {
        //clean up 
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (audioCoder != null) {
            audioCoder.close();
            audioCoder = null;
        }
        if (container !=null) {
            container.close();
            container = null;
        }
    }
}
