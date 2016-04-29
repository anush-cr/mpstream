/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mpstream.datastructures;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IVideoPicture;
/**
 *
 * @author anush_cr
 */
public class Buffer {
    public final List videoList, audioList;
    public Buffer(int asize, int vsize) {
        IVideoPicture[] videoArray = new IVideoPicture[vsize];
        Arrays.fill(videoArray, null);
        IAudioSamples[] audioArray = new IAudioSamples[asize];
        Arrays.fill(audioArray, null);
        this.videoList = Collections.synchronizedList(Arrays.asList(videoArray));
        this.audioList = Collections.synchronizedList(Arrays.asList(audioArray));
        videoArray = null;
        audioArray = null;
    }
}

