package edu.wit.yeatesg.mps.network.clientserver;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.management.RuntimeErrorException;

import edu.wit.yeatesg.mps.network.packets.AsymmetricEncryptionTool;
import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.DuplicateClientException;
import edu.wit.yeatesg.mps.otherdatatypes.ServerFullException;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;

public class NetworkClient implements Runnable
{
	private String name;
	private ClientListener listener;
	private DataInputStream in;
	private DataOutputStream out;
	private Socket cs;
	private AsymmetricEncryptionTool encrypter;

	public NetworkClient(ClientListener listener, String name)
	{
		encrypter = new AsymmetricEncryptionTool(1024);
		this.name = name;
		this.listener = listener;
	}

	public boolean connect(String serverIP, int serverPort, boolean isHost) throws RuntimeException
	{
		String data;
		try
		{
			cs = new Socket(serverIP, serverPort);
			in = new DataInputStream(cs.getInputStream());
			out = new DataOutputStream(cs.getOutputStream());

			SnakeData thisClientsData = new SnakeData();
			thisClientsData.setName(name);
			thisClientsData.setIsHost(isHost);

			SnakeUpdatePacket request = new SnakeUpdatePacket(thisClientsData);
			request.setDataStream(out);
			request.send(encrypter);
			System.out.println("sent req");
			
			byte[] bytes = new byte[in.readInt()];
			in.read(bytes);
			data = new String(bytes);
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
			
			encrypter.sendSharableKey(out);
			
			byte[] serverKey = null;
			try
			{
				serverKey = new byte[in.readInt()];
				in.read(serverKey);
			}
			catch (Exception e) { System.out.println("Server closed during connection accept with NetworkClient"); return false; }
			encrypter.setEncryptionKey(serverKey);
			encrypter.setConnectionEstablished(true);
			
			new LobbyClient(name, this, serverPort);
			startAutoReceiving();
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

	private void startAutoReceiving()
	{
		Thread clientThread = new Thread(this);
		clientThread.start();
	}

	public void setListener(ClientListener newListener)
	{
		listener = newListener;
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				byte[] bytes = new byte[in.readInt()]; in.read(bytes);
				String received = encrypter != null && encrypter.isConnectionEstablished() ? encrypter.decrypt(bytes) : new String(bytes);
				System.out.println("\nEncrypted Data:\n " + new String(bytes));
				System.out.println("Decrypted Data:\n" + received + "\n");
				listener.onReceive(received);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void setName(String name)
	{
		this.name = name;		
	}
	
	public void setEncryptionKey(byte[] keyBytes)
	{
		encrypter.setEncryptionKey(keyBytes);
	}

	public void send(Packet p)
	{
		p.setDataStream(out);
		p.send(encrypter);
	}
}