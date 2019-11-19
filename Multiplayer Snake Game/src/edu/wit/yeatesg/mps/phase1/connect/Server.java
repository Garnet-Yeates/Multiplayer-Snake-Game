package edu.wit.yeatesg.mps.phase1.connect;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.Timer;

import edu.wit.yeatesg.mps.phase0.otherdatatypes.InitiateGamePacket;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeList;
import edu.wit.yeatesg.mps.phase0.packets.MessagePacket;
import edu.wit.yeatesg.mps.phase0.packets.Packet;
import edu.wit.yeatesg.mps.phase0.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.phase3.play.SnakeClient;

public class Server implements Runnable, ActionListener {

	private ServerSocket ss;
	private int port;
	private boolean open;

	public Server(int port) {
		this.port = port;
	}

	public boolean start()
	{
		try {
			ss = new ServerSocket(port);
			Thread startServerThread = new Thread(this);
			startServerThread.setDaemon(true);
			startServerThread.setName("Server");
			open = true;
			startServerThread.start();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void run() {
		while (open) {
			try {
				final Socket s = ss.accept();
				Thread acceptConnections = new Thread(() -> {
					try {
						DataInputStream in = new DataInputStream(s.getInputStream());
						DataOutputStream out = new DataOutputStream(s.getOutputStream());
						if (onAttemptConnect(s, in, out)) {
							while(open) {
								String data = in.readUTF();
								new Thread(() -> onReceive(data)).start();
							}
						} else {
							s.close();
						}
					} catch (Exception e) {	
						System.out.println("Exception at Server.in.readUTF() -> " + e.getMessage());
					}
				});
				acceptConnections.start();
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
	}
	
	public boolean onAttemptConnect(Socket s, DataInputStream in, DataOutputStream out) throws IOException {
		String rawUTF = in.readUTF();
		System.out.println("Attempt Connect From -> " + rawUTF);
		SnakeUpdatePacket clientRequestPacket = (SnakeUpdatePacket) Packet.parsePacket(rawUTF);
		SnakeData newClient = clientRequestPacket.getClientData();
		String clientName = newClient.getClientName();
		newClient.setSocket(s);
		newClient.setOutputStream(out);
		
		MessagePacket responsePacket = new MessagePacket("Server", "");
		if (!isDuplicateClient(clientName) && !isServerFull()) {
			System.out.println("SERVER SAY YES");
			responsePacket.setMessage("CONNECTION ACCEPT");
			send(responsePacket, newClient);
			
			// Client loop will start here (it will start sending packets to its listeners)
			// by this time, the client should be linked to a LobbyGUI so the lobby GUI is listening
			
			// Send join order data of all the already connected clients to the new client
			for (SnakeData client : connectedClients)
			{
				SnakeUpdatePacket pack = new SnakeUpdatePacket(client);
				send(pack, newClient);
				MessagePacket notifyJoin = new MessagePacket(client.getClientName(), "SOMEONE JOINED");
				send(notifyJoin, newClient);
			}					
			
			connectedClients.add(newClient);
			
			// Send the ClientData of the newly connected client to all of the connected clients (including the new client itself)
			SnakeUpdatePacket updatePack = new SnakeUpdatePacket(newClient);
			sendToAll(updatePack);
			// Notify all clients that someone joined so the lobby can be updated properly
			MessagePacket notifyJoin = new MessagePacket(clientName, "SOMEONE JOINED");
			sendToAll(notifyJoin);
			return true;
		} else if (isServerFull()) {
			responsePacket.setMessage("SERVER FULL");
		} else if (isDuplicateClient(clientName)) {
			responsePacket.setMessage("NAME UNAVAILABLE");
		}
		send(responsePacket, newClient);
		return false;
	}
	
	private SnakeList connectedClients = new SnakeList();

	public boolean isDuplicateClient(String name) {
		return connectedClients.contains(name);
	}
	
	public boolean isServerFull() {
		return connectedClients.size() == 4;
	}

	
	// Packet Recevoir Handler
	
	public void onReceive(String data) {
		Packet packetReceiving = Packet.parsePacket(data);
		if (packetReceiving instanceof MessagePacket)
		{	
			MessagePacket msgPacket = (MessagePacket) packetReceiving;
			SnakeData sender = connectedClients.get(msgPacket.getSender());
			switch (msgPacket.getMessage())
			{
			case "GAME START":
				if (sender.isHost())
				{
					MessagePacket gameStartResponse = new MessagePacket("Server", "GAME START"); 
					sendToAll(gameStartResponse);
					onGameStart();
				}
				break;
			case "I EXIT":
				onClientQuit(sender);
				break;
			}
		}
		else if (packetReceiving instanceof SnakeUpdatePacket)
		{
			onReceiveClientDataUpdate((SnakeUpdatePacket) packetReceiving);
		}	
	}

	
	private int TICK_RATE = 75;
	
	private Timer timer;
	
	
	private void onGameStart()
	{		
		/*
		 * Starts InitiateGamePacket to the clients which tells them to start a timer that had
		 * numTicks ticks, is on tick 0 for initialDelay milliseconds, and is on each subsequent
		 * tick for [(gameStartDelay - initialDelay) / numTicks] milliseconds. At each tick,
		 * something different is painted, depending on how I code InitiateGameScript.class in SnakeClient.java
		 */
		final int gameStartDelay = 4000;
		final int numTicks = 3;
		final int initialDelay = 1000;
		InitiateGamePacket gameCounterPack = new InitiateGamePacket(initialDelay, gameStartDelay, numTicks);
		sendToAll(gameCounterPack);
		
		// Start the timer after gameStartDelay and start the game ticks
		timer = new Timer(TICK_RATE, (e) -> tick());
		timer.setInitialDelay(gameStartDelay);
		timer.start();
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		tick();
	}
	
	private int tickNum = 0;
	
	private void tick()
	{
		doSnakeMovements();
		updateAllClients();
		MessagePacket tickPacket = new MessagePacket("Server", "SERVER TICK");
		sendToAll(tickPacket);
//		TickPacket pack = new TickPacket();
//		pack.sendMultiple(numPacketsSent++, connectedClients.getAllOutputStreams());
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
			if (head.getX() > SnakeClient.NUM_HORIZONTAL_UNITS - 1)
				head.setX(0);
			else if (head.getX() < 0)
				head.setX(SnakeClient.NUM_HORIZONTAL_UNITS - 1);
			if (head.getY() > SnakeClient.NUM_VERTICAL_UNITS - 1)
				head.setY(0);
			else if (head.getY() < 0)
				head.setY(SnakeClient.NUM_VERTICAL_UNITS - 1);
			points.set(0, head);
			dat.setPointList(points);
		}
	}
	
	private void updateAllClients()
	{
		for (SnakeData client : connectedClients)
		{
			SnakeUpdatePacket updatePack = new SnakeUpdatePacket(client);
			sendToAll(updatePack);
		}
	}
	
	private void onClientQuit(SnakeData quitter)
	{
		if (quitter.isHost())
		{
			MessagePacket response = new MessagePacket(quitter.getClientName(), "YOU EXIT");
			sendToAll(response);
			
			closeAllConnections();
						
			System.exit(0);
		}
		else
		{
			MessagePacket response = new MessagePacket(quitter.getClientName(), "YOU EXIT");
			send(response, quitter);
			
			closeConnection(quitter);			
			
			MessagePacket responseToOthers = new MessagePacket(quitter.getClientName(), "THEY EXIT");
			sendToAll(responseToOthers);
		}
	}
	
	private void onReceiveClientDataUpdate(SnakeUpdatePacket updatePacket)
	{
		// Update the client's info on the server
		connectedClients.updateBasedOn(updatePacket);
	
		// Bounce packet back to all clients
		sendToAll(updatePacket);
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
	

	public synchronized void send(Packet p, SnakeData to) {
		p.setDataStream(to.getOutputStream());
		p.send();
	}

	public synchronized void sendToAll(Packet p) {
		for (SnakeData dat : connectedClients) {
			p.setDataStream(dat.getOutputStream());
			p.send();
		}
	}
}
