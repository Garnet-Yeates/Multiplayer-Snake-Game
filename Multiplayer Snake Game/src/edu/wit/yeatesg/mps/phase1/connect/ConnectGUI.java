package edu.wit.yeatesg.mps.phase1.connect;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import edu.wit.yeatesg.mps.phase0.packets.Packet;
import edu.wit.yeatesg.mps.phase3.play.SnakeClient;

public class ConnectGUI extends JFrame
{
	private static final long serialVersionUID = 1L;

	private JPanel contentPane;

	private JTextField field_ip;
	private JTextField field_port;
	private JTextField field_name;
	private JLabel label_statusMessage;
	private JButton btn_Connect;
	private JButton btn_Host;

	private Client internalClient;
	
	public ConnectGUI(String defaultClientName)
	{
		internalClient = new Client(null, defaultClientName);
		initFrame(defaultClientName);
		setLookAndFeel();
		setVisible(true);
	}
	
	public ConnectGUI()
	{
		this(null);
	}

	private void onButtonPress(boolean isHost)
	{
		if (isHost)
			field_ip.setText("localhost");
		label_statusMessage.setForeground(new Color(0, 160, 0));
		label_statusMessage.setText(isHost ? "Creating Server.." : "Connecting...");
		EventQueue.invokeLater(() -> attemptConnect(isHost)); // Make sure the label says "Connecting" before the thread freezes waiting to connect
	}

	private Server server;

	private void attemptConnect(boolean isHost)
	{
		try
		{	
			try {
				Integer.parseInt(field_port.getText());
			} catch (Exception e) { 
				throw new RuntimeException("Invalid Port");
			}

			if (!field_ip.getText().equals("localhost") && !field_ip.getText().contains("."))
				throw new RuntimeException("Invalid IP Address");

			if (!SnakeClient.validName(field_name.getText()))
				throw new RuntimeException("Invalid Client Name");

			int port = Integer.parseInt(field_port.getText());

			server = isHost ? (server == null ? new Server(port) : server) : null;		
			
			if (server != null) // This client is hosting the game
			{
				if (!server.start())
					throw new RuntimeException("Couldn't create server");				
			}
			
			internalClient.setName(field_name.getText());
			
			// Inputs are not erroneous, try to connect

			if (internalClient.connect(field_ip.getText(), port, isHost))
				dispose();
		}
		catch (Exception e)
		{
			server = null;
			label_statusMessage.setForeground(Color.RED); // We ran into some error on the way, print the error message
			label_statusMessage.setText(e.getMessage());
		}
		btn_Host.setEnabled(true);
	}
	
	int numPacketsSent;
	
	public void onPacketReceive(Packet pack) { } 
	
	public void initFrame(String clientName)
	{
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 177, 175);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		field_ip = new JTextField();
		field_ip.setHorizontalAlignment(SwingConstants.LEFT);
		field_ip.setBounds(10, 29, 96, 20);
		contentPane.add(field_ip);
		field_ip.setColumns(10);
		field_ip.setText("localhost");

		field_port = new JTextField();
		field_port.setHorizontalAlignment(SwingConstants.LEFT);
		field_port.setColumns(10);
		field_port.setBounds(112, 29, 48, 20);
		field_port.setText("8122");
		contentPane.add(field_port);

		field_name = new JTextField();
		field_name.setText(clientName);
		field_name.setColumns(10);
		field_name.setBounds(10, 70, 150, 20);
		field_name.addKeyListener(new KeyListener() 
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
				if (field_name.getText().length() >= SnakeClient.MAX_NAME_LENGTH)
					e.consume();
			}
			
			public void keyReleased(KeyEvent e) { }
			public void keyPressed(KeyEvent e) { }
		});
		contentPane.add(field_name);

		JLabel label_name = new JLabel("Name");
		label_name.setFont(new Font("Tahoma", Font.BOLD, 11));
		label_name.setBounds(10, 55, 61, 14);
		contentPane.add(label_name);

		label_statusMessage = new JLabel("");
		label_statusMessage.setForeground(Color.RED);
		label_statusMessage.setBounds(10, 128, 148, 14);
		contentPane.add(label_statusMessage);

		JLabel label_ipAddress = new JLabel("IP Address");
		label_ipAddress.setFont(new Font("Tahoma", Font.BOLD, 11));
		label_ipAddress.setBounds(10, 11, 61, 14);
		contentPane.add(label_ipAddress);

		JLabel label_port = new JLabel("Port");
		label_port.setFont(new Font("Tahoma", Font.BOLD, 11));
		label_port.setBounds(116, 11, 53, 14);
		contentPane.add(label_port);

		btn_Connect = new JButton("Connect");
		btn_Connect.setBounds(75, 101, 85, 23);
		btn_Connect.addActionListener((e) -> onButtonPress(false));
		contentPane.add(btn_Connect);

		btn_Host = new JButton("Host");
		btn_Host.setBounds(10, 101, 61, 23);
		btn_Host.addActionListener((e) -> onButtonPress(true));
		contentPane.add(btn_Host);
	}

	public static void setLookAndFeel()
	{
		try
		{
			LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
			UIManager.setLookAndFeel(feels[3].getClassName());
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	

}