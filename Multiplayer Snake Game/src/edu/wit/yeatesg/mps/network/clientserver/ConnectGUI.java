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
		setLookAndFeel();
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
			if (isHost)
				field_ip.setText("localhost");
			label_statusMessage.setForeground(new Color(0, 160, 0));
			label_statusMessage.setText(isHost ? "Creating Server......" : "Connecting...");

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

			if (!field_ip.getText().equals("localhost") && !field_ip.getText().contains("."))
				throw new InvalidInputException("Invalid IP Address");

			if (!GameplayGUI.validName(field_name.getText()))
				throw new InvalidInputException("Invalid Client Name");

			int port = Integer.parseInt(field_port.getText());

			server = isHost ? (server == null ? new MPSServer(port) : server) : null;		

			if (server != null) // This client wants to host the game
				server.start();

			internalClient.setName(field_name.getText());

			// Inputs are not erroneous, try to connect

			boolean b = internalClient.attemptConnect(field_ip.getText(), port, isHost);

			if (b)
				EventQueue.invokeLater(() -> frame.dispose());

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

			field_ip = new JTextField();
			field_ip.setHorizontalAlignment(SwingConstants.LEFT);
			field_ip.setBounds(10, 29, 96, 20);
			field_ip.setColumns(10);
			field_ip.setText("localhost");
			field_ip.addKeyListener(new TextFieldKeyListener(field_ip));
			ConnectGUI.this.add(field_ip);

			field_port = new JTextField();
			field_port.setHorizontalAlignment(SwingConstants.LEFT);
			field_port.setColumns(10);
			field_port.setBounds(112, 29, 48, 20);
			field_port.setText("8122");
			field_port.addKeyListener(new TextFieldKeyListener(field_port));
			ConnectGUI.this.add(field_port);

			field_name = new JTextField();
			field_name.setText("Nom");
			field_name.setColumns(10);
			field_name.setBounds(10, 70, 150, 20);
			field_name.addKeyListener(new TextFieldKeyListener(field_name));
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

	private class TextFieldKeyListener implements KeyListener
	{
		private JTextField typingOn;

		public TextFieldKeyListener(JTextField typingOn)
		{
			this.typingOn = typingOn;
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
			if (typingOn == field_name && typingOn.getText().length() >= MAX_NAME_LENGTH)	
			{
				e.consume();
				typingOn.setText(typingOn.getText().substring(0, MAX_NAME_LENGTH));
			}
			else if (typingOn == field_port)
			{
				if (typingOn.getText().length() >= 5)
				{
					e.consume();
					typingOn.setText(typingOn.getText().substring(0, 5));
				}
				else if (!Character.isDigit(e.getKeyChar()))
				{
					e.consume();
				}
			}
		}
		public void keyReleased(KeyEvent arg0) { }
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