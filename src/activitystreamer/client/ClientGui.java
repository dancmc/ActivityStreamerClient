/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package activitystreamer.client;

import activitystreamer.util.Settings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;


/**
 * Skeleton of this class automatically generated using NetBeans GUI designer
 */

public class ClientGui extends javax.swing.JFrame implements FrontEnd {
    private static final Logger log = LogManager.getLogger();

    /**
     * Creates new form ClientGui
     */
    ClientGui() {

//        try {
//
//            javax.swing.UIManager.setLookAndFeel(new MaterialLookAndFeel());
//
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(ClientGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }

        initComponents();

        documentMessages = areaMessages.getStyledDocument();
        documentLogging = areaLogging.getStyledDocument();
        buttonLogin.setEnabled(false);

        StyleConstants.setForeground(grayColor, Color.gray);
        StyleConstants.setForeground(blueColor, Color.blue);
        StyleConstants.setForeground(pinkColor, Color.PINK);
        StyleConstants.setBold(pinkColor, true);

        scrollLogging.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // Check if user is done dragging the scroll bar
//                if (!e.getValueIsAdjusting()) {
                JScrollBar scrollBar = (JScrollBar) e.getAdjustable();
                int extent = scrollBar.getModel().getExtent();
                int maximum = scrollBar.getModel().getMaximum();
                scrollLoggingMaxed = extent + e.getValue() == maximum;
//                } else {
//                    scrollLoggingMaxed = false;
//                }

            }
        });

        scrollMessages.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // Check if user is done dragging the scroll bar
//                if (!e.getValueIsAdjusting()) {
                JScrollBar scrollBar = (JScrollBar) e.getAdjustable();
                int extent = scrollBar.getModel().getExtent();
                int maximum = scrollBar.getModel().getMaximum();
                scrollMessageMaxed = extent + e.getValue() == maximum;
