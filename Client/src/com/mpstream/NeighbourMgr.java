package com.mpstream;

import com.mpstream.datastructures.configPacket;
import com.mpstream.datastructures.Buffer;
import com.mpstream.datastructures.audioPacket;
import com.mpstream.datastructures.videoPacket;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IVideoPicture;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NeighbourMgr implements Runnable {
    class neighbourHandler implements Runnable {
        private Thread t;
        private String threadName;
        Buffer buffer;
        final Client client;
        neighbourHandler(Buffer b, Client c){
            this.threadName = "Neighbour handler thread";
            this.buffer = b;
            this.client = c;
        }
        @Override
        public void run() {
            try {
                System.out.println("Waiting for neighbours on port " + server.getLocalPort() + "...");
                for(;;) {
                    if(client.exitFlag)
                        break;
                    Socket clientSocket = null;
                    clientSocket = server.accept(); //initialize and close this socket from neighbourMgr
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    frameReceiver f = new frameReceiver(in,buffer,client);
                    f.start();
                }
            } catch(Exception e) {
                e.printStackTrace();                
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
    class frameReceiver implements Runnable {
        private Thread t;
        private String threadName;
        private ObjectInputStream input;
        Buffer buffer;
        final Client client;
        frameReceiver(ObjectInputStream in,Buffer b, Client c){
            this.input = in;
            this.buffer = b;
            this.threadName = "frame receiver thread";
            this.client = c;
        }
        @Override
        public void run() {
            for(;;) {
                if(client.exitFlag)
                    break;
                try {
                    Object obj = input.readObject();
                    if(obj instanceof videoPacket) {
                        videoPacket vp = (videoPacket)obj;
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
                        //videoPic.delete();
                        vp = null;
                    } else if(obj instanceof audioPacket) {
                        audioPacket ap = (audioPacket)obj;
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
                    }
                } catch(IOException ex) {
                    ex.printStackTrace();
                    break;
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
            try {
                input.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                //Logger.getLogger(NeighbourMgr.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private Thread t;
    private String threadName;
    private int tid;
    Buffer buffer;
    final Client client;
    public static ServerSocket server;
    public NeighbourMgr(int tid,Buffer b,Client c){
        this.tid = tid;
        this.buffer = b;
        this.threadName = "request broadcaster thread";
        this.client = c;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public void run() {
        int i = 0;
        int DEFAULT_PORT = 2002;
        DatagramSocket socket = null;
        DatagramPacket packet;
        InetAddress group = null;
        try
        {
            
            server = new ServerSocket(2001,50);
            server.setSoTimeout(10000000);
            neighbourHandler nh = new neighbourHandler(buffer,client);
            nh.start();
            socket = new DatagramSocket();
            group = InetAddress.getByName("127.255.255.255");
            packet = new DatagramPacket(new byte[1], 1, group, DEFAULT_PORT);
            configPacket cp = new configPacket(configPacket.CMD.NREQ);
            cp.setTransactionId(tid);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(;;) {
                if(client.exitFlag)
                    break;
                baos.reset();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.reset();
                oos.writeObject(cp);
                oos.flush();
                byte[] buf = baos.toByteArray();
                packet = new DatagramPacket(new byte[1], 1, group, DEFAULT_PORT);  
                packet.setData(buf);
                packet.setLength(buf.length);
                socket.send(packet);
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements())
                {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                    Iterator<InterfaceAddress> it = interfaceAddresses.iterator();
                    while(it.hasNext()) {
                        InterfaceAddress ia = it.next();
                        InetAddress broadcastAddr = ia.getBroadcast();
                        if(broadcastAddr != null) {
                            System.out.println(ia.getAddress().getHostAddress());
                            packet = new DatagramPacket(new byte[1], 1, broadcastAddr, DEFAULT_PORT);
                            packet.setData(buf);
                            packet.setLength(buf.length);
                            socket.send(packet);
                        }
                    }
                }        
                buf = null;
                Thread.sleep(3000);
            }
            socket.close();
            server.close();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            System.out.println("Problem creating socket on port: " + DEFAULT_PORT );
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



