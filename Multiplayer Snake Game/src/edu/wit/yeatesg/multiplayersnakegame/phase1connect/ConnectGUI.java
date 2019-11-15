package edu.wit.yeatesg.multiplayersnakegame.phase1connect;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.MessagePacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.Packet;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.SnakeUpdatePacket;
import edu.wit.yeatesg.multiplayersnakegame.phase2play.Client;
import edu.wit.yeatesg.multiplayersnakegame.server.Server;

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

	public ConnectGUI()
	{
		setLookAndFeel();
		initFrame();
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
		String errMsg = "";
		try
		{	
			errMsg = "Invalid Port";
			Integer.parseInt(field_port.getText());

			errMsg = "Invalid IP Address";
			if (!field_ip.getText().equals("localhost") && !field_ip.getText().contains("."))
				throw new RuntimeException();

			errMsg = "Invalid Client Name";
			if (!Client.validName(field_name.getText()))
				throw new RuntimeException();

			errMsg = "Connection Refused";

			int port = Integer.parseInt(field_port.getText());

			server = isHost ? (server == null ? new Server(port) : server) : null;
			
			if (server != null) // This client is hosting the game
			{
				errMsg = "Couldn't create server";
				
				server.startServer();
	
				if (!server.isRunning()) // Server start failed
					throw new RuntimeException();				
			}

			// Inputs are not erroneous, try to connect

			Socket clientSocket = new Socket(field_ip.getText(), Integer.parseInt(field_port.getText()));
			DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());

			// Send a request packet to the server
			SnakeData data = new SnakeData();
			data.setName(field_name.getText());
			data.setIsHost(isHost);
			Packet requestPacket = new SnakeUpdatePacket(data);
			requestPacket.setDataStream(outputStream);
			requestPacket.send();

			Packet responsePacket = Packet.parsePacket(inputStream.readUTF());

			if (responsePacket instanceof ErrorPacket) // If the server responds with an error (connection was denied for some reason)
			{
				ErrorPacket errPacket = (ErrorPacket) responsePacket;
				label_statusMessage.setForeground(Color.RED);
				label_statusMessage.setText(errPacket.getErrorMessage());
				clientSocket.close();
			}
			else if (responsePacket instanceof MessagePacket) // If the server responds with a message (connection was accepted)
			{	
				// The server's response to our request contains the server's name and port so the client can store this information
				MessagePacket serverResponse = (MessagePacket) responsePacket;
				String connectedServerName = serverResponse.getSender();
				String connectedServerPort = serverResponse.getMessage();

				new LobbyGUI(field_name.getText(), clientSocket, inputStream, outputStream, connectedServerName, connectedServerPort, getX(), getY());
				dispose(); // We are now done with this GUI
			}
		}
		catch (Exception e)
		{
			label_statusMessage.setForeground(Color.RED); // We ran into some error on the way, print the error message
			label_statusMessage.setText(errMsg);
		}
		btn_Host.setEnabled(true);
	}
	
	public void initFrame()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 223, 169);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		field_ip = new JTextField();
		field_ip.setHorizontalAlignment(SwingConstants.LEFT);
		field_ip.setBounds(10, 29, 108, 20);
		contentPane.add(field_ip);
		field_ip.setColumns(10);
		field_ip.setText("localhost");

		field_port = new JTextField();
		field_port.setHorizontalAlignment(SwingConstants.LEFT);
		field_port.setColumns(10);
		field_port.setBounds(142, 29, 53, 20);
		field_port.setText("8122");
		contentPane.add(field_port);

		field_name = new JTextField();
		field_name.setText("Nom");
		field_name.setColumns(10);
		field_name.setBounds(10, 70, 108, 20);
		contentPane.add(field_name);

		JLabel label_name = new JLabel("Name");
		label_name.setFont(new Font("Tahoma", Font.BOLD, 11));
		label_name.setBounds(10, 55, 61, 14);
		contentPane.add(label_name);

		label_statusMessage = new JLabel("");
		label_statusMessage.setForeground(Color.RED);
		label_statusMessage.setBounds(10, 101, 112, 14);
		contentPane.add(label_statusMessage);

		JLabel label_ipAddress = new JLabel("IP Address");
		label_ipAddress.setFont(new Font("Tahoma", Font.BOLD, 11));
		label_ipAddress.setBounds(10, 11, 61, 14);
		contentPane.add(label_ipAddress);

		JLabel label_port = new JLabel("Port");
		label_port.setFont(new Font("Tahoma", Font.BOLD, 11));
		label_port.setBounds(142, 11, 53, 14);
		contentPane.add(label_port);

		JLabel label_Colon = new JLabel(":\r\n");
		label_Colon.setFont(new Font("Tahoma", Font.PLAIN, 32));
		label_Colon.setBounds(124, 16, 13, 36);
		contentPane.add(label_Colon);

		btn_Connect = new JButton("Connect");
		btn_Connect.setBounds(125, 69, 76, 23);
		btn_Connect.addActionListener((e) -> onButtonPress(false));
		contentPane.add(btn_Connect);

		btn_Host = new JButton("Host");
		btn_Host.setBounds(125, 95, 76, 23);
		btn_Host.addActionListener((e) -> onButtonPress(true));
		contentPane.add(btn_Host);

		setVisible(true);
	}

	public static void setLookAndFeel()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}