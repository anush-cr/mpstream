/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mpstream.datastructures;
import java.io.Serializable;
/**
 *
 * @author anush_cr
 */
public class configPacket implements Serializable{
    public final configPacket.CMD cmd;
    private int videoId = -1;
    public int[] neighbours;
    private int TransactionId = -1,Asize=-1,Vsize=-1;
    public enum CMD {
        REQ,DONE,RSND,STOP,HLPR,META,ERR,NREQ
    }
    public configPacket(configPacket.CMD c) {
        this.cmd = c;
    }
    public configPacket(configPacket.CMD c, int vid) {
        this.cmd = c;
        this.videoId = vid;
    }
    public void setTransactionId(int t) {
        this.TransactionId = t;
    }
    public int getTransactionId() {
        return this.TransactionId;
    }
    public void setVideoId(int t) {
        this.videoId = t;
    }
    public int getVideoId() {
        return this.videoId;
    }
    public void setAsize(int a) {
        this.Asize = a;
    }
    public int getAsize() {
        return this.Asize;
    }
    public void setVsize(int v) {
        this.Vsize = v;
    }
    public int getVsize() {
        return this.Vsize;
    }
}
