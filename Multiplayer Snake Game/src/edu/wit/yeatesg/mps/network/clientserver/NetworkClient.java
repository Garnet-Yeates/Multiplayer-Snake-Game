package edu.wit.yeatesg.mps.network.clientserver;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.management.RuntimeErrorException;

import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.DuplicateClientException;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;

public class NetworkClient
{
	private String identification;
	private ClientListener listener;
	private SocketSecurityTool encrypter;

	public NetworkClient(ClientListener listener, String name)
	{
		encrypter = new SocketSecurityTool(1024);
		this.identification = name;
		this.listener = listener;
	}

	private Socket cs;
	private DataInputStream in;
	private DataOutputStream out;
	
	/**
	 * Attempts to connect this NetworkClient to a Server. The server that this NetworkClient is connecting to should be a Snake
	 * Game server created by another client using this program. Connection Protocol:<br>
	 * -> Send a SnakeUpdatePacket as a connection request.<br>
	 * <- Receive a MessagePacket response saying CONNECTION ACCEPT or an error message.<br>
	 * -> If connection was accepted, send this Client's encryption key to the server as a byte[]. Otherwise return false.<br>
	 * <- Receive a byte[] containing the Server's encryption key<br>
	 * After receiving the Server's encryption key, a new LobbyClient will be created. Once the Lobby is created,
	 * this NetworkClient's {@link ClientListener} will be set to the new LobbyClient (so that all packets received 
	 * from the server will be routed to the LobbyClient), and {@link #startAutoReceiving()} is called from the
	 * LobbyClient constructor. Finally, the method will return true.
	 * @param serverIP the IP address of the server that we are connecting to
	 * @param serverPort the port of the server that we are connecting to
	 * @param isHost whether or not this client is the one hosting the server
	 * @return true if the connection was successful
	 * @throws RuntimeException as an error message that is sent to the ConnectClient to inform the user of the connection failure. 
	 */
	public boolean connect(String serverIP, int serverPort, boolean isHost) throws RuntimeException
	{
		String data;
		try
		{
			cs = new Socket(serverIP, serverPort);
			in = new DataInputStream(cs.getInputStream());
			out = new DataOutputStream(cs.getOutputStream());

			SnakeData thisClientsData = new SnakeData();
			thisClientsData.setName(identification);
			thisClientsData.setIsHost(isHost);

			SnakeUpdatePacket request = new SnakeUpdatePacket(thisClientsData);
			request.setDataStream(out);
			request.send(null);
			
			data = manualReceiveString();
		}
		catch (IOException e)
		{
			String message = e instanceof UnknownHostException ? "Unknown Host" : "Connection Refused";
			throw new RuntimeException(message);
		}
		
		MessagePacket resp = (MessagePacket) Packet.parsePacket(data);

		switch (resp.getMessage())
		{
		case "CONNECTION ACCEPT":
			encrypter.sendPublicKey(out);
			
			byte[] serverKey = manualReceiveBytes();
			encrypter.setPartnerKey(serverKey);
			
			enableEncryptedMode();
			
			new LobbyClient(identification, this, serverPort);
			return true;
		case "GAME ACTIVE":
			throw new RuntimeException("Game already started");
		case "SERVER FULL":
			throw new RuntimeException("Server is full");
		case "NAME TAKEN":
			throw new RuntimeException("Your name is taken");
		}
		return false;
	}
	
	private boolean encryptedMode = false;
	
	/**
	 * Enables encrypted mode, meaning that {@link #manualReceiveBytes()}, {@link #manualReceiveString()},
	 * and {@link ClientListener#onAutoReceive(String)} will expect the incoming data to have been encrypted
	 * using the public key of this NetworkClient's {@link #encrypter}. To share the public key of this
	 * NetworkClient's encryption tool, use {@link SocketSecurityTool#getPublicKey()}.
	 */
	public void enableEncryptedMode()
	{
		encryptedMode = true;
	}
	
	/**
	 * Disables encryption mode, so that relevant methods described in {@link #enableEncryptedMode()} will stop
	 * expecting the incoming data to be encrypted with the public key of NetworkClient's AsymmetricEncryptionTool.
	 * @see #enableEncryptedMode()
	 */
	public void disableEncryptedMode()
	{
		encryptedMode = false;
	}
	
