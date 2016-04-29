package neighbour;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import javax.swing.Box;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.net.URLDecoder;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.ArrayList;
import com.mpstream.datastructures.configPacket;

public class Neighbour {
    private static JFrame mainFrame;
    private static JTextArea statusLabel; //used in proxy class
    private static JPanel controlPanel;
    private static JButton acceptButton;
    private static JButton declineButton;
    private static JLabel state;
    private static JLabel request;
    private static boolean helping = false; //used in proxy class consider making this volatile as well and extend base class
    private static ArrayList declined = new ArrayList();
    
    class Proxy implements Runnable {
        private Thread t;
        private String threadName;
        String address;
        int tid;
        
        Proxy(int id,String addr){
            this.address = addr;
            this.threadName = "Proxy handler thread";
            this.tid = id;
        }
        
        @Override
        public void run() {
            try {
                configPacket cp = new configPacket(configPacket.CMD.HLPR);
                cp.setTransactionId(tid);
                String path = Neighbour.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedPath = URLDecoder.decode((new File(path)).getParentFile().getPath() , "UTF-8");
                decodedPath += "/assets/server.txt"; //UNIX path
                //decodedPath += "\\assets\\server.txt"; //windows path
                statusLabel.setText("Status bar:\n"+"looking up server ip address from "+decodedPath);
                //System.out.println(decodedPath);
                FileReader fr = new FileReader(decodedPath);
                BufferedReader br = new BufferedReader(fr);
                String serverName = br.readLine();
                //String serverName = "192.168.0.108";
                String clientName = address;
                int sport = 2000, cport = 2001, i = 0;
                System.out.println("Connecting to " + serverName +" on port " + sport);
                System.out.println("Connecting to " + clientName +" on port " + cport);
                Socket server = new Socket(serverName, sport);
                Socket client = new Socket(clientName, cport);
                System.out.println("Just connected to " + server.getRemoteSocketAddress());
                System.out.println("Just connected to " + client.getRemoteSocketAddress());
                ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
                out.writeObject(cp);
                ObjectOutputStream out2 = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(server.getInputStream());
                for(;;) {
                    i++;
                    try {
                        if(i%20 == 0)
                            out2.reset();
                        Object obj = in.readObject();
                        if(obj != null){
                            //System.out.println(obj.toString());
                            out2.writeObject(obj);
                        }

                    } catch (ClassNotFoundException ex) {
                        ex.printStackTrace();
                        break;
                    }
                }
            } catch(IOException ex) {
                ex.printStackTrace();
                statusLabel.setText(ex.toString());
            }
            helping = false;
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
    
    private class ClickListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JButton btn = (JButton)e.getSource();
            if (btn.getText() == "Accept") {
                int tid = (Integer)btn.getClientProperty("tid");
                String addr = (String)btn.getClientProperty("addr");
                helping = true;
                Proxy p = new Proxy(tid,addr);
                p.start();
            } else if (btn.getText() == "Decline") {
                int tid = (Integer)btn.getClientProperty("tid");
                declined.add(tid);
                controlPanel.setVisible(false);                                
            }
        }
    }
    
    public Neighbour() {
        prepareGUI();
    }
    
    private void prepareGUI() {
        mainFrame = new JFrame("Neighbour - Multipath Streaming");
        mainFrame.setSize(450,250);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.addWindowListener(new WindowAdapter() {
           @Override
           public void windowClosing(WindowEvent windowEvent){
              System.exit(0);
           }        
        });
        controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(450, 150));
        controlPanel.setBackground(Color.white);
        controlPanel.setLayout(new FlowLayout(FlowLayout.TRAILING,0,0));
        state = new JLabel(" Neighbour state: Idle");
        state.setOpaque(true);
        state.setPreferredSize(new Dimension(450, 30));
        state.setBackground(Color.LIGHT_GRAY);
        request = new JLabel("No requests");
        request.setOpaque(true);
        request.setPreferredSize(new Dimension(450, 30));
        request.setHorizontalAlignment(SwingConstants.CENTER);
        controlPanel.add(request);
        controlPanel.add(Box.createRigidArea(new Dimension(450,15))); 
        acceptButton = new JButton("Accept");
        acceptButton.setBackground(Color.LIGHT_GRAY);
        acceptButton.setPreferredSize(new Dimension(190, 45));
        acceptButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        acceptButton.addActionListener(new ClickListener());
        controlPanel.add(acceptButton);
        controlPanel.add(Box.createRigidArea(new Dimension(20,0)));
        declineButton = new JButton("Decline");
        declineButton.setBackground(Color.LIGHT_GRAY);
        declineButton.setPreferredSize(new Dimension(190, 45));
        declineButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        declineButton.addActionListener(new ClickListener());
        controlPanel.add(declineButton);
        controlPanel.add(Box.createRigidArea(new Dimension(25,0)));
        statusLabel = new JTextArea("");
        statusLabel.setPreferredSize(new Dimension(450, 100));
        statusLabel.setOpaque(true);
        statusLabel.setLineWrap(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusLabel.setBackground(Color.BLACK);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setText("Status bar:\n");
        controlPanel.setVisible(false);
        mainFrame.add(state,BorderLayout.NORTH);
        mainFrame.add(controlPanel,BorderLayout.LINE_END);
        mainFrame.add(statusLabel,BorderLayout.SOUTH);
        mainFrame.setVisible(true);
    }
    
    public static void main(String[] args) {
        Neighbour n = new Neighbour();
        int port = 2002;
        InetAddress host;
        MulticastSocket socket;
        DatagramPacket packet;
        for(;;) {
            try {
                if(helping){
                    state.setText(" Neighbour state: Helping Client");
                    controlPanel.setVisible(false);
                    Thread.sleep(2000);
                } else {
                    state.setText(" Neighbour state: Idle");
                    socket = new MulticastSocket(port);
                    byte[] buf = new byte[3000];
                    packet=new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    System.out.println("Broadcast packet address: "+packet.getAddress().toString());
                    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    configPacket cp = (configPacket) ois.readObject();
                    String clientAddr = (packet.getAddress().toString()).substring(1);
                    socket.close();
                    int tid = cp.getTransactionId();
                    if(!declined.contains(tid)) {
                        request.setText("Client requesting help for transaction id: "+tid);
                        acceptButton.putClientProperty("tid", tid);
                        acceptButton.putClientProperty("addr", clientAddr);
                        declineButton.putClientProperty("tid", tid);
                        controlPanel.setVisible(true);
                    }
                    ois.close();
                    bais.close();
                    cp = null;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
}