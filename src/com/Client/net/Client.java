/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.Client.net;

import com.net.FileMessage;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import com.net.Message;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import javax.swing.JFileChooser;

/**
 *
 * @author Александр
 */
public class Client extends javax.swing.JFrame {

    private ChatAccess chat;
    private String name;
    private Vector clients = new Vector();
    private Map<String, Integer> pane = new HashMap<String, Integer>();
    private Map<String, OutputStream> streams = new HashMap<String, OutputStream>();
    private final Timer stopper;
    private boolean init = false;
    private int x = 0;
    private int y = 0;
    private ImageIcon icon = new ImageIcon("images/message.png");
    private ImageIcon send = new ImageIcon("images/send.png");
    private ImageIcon send_d = new ImageIcon("images/send_d.png");
    private ImageIcon send_g = new ImageIcon("images/send_g.png");
    private ImageIcon add = new ImageIcon("images/add.png");
    private ImageIcon add_d = new ImageIcon("images/add_d.png");
    private ImageIcon add_g = new ImageIcon("images/add_g.png");
    private ImageIcon sendfile = new ImageIcon("images/sendfile.png");
    private ImageIcon sendfile_d = new ImageIcon("images/sendfile_d.png");
    private ImageIcon sendfile_g = new ImageIcon("images/sendfile_g.png");
    private final WaitLayerUI layerUI = new WaitLayerUI();
    private Image background = Toolkit.getDefaultToolkit().createImage("images/background.jpg");
    private Image sendBackground = Toolkit.getDefaultToolkit().createImage("images/sendBackground.jpg");
    private Image root = Toolkit.getDefaultToolkit().createImage("images/chat.png");
    private TapTapTap Tap;
    
    public class ChatAccess {
        private Socket socket;
        private OutputStream out;
        private InputStream in;
        private Socket fsocket;
        private OutputStream fout;
        private InputStream fin;
        private byte[] buffer = new byte[4096];
        private byte[] fbuffer = new byte[10240];
        
