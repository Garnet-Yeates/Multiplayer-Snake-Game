package edu.wit.yeatesg.mps.network.clientserver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import edu.wit.yeatesg.mps.network.clientserver.MPSClient.NotConnectedException;
import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.Snake;

/**
 * The NetworkClient object is meant to be used as an 'internal' client inside
 * of the GUI windows to allow them to interact with a server. Its purpose is to
 * connect to a server according to some hard coded protocol (for example this
 * program has a 4 step protocol) and relay packets that are received from the
 * server to the GUI class that the NetworkClient currently inside of. For
 * example, once a NetworkClient successfully connects to a server and finishes
 * the connection protocol, a pre-game LobbyGUI will open up to show the user
 * who they are about to play with. The only way for the LobbyGUI to send
 * packets to the server is by having a reference to this {@link MPSClient}
 * inside of it. Similarly, the only way for the LobbyGUI to receive packets
 * from the server connected to this NetworkClient is to implement
 * {@link ClientListener} and use
 * {@link MPSClient#setListener(ClientListener)} to set itself as the
 * listener. Once this is established, the GUI that is connected to this
 * NetworkClient can send/receive packets. And whenever that GUI is done being
 * used (i.e when the game starts, a GameplayGUI opens up and the LobbyGUI
 * closes), you can simply use {@link #setListener(ClientListener)} again to set
 * the listener to the new GUI.<br>
 * <br>
 * For reference, the connection protocol that I coded for this Multiplayer
 * Snake Game:<br>
 * -> Send a SnakeUpdatePacket as a connection request.<br>
 * <- Receive a MessagePacket response saying CONNECTION ACCEPT or an error
 * message.<br>
 * -> If connection was accepted, send this Client's encryption key to the
 * server as a byte[]. Otherwise return false.<br>
 * <- Receive a byte[] containing the Server's encryption key<br>
 * 
 * @author yeatesg
 */
public class MPSClient extends SecureSocket
{
	private String identification;
	private ClientListener listener;

	/**
	 * Constructs a new NetworkClient with the desired name to be used for
	 * identification purposes. Upon construction, the {@link #listener} is left as
	 * null so auto receiving packets from the server and routing them to the
	 * listener is not possible (it will throw an exception). The NetworkClient
	 * generally shouldn't have a listener set until it successfully connects to a
	 * server (meaning {@link #attemptConnect(String, int, boolean)} is called and
	 * returns true)
	 */
	public MPSClient(String name)
	{
		super(false);
		this.identification = name;	
	}

	private Socket internal;
	private BufferedInputStream in;
	private BufferedOutputStream out;
	
	/** Represents the public key of some other SocketSecurityTool that this tool is sending encrypted data to */
	private PublicKey serverKey;

