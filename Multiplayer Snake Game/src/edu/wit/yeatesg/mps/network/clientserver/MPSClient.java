package edu.wit.yeatesg.mps.network.clientserver;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.Snake;

public class MPSClient extends SecureSocket
{
	private String identification;

	public MPSClient(String name)
	{
		super(false);
		this.identification = name;	
	}

	public String getName()
	{
		return identification;
	}

	private Socket internal;
	private BufferedInputStream in;
	private BufferedOutputStream out;

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

	private ClientListener listener;

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

		public void setActive(boolean active) 
		{
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
	public void setListener(ClientListener newListener) 
	{
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
	public static class ListenerNotFoundException extends RuntimeException
	{
		private static final long serialVersionUID = 7795590181284875760L;

		@Override
		public String getMessage()
		{
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
	public static class NotConnectedException extends RuntimeException
	{
		private static final long serialVersionUID = 5167690960624627277L;

		@Override
		public String getMessage()
		{
			return "Cannot send packets until connected to a server!";
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
}
