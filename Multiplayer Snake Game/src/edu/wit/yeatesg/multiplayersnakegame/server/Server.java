package edu.wit.yeatesg.multiplayersnakegame.server;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.sound.midi.Receiver;
import javax.swing.Timer;
import javax.swing.plaf.SliderUI;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.Point;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.PointList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.InitiateGamePacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.MessagePacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.Packet;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.PacketListener;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.PacketReceiver;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.SnakeUpdatePacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.TickPacket;
import edu.wit.yeatesg.multiplayersnakegame.phase2play.Client;

public class Server implements ActionListener, PacketListener
{
	private static final Random R = new Random();

	private ServerSocket ss;

	private String serverName;

	private SnakeList connectedClients;
		
	private int port;
	
	public Server(int port)
	{	
		this.port = port;
		serverName = "Server";
		connectedClients = new SnakeList();
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
	
	private PacketReceiver receiver;
	
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
		private SnakeData client;

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
				
				receiver = new PacketReceiver(inputStream, outputStream, Server.this);
				receiver.setReceiving("Serer");
				
				receiver.manualReceive();
				SnakeUpdatePacket clientRequestPacket = (SnakeUpdatePacket) receiver.manualConsumePacket();
				
				SnakeData justJoinedClientData = clientRequestPacket.getClientData();
	
				justJoinedClientData.setOutputStream(outputStream);
				justJoinedClientData.setSocket(s);
				String clientName = justJoinedClientData.getClientName();
								
				justJoinedClientData.setIsHost(connectedClients.size() == 0);
				boolean isFull = connectedClients.size() == 4;
				boolean clientNameTaken = connectedClients.contains(clientName);				
				
				if (!isFull && !clientNameTaken)
				{	
					MessagePacket accepted = new MessagePacket(serverName, "CONNECTION_ACCEPT");
					accepted.setDataStream(justJoinedClientData.getOutputStream());
					accepted.send(); // After this is received, the client's while loop will start
										
					// The server is now connected to the LobbyGUI
					
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
					
					receiver.startAutoReceiving();
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

	public void onPacketReceive(Packet packetReceiving)
	{
		if (packetReceiving instanceof MessagePacket)
		{	
			MessagePacket msgPacket = (MessagePacket) packetReceiving;
			SnakeData sender = connectedClients.get(msgPacket.getSender());
			switch (msgPacket.getMessage())
			{
			case "GAME_START":
				if (sender.isHost())
				{
					MessagePacket gameStartResponse = new MessagePacket(serverName, "GAME_START"); 
					System.out.println(connectedClients.size());
					gameStartResponse.sendMultiple(connectedClients.getAllOutputStreams());
					onGameStart();
				}

				break;
			case "I_EXIT":
				onClientQuit(sender);
				break;
			}
		}
		else if (packetReceiving instanceof SnakeUpdatePacket)
		{
			onReceiveClientDataUpdate((SnakeUpdatePacket) packetReceiving);
		}	
	}
	
	private int TICK_RATE = 120;
	
	private Timer timer;
	
	private boolean gamePaused = true;
	
	private void onGameStart()
	{		
		System.out.println("GAME STAHT");
		gamePaused = false;
		canPause = true;
		
		/*
		 * Starts InitiateGamePacket to the clients which tells them to start a timer that had
		 * numTicks ticks, is on tick 0 for initialDelay milliseconds, and is on each subsequent
		 * tick for [(gameStartDelay - initialDelay) / numTicks] milliseconds. At each tick,
		 * something different is painted
		 */
		final int gameStartDelay = 4000;
		final int numTicks = 3;
		final int initialDelay = 1000;
		InitiateGamePacket gameCounterPack = new InitiateGamePacket(initialDelay, gameStartDelay, numTicks);
		gameCounterPack.sendMultiple(connectedClients.getAllOutputStreams());
				
		// Start the timer after gameStartDelay and start the game ticks
		timer = new Timer(TICK_RATE, (e) -> tick());
		timer.setInitialDelay(gameStartDelay);
	//	timer.start();
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		tick();
	}
	
	private int tickNum = 0;
	
	private void tick()
	{
		System.out.println("SERVER TICK");
		doSnakeMovements();
		updateAllClients();
		TickPacket pack = new TickPacket();
		pack.sendMultiple(connectedClients.getAllOutputStreams());
		tickNum++;

		//	doSnakeMovements();
		//	updateAllClients();
	

		
	//	updateAllClients();
		
		// Do snakemovements
		// Send updated clientdata to all clients
		
		// Do snake movements ie update all the SnakeDataForServer stuff
		// Send update packets to all the clients so the thing can be repainted for them
	}
	
	private void doSnakeMovements()
	{
		for (SnakeData dat : connectedClients)
		{
			PointList points = dat.getPointList();
			PointList clone = points.clone();
			for (int i = 1; i < points.size(); i++)
				points.set(i, clone.get(i - 1));
			Point head = clone.get(0);
			Point dir = dat.getDirection().getVector();
			head = new Point(head.getX() + dir.getX(), head.getY() + dir.getY());
			if (head.getX() > Client.NUM_HORIZONTAL_UNITS - 1)
				head.setX(0);
			else if (head.getX() < 0)
				head.setX(Client.NUM_HORIZONTAL_UNITS - 1);
			
			if (head.getY() > Client.NUM_VERTICAL_UNITS - 1)
				head.setY(0);
			else if (head.getY() < 0)
				head.setY(Client.NUM_VERTICAL_UNITS - 1);
			points.set(0, head);
			dat.setPointList(points);
		}
	}
	
	private void updateAllClients()
	{
		for (SnakeData client : connectedClients)
		{
			SnakeUpdatePacket updatePack = new SnakeUpdatePacket(client);
			updatePack.sendMultiple(connectedClients.getAllOutputStreams());//*/
			//MessagePacket msg = new MessagePacket(serverName, "fikeeydikeyy");
			//msg.sendMultiple(connectedClients.getAllOutputStreams());
		}
	}
	
	private boolean canPause = false;
	
	/*public void unPause()
	{
		if (!canPause)
		{
			timer.start();
			gamePaused = false;
			canPause = true;
		}
	}
	
	public void pause()
	{
		if (canPause)
		{
			timer.stop();
			gamePaused = true;
			canPause = false;
		}
	}*/
	
	private void onClientQuit(SnakeData quitter)
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
	
	private void closeConnection(SnakeData exiting)
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
		ArrayList<SnakeData> copy = new ArrayList<>();
		for (SnakeData dat : connectedClients) // Avoid ConcurrentModificationException here
			copy.add(dat);
		for (SnakeData dat : copy)
			closeConnection(dat);
	}
}