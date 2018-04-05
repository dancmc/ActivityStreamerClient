package activitystreamer.client;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

@SuppressWarnings("serial")
public class TextFrame extends JFrame implements ActionListener {
	private static final Logger log = LogManager.getLogger();

	private JTextArea inputText;
	private JTextArea outputText;
	private JButton sendButton;
	private JButton disconnectButton;
	private JScrollPane inputScroll;
    private JScrollPane outputScroll;
    private boolean outputScrollMax = true;

	public TextFrame(){


		setTitle("ActivityStreamer Text I/O");
		setSize(1280,768);
        setLocationRelativeTo(null);

		setVisible(true);


		JPanel mainPanel = new JPanel();


//		try {
//			UIManager.setLookAndFeel (new MaterialLookAndFeel());
//		}
//		catch (UnsupportedLookAndFeelException e) {
//			e.printStackTrace ();
//		}
		mainPanel.setLayout(new GridLayout(1,2));
		JPanel inputPanel = new JPanel();
		JPanel outputPanel = new JPanel();
		inputPanel.setLayout(new BorderLayout());
		outputPanel.setLayout(new BorderLayout());
		Border lineBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray),"JSON input, to send to server");
		inputPanel.setBorder(lineBorder);
		lineBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray),"JSON output, received from server");
		outputPanel.setBorder(lineBorder);
		outputPanel.setName("Text output");

		inputText = new JTextArea();
		inputScroll = new JScrollPane(inputText);
		inputPanel.add(inputScroll,BorderLayout.CENTER);

		JPanel buttonGroup = new JPanel();
		sendButton = new JButton("Send");
		disconnectButton = new JButton("Disconnect");
		buttonGroup.add(sendButton);
		buttonGroup.add(disconnectButton);
		inputPanel.add(buttonGroup,BorderLayout.SOUTH);
		sendButton.addActionListener(this);
		disconnectButton.addActionListener(this);


		outputText = new JTextArea();
		outputScroll = new JScrollPane(outputText);
        outputScroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener(){
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // Check if user has done dragging the scroll bar
                if(!e.getValueIsAdjusting()){
                    JScrollBar scrollBar = (JScrollBar) e.getAdjustable();
                    int extent = scrollBar.getModel().getExtent();
                    int maximum = scrollBar.getModel().getMaximum();
                    outputScrollMax = extent + e.getValue() == maximum;
                } else {
                    outputScrollMax = false;
                }

            }
        });
		outputPanel.add(outputScroll,BorderLayout.CENTER);


		mainPanel.add(inputPanel);
		mainPanel.add(outputPanel);
		add(mainPanel);


		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


		revalidate();



	}

	private String prettifyJson(JSONObject obj){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(obj.toString());
        return gson.toJson(je);
    }

	public void setOutputText(final JSONObject obj){

		outputText.setText(prettifyJson(obj));
		outputText.revalidate();
		outputText.repaint();
	}

	public void appendOutputText(String jsonString){
	    appendOutputText(new JSONObject(jsonString));
    }

	public void appendOutputText(final JSONObject obj){

	    outputText.append(prettifyJson(obj));
        if(outputScrollMax){
            outputText.setCaretPosition(outputText.getDocument().getLength());
        }
    }
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==sendButton){
			String msg = inputText.getText().trim().replaceAll("\r","").replaceAll("\n","").replaceAll("\t", "");
			JSONObject obj;
			try {
				obj = new JSONObject(msg);
				ClientControl.getInstance().sendActivityObject(obj);
			} catch (JSONException e1) {
				log.error("invalid JSON object entered into input text field, data not sent");
			}
			
		} else if(e.getSource()==disconnectButton){
			ClientControl.getInstance().disconnect();
		}
	}
}
