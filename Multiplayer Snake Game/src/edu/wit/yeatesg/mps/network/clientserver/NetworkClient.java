package edu.wit.yeatesg.mps.network.clientserver;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;

/**
 * The NetworkClient object is meant to be used as an 'internal' client inside of the GUI windows to allow them to
 * interact with a server. Its purpose is to connect to a server according to some hard coded protocol (for example
 * this program has a 4 step protocol) and relay packets that are received from the server to the GUI class that the
 * NetworkClient currently inside of. For example, once a NetworkClient successfully connects to a server and
 * finishes the connection protocol, a pre-game LobbyGUI will open up to show the user who they are about to
 * play with. The only way for the LobbyGUI to send packets to the server is by having a reference to this
 * {@link NetworkClient} inside of it. Similarly, the only way for the LobbyGUI to receive packets from the server
 * connected to this NetworkClient is to implement {@link ClientListener} and use 
 * {@link NetworkClient#setListener(ClientListener)} to set itself as the listener. Once this is established,
 * the GUI that is connected to this NetworkClient can send/receive packets. And whenever that GUI is done being
 * used (i.e when the game starts, a GameplayGUI opens up and the LobbyGUI closes), you can simply use
 * {@link #setListener(ClientListener)} again to set the listener to the new GUI.<br><br>
 * For reference, the connection protocol that I coded for this Multiplayer Snake Game:<br>
 * -> Send a SnakeUpdatePacket as a connection request.<br>
 * <- Receive a MessagePacket response saying CONNECTION ACCEPT or an error message.<br>
 * -> If connection was accepted, send this Client's encryption key to the server as a byte[]. Otherwise return false.<br>
 * <- Receive a byte[] containing the Server's encryption key<br>
 * @author yeatesg
 */
public class NetworkClient
{
	private String identification;
	private ClientListener listener;
	private SocketSecurityTool encrypter;

