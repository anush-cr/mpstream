package com.mpstream;

import com.mpstream.datastructures.ImageComponent;
import com.mpstream.datastructures.*;
import com.mpstream.datastructures.configPacket;
import com.xuggle.xuggler.Utils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;


public class Client {
    public JFrame mainFrame;
    public JTextArea statusLabel;
    private JPanel controlPanel;
    private JPanel videoPanel;
    private JButton backButton;
    private JLabel videoName;
    public ImageComponent videoScreen;
    private BufferedImage image;
    private Client client;
    public volatile boolean exitFlag; //shared by all threads
    
    private class VideoClickListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton btn = (JButton)e.getSource();
            int id = Integer.parseInt((String)btn.getClientProperty("id"));
            if(id==-1){
                exitFlag = true;
                mainFrame.add(controlPanel,BorderLayout.NORTH);
                mainFrame.remove(videoPanel);
            }
            else {
                String name = (String)btn.getClientProperty("name");
                int asize = Integer.parseInt((String)btn.getClientProperty("asize"));
                int vsize = Integer.parseInt((String)btn.getClientProperty("vsize"));
                videoName.setText("Playing: "+name);
                mainFrame.remove(controlPanel);
                mainFrame.add(videoPanel,BorderLayout.NORTH);
                try {
                    configPacket cp = new configPacket(configPacket.CMD.REQ, id);
                    String serverName = "192.168.0.104";
                    int port = 2000;
                    System.out.println("Connecting to " + serverName +
                            " on port " + port);
                    Socket server = new Socket(serverName, port);
                    System.out.println("Just connected to " 
                            + server.getRemoteSocketAddress());
                    ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
                    out.writeObject(cp);
                    ObjectInputStream in = new ObjectInputStream(server.getInputStream());
                    try {
                        Object obj = in.readObject();
                        if(obj instanceof configPacket) {
                            configPacket resPacket = (configPacket)obj;
                            if(resPacket.cmd == configPacket.CMD.META) {
                                exitFlag = false;
                                //TODO: change parameteres to asize and vsize
                                Buffer buffer = new Buffer(resPacket.getAsize()+1,resPacket.getVsize()+1);
                                //Buffer buffer = new Buffer(8000,5000);
                                //TODO: initialize player
                                Player Pl = new Player(buffer,client);
                                Pl.start();
                                PlanManager P = new PlanManager(server, in, mainFrame, videoScreen,buffer,client);
                                P.start();
                                NeighbourMgr N = new NeighbourMgr (resPacket.getTransactionId(),buffer,client);
                                N.start();
                            }
                        }
                    } catch (ClassNotFoundException ex) {
                        ex.printStackTrace();
                    }
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
            mainFrame.validate();
            mainFrame.repaint();
        }
        
    }
    
    public Client(){
        prepareGUI();
    }
    
    public static void main(String[] args) {
        Client c = new Client();
        c.execute();
    }
    
    private void execute() {
        client = this;
        Component[] components = videoPanel.getComponents();
        //System.out.println(controlPanel.getTopLevelAncestor().toString());
        for (int i = 0; i < components.length; i++) {
            //System.out.println(components[i].toString());
            //Rectangle bounds = components[i].getBounds();
        }
    }
    
    private void prepareGUI() {
        mainFrame = new JFrame("Client - Multipath Streaming");
        mainFrame.setSize(640,450);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.addWindowListener(new WindowAdapter() {
           @Override
           public void windowClosing(WindowEvent windowEvent){
              System.exit(0);
           }        
        });
        
        controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(640, 380));
        controlPanel.setLayout(new FlowLayout(FlowLayout.TRAILING,0,0));
        JLabel lbl = new JLabel(" Video List");
        lbl.setOpaque(true);
        lbl.setPreferredSize(new Dimension(640, 30));
        controlPanel.add(lbl);
        JSONParser parser = new JSONParser();
        int i = 0;
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader("data/video_list.json"));
            for (Object o : a){
                JSONObject file = (JSONObject) o;
                String id = (String) file.get("id");
                String name = (String) file.get("name");
                String loc = (String) file.get("loc");
                String vsize = (String) file.get("vsize");
                String asize = (String) file.get("asize");
                String len = (String) file.get("len");
                String vname = "  "+(i+1)+". "+name+" - "+len;
                JButton btn = new JButton("Play");
                btn.putClientProperty("id", id);
                btn.putClientProperty("name", name);
                btn.putClientProperty("asize", asize);
                btn.putClientProperty("vsize", vsize);
                btn.addActionListener(new VideoClickListener());
                btn.setBackground(Color.LIGHT_GRAY);
                btn.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                lbl = new JLabel(vname);
                lbl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
                lbl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
                lbl.setOpaque(true);
                for(int j=0;j<2;j++) {
                    if(j == 0){
                        lbl.setPreferredSize(new Dimension(540, 50));
                        controlPanel.add(lbl);
                    } else{
                        btn.setPreferredSize(new Dimension(100, 50));
                        controlPanel.add(btn);
                    }   
                } 
                i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }       
        videoPanel = new JPanel();
        videoPanel.setPreferredSize(new Dimension(640, 380));
        videoPanel.setLayout(new BorderLayout());
        backButton = new JButton("Back to video list");
        backButton.setPreferredSize(new Dimension(150,20));
        backButton.setBackground(Color.LIGHT_GRAY);
        backButton.setBorder(BorderFactory.createEmptyBorder());
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.putClientProperty("id", "-1");
        backButton.addActionListener(new VideoClickListener());
        videoName = new JLabel("Video Name");
        videoName.setPreferredSize(new Dimension(490,20));
        videoName.setBackground(Color.LIGHT_GRAY);
        videoName.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        videoScreen = new ImageComponent(640,360);
        videoScreen.setPreferredSize(new Dimension(640, 360));
        videoScreen.setLayout(new BorderLayout());
        videoScreen.setImage(image);
        videoPanel.add(backButton,BorderLayout.LINE_START);
        videoPanel.add(videoName,BorderLayout.LINE_END);
        videoPanel.add(videoScreen, BorderLayout.SOUTH);
        statusLabel = new JTextArea("");
        statusLabel.setPreferredSize(new Dimension(640, 70));
        statusLabel.setOpaque(true);
        statusLabel.setLineWrap(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusLabel.setBackground(Color.BLACK);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setText("Status bar:\nDisplaying Video list");
        mainFrame.add(controlPanel,BorderLayout.NORTH);
        mainFrame.add(statusLabel,BorderLayout.SOUTH);
        mainFrame.setVisible(true);
    }    
}