        public ChatAccess(String server, int port, int fport) {
            try {
                socket = new Socket(server, port);
                out = socket.getOutputStream();
                in = socket.getInputStream();
                
                fsocket = new Socket(server, fport);
                fout = fsocket.getOutputStream();
                fin = fsocket.getInputStream();

                Thread receivingThread = new Thread() {

                    @Override
                    public void run() {
                        while (!socket.isClosed()) {
                            try {
                                in.read(buffer);
                            } catch (IOException ex) {
                                if ("Socket closed".equals(ex.getMessage())) {
                                    break;
                                }
                                JOptionPane.showMessageDialog(rootPane, "Connection lost!", "Chat", JOptionPane.ERROR_MESSAGE);
                                System.out.println("Connection lost!");
                                close();
                                break;
                            }
                            try {
                                Message msg = (Message) deserialize(buffer);
                                decode(msg);
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(rootPane, "Connection lost!", "Chat", JOptionPane.ERROR_MESSAGE);
                                System.out.println("Connection lost!");
                                close();
                            } catch (ClassNotFoundException ex) {
                                JOptionPane.showMessageDialog(rootPane, "Connection lost!", "Chat", JOptionPane.ERROR_MESSAGE);
                                System.out.println("Connection lost!");
                                close();
                            }
                        }
                    }
                };
                receivingThread.start();
                
                Thread freceiving = new Thread() {
                    @Override
                    public void run() {
                        while (!fsocket.isClosed()) {
                            try {
                                fin.read(fbuffer);
                                receive(fbuffer);
                            } catch (IOException ex) {
                                if ("Socket closed".equals(ex.getMessage())) {
                                    break;
                                }
                                JOptionPane.showMessageDialog(rootPane, "Connection lost!", "Receiving file", JOptionPane.ERROR_MESSAGE);
                                System.out.println("Can't receive file!");
                                fclose();
                                break;
                            }
                        }
                    }
                };
                freceiving.start();
                
            } catch (UnknownHostException ex) {
                JOptionPane.showMessageDialog(null, "Unnable to connect!", "Chat", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Unnable to connect!", "Chat", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }

        private byte[] serialize(Object obj) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(obj);
            return out.toByteArray();
        }

        private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();
        }

        public void send(Message m) {
            try {
                byte[] b = serialize(m);
                out.write(b);
                out.flush();
            } catch (IOException ex) {
                close();
            }
        }
        
        public void send(FileMessage m) {
            try {
                byte[] b = serialize(m);
                ByteBuffer byteB = ByteBuffer.allocate(2*1024);
                if (m.type == 2) {
                    byteB.putInt(b.length);
                }
                byteB.put(b);
                send(byteB.array());
            } catch (IOException ex) {
                fclose();
            }
        }

        public void send(byte[] b) {
            try {
                fout.write(b);
                fout.flush();
            } catch (IOException ex) {
                fclose();
            }
        }

        public void send(File f, Vector v, String from) {
            try {
                String fName = f.toString();
                fName = fName.substring(fName.lastIndexOf("\\") + 1);
                InputStream is = new FileInputStream(f);
                ByteBuffer byteB = ByteBuffer.allocate(10*1024);
                byte[] buf = new byte[1024*8];
                byte[] sendB;
                FileMessage msg;
                long result = 0;
                for ( int count = -1; ( count = is.read(buf) ) != -1; ) {
                    result+=count;
                    byteB.clear();
                    msg = new FileMessage((byte)3 , from, v, fName, result==f.length());
                    sendB = serialize(msg);
                    byteB.putInt(sendB.length);
                    byteB.put(sendB);
                    byteB.putInt(count);
                    byteB.put(buf);
                    send(byteB.array());
                }
                is.close();
            } catch (IOException ex) {
                fclose();
            }
        }
        
        public void receive(byte[] buf) {
            ByteBuffer byteB = ByteBuffer.allocate(10240);
            byteB.put(buf);
            byteB.flip();
            int msgS = byteB.getInt();
            byte[] b = new byte[msgS];
            byteB.get(b);
            FileMessage msg = null;
            try {
                msg = (FileMessage) deserialize(b);
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                return;
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            OutputStream os = null;
            if (streams.get(msg.file) == null) {
                try {
                    os = new FileOutputStream(msg.file);
                    streams.put(msg.file, os);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                os = streams.get(msg.file);
            }
            int part = byteB.getInt();
            byte[] recvB = new byte[part];
            byteB.get(recvB);
            try {
                os.write(recvB);
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (msg.end) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
                streams.remove(msg.file);
                Message m = new Message((byte)3, msg.clients, msg.from, msg.file);
                decode(m);
            }
        }

        private void decode(Message msg) {
            switch (msg.type) {
                case 0: {
                    if (msg.text.equals("Dublicate Name!")) {
                        init = false;
                    } else {
                        System.out.println(msg.type + "  " + msg.clients + "  " + msg.text);
                        clients = msg.clients;
                        init = true;
                    }
                    break;
                }
                case 1: {
                    System.out.println(msg.type + "  " + msg.from + "  " + msg.clients + "  " + msg.text);
                    Vector v = msg.clients;
                    v.remove(name);
                    v.add(msg.from);
                    Collections.sort(v);
                    String s = new String();
                    for (int i=0; i<v.size(); i++) {
                        if (i > 0) {
                            s+=", ";
                        }
                        s+=v.get(i);
                    }
                    Date d = new Date();
                    DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm");
                    String from = msg.from + ",   " + df.format(d) + ":   ";
                    if (jTabbedPane1.getSelectedIndex()!=-1 && pane.get(s)!=null && pane.get(s)==jTabbedPane1.getSelectedIndex()) {
                        ImageTextArea text = (ImageTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(0))).getViewport())).getView();
                        text.setText(text.getText() + from + msg.text + "\n");
                    } else {
                        if (pane.get(s)!=null) {
                            ImageTextArea text = (ImageTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(pane.get(s))).getComponent(0))).getViewport())).getView();
                            text.setText(text.getText() + from + msg.text + "\n");
                            jTabbedPane1.setIconAt(pane.get(s), icon);
                        } else {
                            if (msg.clients.size()==1) {
                                CellRenderer rend = (CellRenderer) jList1.getCellRenderer();
                                rend.setImageIcon(icon, msg.from);
                                String str = msg.from;
                                msg.from = from;
                                rend.addMessage(str, msg);
                                jList1.setCellRenderer(rend);
                                jList1.repaint();
                            } else {
                                if (jTabbedPane1.getTabCount()==0) {
                                    addPane(s, null, from + msg.text + "\n");
                                } else {
                                    addPane(s, icon, from + msg.text + "\n");
                                }
                            }
                        }
                    }
                    playSound("message.wav");
                    break;
                }
                    
                case 3: {
                    System.out.println(msg.type + "  " + msg.from + "  " + msg.clients + "  " + msg.text);
                    Vector v = msg.clients;
                    v.remove(name);
                    v.add(msg.from);
                    Collections.sort(v);
                    String s = new String();
                    for (int i=0; i<v.size(); i++) {
                        if (i > 0) {
                            s+=", ";
                        }
                        s+=v.get(i);
                    }
                    String txt = msg.text; 
                    Date d = new Date();
                    DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm");
                    String from = msg.from + ",   " + df.format(d) + ":   ";
                    if (jTabbedPane1.getSelectedIndex()!=-1 && pane.get(s)!=null && pane.get(s)==jTabbedPane1.getSelectedIndex()) {
                        ImageTextArea text = (ImageTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(0))).getViewport())).getView();
                        text.setText(text.getText() + from + "Received file " + txt + "...\n");
                    } else {
                        if (pane.get(s)!=null) {
                            ImageTextArea text = (ImageTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(pane.get(s))).getComponent(0))).getViewport())).getView();
                            text.setText(text.getText() + from + "Received file " + txt + "...\n");
                            jTabbedPane1.setIconAt(pane.get(s), icon);
                        } else {
                            if (msg.clients.size()==1) {
                                CellRenderer rend = (CellRenderer) jList1.getCellRenderer();
                                rend.setImageIcon(icon, msg.from);
                                String str = msg.from;
                                msg.from = from;
                                msg.text = "Received file " + txt  + "...";
                                rend.addMessage(str, msg);
                                jList1.setCellRenderer(rend);
                                jList1.repaint();
                            } else {
                                if (jTabbedPane1.getTabCount()==0) {
                                    addPane(s, null, from + "Received file " + txt + "...\n");
                                } else {
                                    addPane(s, icon, from + "Received file " + txt + "...\n");
                                }
                            }
                        }
                    }
                    playSound("information.wav");
                    break;
                }

                case 4: {
                    System.out.println(msg.type + "  " + msg.text);
                    clients.add(msg.text);
                    jList1.setListData(clients);
                    playSound("in.wav");
                    break;
                }
                case 5: {
                    System.out.println(msg.type + "  " + msg.clients + "  " + msg.text);
                    clients.remove(msg.text);
                    for (int i=0; i<jTabbedPane1.getTabCount(); i++) {
                        if (jTabbedPane1.getTitleAt(i).trim().equals(msg.text)) {
                            JTextArea text = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(0))).getViewport())).getView();
                            JTextArea edit = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(1))).getViewport())).getView();
                            text.setText(text.getText() + msg.text + " disconnected\n");
                            edit.setEditable(false);
                        } else {
                            int a = jTabbedPane1.getTitleAt(i).indexOf(", " + msg.text);
                            int b = jTabbedPane1.getTitleAt(i).indexOf(", " + msg.text + ", ");
                            int c = jTabbedPane1.getTitleAt(i).indexOf(msg.text + ", ");
                            if (a!=-1 || b!=-1 || c!=-1) {
                                String str = new String();
                                pane.remove(jTabbedPane1.getTitleAt(i).trim());
                                if (i!=jTabbedPane1.getTabCount()-1) {
                                    Set<Map.Entry<String, Integer>> panes = pane.entrySet();
                                    for (Map.Entry<String, Integer> pan:panes) {
                                        if (pan.getValue()>i) {
                                            pan.setValue(pan.getValue()-1);
                                        }
                                    }
                                }
                                if (b!=-1) {
                                    str = jTabbedPane1.getTitleAt(i).replaceFirst(", " + msg.text + ", ", ", ") + "   ";
                                } else {
                                    if (a!=-1) {
                                        str = jTabbedPane1.getTitleAt(i).trim().replaceFirst(", " + msg.text, "   ");
                                    }
                                }
                                if (c!=-1) {
                                    str = jTabbedPane1.getTitleAt(i).trim().substring((msg.text + ", ").length()) + "   ";
                                }
                                JTextArea text = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(i)).getComponent(0))).getViewport())).getView();
                                text.setText(text.getText() + msg.text + "disconnected\n");
                                jTabbedPane1.setTitleAt(i, str);
                                if (pane.get(str.trim())!=null) {
                                    text.setText(text.getText() + "Such tab already axists! This tab has been disabled!\n");
                                    JTextArea edit = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(i)).getComponent(1))).getViewport())).getView();
                                    edit.setEditable(false);
                                } else {
                                    pane.put(str.trim(), i);
                                }
                            }
                        }
                    }
                    jList1.setListData(clients);
                    playSound("out.wav");
                    break;
                }
            }
        }
        
        public synchronized void close() {
            if (!socket.isClosed()) {
                try {
                    socket.close();
                    if (!fsocket.isClosed()) {
                        fsocket.close();
                    }
                    System.exit(0);
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        public synchronized void fclose() {
            if (!fsocket.isClosed()) {
                try {
                    fsocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Creates new form Client
     */
    
    public Client(String addres, int port, int server) {
        initComponents();
        Rectangle r1 = jButton2.getBounds();
        jButton2.setText(null);
        jButton2.setBounds(r1.x + (r1.width - send.getIconWidth()), r1.y, send.getIconWidth(), send.getIconHeight());
        Rectangle r2 = jButton3.getBounds();
        jButton3.setText(null);
        jButton3.setBounds(r2.x + (r2.width - add.getIconWidth()), r2.y, add.getIconWidth(), add.getIconHeight());
        Rectangle r3 = jButton6.getBounds();
        jButton6.setText(null);
        jButton6.setBounds(r3.x + (r3.width - add.getIconWidth()), r3.y, add.getIconWidth(), add.getIconHeight());
        jList1.setCellRenderer(new CellRenderer());
        this.chat = new ChatAccess(addres, port, server);
        jLayeredPane2.setVisible(false);
        jList2.setCellRenderer(new ListRenderer());
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        jFrame1.setLocationRelativeTo(null);
        jFrame1.setSize(new Dimension(160, 360));
        
        jTabbedPane1.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (jTabbedPane1.getTabCount()==0) return; 
                if (jTabbedPane1.getIconAt(jTabbedPane1.getSelectedIndex())!=null) {
                    jTabbedPane1.setIconAt(jTabbedPane1.getSelectedIndex(), null);
                }
                jTabbedPane1.invalidate();
                if (jTabbedPane1.getSelectedIndex()!=-1) {
                    ((JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(0))).getViewport())).getView()).repaint();
                }
            }
        });
        
        jList2.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int xCell = new JCheckBox().getPreferredSize().width;
                int yCell = new JCheckBox().getPreferredSize().height;
                JList list = (JList) e.getSource();
                int index = list.locationToIndex(e.getPoint());
                if (e.getX()>list.getCellBounds(index, index).x + xCell) {
                    return;
                }
                if (e.getY()>list.getCellBounds(index, index).y + yCell) {
                    return;
                }
                CheckListItem item = (CheckListItem) list.getModel().getElementAt(index);
                item.setSelected(!item.isSelected());
                list.repaint(list.getCellBounds(index, index));
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        
        stopper = new Timer(3000, new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Client.this.setEnabled(true);
                Client.this.show(true);
                if (init) {
                    chat.send(new FileMessage((byte)2, name));
                    Client.this.setVisible(false);
                    playSound("enter.wav");
                    if (clients!=null) {
                        jList1.setListData(clients);
                    }
                    resize(700,500);
                    Client.this.setLocationRelativeTo(null);
                    jLayeredPane1.setVisible(false);
                    jLayeredPane2.setVisible(true);
                    jButton2.setVisible(false);
                    jButton3.setVisible(false);
                    jButton6.setVisible(false);
                    Client.this.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(null, "Such name already exists!", "Chat", JOptionPane.ERROR_MESSAGE);
                    playSound("error.wav");
                    jTextField1.setText("");
                    jTextField1.requestFocus();
                }
            }
        });
        stopper.setRepeats(false);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFrame1 = new javax.swing.JFrame();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jFileChooser1 = new javax.swing.JFileChooser();
        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLayeredPane2 = new javax.swing.JLayeredPane();
        jButton6 = new javax.swing.JButton(sendfile)  {{
            setPressedIcon(sendfile_d);
            setRolloverIcon(sendfile_g);
        }};
        jButton2 = new JButton(send) {{
            setPressedIcon(send_d);
            setRolloverIcon(send_g);
        }};
        jButton3 = new JButton(add) {{
            setPressedIcon(add_d);
            setRolloverIcon(add_g);
        }};
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new JList(new DefaultListModel());
        jTabbedPane1 = new ClosableTabbedPane();

        jFrame1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jFrame1.setUndecorated(true);
        jFrame1.setResizable(false);
        jFrame1.setType(java.awt.Window.Type.POPUP);
        jFrame1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jFrame1MousePressed(evt);
            }
        });
        jFrame1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jFrame1MouseDragged(evt);
            }
        });

        jScrollPane3.setViewportView(jList2);

        jButton4.setText("Cancel");
        jButton4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton4MouseClicked(evt);
            }
        });
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setText("OK");
        jButton5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton5MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jFrame1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jFrame1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton5))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jFrame1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 302, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton4)
                    .addComponent(jButton5))
                .addGap(0, 8, Short.MAX_VALUE))
        );

        jFileChooser1.setMinimumSize(new java.awt.Dimension(550, 350));
        jFileChooser1.setPreferredSize(new java.awt.Dimension(600, 400));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(204, 255, 255));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setIconImage(root);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLayeredPane1.setBackground(new java.awt.Color(204, 255, 255));
        jLayeredPane1.setPreferredSize(new java.awt.Dimension(150, 300));

        jLabel1.setFont(new java.awt.Font("SimSun-ExtB", 0, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("CHAT");
        jLabel1.setBounds(50, 10, 50, 19);
        jLayeredPane1.add(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Enter name:");
        jLabel2.setBounds(36, 90, 80, 30);
        jLayeredPane1.add(jLabel2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jTextField1.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                jTextField1CaretUpdate(evt);
            }
        });
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField1KeyPressed(evt);
            }
        });
        jTextField1.setBounds(10, 130, 130, 30);
        jLayeredPane1.add(jTextField1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton1.setText("Enter");
        jButton1.setEnabled(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton1.setBounds(39, 270, 70, 23);
        jLayeredPane1.add(jButton1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLayeredPane2.setBackground(new java.awt.Color(204, 255, 255));

        jButton6.setIcon(sendfile);
        jButton6.setLabel("Send file");
        jButton6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton6MouseClicked(evt);
            }
        });
        jButton6.setBounds(604, 400, 73, 23);
        jLayeredPane2.add(jButton6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton2.setFocusPainted(false);

        jButton2.setContentAreaFilled(false);
        jButton2.setText("Send");
        jButton2.setToolTipText("Send message");
        jButton2.setBorder(null);
        jButton2.setBorderPainted(false);
        jButton2.setMaximumSize(new java.awt.Dimension(10000, 10000));
        jButton2.setMinimumSize(new java.awt.Dimension(1, 1));
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton2MouseClicked(evt);
            }
        });
        jButton2.setBounds(598, 365, 80, 20);
        jLayeredPane2.add(jButton2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton3.setText("Add");
        jButton3.setToolTipText("Add to dialog");
        jButton3.setBorder(null);
        jButton3.setMaximumSize(new java.awt.Dimension(1000000, 1000000));
        jButton3.setMinimumSize(new java.awt.Dimension(1, 1));
        jButton3.setPreferredSize(new java.awt.Dimension(190, 23));
        jButton3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton3MouseClicked(evt);
            }
        });
        jButton3.setBounds(568, 435, 110, 20);
        jLayeredPane2.add(jButton3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(260, 130));

        jList1.setBackground(new java.awt.Color(245, 245, 245));
        jList1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jList1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1.setOpaque(false);
        jList1.setSelectionBackground(new java.awt.Color(204, 204, 255));
        jList1.setSelectionForeground(new java.awt.Color(0, 0, 0));
        jList1.setVerifyInputWhenFocusTarget(false);
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);

        jScrollPane1.setBounds(7, 7, 160, 460);
        jLayeredPane2.add(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(520, 250));
        jTabbedPane1.addContainerListener(new java.awt.event.ContainerAdapter() {
            public void componentAdded(java.awt.event.ContainerEvent evt) {
                jTabbedPane1ComponentAdded(evt);
            }
            public void componentRemoved(java.awt.event.ContainerEvent evt) {
                jTabbedPane1ComponentRemoved(evt);
            }
        });
        jTabbedPane1.setBounds(175, 7, 512, 460);
        jLayeredPane2.add(jTabbedPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLayeredPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLayeredPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed
        // TODO add your handling code here:
        if (evt.getKeyCode()==KeyEvent.VK_ENTER) {
            if (jTextField1.getText().equals("")) {
                JOptionPane.showMessageDialog(rootPane, "Enter Name!", "Chat", JOptionPane.WARNING_MESSAGE);
                jTextField1.requestFocus();
            } else {
                name = jTextField1.getText();
                Rectangle rec = new Rectangle();
                Point loc = this.getLocationOnScreen();
                Dimension size = this.getSize();
                rec.setSize(size);
                rec.setLocation(loc);
                this.setEnabled(false);
                Tap = new TapTapTap(rec);
                initialisation();
                if (!stopper.isRunning()) {
                    stopper.start();
                }
            }
        }
    }//GEN-LAST:event_jTextField1KeyPressed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        if (jButton1.isEnabled()) {
            name = jTextField1.getText();
            initialisation();
            Rectangle rec = new Rectangle();
            Point loc = this.getLocationOnScreen();
            Dimension size = this.getSize();
            rec.setSize(size);
            rec.setLocation(loc);
            this.setEnabled(false);
            Tap = new TapTapTap(rec);
            if (!stopper.isRunning()) {
                stopper.start();
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        Message msg = new Message((byte)5, name);
        chat.send(msg);
    }//GEN-LAST:event_formWindowClosing

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount()==1 || jList1.getSelectedIndex()==-1) return;
        int yCell = new JCheckBox().getPreferredSize().height;
        JList list = (JList) evt.getSource();
        int index = list.locationToIndex(evt.getPoint());
        if (evt.getY()>list.getCellBounds(index, index).y + yCell) {
            return;
        }
        String s = (String) jList1.getSelectedValue();
        if (pane.get(s)==null) {
            addPane(s);
            jTabbedPane1.setSelectedIndex(jTabbedPane1.getTabCount() - 1);
            CellRenderer rend = (CellRenderer) jList1.getCellRenderer();
            if (rend.getIcon(s)!=null) {
                JTextArea text = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(0))).getViewport())).getView();
                rend.removeImageIcon(s);
                while (rend.getMessages(s).size() > 0) {
                    Message m = (Message) rend.getFirstMessage(s);
                    text.setText(text.getText() + m.from + m.text + "\n");
                    rend.removeFirstMessage(s);
                }
            }
            jList1.setCellRenderer(rend);
            jList1.repaint();
            jTabbedPane1.repaint();
        } else {
            jTabbedPane1.setSelectedIndex(pane.get(s));
        }
    }//GEN-LAST:event_jList1MouseClicked

    private void jButton2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseClicked
        // TODO add your handling code here:
        sendM();
    }//GEN-LAST:event_jButton2MouseClicked

    private void jTextField1CaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_jTextField1CaretUpdate
        // TODO add your handling code here:
        boolean bool = false;
        try {
            bool = jTextField1.getText(0, 1).equals(" ");
        } catch (BadLocationException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (jTextField1.getText().equals("") || bool) {
            jButton1.setEnabled(false);
        } else {
            if (jButton1.isEnabled()) {
                return;
            } else {
                jButton1.setEnabled(true);
            }
        }
    }//GEN-LAST:event_jTextField1CaretUpdate

    private void jTabbedPane1ComponentRemoved(java.awt.event.ContainerEvent evt) {//GEN-FIRST:event_jTabbedPane1ComponentRemoved
        // TODO add your handling code here:
        ClosableTabbedPane tab = (ClosableTabbedPane) evt.getSource();
        for (int j=0; j<jTabbedPane1.getTabCount(); j++) {
            if (jTabbedPane1.getTitleAt(j).trim().equals(tab.getRemoved())) {
                return;
            }
        }
        int i = pane.remove(tab.getRemoved());
        Set<Map.Entry<String, Integer>> panes = pane.entrySet();
        for (Map.Entry<String, Integer> pan:panes) {
            if (pan.getValue()>i) {
                pan.setValue(pan.getValue()-1);
            }
        }
        if (jTabbedPane1.getTabCount()==0) {
            jButton2.setVisible(false);
            jButton3.setVisible(false);
            jButton6.setVisible(false);
        }
    }//GEN-LAST:event_jTabbedPane1ComponentRemoved

    private void jTabbedPane1ComponentAdded(java.awt.event.ContainerEvent evt) {//GEN-FIRST:event_jTabbedPane1ComponentAdded
        // TODO add your handling code here:
        if (jTabbedPane1.getTabCount()==1) {
            jButton2.setVisible(true);
            jButton3.setVisible(true);
            jButton6.setVisible(true);
        }
    }//GEN-LAST:event_jTabbedPane1ComponentAdded

    private void jButton3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton3MouseClicked
        // TODO add your handling code here:
        String s = jTabbedPane1.getTitleAt(jTabbedPane1.getSelectedIndex()).trim();
        s = s.replaceAll(", ", " ");
        s+=" ";
        Vector comp = new Vector();
        if (clients.size()==1) {
            JOptionPane.showMessageDialog(rootPane, "Nothing to add!", "Chat", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (int i=0; i<clients.size(); i++) {
            comp.add(new CheckListItem(clients.get(i).toString(), s.indexOf(clients.get(i)+" ")!=-1));
        }
        Collections.sort(comp);
        jList2.setListData(comp);
        jFrame1.setVisible(true);
    }//GEN-LAST:event_jButton3MouseClicked

    private void jFrame1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jFrame1MouseDragged
        // TODO add your handling code here:
        jFrame1.setLocation(evt.getXOnScreen() - x, evt.getYOnScreen() - y);
    }//GEN-LAST:event_jFrame1MouseDragged

    private void jButton4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton4MouseClicked
        // TODO add your handling code here:
        jFrame1.setVisible(false);
    }//GEN-LAST:event_jButton4MouseClicked

    private void jFrame1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jFrame1MousePressed
        // TODO add your handling code here:
        x = evt.getXOnScreen() - jFrame1.getLocation().x;
        y = evt.getYOnScreen() - jFrame1.getLocation().y;
    }//GEN-LAST:event_jFrame1MousePressed

    private void jButton5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton5MouseClicked
        // TODO add your handling code here:
        ListModel list = jList2.getModel();
        String s = new String();
        int select = 0;
        for (int i=0; i<list.getSize(); i++) {
            CheckListItem item = (CheckListItem) list.getElementAt(i);
            if (item.isSelected()) {
                select++;
                s+=item.toString() + ", ";
            }
        }
        if (select == 0) {
            JOptionPane.showMessageDialog(rootPane, "Nothing checked!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        s = s.substring(0, s.length()-2);
        if (pane.get(s)!=null) {
            if (pane.get(s)==jTabbedPane1.getSelectedIndex()) {
                jFrame1.setVisible(false);
                return;
            }
            JOptionPane.showMessageDialog(rootPane, "Such tab already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        pane.remove(jTabbedPane1.getTitleAt(jTabbedPane1.getSelectedIndex()).trim());
        pane.put(s, jTabbedPane1.getSelectedIndex());
        jFrame1.setVisible(false);
        jTabbedPane1.setTitleAt(jTabbedPane1.getSelectedIndex(), s + "   ");
    }//GEN-LAST:event_jButton5MouseClicked

    private void jButton6MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton6MouseClicked
        // TODO add your handling code here:
        sendFile();
    }//GEN-LAST:event_jButton6MouseClicked

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton4ActionPerformed

    private void addPane(String s) {
        Panel p = new Panel();
        FlowLayout man = (FlowLayout) p.getLayout();
        man.setAlignment(FlowLayout.LEFT);
        p.setLayout(man);
        ImageTextArea textL = new ImageTextArea(background);
        final ImageTextArea textA = new ImageTextArea(sendBackground);

        textA.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "key-enter");
        textA.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.CTRL_MASK), "key-ctrl+enter");
        textA.getActionMap().put("key-enter", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                sendM();
            }
        });
        textA.getActionMap().put("key-ctrl+enter", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                textA.setText(textA.getText() + "\n");
            }
        });
        textA.getActionMap().remove("pressed ENTER");
        
        textL.setEditable(false);
        textL.setBackground(new Color(230, 230, 230));
        textL.setForeground(Color.BLUE);
        textA.setBackground(new Color(250, 250, 250));
        final JScrollPane scrollT = new JScrollPane(textL);
        final JScrollPane scrollA = new JScrollPane(textA);
        JViewport viewT = scrollT.getViewport();
        JViewport viewA = scrollA.getViewport();
        
        viewT.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                scrollT.repaint();
            }
        });
        
        viewA.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                scrollA.repaint();
            }
        });
        
        textL.setScrollPane(scrollT);
        textA.setScrollPane(scrollA);
        scrollT.setPreferredSize(new Dimension(500, 320));
        scrollA.setPreferredSize(new Dimension(460, 97));
        p.add(scrollT);
        p.add(scrollA);
        jTabbedPane1.addTab(s, p);
        int i = jTabbedPane1.getTabCount()-1;
        pane.put(s, i);
    }

    private void addPane(String s, ImageIcon ic, String text) {
        addPane(s);
        int i = jTabbedPane1.getTabCount()-1;
        JTextArea textO = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(i)).getComponent(0))).getViewport())).getView();
        jTabbedPane1.setIconAt(i, ic);
        jTabbedPane1.repaint();
        textO.setText(text);
    }
    
    private void sendM() {
        JTextArea c = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(1))).getViewport())).getView();
        JTextArea text = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(0))).getViewport())).getView();
        String st = c.getText();
        for (int a=0; a<st.length(); a++) {
            if (st.substring(a, a + 1).equals("\n")) {
                if (a==st.length()-1) {
                    c.setText("");
                    return;
                }
            }
        }
        if (st.equals("") || jTabbedPane1.getSelectedIndex()==-1) return;
        Message msg;
        String s = jTabbedPane1.getTitleAt(jTabbedPane1.getSelectedIndex()).trim();
        String[] sub = s.split(", ");
        Vector v = new Vector(Arrays.asList(sub));
        Date d = new Date();
        DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm");
        String from = name + ",   " + df.format(d) + ":   ";
        msg = new Message((byte)1, v, name, c.getText());
        chat.send(msg);
        text.setText(text.getText() + from + c.getText() + "\n");
        c.setText("");
    }
    
    private void initialisation() {
        try {
            Message m = new Message((byte)0, name);
            chat.send(m);
        } catch (Exception ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void playSound(String file){
        JPlayer play = new JPlayer(new File("sounds/" + file).getAbsolutePath());
    }
    
    private synchronized void sendFile() {
        int sendF = jFileChooser1.showOpenDialog(this);
        if (jFileChooser1.getSelectedFile() == null || sendF==JFileChooser.CANCEL_OPTION) {
            return;
        }
        File file = jFileChooser1.getSelectedFile();
        String s = jTabbedPane1.getTitleAt(jTabbedPane1.getSelectedIndex()).trim();
        String[] sub = s.split(", ");
        Vector v = new Vector(Arrays.asList(sub));
        Date d = new Date();
        DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm");
        String from = name + ",   " + df.format(d) + ":   ";
        String fName = file.toString();
        fName = fName.substring(fName.lastIndexOf("\\") + 1, fName.length());
        JTextArea text = (JTextArea) (((JViewport) ((JScrollPane) (((Panel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex())).getComponent(0))).getViewport())).getView();
        text.setText(text.getText() + from + "Sending file " + fName + " ...\n");
        chat.send(file, v, name);
    }
 
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws UnsupportedLookAndFeelException {
        try{
            UIManager.setLookAndFeel("com.birosoft.liquid.LiquidLookAndFeel");
            UIManager.put("TextArea.font", new Font("TimesRoman", Font.PLAIN, 12));
        }catch(Exception e) {
            e.printStackTrace();
        }
        Client cl = new Client("localhost", 8189, 8190);
    }
   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JList jList1;
    private javax.swing.JList jList2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
