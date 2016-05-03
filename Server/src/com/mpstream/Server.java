package com.mpstream;

import java.net.*;
import java.io.*;
import com.mpstream.datastructures.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

class Metadata {
    public final ArrayList<transaction> transactions = new ArrayList<transaction>();
    
    static final private Metadata INSTANCE = new Metadata();
    static public Metadata getInstance() { return INSTANCE; }
}

class PlanHandler implements Runnable {
    private Thread t;
    private String threadName;
    int tid;
    transaction tn;
    ObjectInputStream input;
    PlanHandler(int id){
        this.threadName = "Plan handler thread";
        tid = id;
        tn = (transaction)Metadata.getInstance().transactions.get(tid);
        input = (ObjectInputStream)tn.in;
    }
    @Override
    public void run() {
        for(;;) {
            if(tn.err)
                break;
            try {
                Object obj = input.readObject();
                if(obj instanceof configPacket) {
                    configPacket cp = (configPacket)obj;
                    if(cp.cmd == configPacket.CMD.STOP)
                        break;
                }
            } catch(IOException ex) {
                Logger.getLogger(PlanHandler.class.getName()).log(Level.SEVERE, null, ex);
                break;
            } catch(ClassNotFoundException ex) {
                Logger.getLogger(PlanHandler.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
        }
        tn.err = true;
        try {
            tn.in.close();
        } catch (IOException ex) {
            Logger.getLogger(PlanHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        Iterator iter = tn.devices.iterator();
        while(iter.hasNext()) {
            Object device = iter.next();
            ObjectOutputStream out = (ObjectOutputStream)device;
            try {
                out.close();
                tn.devices.remove(device);
            } catch (IOException ex) {
                tn.devices.remove(device);
                Logger.getLogger(PlanHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Thread " +  threadName + " exiting.");
    }
    public void start() {
        System.out.println("Starting " +  threadName );
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }
}

class frameDistributor implements Runnable {
    private Thread t;
    private String threadName;
    int tid;
    String videoLoc;
    transaction tn;
    reader r;
    ArrayList neighbours;
    ObjectOutputStream output;
    ObjectOutputStream out;
    frameDistributor(int id,String loc){
        this.threadName = "frame distributor thread";
        tid = id;
        videoLoc = loc;  
        r = new reader(videoLoc);
        this.tn = (transaction)Metadata.getInstance().transactions.get(tid);
        output = (ObjectOutputStream)tn.out;
    }
    @Override
    public void run() {
            int i = 0;
        for(;;) {
            if(tn.err)
                break;
            if(i++%100 == 0){
                Iterator iter = tn.devices.iterator();
                while(iter.hasNext()) {
                    Object device = iter.next();
                    ObjectOutputStream out = (ObjectOutputStream)device;
                    try {
                        out.reset();
                    } catch (IOException ex) {
                        tn.devices.remove(device);
                        Logger.getLogger(frameDistributor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if(tn.devices.size() == 0)
                break;
            Iterator iter = tn.devices.iterator();
            while(iter.hasNext()) {
                Object device = iter.next();
                Object obj = r.readPacket();
                while(obj == null){
                    obj = r.readPacket();
                }
                if(obj != null) {
                    if(obj instanceof Integer && (int)obj == -1) {
                        obj = null;
                        configPacket cp = new configPacket(configPacket.CMD.DONE);
                        try {
                            output.writeObject(cp);
                            cp = null;
                        } catch(IOException ex) {
                            Logger.getLogger(frameDistributor.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                        break;
                    }
                    else if(obj instanceof videoPacket || obj instanceof audioPacket) {
                        try {
                            try {
                                ObjectOutputStream out = (ObjectOutputStream)device;
                                out.writeObject(obj);
                            } catch(SocketException ex) {
                                tn.devices.remove(device);
                                Logger.getLogger(frameDistributor.class.getName()).log(Level.SEVERE, null, ex);
                            }                                
                            obj = null;
                        } catch(Exception ex) {
                            Logger.getLogger(frameDistributor.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        try {
            output.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        tn.err = true;
        System.out.println("Thread " +  threadName + " exiting.");
    }
    public void start() {
        System.out.println("Starting " +  threadName );
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }
}

public class Server {
    public static void main(String[] args) {
        try {
            InetAddress addr = InetAddress.getByName("127.0.0.1");
            ServerSocket s = new ServerSocket(2000,50);
            s.setSoTimeout(10000000);
            System.out.println("Waiting for client on port " + s.getLocalPort() + "...");
            for(;;) {
                Socket clientSocket = null;
                clientSocket = s.accept();
                System.out.println(clientSocket.toString());
                System.out.println("Connected to :" + clientSocket.getLocalPort() + " at "+clientSocket.getLocalAddress());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                Object obj = in.readObject();
                if(obj instanceof configPacket) {
                    configPacket cp = (configPacket)obj;
                    if(cp.cmd == configPacket.CMD.REQ) {
                        int vid = cp.getVideoId();
                        String name = "",loc="";
                        int asize=0,vsize=0;
                        boolean found = false;
                        JSONParser parser = new JSONParser();
                        try {
                            JSONArray a = (JSONArray) parser.parse(new FileReader("data/video_list.json"));
                            for (Object o : a){
                                JSONObject file = (JSONObject) o;
                                int vid2 = Integer.parseInt((String) file.get("id"));
                                name = (String) file.get("name");
                                loc = (String) file.get("loc");
                                vsize = Integer.parseInt((String) file.get("vsize"));
                                asize = Integer.parseInt((String) file.get("asize"));
                                if(vid == vid2) {
                                    found = true;
                                    break;
                                }
                            }
                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ParseException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        if(!found) {
                            //return error to client
                            configPacket resPacket = new configPacket(configPacket.CMD.ERR);
                            out.writeObject(resPacket);
                        } else {
                            System.out.println("Client requested Video: "+name); 
                            transaction t = new transaction(in,out);
                            Metadata.getInstance().transactions.add(t);
                            int tid = Metadata.getInstance().transactions.size()-1;
                            configPacket resPacket = new configPacket(configPacket.CMD.META);
                            resPacket.setTransactionId(tid);
                            resPacket.setAsize(asize);
                            resPacket.setVsize(vsize);
                            out.writeObject(resPacket);
                            t.devices.add(out);
                            PlanHandler P = new PlanHandler(tid);
                            P.start();
                            frameDistributor F = new frameDistributor(tid,loc);
                            F.start();
                        }
                    } else if(cp.cmd == configPacket.CMD.HLPR) {
                        int tid = cp.getTransactionId();
                        transaction tn = (transaction)Metadata.getInstance().transactions.get(tid);
                        tn.devices.add(out);
                    }
                }
            }
        } catch(Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}