	/**
	 * Manually receives the next array of bytes that is received by {@link #in} from the Socket that this
	 * Client is connected to. When calling this method, keep in mind that the program is assuming that:<br>
	 * a) The ServerSocket is going to first send an integer representing the size of the array of bytes<br>
	 * b) The ServerSocket is going to next send the array of bytes<br>
	 * c) Whether the information is encrypted is equivalent to the value of {@link #encryptedMode}
	 * @return the next byte[] array that was sent by the connected DataInputStream, or null if either of the
	 * above conditions are not met
	 */
	public byte[] manualReceiveBytes()
	{
		try
		{
			byte[] bytes = new byte[in.readInt()];
			in.read(bytes);
			return encryptedMode ? encrypter.decryptBytes(bytes) : bytes;
		}
		catch (Exception e) { return null; }
	}
	
	/**
	 * Manually receives the next String that is received by {@link #in} from the Socket that this
	 * Client is connected to. When calling this method, keep in mind that the program is assuming that:<br>
	 * a) The ServerSocket is going to first send an integer representing the size of the array of bytes of this string<br>
	 * b) The ServerSocket is going to next send the array of bytes created from {@link String#getBytes()} <br>
	 * c) Whether the information is encrypted is equivalent to the value of {@link #encryptedMode}
	 * @return the next byte[] array that was sent by the connected DataInputStream, or null if either of the
	 * above conditions are not met
	 */
	public String manualReceiveString() 
	{
		try
		{
			byte[] bytes = new byte[in.readInt()];
			in.read(bytes);
			return encryptedMode ? encrypter.decryptStringBytes(bytes) : new String(bytes);
		}
		catch (Exception e) { return null; }
	}

	private PacketReceiveThread packetReceiveThread;
	
	/**
	 * This class defines a separate thread that uses a loop to automatically receive packets from {@link #in} and send them
	 * to this NetworkClient's {@link #listener}. Whenever {@link #startAutoReceiving()} is called, a new PacketReceiveThread
	 * is created and {@link #active} is set to true. The thread will continue to send packets to the listener until {@link #active}
	 * is set to false. When {@link #stopAutoReceiving()} is called, {@link #active} is set to false and the current PacketReceiveThread
	 * will end.
	 * @author yeatesg
	 */
	private class PacketReceiveThread extends Thread implements Runnable
	{
		private boolean active = true;
		
		@Override
		public void run()
		{
			while (active)
			{
				try
				{
					byte[] bytes = new byte[in.readInt()]; in.read(bytes);
					String received = encryptedMode ? encrypter.decryptStringBytes(bytes) : new String(bytes);
				System.out.println("\nEncrypted Data:\n " + new String(bytes));
					System.out.println("Decrypted Data:\n" + received + "\n");
					listener.onAutoReceive(received);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		public void setActive(boolean active)
		{
			this.active = active;
		}
	}
	
	/**
	 * This method makes it so that this NetworkClient automatically receives packets on a separate thread and sends
	 * them to the current {@link #listener}. It does this by creating a new {@link PacketReceiveThread} and calling 
	 * {@link Thread#start()}. If a PacketReceiveThread was already running when this method was called, then
	 * {@link #stopAutoReceiving()} will be called.
	 */
	public void startAutoReceiving()
	{
		if (packetReceiveThread != null)
			stopAutoReceiving();
		packetReceiveThread = new PacketReceiveThread();
		packetReceiveThread.start();
	}
	
	/**
	 * Ends the current PacketReceiveThread by calling setActive(false) on the thread. When this is called
	 * packets will stop being processed automatically on a separate thread, and consequentially, packets will no
	 * longer be automatically sent to the {@link #listener}.
	 * 
	 */
	public void stopAutoReceiving()
	{
		if (packetReceiveThread != null)
			packetReceiveThread.setActive(false);
	}
	
	/**
	 * Sets this NetworkClient's {@link #listener} to the given listener. When packets are automatically
	 * received from a {@link PacketReceiveThread}, {@link ClientListener#onAutoReceive(String)} is called
	 * on the current {@link #listener}
	 * @param newListener
	 */
	public void setListener(ClientListener newListener)
	{
		listener = newListener;
	}

	/**
	 * Sets the name of this NetworkClient to the desired name. According to the protocol of the snake
	 * client/server, the NetworkClient's name is used for identification, so there cannot be two client's 
	 * with the same name connected to the server because it will make it impossible for the server to
	 * differentiate between them.
	 * @param name the new name of this client
	 */
	public void setName(String name)
	{
		this.identification = name;		
	}
	
	
	public void receiveEncryptionKey(byte[] keyBytes)
	{
		encrypter.setPartnerKey(keyBytes);
	}

	public void send(Packet p)
	{
		p.setDataStream(out);
		p.send(encrypter);
	}
}