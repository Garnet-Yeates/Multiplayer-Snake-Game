package edu.wit.yeatesg.mps.network.clientserver;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import edu.wit.yeatesg.mps.network.packets.Packet;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

public class ConnectGUI extends JPanel
{
	private static final long serialVersionUID = 1L;

	private NetworkClient internalClient;
	
	private ConnectFrame frame;
	
	public ConnectGUI(String defaultClientName)
	{
		frame = new ConnectFrame();
		internalClient = new NetworkClient(defaultClientName);
		setLookAndFeel();
		frame.setVisible(true);
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
			try { Integer.parseInt(field_port.getText()); }
			catch (Exception e) {  throw new RuntimeException("Invalid Port"); }

			if (!field_ip.getText().equals("localhost") && !field_ip.getText().contains("."))
				throw new RuntimeException("Invalid IP Address");

			if (!GameplayGUI.validName(field_name.getText()))
				throw new RuntimeException("Invalid Client Name");

			int port = Integer.parseInt(field_port.getText());

			server = isHost ? (server == null ? new Server(port) : server) : null;		
			
			if (server != null) // This client wants to host the game
				if (!server.start())
					throw new RuntimeException("Couldn't create server");				
			
			internalClient.setName(field_name.getText());
			
			// Inputs are not erroneous, try to connect

			if (internalClient.attemptConnect(field_ip.getText(), port, isHost))
				frame.dispose();
		}
		catch (Exception e)
		{
			server = null;
			label_statusMessage.setForeground(Color.RED); // We ran into some error on the way, print the error message
			label_statusMessage.setText(e.getMessage());
		}
		btn_Host.setEnabled(true);
	}
	
	private int numPacketsSent;
	
	public void onPacketReceive(Packet pack) { } 
	
	private JTextField field_ip;
	private JTextField field_port;
	private JTextField field_name;
	private JLabel label_statusMessage;
	private JButton btn_Connect;
	private JButton btn_Host;

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
	
	private class ConnectFrame extends JFrame
	{
		private static final long serialVersionUID = 5135113137603592910L;
		
		public ConnectFrame()
		{
			setResizable(false);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setBounds(100, 100, 175 + JAR_OFFSET_X, 175 + JAR_OFFSET_Y);
			ConnectGUI.this.setBorder(new EmptyBorder(5, 5, 5, 5));
			setContentPane(ConnectGUI.this);
			ConnectGUI.this.setLayout(null);

			field_ip = new JTextField();
			field_ip.setHorizontalAlignment(SwingConstants.LEFT);
			field_ip.setBounds(10, 29, 96, 20);
			ConnectGUI.this.add(field_ip);
			field_ip.setColumns(10);
			field_ip.setText("localhost");

			field_port = new JTextField();
			field_port.setHorizontalAlignment(SwingConstants.LEFT);
			field_port.setColumns(10);
			field_port.setBounds(112, 29, 48, 20);
			field_port.setText("8122");
			ConnectGUI.this.add(field_port);

			field_name = new JTextField();
			field_name.setText("Nom");
			field_name.setColumns(10);
			field_name.setBounds(10, 70, 150, 20);
			field_name.addKeyListener(new KeyListener() 
			{
				@Override
				public void keyTyped(KeyEvent e)
				{
					if (field_name.getText().length() >= MAX_NAME_LENGTH)
						e.consume();
				}
				
				public void keyReleased(KeyEvent e) { }
				public void keyPressed(KeyEvent e) { }
			});
			ConnectGUI.this.add(field_name);

			JLabel label_name = new JLabel("Name");
			label_name.setFont(new Font("Tahoma", Font.BOLD, 11));
			label_name.setBounds(10, 55, 61, 14);
			ConnectGUI.this.add(label_name);

			label_statusMessage = new JLabel("");
			label_statusMessage.setForeground(Color.RED);
			label_statusMessage.setBounds(10, 128, 148, 14);
			ConnectGUI.this.add(label_statusMessage);

			JLabel label_ipAddress = new JLabel("IP Address");
			label_ipAddress.setFont(new Font("Tahoma", Font.BOLD, 11));
			label_ipAddress.setBounds(10, 11, 61, 14);
			ConnectGUI.this.add(label_ipAddress);

			JLabel label_port = new JLabel("Port");
			label_port.setFont(new Font("Tahoma", Font.BOLD, 11));
			label_port.setBounds(116, 11, 53, 14);
			ConnectGUI.this.add(label_port);

			btn_Connect = new JButton("Connect");
			btn_Connect.setBounds(75, 101, 85, 23);
			btn_Connect.addActionListener((e) -> onButtonPress(false));
			ConnectGUI.this.add(btn_Connect);

			btn_Host = new JButton("Host");
			btn_Host.setBounds(10, 101, 61, 23);
			btn_Host.addActionListener((e) -> onButtonPress(true));
			ConnectGUI.this.add(btn_Host);			
		}
	}
}