package edu.wit.yeatesg.multiplayersnakegame.gui;

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

import edu.wit.yeatesg.multiplayersnakegame.application.Client;
import edu.wit.yeatesg.multiplayersnakegame.application.Server;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientData;
import edu.wit.yeatesg.multiplayersnakegame.packets.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.packets.MessagePacket;
import edu.wit.yeatesg.multiplayersnakegame.packets.Packet;
import edu.wit.yeatesg.multiplayersnakegame.packets.UpdateSingleClientPacket;

public class ConnectGUI extends JFrame
{
	private JPanel contentPane;

	private JTextField field_ip;
	private JTextField field_port;
	private JTextField field_name;
	private JLabel label_statusMessage;
	private JButton btn_Connect;
	private JButton btn_Host;

	/**
	 * Create the frame.
	 */
	public ConnectGUI()
	{
		setLookAndFeel();
		initFrame();
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

		field_port = new JTextField();
		field_port.setHorizontalAlignment(SwingConstants.LEFT);
		field_port.setColumns(10);
		field_port.setBounds(142, 29, 53, 20);
		contentPane.add(field_port);

		field_name = new JTextField();
		field_name.setText("1234567890");
		field_name.setColumns(10);
		field_name.setBounds(10, 70, 108, 20);
		contentPane.add(field_name);

		JLabel label_name = new JLabel("Name");
		label_name.setFont(new Font("Tahoma", Font.BOLD, 11));
		label_name.setBounds(10, 55, 61, 14);
		contentPane.add(label_name);

		label_statusMessage = new JLabel("");
		label_statusMessage.setForeground(Color.RED);
		label_statusMessage.setBounds(10, 101, 108, 14);
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

	private void onButtonPress(boolean isHost)
	{
		if (isHost)
			field_ip.setText("localhost");
		label_statusMessage.setForeground(Color.GREEN);
		label_statusMessage.setText("Connecting...");
		EventQueue.invokeLater(() -> // Make sure the label says "Connecting" before the thread freezes waiting to connect
		{
			attemptConnect(isHost);
		});
	}

	private Server hosting;

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

			hosting = isHost ? (hosting == null ? new Server(port) : hosting) : null;
			if (isHost)
			{
				ServerStartRunnable serverStart = new ServerStartRunnable();
				Thread startServerThread = new Thread(serverStart);
				startServerThread.start();
				while (serverStart.startFailed == null); // Wait for the server to start/fail starting before we move on
				
				errMsg = "Server Creation Failed";
				if (serverStart.startFailed)
					throw new RuntimeException();				
			}

						
			// Inputs are not erroneous, try to connect

			Socket clientSocket = new Socket(field_ip.getText(), Integer.parseInt(field_port.getText()));
			DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());

			// Send a request packet to the server
			ClientData data = new ClientData();
			data.setName(field_name.getText());
			data.setIsHost(isHost);
			Packet requestPacket = new UpdateSingleClientPacket(data);
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

				new LobbyGUI(hosting, field_name.getText(), clientSocket, inputStream, outputStream, connectedServerName, connectedServerPort);
				dispose(); // We are now done with this GUI
			}
		}
		catch (Exception e)
		{
			label_statusMessage.setForeground(Color.RED); // We ran into some error on the way, print the error message
			label_statusMessage.setText(errMsg);
		}				
	}
	
	class ServerStartRunnable implements Runnable
	{
		Boolean startFailed;
		
		@Override
		public void run()
		{
			if (hosting != null)
			{
				try
				{
					hosting.startServer(); 	// Inside the startServer() method, a new Thread is created, 
					startFailed = false;    // enclosing the while loop where serverSocket.open() is called. 
				}                           // This prevents the program from crashing. See Server#startServer().
				catch (IOException e)
				{
					startFailed = true;
				} 	
			}
		}
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