//                } else {
//                    scrollMessageMaxed = false;
//                }

            }
        });

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                setVisible(true);
            }
        });
    }


    // ACTION CALLBACKS

    private void buttonDisconnectMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonDisconnectMouseReleased
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ClientControl.getInstance().disconnect();
                return null;
            }
        }.execute();
    }//GEN-LAST:event_buttonDisconnectMouseReleased

    private void buttonReconnectMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonReconnectMouseReleased
        final String hostname = editHostname.getText();
        final String port = editPort.getText();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    int portInt = Integer.parseInt(port);
                    ClientControl.getInstance().reconnect(hostname, portInt);
                } catch (NumberFormatException e) {
                    String error = "failed to format port : " + e.getMessage();
                    log.error(error);
                    appendLog(error);
                }
                return null;
            }
        }.execute();


    }//GEN-LAST:event_buttonReconnectMouseReleased


    private void buttonClearLogsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonClearLogsMouseReleased
        areaLogging.setText("");
    }//GEN-LAST:event_buttonClearLogsMouseReleased

    private void buttonClearMessagesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonClearMessagesMouseReleased
        areaMessages.setText("");
    }//GEN-LAST:event_buttonClearMessagesMouseReleased

    private void buttonRegisterMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonRegisterMouseReleased

        final String username = editUsername.getText();
        final String secret = editSecret.getText();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ClientControl.getInstance().register(username, secret);
                return null;
            }
        }.execute();
    }//GEN-LAST:event_buttonRegisterMouseReleased

    private void buttonLoginMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonLoginMouseReleased

        final String username = editUsername.getText();
        final String secret = editSecret.getText();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ClientControl.getInstance().login(username, secret);
                return null;
            }
        }.execute();

    }//GEN-LAST:event_buttonLoginMouseReleased

    private void buttonSendMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonSendMouseReleased
        String message = areaMessageInput.getText().trim().replaceAll("\r", "").replaceAll("\n", "").replaceAll("\t", "");

        sendActivity(message);

    }//GEN-LAST:event_buttonSendMouseReleased

    /*
    These methods can be called from either EDT or other threads, have to check
    Lambdas in J8 would help a lot here....

    Things that need to happen :
        - setup (set input hostname & port to Settings, set input username and secret as well)
        - connected (set hostname and port labels, change indicator to green)
        - disconnected (set hostname and port labels, change indicator to red)
        - loggedin (set username label, change indicator to green)
        - loggedout (set username label, change indicator to red)
        - appendLog
        - appendActivity
        - sendActivity
     */


    private void executeOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception e) {
                log.error("update GUI failed : " + e.getMessage());
            }
        }
    }

    @Override
    public void setup() {

        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                editHostname.setText(Settings.getRemoteHostname());
                editPort.setText("" + Settings.getRemotePort());
                editUsername.setText(Settings.getUsername());
                editSecret.setText(Settings.getSecret());
            }
        });
    }


    @Override
    public void connected(final String hostname, final int port) {

        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                labelHostname.setText(hostname);
                labelPort.setText("" + port);
                indicatorConnected.setForeground(Color.GREEN);
                buttonLogin.setEnabled(true);
            }
        });
    }

    @Override
    public void disconnected() {

        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                if (!ClientControl.getInstance().isConnected()) {
                    labelHostname.setText("-");
                    labelPort.setText("-");
                    indicatorConnected.setForeground(Color.RED);
                    buttonLogin.setEnabled(false);
                }

                // may ont need the check since this is run synchronously with the disconnect
//                if (!ClientControl.getInstance().isLoggedIn()) {
                loggedOut();
//                }
            }
        });

    }


    @Override
    public void loggedIn() {
        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                labelUsername.setText(Settings.getUsername());
                indicatorLoggedIn.setForeground(Color.GREEN);
            }
        });
    }

    @Override
    public void loggedOut() {
        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                labelUsername.setText("-");
                indicatorLoggedIn.setForeground(Color.RED);
            }
        });

    }


    @Override
    public void appendLog(final String message) {
        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                SimpleAttributeSet keyWord = null;
                try {
                    if (!logColorToggle) {
                        keyWord = grayColor;
                    }
                    documentLogging.insertString(documentLogging.getLength(), message + "\n", keyWord);
                    logColorToggle = !logColorToggle;
                } catch (Exception e) {
                    log.error("ERROR - error adding text to logging pane : " + e.getMessage());
                }

                // unfortunately this doesn't matter with textpane
//                if (scrollLoggingMaxed) {
//                    areaLogging.setCaretPosition(areaLogging.getDocument().getLength());
//                }
            }
        });
    }


    @Override
    public void appendActivity(final JSONObject activity) {

        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                boolean hasUser = activity.has("authenticated_user");
                String user = activity.optString("authenticated_user", "unauthenticated");

                try {
                    documentMessages.insertString(documentMessages.getLength(), user + " : \n\n", (!hasUser) ? pinkColor : blueColor);
                    documentMessages.insertString(documentMessages.getLength(), prettifyJson(activity) + "\n\n", null);
                } catch (Exception e) {
                    log.error("ERROR - error adding text to message pane : " + e.getMessage());
                }

                // unfortunately this doesn't work with textpane
//                if (scrollMessageMaxed) {
//                    areaMessages.setCaretPosition(areaMessages.getDocument().getLength());
//                }
            }
        });
    }

    private String prettifyJson(JSONObject obj) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(obj.toString());
        return gson.toJson(je);
    }


    // called from EDT, needs a SwingWorker
    @Override
    public void sendActivity(final String message) {

        new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() throws Exception {
                JSONObject activity;

                try {
                    activity = new JSONObject(message);
                } catch (JSONException e) {
                    ClientControl.getInstance().outputError(log, "ERROR - tried to send activity message but failed to parse");
                    return false;
                }
                return ClientControl.getInstance().sendActivityObject(activity);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        areaMessageInput.setText("");
                    } else {
                        // todo maybe flash warning
                    }
                } catch (Exception e) {
                    log.error("ERROR - failed to clear message input");
                }
            }
        }.execute();

    }

    private SimpleAttributeSet pinkColor = new SimpleAttributeSet();
    private SimpleAttributeSet blueColor = new SimpleAttributeSet();
    private SimpleAttributeSet grayColor = new SimpleAttributeSet();
    private boolean logColorToggle = true;
    private boolean scrollLoggingMaxed = true;
    private boolean scrollMessageMaxed = true;
    private StyledDocument documentMessages;
    private StyledDocument documentLogging;


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        staticConnected = new javax.swing.JLabel();
        labelHostname = new javax.swing.JLabel();
        labelPort = new javax.swing.JLabel();
        staticHostname = new javax.swing.JLabel();
        editHostname = new javax.swing.JTextField();
        editPort = new javax.swing.JTextField();
        staticPort = new javax.swing.JLabel();
        buttonReconnect = new javax.swing.JButton();
        indicatorConnected = new javax.swing.JLabel();
        buttonDisconnect = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        staticLoggedIn = new javax.swing.JLabel();
        labelUsername = new javax.swing.JLabel();
        buttonLogin = new javax.swing.JButton();
        editSecret = new javax.swing.JTextField();
        editUsername = new javax.swing.JTextField();
        staticSecret = new javax.swing.JLabel();
        staticUsername = new javax.swing.JLabel();
        indicatorLoggedIn = new javax.swing.JLabel();
        buttonRegister = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        staticLogging = new javax.swing.JLabel();
        scrollLogging = new javax.swing.JScrollPane();
        areaLogging = new javax.swing.JTextPane();
        staticMessages = new javax.swing.JLabel();
        scrollMessages = new javax.swing.JScrollPane();
        areaMessages = new javax.swing.JTextPane();
        scrollMessageInput = new javax.swing.JScrollPane();
        areaMessageInput = new javax.swing.JTextArea();
        buttonSend = new javax.swing.JButton();
        buttonClearMessages = new javax.swing.JButton();
        buttonClearLogs = new javax.swing.JButton();

        jMenu1.setText("jMenu1");

        jMenu2.setText("jMenu2");

        jMenu3.setText("jMenu3");

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("jCheckBoxMenuItem1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1280, 800));

        jPanel1.setBackground(new java.awt.Color(125, 208, 195));
        jPanel1.setMinimumSize(new java.awt.Dimension(1280, 153));
        jPanel1.setPreferredSize(new java.awt.Dimension(1280, 153));

        jPanel2.setMinimumSize(new java.awt.Dimension(640, 100));
        jPanel2.setOpaque(false);
        jPanel2.setPreferredSize(new java.awt.Dimension(640, 80));

        staticConnected.setFont(new java.awt.Font("Century Gothic", 1, 14)); // NOI18N
        staticConnected.setForeground(new java.awt.Color(255, 255, 255));
        staticConnected.setText("Connected");

        labelHostname.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        labelHostname.setForeground(new java.awt.Color(255, 255, 255));
        labelHostname.setText("-");

        labelPort.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        labelPort.setForeground(new java.awt.Color(255, 255, 255));
        labelPort.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        labelPort.setText("-");

        staticHostname.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        staticHostname.setForeground(new java.awt.Color(255, 255, 255));
        staticHostname.setLabelFor(editHostname);
        staticHostname.setText("Hostname");

        editHostname.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        editHostname.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        editPort.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        editPort.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        staticPort.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        staticPort.setForeground(new java.awt.Color(255, 255, 255));
        staticPort.setText("Port");

        buttonReconnect.setBackground(new java.awt.Color(255, 255, 255));
        buttonReconnect.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        buttonReconnect.setText("Reconnect");
        buttonReconnect.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 15, 5, 15));
        buttonReconnect.setOpaque(true);
        buttonReconnect.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonReconnectMouseReleased(evt);
            }
        });

        indicatorConnected.setFont(new java.awt.Font("Lucida Grande", 0, 20)); // NOI18N
        indicatorConnected.setForeground(new java.awt.Color(255, 0, 51));
        indicatorConnected.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        indicatorConnected.setText("•");

        buttonDisconnect.setBackground(new java.awt.Color(255, 255, 255));
        buttonDisconnect.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        buttonDisconnect.setText("Disconnect");
        buttonDisconnect.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 15, 5, 15));
        buttonDisconnect.setOpaque(true);
        buttonDisconnect.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonDisconnectMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(17, 17, 17)
                                .addComponent(indicatorConnected, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(buttonDisconnect)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(staticConnected)
                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addGap(21, 21, 21)
                                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(labelPort, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(labelHostname, javax.swing.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE))))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(staticHostname)
                                                        .addComponent(staticPort))
                                                .addGap(18, 18, 18)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(editHostname, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(editPort, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(buttonReconnect)))
                                                .addGap(0, 116, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(staticConnected)
                                        .addComponent(indicatorConnected, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(staticHostname)
                                        .addComponent(editHostname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(2, 2, 2)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(labelHostname)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(labelPort))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(staticPort, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(editPort)
                                                .addComponent(buttonReconnect)))
                                .addGap(14, 14, 14)
                                .addComponent(buttonDisconnect))
        );

        jPanel3.setMinimumSize(new java.awt.Dimension(640, 100));
        jPanel3.setOpaque(false);
        jPanel3.setPreferredSize(new java.awt.Dimension(640, 114));

        staticLoggedIn.setFont(new java.awt.Font("Century Gothic", 1, 14)); // NOI18N
        staticLoggedIn.setForeground(new java.awt.Color(255, 255, 255));
        staticLoggedIn.setText("Logged in ");

        labelUsername.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        labelUsername.setForeground(new java.awt.Color(255, 255, 255));
        labelUsername.setText("-");

        buttonLogin.setBackground(new java.awt.Color(255, 255, 255));
        buttonLogin.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        buttonLogin.setText("Login");
        buttonLogin.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 15, 5, 15));
        buttonLogin.setOpaque(true);
        buttonLogin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonLoginMouseReleased(evt);
            }
        });

        editSecret.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        editSecret.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        editSecret.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSecretActionPerformed(evt);
            }
        });

        editUsername.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        editUsername.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        staticSecret.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        staticSecret.setForeground(new java.awt.Color(255, 255, 255));
        staticSecret.setText("Secret");

        staticUsername.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        staticUsername.setForeground(new java.awt.Color(255, 255, 255));
        staticUsername.setText("Username");

        indicatorLoggedIn.setFont(new java.awt.Font("Lucida Grande", 0, 20)); // NOI18N
        indicatorLoggedIn.setForeground(new java.awt.Color(255, 51, 102));
        indicatorLoggedIn.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        indicatorLoggedIn.setText("•");

        buttonRegister.setBackground(new java.awt.Color(255, 255, 255));
        buttonRegister.setFont(new java.awt.Font("Century Gothic", 0, 13)); // NOI18N
        buttonRegister.setText("Register");
        buttonRegister.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 15, 5, 15));
        buttonRegister.setOpaque(true);
        buttonRegister.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonRegisterMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addGap(52, 52, 52)
                                                .addComponent(labelUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(indicatorLoggedIn, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(staticLoggedIn)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 125, Short.MAX_VALUE)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(staticUsername)
                                        .addComponent(staticSecret))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(editUsername, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                                        .addComponent(editSecret)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addComponent(buttonRegister)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(buttonLogin)))
                                .addGap(87, 87, 87))
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(editUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(staticUsername))
                                                .addGap(15, 15, 15)
                                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(editSecret, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(staticSecret))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(buttonLogin)
                                                        .addComponent(buttonRegister))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(staticLoggedIn)
                                                        .addComponent(indicatorLoggedIn, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(labelUsername)
                                                .addGap(0, 0, Short.MAX_VALUE))))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE))
                                .addContainerGap(23, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(38, 46, 65));
        jPanel4.setMinimumSize(new java.awt.Dimension(1280, 606));
        jPanel4.setPreferredSize(new java.awt.Dimension(1280, 606));

        staticLogging.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        staticLogging.setForeground(new java.awt.Color(255, 255, 255));
        staticLogging.setText("Logging");

        scrollLogging.setBorder(null);
        scrollLogging.setMinimumSize(new java.awt.Dimension(596, 537));
        scrollLogging.setPreferredSize(new java.awt.Dimension(596, 537));

        areaLogging.setEditable(false);
        areaLogging.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        areaLogging.setFont(new java.awt.Font("Lucida Console", 0, 13)); // NOI18N
        scrollLogging.setViewportView(areaLogging);

        staticMessages.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        staticMessages.setForeground(new java.awt.Color(255, 255, 255));
        staticMessages.setText("Messages");

        scrollMessages.setBorder(null);
        scrollMessages.setMinimumSize(new java.awt.Dimension(585, 453));
        scrollMessages.setPreferredSize(new java.awt.Dimension(585, 453));

        areaMessages.setEditable(false);
        areaMessages.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        areaMessages.setFont(new java.awt.Font("Lucida Console", 0, 13)); // NOI18N
        scrollMessages.setViewportView(areaMessages);

        scrollMessageInput.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        scrollMessageInput.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollMessageInput.setMinimumSize(new java.awt.Dimension(515, 77));
        scrollMessageInput.setPreferredSize(new java.awt.Dimension(515, 77));

        areaMessageInput.setColumns(20);
        areaMessageInput.setRows(5);
        areaMessageInput.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        scrollMessageInput.setViewportView(areaMessageInput);

        buttonSend.setFont(new java.awt.Font("Century Gothic", 0, 14)); // NOI18N
        buttonSend.setText("Send");
        buttonSend.setMaximumSize(new java.awt.Dimension(100, 29));
        buttonSend.setMinimumSize(new java.awt.Dimension(88, 29));
        buttonSend.setPreferredSize(new java.awt.Dimension(88, 29));
        buttonSend.setSize(new java.awt.Dimension(80, 0));
        buttonSend.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonSendMouseReleased(evt);
            }
        });

        buttonClearMessages.setBackground(new java.awt.Color(255, 255, 255));
        buttonClearMessages.setFont(new java.awt.Font("Century Gothic", 0, 12)); // NOI18N
        buttonClearMessages.setText("Clear");
        buttonClearMessages.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 13, 5, 13));
        buttonClearMessages.setOpaque(true);
        buttonClearMessages.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonClearMessagesMouseReleased(evt);
            }
        });

        buttonClearLogs.setBackground(new java.awt.Color(255, 255, 255));
        buttonClearLogs.setFont(new java.awt.Font("Century Gothic", 0, 12)); // NOI18N
        buttonClearLogs.setText("Clear");
        buttonClearLogs.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 13, 5, 13));
        buttonClearLogs.setOpaque(true);
        buttonClearLogs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonClearLogsMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGap(33, 33, 33)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addComponent(staticLogging)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(buttonClearLogs))
                                        .addComponent(scrollLogging, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(49, 49, 49)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addComponent(staticMessages)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(buttonClearMessages))
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addComponent(scrollMessageInput, javax.swing.GroupLayout.PREFERRED_SIZE, 515, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(buttonSend, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                                        .addComponent(scrollMessages, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(29, 29, 29))
        );
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addComponent(staticLogging)
                                                .addGap(24, 24, 24)
                                                .addComponent(scrollLogging, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(buttonClearLogs)
                                                        .addComponent(buttonClearMessages)
                                                        .addComponent(staticMessages))
                                                .addGap(16, 16, 16)
                                                .addComponent(scrollMessages, javax.swing.GroupLayout.DEFAULT_SIZE, 505, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(buttonSend, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(scrollMessageInput, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(20, 20, 20))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 661, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void editSecretActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSecretActionPerformed
    }//GEN-LAST:event_editSecretActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextPane areaLogging;
    private javax.swing.JTextArea areaMessageInput;
    private javax.swing.JTextPane areaMessages;
    private javax.swing.JButton buttonClearLogs;
    private javax.swing.JButton buttonClearMessages;
    private javax.swing.JButton buttonDisconnect;
    private javax.swing.JButton buttonLogin;
    private javax.swing.JButton buttonReconnect;
    private javax.swing.JButton buttonRegister;
    private javax.swing.JButton buttonSend;
    private javax.swing.JTextField editHostname;
    private javax.swing.JTextField editPort;
    private javax.swing.JTextField editSecret;
    private javax.swing.JTextField editUsername;
    private javax.swing.JLabel indicatorConnected;
    private javax.swing.JLabel indicatorLoggedIn;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JLabel labelHostname;
    private javax.swing.JLabel labelPort;
    private javax.swing.JLabel labelUsername;
    private javax.swing.JScrollPane scrollLogging;
    private javax.swing.JScrollPane scrollMessageInput;
    private javax.swing.JScrollPane scrollMessages;
    private javax.swing.JLabel staticConnected;
    private javax.swing.JLabel staticHostname;
    private javax.swing.JLabel staticLoggedIn;
    private javax.swing.JLabel staticLogging;
    private javax.swing.JLabel staticMessages;
    private javax.swing.JLabel staticPort;
    private javax.swing.JLabel staticSecret;
    private javax.swing.JLabel staticUsername;
    // End of variables declaration//GEN-END:variables


}
