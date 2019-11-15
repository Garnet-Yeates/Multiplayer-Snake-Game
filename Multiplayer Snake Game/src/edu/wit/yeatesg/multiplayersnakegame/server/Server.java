package edu.wit.yeatesg.multiplayersnakegame.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.MessagePacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.Packet;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.SnakeUpdatePacket;

public class Server
{
	private static final Random R = new Random();

	private ServerSocket ss;

	private String serverName;

	private SnakeListForServer connectedClients;
		
	private int port;
	
	public Server(int port)
	{	
		this.port = port;
		serverName = "Server";
		connectedClients = new SnakeListForServer();
	}
	
	private boolean isRunning = true;
	
	public boolean isRunning()
	{
		return isRunning;
	}
	
	public void startServer()
	{
		try
		{
			ss = new ServerSocket(port);

			Thread t = new Thread(() ->  // Run in different Thread so it doesn't freeze at the while loop. If we didn't do
			{                            // this then whatever thread calls startServer() will freeze until the server closes
				while (true)             
				{                        
					try              	 
					{
						final Socket s = ss.accept(); // Handle clients in separate threads to allow for multi connecting
						Thread packetReceiveThread = new Thread(new PacketReceiveThread(s));
						packetReceiveThread.start();
					}
					catch (Exception e)
					{
						break;
					}

				}
			});
			t.start();
		}
		catch (IOException e)
		{
			isRunning = false;
		}
	}
	
	/**
	 * Every time a new Client is connected, a new thread is created with this as the Runnable. The
	 * reason for using multi-threading is so the server can handle multiple client connections at once
	 * (otherwise the server would only be able to communicate with one client at a time because when a
	 * socket is waiting for a message the code is trapped/interrupted
	 * @author yeatesg
	 *
	 */
	class PacketReceiveThread implements Runnable
	{
		private boolean active = true;
		private Socket s;
		private SnakeDataForServer client;

		public PacketReceiveThread(Socket s)
		{
			this.s = s;
		}

		@Override
		public void run()
		{
			try
			{
				DataInputStream inputStream = new DataInputStream(s.getInputStream());
				DataOutputStream outputStream = new DataOutputStream(s.getOutputStream());

				SnakeUpdatePacket clientRequestPacket = (SnakeUpdatePacket) Packet.parsePacket(inputStream.readUTF());
				SnakeData data = clientRequestPacket.getClientData();
				String clientName = data.getClientName();
				SnakeDataForServer justJoinedClientData = new SnakeDataForServer(data, s, outputStream); 
				
				justJoinedClientData.setIsHost(connectedClients.size() == 0);
				boolean isFull = connectedClients.size() == 4;
				boolean clientNameTaken = connectedClients.contains(clientName);				
				
				if (!isFull && !clientNameTaken)
				{	
					MessagePacket accepted = new MessagePacket(serverName, "CONNECTION_ACCEPT");
					accepted.setDataStream(justJoinedClientData.getOutputStream());
					accepted.send(); // After this is received, the client's while loop will start
					
					// Send the ClientData of the clients that were already connected to the newly connected client
					for (SnakeData client : connectedClients)
					{
						SnakeUpdatePacket pack = new SnakeUpdatePacket(client);
						pack.setDataStream(justJoinedClientData.getOutputStream());
						pack.send();
						MessagePacket notifyJoin = new MessagePacket(client.getClientName(), "SOMEONE_JOIN");
						notifyJoin.setDataStream(justJoinedClientData.getOutputStream());
						notifyJoin.send();
					}					
					
					connectedClients.add(justJoinedClientData);

					// Send the ClientData of the newly connected client to all of the connected clients (including the new client itself)
					SnakeUpdatePacket updatePack = new SnakeUpdatePacket(justJoinedClientData);
					updatePack.sendMultiple(connectedClients.getAllOutputStreams());

					// Notify all clients that someone joined so the lobby can be updated properly
					MessagePacket notifyJoin = new MessagePacket(justJoinedClientData.getClientName(), "SOMEONE_JOIN");
					notifyJoin.sendMultiple(connectedClients.getAllOutputStreams());
					
					while (active) // Constantly wait for new packets from this client on this separate Thread
						onPacketReceive(Packet.parsePacket(inputStream.readUTF()));
				}
				else if (isFull)
				{
					ErrorPacket responsePacket = new ErrorPacket("This server is full");
					responsePacket.setDataStream(outputStream);
					responsePacket.send();
				}
				else if (clientNameTaken)
				{
					ErrorPacket responsePacket = new ErrorPacket("Your name is taken");
					responsePacket.setDataStream(outputStream);
					responsePacket.send();
				}
			}
			catch (IOException e)
			{
				System.out.println("IOException for client named " + client + ". Removing from server");
				connectedClients.remove(client);
			}
		}
		public void stopRunning()
		{
			active = false;
		}
	}

	private void onPacketReceive(Packet packet)
	{
		if (packet instanceof MessagePacket)
		{	
			MessagePacket msgPacket = (MessagePacket) packet;
			SnakeDataForServer sender = (SnakeDataForServer) connectedClients.get(msgPacket.getSender());
			switch (msgPacket.getMessage())
			{
			case "START_GAME":
				if (sender.isHost())
				onGameStart();
				break;
			case "I_EXIT":
				onClientQuit(sender);
				break;
			}
		}
		else if (packet instanceof SnakeUpdatePacket)
		{
			onReceiveClientDataUpdate((SnakeUpdatePacket) packet);
		}
	}
	
	private void onGameStart()
	{
		// TODO implement
		// The inputstream is now going to be connected to a client, not the lobbyGUIs. So send a clientdatlistupdate
		// to all the fresh clients so they know what to do
	}
	
	private void tick()
	{
		
	}
	
	private void onClientQuit(SnakeDataForServer quitter)
	{
		if (quitter.isHost())
		{
			MessagePacket response = new MessagePacket(quitter.getClientName(), "YOU_EXIT");
			response.sendMultiple(connectedClients.getAllOutputStreams());
	
			closeAllConnections();
			System.exit(0);
		}
		else
		{
			MessagePacket response = new MessagePacket(quitter.getClientName(), "YOU_EXIT");
			response.setDataStream(quitter.getOutputStream());
			response.send();
			
			closeConnection(quitter);			
			
			MessagePacket responseToOthers = new MessagePacket(quitter.getClientName(), "THEY_EXIT");
			responseToOthers.sendMultiple(connectedClients.getAllOutputStreams());
		}
	}
	
	private void onReceiveClientDataUpdate(SnakeUpdatePacket updatePacket)
	{
		// Update the client's info on the server
		connectedClients.updateBasedOn(updatePacket);
		
		// Bounce packet back to all clients
		updatePacket.sendMultiple(connectedClients.getAllOutputStreams());
	}
	
	private void closeConnection(SnakeDataForServer exiting)
	{
		try
		{
			exiting.getSocket().close();
		}
		catch (IOException e) { }
		connectedClients.remove(exiting);
	}
	
	private void closeAllConnections()
	{
		ArrayList<SnakeDataForServer> copy = new ArrayList<>();
		for (SnakeData dat : connectedClients) // Avoid ConcurrentModificationException here
			copy.add((SnakeDataForServer) dat);
		for (SnakeDataForServer dat : copy)
			closeConnection(dat);
	}
}