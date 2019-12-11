package edu.wit.yeatesg.mps.network.clientserver;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import edu.wit.yeatesg.mps.network.clientserver.MPSClient.ActiveGameException;
import edu.wit.yeatesg.mps.network.clientserver.MPSClient.ConnectionFailedException;
import edu.wit.yeatesg.mps.network.clientserver.MPSClient.DuplicateNameException;
import edu.wit.yeatesg.mps.network.clientserver.MPSClient.ServerFullException;
import edu.wit.yeatesg.mps.network.clientserver.MPSServer.ServerStartFailedException;
import edu.wit.yeatesg.mps.network.packets.Packet;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

public class ConnectGUI extends JPanel
{
	private static final long serialVersionUID = -3847544782116562853L;

	private MPSClient internalClient;

	private ConnectFrame frame;

	public ConnectGUI(String defaultClientName)
	{
		frame = new ConnectFrame();
		internalClient = new MPSClient(defaultClientName);
		frame.setVisible(true);
	}

	public ConnectGUI()
	{
		this(null);
	}

	private void onButtonPress(boolean isHost)
	{
		if (btn_Connect.isEnabled())
		{
			label_statusMessage.setForeground(new Color(0, 140, 0));
			label_statusMessage.setText(isHost ? "Creating Server..." : "Connecting...");

			disableButtons(1500);

			Thread attemptConnectThread = new Thread(() -> attemptConnect(isHost));
			attemptConnectThread.setDaemon(true);
			attemptConnectThread.start();
		}	
	}

	private void disableButtons(int forHowLong)
	{
		btn_Connect.setEnabled(false);
		btn_Host.setFocusable(false);
		Timer enableConnect = new Timer(forHowLong, (e) -> btn_Connect.setEnabled(true));
		Timer enableHost = new Timer(forHowLong + 1500, (e) -> btn_Host.setFocusable(true));
		enableConnect.setRepeats(false);
		enableHost.setRepeats(false);
		enableConnect.start();
		enableHost.start();
	}

	private MPSServer server;

	private void attemptConnect(boolean isHost)
	{
		try
		{	
			try { Integer.parseInt(field_port.getText()); }
			catch (NumberFormatException e) {  throw new InvalidInputException("Invalid Port"); }
			
			if (field_ip.getText().equals(""))
				field_ip.setText("localhost");
			
			if (!isHost && !field_ip.getText().contains(".") && !field_ip.getText().equals("localhost"))
				throw new InvalidInputException("Invalid IP Address");

			if (!GameplayGUI.validName(field_name.getText()))
				throw new InvalidInputException("Invalid Client Name");

			int port = Integer.parseInt(field_port.getText());

			server = isHost ? (server == null ? new MPSServer(port) : server) : null;		

			if (server != null) // This client wants to host the game
				server.start();

			internalClient.setName(field_name.getText());

			// Inputs are not erroneous, try to connect

			if (internalClient.attemptConnect(field_ip.getText(), port, isHost))
			{
				EventQueue.invokeLater(() ->
				{
					new LobbyGUI(internalClient.getName(), internalClient, port);
					frame.dispose();
				});
			}
		}
		catch (ServerStartFailedException | InvalidInputException | ConnectionFailedException | ServerFullException | ActiveGameException | DuplicateNameException e)
		{
			server = null;
			label_statusMessage.setForeground(Color.RED); // We ran into some error on the way, print the error message
			label_statusMessage.setText(e.getMessage());
		}
		btn_Host.setEnabled(true);
	}

	private JTextField field_ip;
	private JTextField field_port;
	private JTextField field_name;
	private JLabel label_statusMessage;
	private JButton btn_Connect;
	private JButton btn_Host;

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
			
			field_ip = new GTextField();
			field_ip.setHorizontalAlignment(SwingConstants.LEFT);
			field_ip.setBounds(10, 29, 96, 20);
			field_ip.setColumns(10);
			field_ip.setText("localhost");
			field_ip.grabFocus();
			ConnectGUI.this.add(field_ip);

			field_port = new GTextField();
			field_port.setHorizontalAlignment(SwingConstants.LEFT);
			field_port.setColumns(10);
			field_port.setBounds(112, 29, 48, 20);
			field_port.setText("8122");
			ConnectGUI.this.add(field_port);

			String randName = "";
			Random randNameGen = new Random();
			for (int i = 0; i < 17; i++)
				randName += randNameGen.nextInt(10) + "";
			field_name = new GTextField();
			field_name.setText(randName);
			field_name.setColumns(10);
			field_name.setBounds(10, 70, 150, 20);
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
	
	private class GTextField extends JTextField implements KeyListener, FocusListener, MouseListener
	{
		private static final long serialVersionUID = 599016001086758578L;

		public GTextField(String text)
		{
			super(text);
			addMouseListener(this);
			addFocusListener(this);
			addKeyListener(this);
		}
		
		public GTextField()
		{
			this(null);
		}
		
		@Override
		public void keyPressed(KeyEvent e)
		{			
			if (e.getKeyCode() == KeyEvent.VK_ENTER)
				if (btn_Connect.isEnabled())
					onButtonPress(false);				
		}

		public void keyTyped(KeyEvent e)
		{
			if (this == field_name && getText().length() >= MAX_NAME_LENGTH)	
			{
				e.consume();
				setText(getText().substring(0, MAX_NAME_LENGTH));
			}
			else if (this == field_port)
			{
				if (getText().length() >= 5)
				{
					e.consume();
					setText(getText().substring(0, 5));
				}
				else if (!Character.isDigit(e.getKeyChar()))
				{
					e.consume();
				}
			}
		}
		public void keyReleased(KeyEvent arg0) { }

	
		@Override
		public void focusGained(FocusEvent arg0)
		{
	//		if (this != field_ip)
	//			setText("");
		}

		@Override
		public void focusLost(FocusEvent arg0)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{
			setText("");
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
	}


	/**
	 * Checked exception for when the user input in one of the text fields of the ConnectGUI is invalid
	 * (i.e they typed a String for the port, or their name is invalid)
	 * @author yeatesg
	 */
	private static class InvalidInputException extends Exception
	{
		private static final long serialVersionUID = 8969837391732888398L;

		private String message;

		private InvalidInputException(String message)
		{
			this.message = message;
		}

		@Override
		public String getMessage()
		{
			return message;
		}
	}
}