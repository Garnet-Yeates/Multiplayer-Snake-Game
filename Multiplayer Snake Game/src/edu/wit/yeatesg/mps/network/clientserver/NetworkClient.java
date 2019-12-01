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
import edu.wit.yeatesg.mps.otherdatatypes.ServerFullException;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;

public class NetworkClient implements Runnable
{
	private String name;
	private ClientListener listener;
	private DataInputStream in;
	private DataOutputStream out;
	private Socket cs;

	public NetworkClient(ClientListener listener, String name)
	{
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
			request.send();
			
			data = in.readUTF();
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
		listener.setOutputStream(out);
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				String data = in.readUTF();
				listener.onReceive(data);
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


}