	/**
	 * Constructs a new NetworkClient with the desired name to be used for identification purposes.
	 * Upon construction, the {@link #listener} is left as null so auto receiving packets from the server
	 * and routing them to the listener is not possible (it will throw an exception). The NetworkClient generally shouldn't have a listener set until it
	 * successfully connects to a server (meaning {@link #attemptConnect(String, int, boolean)} is called
	 * and returns true)
	 */
	public NetworkClient(String name)
	{
		encrypter = new SocketSecurityTool(1024);
		this.identification = name;
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
	 * After receiving the Server's encryption key, a new LobbyGUI will be created. Once the Lobby is created,
	 * this NetworkClient's {@link ClientListener} will be set to the new LobbyGUI (so that all packets received 
	 * from the server will be routed to the LobbyGUI), and {@link #startAutoReceiving()} is called from the
	 * LobbyGUI constructor. Finally, the method will return true.
	 * @param serverIP the IP address of the server that we are connecting to
	 * @param serverPort the port of the server that we are connecting to
	 * @param isHost whether or not this client is the one hosting the server
	 * @return true if the connection was successful
	 * @throws RuntimeException as an error message that is sent to the ConnectClient to inform the user of the connection failure. 
	 */
	public boolean attemptConnect(String serverIP, int serverPort, boolean isHost) throws ServerFullException, ActiveGameException, DuplicateNameException
	{
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
			request.write(null);

			String data = manualReceiveString();
			MessagePacket resp = (MessagePacket) Packet.parsePacket(data);

			switch (resp.getMessage())
			{
			case "CONNECTION ACCEPT":
				encrypter.sendPublicKey(out);

				byte[] serverKey = manualReceiveBytes();
				encrypter.setPartnerKey(serverKey);

				enableEncryptedMode();

				new LobbyGUI(identification, this, serverPort);
				return true;
			case "GAME ACTIVE":
				throw new ActiveGameException();
			case "SERVER FULL":
				throw new ServerFullException();
			case "NAME TAKEN":
				throw new DuplicateNameException();
			}
			return false;

		}
		catch (IOException e)
		{
			String message = e instanceof UnknownHostException ? "Unknown Host" : "Connection Refused";
			throw new ConnectionFailedException(message);
		}
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
	 * c) Whether it is expecting encrypted information is equivalent to the value of {@link #encryptedMode}
	 * @return the next byte[] array that was sent by the connected DataInputStream, or null if either of the
	 * above conditions are not met
	 */
	public byte[] manualReceiveBytes() throws NotConnectedException, SocketClosedException
	{
		try
		{
			byte[] bytes = new byte[in.readInt()];
			in.read(bytes);
			byte[] received = encryptedMode ? encrypter.decryptBytes(bytes) : bytes;
			return received;
		}
		catch (NullPointerException e) // Only catch the IOException, if a DecryptionFailedException is thrown, I coded something wrong and the program will crash
		{ 
			throw new NotConnectedException();
		}
		catch (IOException e)
		{
			throw new SocketClosedException();
		}
	}

	/**
	 * Manually receives the next String that is received by {@link #in} from the Socket that this
	 * Client is connected to. When calling this method, keep in mind that the program is assuming that:<br>
	 * a) The ServerSocket is going to first send an integer representing the size of the array of bytes of this string<br>
	 * b) The ServerSocket is going to next send the array of bytes created from {@link String#getBytes()} <br>
	 * c) Whether it is expecting encrypted information is equivalent to the value of {@link #encryptedMode}
	 * @return the next byte[] array that was sent by the connected DataInputStream, or null if either of the
	 * above conditions are not met
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidKeyException 
	 * @see {@link Server} to see the protocol from the server-side
	 */
	public String manualReceiveString() 
	{
		byte[] stringBytes = manualReceiveBytes();
		String received = encryptedMode ? encrypter.decryptString(stringBytes) : new String(stringBytes);
		return received;
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
		public void run() throws ListenerNotFoundException, NotConnectedException, SocketClosedException
		{
			while (active)
			{
				try
				{
					byte[] bytes = new byte[in.readInt()]; in.read(bytes);
					String received = encryptedMode ? encrypter.decryptString(bytes) : new String(bytes);
					System.out.println("\nEncrypted Data:\n " + new String(bytes));
					System.out.println("Decrypted Data:\n" + received + "\n");
					listener.onAutoReceive(received);
				}
				catch (NullPointerException e)
				{
					if (listener == null)
						throw new ListenerNotFoundException();
					else if (in == null)
						throw new NotConnectedException();
				}
				catch (IOException e)
				{
					throw new SocketClosedException();
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
	public void startAutoReceiving() throws NotConnectedException, ListenerNotFoundException
	{
		if (in == null)
			throw new NotConnectedException();
		if (listener == null)
			throw new ListenerNotFoundException();
		if (packetReceiveThread != null)
			stopAutoReceiving();
		packetReceiveThread = new PacketReceiveThread();
		packetReceiveThread.start();
	}

	/**
	 * Ends the current PacketReceiveThread by calling setActive(false) on the thread. When this is called
	 * packets will stop being processed automatically on a separate thread, and consequentially, packets will no
	 * longer be automatically sent to the {@link #listener}.
	 */
	public void stopAutoReceiving()
	{
		if (packetReceiveThread != null)
			packetReceiveThread.setActive(false);
	}

	/**
	 * Sets this NetworkClient's {@link #listener} to the given listener. When packets are automatically
	 * received from a {@link PacketReceiveThread}, {@link ClientListener#onAutoReceive(String)} is called
	 * on the current {@link #listener}.
	 * @param newListener
	 */
	public void setListener(ClientListener newListener)
	{
		listener = newListener;
	}

	/**
	 * Sets the name of this NetworkClient to the desired name. According to the protocol of the snake
	 * client/server, the NetworkClient's name is used for identification, so there cannot be two clients 
	 * with the same name connected to the server because it will make it impossible for the server to
	 * differentiate between them.
	 * @param name the new name of this client.
	 */
	public void setName(String name)
	{
		this.identification = name;		
	}

	/**
	 * Sends the desired packet to the Socket that this NetworkClient is connected to.
	 * @param p the packet that is being sent.
	 */
	public void send(Packet p) throws NotConnectedException
	{
		if (out == null)
			throw new NotConnectedException();
		p.setDataStream(out);
		p.write(encrypter);
	}
}
/**
 * This exception is thrown whenever the user tries to send or receive packets from a Server when they are
 * not yet connected to it (socket, in, and out are all null).
 * @author yeatesg
 */
class NotConnectedException extends RuntimeException
{
	private static final long serialVersionUID = 5167690960624627277L;

	@Override
	public String getMessage()
	{
		return "Cannot send packets until connected to a server!";
	}
}
/**
 * This exception is thrown when {@link NetworkClient#attemptConnect(String, int, boolean)} is unable to connect to
 * the server because either an UnknownHostException or an IOException.
 * @author yeatesg
 */
class ConnectionFailedException extends RuntimeException
{
	private static final long serialVersionUID = 4938204820624938277L;

	private String message;

	public ConnectionFailedException(String message)
	{
		this.message = message;
	}

	@Override
	public String getMessage()
	{
		return message;
	}
}
/**
 * This exception is thrown in the case where this NetworkClient was able to connect to a server, but the server
 * denied the connection request because there was already a connected client with the same name.
 * @author yeatesg
 */
class DuplicateNameException extends RuntimeException
{
	private static final long serialVersionUID = 1485720276290184742L;

	@Override
	public String getMessage()
	{
		return "Your name is taken";
	}
}
/**
 * This exception is thrown in the case where this NetworkClient was able to connect to a server, but the server
 * denied the connection request because the server is full.
 * @author yeatesg
 */
class ServerFullException extends RuntimeException
{
	private static final long serialVersionUID = 3948558930285749302L;

	@Override
	public String getMessage()
	{
		return "Server is full";
	}
}
/**
 * This exception is thrown in the case where this NetworkClient was able to connect to a server, but the server
 * denied the connection request because a game is currently active.
 * @author yeatesg
 */
class ActiveGameException extends RuntimeException
{
	private static final long serialVersionUID = 9011662670694108221L;

	@Override
	public String getMessage()
	{
		return "Game already started";
	}
}
/**
 * This exception is thrown in the case where this NetworkClient tried to call {@link NetworkClient#startAutoReceiving()}, or
 * a PacketReceiveThread tried to auto-receive a packet but there is no current {@link ClientListener} to send the packet to.
 * @author yeatesg
 */
class ListenerNotFoundException extends RuntimeException
{
	private static final long serialVersionUID = 7795590181284875760L;

	@Override
	public String getMessage()
	{
		return "Auto-Receive failed! Make sure this NetworkClient has a listener before auto receiving packets.";
	}
}
/**
 * This exception is thrown in the case where this NetworkClient tries to send/receive data from the socket but the socket
 * was closed and threw an IOException.
 * @author yeatesg
 */
class SocketClosedException extends RuntimeException
{
	private static final long serialVersionUID = 6067006771379735990L;
	
	@Override
	public String getMessage()
	{
		return "Tried to read/write information to the connected Socket but it was been closed";
	}
}
