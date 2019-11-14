package edu.wit.yeatesg.multiplayersnakegame.application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientDataSet;
import edu.wit.yeatesg.multiplayersnakegame.packets.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.packets.MessagePacket;
import edu.wit.yeatesg.multiplayersnakegame.packets.Packet;
import edu.wit.yeatesg.multiplayersnakegame.packets.UpdateSingleClientPacket;

public class Server
{
	private static final Random R = new Random();
	private static final long serialVersionUID = -1455481211578890895L;

	private ServerSocket ss;

	private String serverName;

	private ClientDataSet connectedClients;
		
	private int port;
	
	public Server(int port)
	{	
		this.port = port;
		serverName = "Server";
		connectedClients = new ClientDataSet();
	}
	
	public void startServer() throws IOException
	{
		ss = new ServerSocket(port);
		
		Thread t = new Thread(() ->  // Open socket on another thread because we don't want to freeze 
		{                            // the code in the ConnectGUI where startServer() was originally 
			while (true)             // called. If we don't do this, then the startFailed field in
			{                        // ServerStartRunnable will always be null. This will freeze the
				try              	 // program because attemptConnect() needs startFailed to be != null
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
				ClientDataForServer justJoinedClientData = new ClientDataForServer(data, s, outputStream); 
				
				justJoinedClientData.setIsHost(connectedClients.size() == 0);
				boolean isFull = connectedClients.size() == 4;
				boolean clientNameTaken = connectedClients.contains(clientName);				
				
				if (!isFull && !clientNameTaken)
				{	
					MessagePacket accepted = new MessagePacket(serverName, "CONNECTION_ACCEPT");
					accepted.setDataStream(justJoinedClientData.getOutputStream());
					accepted.send(); // After this is received, the client's while loop will start
					
					// Send the ClientData of the clients that were already connected to the newly connected client
					for (ClientData client : connectedClients)
					{
						UpdateSingleClientPacket pack = new UpdateSingleClientPacket(client);
						pack.setDataStream(justJoinedClientData.getOutputStream());
						pack.send();
						MessagePacket notifyJoin = new MessagePacket(client.getClientName(), "SOMEONE_JOIN");
						notifyJoin.setDataStream(justJoinedClientData.getOutputStream());
						notifyJoin.send();
					}					
					
					connectedClients.add(justJoinedClientData);

					// Send the ClientData of the newly connected client to all of the connected clients (including the new client itself)
					UpdateSingleClientPacket updatePack = new UpdateSingleClientPacket(justJoinedClientData);
					updatePack.sendMultiple(getAllOutputStreams());

					// Notify all clients that someone joined so the lobby can be updated properly
					MessagePacket notifyJoin = new MessagePacket(justJoinedClientData.getClientName(), "SOMEONE_JOIN");
					notifyJoin.sendMultiple(getAllOutputStreams());
					
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
				connectedClients.remove(client);
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
		for (ClientData data : connectedClients)
			osList.add(((ClientDataForServer) data).getOutputStream());	
		return osList;
	}

	private void onPacketReceive(Packet packet)
	{
		if (packet instanceof MessagePacket)
		{	
			MessagePacket msgPacket = (MessagePacket) packet;
			ClientDataForServer sender = (ClientDataForServer) connectedClients.get(msgPacket.getSender());
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
			
			MessagePacket responseToOthers = new MessagePacket(quitter.getClientName(), "THEY_EXIT");
			responseToOthers.sendMultiple(getAllOutputStreams());
		}
	}
	
	private void onReceiveClientDataUpdate(UpdateSingleClientPacket updatePacket)
	{
		ArrayList<ClientDataForServer> oldList = new ArrayList<>();
		for (ClientData data : connectedClients)
			oldList.add((ClientDataForServer) data);
		
		// Update the client's info on the server
		connectedClients.updateBasedOn(updatePacket);
		
		for (int i = 0; i < connectedClients.size(); i++)
		{
			ClientDataForServer serverSideClientData = new ClientDataForServer(connectedClients.get(i));
			serverSideClientData.setSocket(oldList.get(i).getSocket());
			serverSideClientData.setOutputStream(oldList.get(i).getOutputStream());
			connectedClients.set(i, serverSideClientData);
		}
		
		// Bounce packet back to all clients
		updatePacket.sendMultiple(getAllOutputStreams());
	}
	
	private void closeConnection(ClientDataForServer exiting)
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
		ArrayList<ClientDataForServer> copy = new ArrayList<>();
		for (ClientData dat : connectedClients) // Avoid ConcurrentModificationException here
			copy.add((ClientDataForServer) dat);
		for (ClientDataForServer dat : copy)
			closeConnection(dat);
	}
}