	public void setPartnerKey(byte[] encodedKey)
	{
		try
		{
			serverKey = publicKeyFromEncoded(encodedKey);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void tradeKeys() throws NotConnectedException, IOException, DecryptionFailedException
	{
		if (ENCRYPT_ENABLED)
		{
			sendPublicKey(out);
			byte[] serverKey = nextSecureBytes(in);
			setPartnerKey(serverKey);
		}
	}
		
	public void sendPacket(Packet p, boolean encrypting) throws EncryptionFailedException
	{
		sendPacket(p, out, encrypting ? serverKey : null);
	}
	
	public Packet nextSecurePacket() throws DecryptionFailedException, IOException
	{
		return nextPacket(in);
	}
	
	/**
	 * Attempts to connect this NetworkClient to a Server. The server that this
	 * NetworkClient is connecting to should be a Snake Game server created by
	 * another client using this program. Connection Protocol:<br>
	 * -> Send a SnakeUpdatePacket as a connection request.<br>
	 * <- Receive a MessagePacket response saying CONNECTION ACCEPT or an error
	 * message.<br>
	 * -> If connection was accepted, send this Client's encryption key to the
	 * server as a byte[]. Otherwise return false.<br>
	 * <- Receive a byte[] containing the Server's encryption key<br>
	 * After receiving the Server's encryption key, a new LobbyGUI will be created.
	 * Once the Lobby is created, this NetworkClient's {@link ClientListener} will
	 * be set to the new LobbyGUI (so that all packets received from the server will
	 * be routed to the LobbyGUI), and {@link #startAutoReceiving()} is called from
	 * the LobbyGUI constructor. Finally, the method will return true.
	 * 
	 * @param serverIP   the IP address of the server that we are connecting to
	 * @param serverPort the port of the server that we are connecting to
	 * @param isHost     whether or not this client is the one hosting the server
	 * @return true if the connection was successful
	 * @throws RuntimeException as an error message that is sent to the
	 *                          ConnectClient to inform the user of the connection
	 *                          failure.
	 */
	public boolean attemptConnect(String serverIP, int serverPort, boolean isHost) throws ConnectionFailedException, ServerFullException, ActiveGameException, DuplicateNameException
	{
		try
		{
			super.initKeys();
			
			System.out.println("Attempt connect in network clint");

			internal = new Socket(serverIP, serverPort);
			internal.setTcpNoDelay(true);
			in = new BufferedInputStream(internal.getInputStream());
			out = new BufferedOutputStream(internal.getOutputStream());
			
			Snake thisClientsData = new Snake();
			thisClientsData.setName(identification);
			thisClientsData.setIsHost(isHost);
			SnakeUpdatePacket request = new SnakeUpdatePacket(thisClientsData);
			send(request);

			tradeKeys();
			
			MessagePacket resp = (MessagePacket) nextSecurePacket();

			switch (resp.getMessage())
			{
				case "CONNECTION ACCEPT":

					enableEncryptedMode();

					// TODO thing
				//	new LobbyGUI(identification, this, serverPort);
					EventQueue.invokeLater(() -> new LobbyGUI(identification, this, serverPort));
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
		catch (DecryptionFailedException e)
		{
			throw new ConnectionFailedException(e.getMessage());
		} 
	}

	// Checked exceptions for attemptConnect(ip, port, isHost) are below

	/**
	 * This exception is thrown when
	 * {@link MPSClient#attemptConnect(String, int, boolean)} is unable to
	 * connect to the server because either an UnknownHostException or an
	 * IOException.
	 * 
	 * @author yeatesg
	 */
	public static class ConnectionFailedException extends Exception
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
	 * This exception is thrown in the case where this NetworkClient was able to
	 * connect to a server, but the server denied the connection request because
	 * there was already a connected client with the same name.
	 * 
	 * @author yeatesg
	 */
	public static class DuplicateNameException extends Exception
	{
		private static final long serialVersionUID = 1485720276290184742L;

		@Override
		public String getMessage()
		{
			return "Your name is taken";
		}
	}

	/**
	 * This exception is thrown in the case where this NetworkClient was able to
	 * connect to a server, but the server denied the connection request because the
	 * server is full.
	 * 
	 * @author yeatesg
	 */
	public static class ServerFullException extends Exception 
	{
		private static final long serialVersionUID = 3948558930285749302L;

		@Override
		public String getMessage() 
		{
			return "Server is full";
		}
	}

	/**
	 * Checked exception that must be dealt with This exception is thrown in the
	 * case where this NetworkClient was able to connect to a server, but the server
	 * denied the connection request because a game is currently active.
	 * 
	 * @author yeatesg
	 */
	public static class ActiveGameException extends Exception
	{
		private static final long serialVersionUID = 9011662670694108221L;

		@Override
		public String getMessage()
		{
			return "Game already started";
		}
	}

	private boolean sendingEncrypted = false;

	/**
	 * Enables encrypted mode, meaning that {@link #nextSecureBytes()},
	 * {@link #nextSecureString()}, and
	 * {@link ClientListener#onAutoReceive(String)} will expect the incoming data to
	 * have been encrypted using the public key of this NetworkClient's
	 * {@link #encryptor}. To share the public key of this NetworkClient's
	 * encryption tool, use {@link SocketSecurityTool#getPublicKey()}.
	 */
	public void enableEncryptedMode()
	{
		sendingEncrypted = true;
	}

	/**
	 * Disables encryption mode, so that relevant methods described in
	 * {@link #enableEncryptedMode()} will stop expecting the incoming data to be
	 * encrypted with the public key of NetworkClient's AsymmetricEncryptionTool.
	 * 
	 * @see #enableEncryptedMode()
	 */
	public void disableEncryptedMode() 
	{
		sendingEncrypted = false;
	}

	public interface ClientListener
	{
		public void onAutoReceive(Packet received);
	}

	private AutoReceiveThread packetReceiveThread;

	/**
	 * This class defines a separate thread that uses a loop to automatically
	 * receive packets from {@link #in} and send them to this NetworkClient's
	 * {@link #listener}. Whenever {@link #startAutoReceiving()} is called, a new
	 * PacketReceiveThread is created and {@link #active} is set to true. The thread
	 * will continue to send packets to the listener until {@link #active} is set to
	 * false. When {@link #stopAutoReceiving()} is called, {@link #active} is set to
	 * false and the current PacketReceiveThread will end.
	 * 
	 * @author yeatesg
	 */
	private class AutoReceiveThread extends Thread
	{
		private boolean active = true;

		@Override
		public void run() throws ListenerNotFoundException, NotConnectedException
		{
			while (active)
			{
				Packet received;
				try
				{
					System.out.println("NetworkClient waiting for packet...");
					received = nextSecurePacket();
					listener.onAutoReceive(received);
				}
				catch (IOException | DecryptionFailedException e)
				{
					System.out.println(identification + " -> Error auto receiving a String, requesting update");
					MessagePacket updateRequestPacket = new MessagePacket(identification, "UPDATE ME");
					send(updateRequestPacket);
				}
				catch (NullPointerException e)
				{
					if (listener == null)
						throw new ListenerNotFoundException();
					else if (internal == null)
						throw new NotConnectedException();
				}
			}
		}

		public void setActive(boolean active) {
			this.active = active;
		}
	}

	/**
	 * Sets this NetworkClient's {@link #listener} to the given listener. When
	 * packets are automatically received from a {@link AutoReceiveThread},
	 * {@link ClientListener#onAutoReceive(String)} is called on the current
	 * {@link #listener}.
	 * 
	 * @param newListener
	 */
	public void setListener(ClientListener newListener) {
		listener = newListener;
	}

	/**
	 * This method makes it so that this NetworkClient automatically receives
	 * packets on a separate thread and sends them to the current {@link #listener}.
	 * It does this by creating a new {@link AutoReceiveThread} and calling
	 * {@link Thread#start()}. If a PacketReceiveThread was already running when
	 * this method was called, then {@link #stopAutoReceiving()} will be called.
	 */
	public void startAutoReceiving() throws NotConnectedException, ListenerNotFoundException
	{
		if (internal == null)
			throw new NotConnectedException();
		if (listener == null)
			throw new ListenerNotFoundException();
		stopAutoReceiving();
		packetReceiveThread = new AutoReceiveThread();
		packetReceiveThread.start();
	}

	/**
	 * Ends the current PacketReceiveThread by calling setActive(false) on the
	 * thread. When this is called packets will stop being processed automatically
	 * on a separate thread, and consequentially, packets will no longer be
	 * automatically sent to the {@link #listener}.
	 */
	public void stopAutoReceiving()
	{
		if (packetReceiveThread != null)
			packetReceiveThread.setActive(false);
	}


	/**
	 * Sets the name of this NetworkClient to the desired name. According to the
	 * protocol of the snake client/server, the NetworkClient's name is used for
	 * identification, so there cannot be two clients with the same name connected
	 * to the server because it will make it impossible for the server to
	 * differentiate between them.
	 * 
	 * @param name the new name of this client.
	 */
	public void setName(String name)
	{
		this.identification = name;
	}

	/**
	 * Sends the desired packet to the Socket that this NetworkClient is connected
	 * to.
	 * 
	 * @param p the packet that is being sent.
	 */
	public synchronized void send(Packet p) throws NotConnectedException
	{
		if (internal == null)
			throw new NotConnectedException();
		try
		{
			sendPacket(p, sendingEncrypted);
		}
		catch (EncryptionFailedException e)
		{
			System.out.println("Packet Send Failed");
			e.printStackTrace();
			System.exit(0);
		}
	}

	// UNCHECKED EXCEPTIONS BELOW (SHOULDN'T HAVE TO BE HANDLED, IF THESE ARE THROWN
	// THERE IS SOMETHING WRONG WITH MY CODE)

	/**
	 * This unchecked exception is thrown in the case where this NetworkClient tried
	 * to call {@link MPSClient#startAutoReceiving()}, or a PacketReceiveThread
	 * tried to auto-receive a packet, but there is no current
	 * {@link ClientListener} to send the packet to.
	 * 
	 * @author yeatesg
	 */
	public static class ListenerNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 7795590181284875760L;

		@Override
		public String getMessage() {
			return "Auto-Receive failed! Make sure this NetworkClient has a listener before auto receiving packets.";
		}
	}

	/**
	 * This unchecked exception is thrown whenever the user tries to send or receive
	 * packets from a Server when they are not yet connected to it (socket, in, and
	 * out are all null).
	 * 
	 * @author yeatesg
	 */
	public static class NotConnectedException extends RuntimeException {
		private static final long serialVersionUID = 5167690960624627277L;

		@Override
		public String getMessage() {
			return "Cannot send packets until connected to a server!";
		}
	}

}
