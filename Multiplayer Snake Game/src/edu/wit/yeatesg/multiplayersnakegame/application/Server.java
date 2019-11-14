package edu.wit.yeatesg.multiplayersnakegame.application;

import java.awt.EventQueue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientDataSet;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.Color;
import edu.wit.yeatesg.multiplayersnakegame.packets.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.packets.MessagePacket;
import edu.wit.yeatesg.multiplayersnakegame.packets.Packet;
import edu.wit.yeatesg.multiplayersnakegame.packets.UpdateAllClientsPacket;
import edu.wit.yeatesg.multiplayersnakegame.packets.UpdateSingleClientPacket;

public class Server
{
	private static final Random R = new Random();
	private static final long serialVersionUID = -1455481211578890895L;

	private ServerSocket ss;

	private String serverName;

	private ClientDataSet connectedSnakes;
		
	private int port;
	
	public Server(int port)
	{	
		this.port = port;
		serverName = "Server";
		connectedSnakes = new ClientDataSet();
	}
	
	public void startServer() throws IOException
	{
		ss = new ServerSocket(port);
		
		Thread t = new Thread(() ->  // Open socket on another thread because we don't want to freeze 
		{                            // the code in the ConnectGUI where startServer() was originally 
			while (true)             // called. If we don't do this, then the startFailed field in
			{                        // ServerStartRunnable will always be null. This will freeze the
				                     // program
				try           
				{
					final Socket s = ss.accept(); // Handle clients in separate threads to allow for multi connecting
					Thread packetReceiveThread = new Thread(new PacketReceiveThread(s));
					packetReceiveThread.start();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

			}
		});
		t.start();
	}

	static class ClientDataForServer extends ClientData
	{
		private Socket socket;
		private DataOutputStream outputStream;
		
		public ClientDataForServer(ClientData parent, Socket socket, DataOutputStream outputStream)
		{
			super(parent.getClientName(),
					parent.getColor(),
					parent.getDirection(),
					parent.getHeadLocation(),
					parent.getNextHeadLocation(),
					parent.isHost());
			this.socket = socket;
			this.outputStream = outputStream;
		}
		
		public ClientDataForServer(ClientData parent)
		{
			this(parent, null, null);
		}
		
		public Socket getSocket()
		{
			return socket;
		}
		
		public void setSocket(Socket s)
		{
			socket = s;
		}
		
		public DataOutputStream getOutputStream()
		{
			return outputStream;
		}
		
		public void setOutputStream(DataOutputStream os)
		{
			outputStream = os;
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
		private ClientDataForServer client;

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

				UpdateSingleClientPacket clientRequestPacket = (UpdateSingleClientPacket) Packet.parsePacket(inputStream.readUTF());
				ClientData data = clientRequestPacket.getClientData();
				String clientName = data.getClientName();
				ClientDataForServer serverSideClientData = new ClientDataForServer(data, s, outputStream); 
				
				boolean isEmpty = connectedSnakes.size() == 0;
				boolean isFull = connectedSnakes.size() == 4;
				boolean clientNameTaken = connectedSnakes.contains(clientName);
				
				if (!isFull && !clientNameTaken)
				{
					connectedSnakes.add(serverSideClientData);
					
					MessagePacket accepted = new MessagePacket(serverName, "CONNECTION_ACCEPT");
					accepted.setDataStream(serverSideClientData.getOutputStream());
					accepted.send();
					
					// At this point the client's while loop has started
					
					if (isEmpty) // If they are the first connected client, they are now the host
						serverSideClientData.setIsHost(true);
					
					// Send new connectedSnakes list to all clients so they know who is connected
					UpdateAllClientsPacket dataListUpdate = new UpdateAllClientsPacket(connectedSnakes);
					dataListUpdate.sendMultiple(getAllOutputStreams());
					
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
			catch (Exception e)
			{
				connectedSnakes.remove(client);
			}	
		}

		public void stopRunning()
		{
			active = false;
		}
	}
	
	private ArrayList<DataOutputStream> getAllOutputStreams()
	{
		ArrayList<DataOutputStream> osList = new ArrayList<>();
		for (ClientData data : connectedSnakes)
			osList.add(((ClientDataForServer) data).getOutputStream());
		return osList;
	}

	private void onPacketReceive(Packet packet)
	{
		if (packet instanceof MessagePacket)
		{	
			MessagePacket msgPacket = (MessagePacket) packet;
			ClientDataForServer sender = (ClientDataForServer) connectedSnakes.get(msgPacket.getSender());
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
		else if (packet instanceof UpdateSingleClientPacket)
		{
			onReceiveClientDataUpdate((UpdateSingleClientPacket) packet);
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
	
	private void onClientQuit(ClientDataForServer quitter)
	{
		if (quitter.isHost())
		{
			MessagePacket response = new MessagePacket(quitter.getClientName(), "YOU_EXIT");
			response.sendMultiple(getAllOutputStreams());
	
			closeAllConnections();
			System.exit(0);
		}
		else
		{
			MessagePacket response = new MessagePacket(quitter.getClientName(), "YOU_EXIT");
			response.setDataStream(quitter.getOutputStream());
			response.send();

			closeConnection(quitter);
			
			// Update other clients' list of connected snakes
			UpdateAllClientsPacket updatePacket = new UpdateAllClientsPacket(connectedSnakes);
			updatePacket.sendMultiple(getAllOutputStreams());
			
		}
	}
	
	private void onReceiveClientDataUpdate(UpdateSingleClientPacket updatePacket)
	{
		// Update the client's info on the server
		connectedSnakes.updateBasedOn(updatePacket);
		
		// Update other clients' list of connected snakes 
		UpdateAllClientsPacket updateClients = new UpdateAllClientsPacket(connectedSnakes);
		updateClients.sendMultiple(getAllOutputStreams());
	}
	
	private void closeConnection(ClientDataForServer exiting)
	{
		try
		{
			exiting.getSocket().close();
		}
		catch (IOException e) { }
		connectedSnakes.remove(exiting);
	}
	
	private void closeAllConnections()
	{
		ArrayList<ClientDataForServer> copy = new ArrayList<>();
		for (ClientData dat : connectedSnakes) // Avoid ConcurrentModificationException here
			copy.add((ClientDataForServer) dat);
		for (ClientDataForServer dat : copy)
			closeConnection(dat);
	}
}