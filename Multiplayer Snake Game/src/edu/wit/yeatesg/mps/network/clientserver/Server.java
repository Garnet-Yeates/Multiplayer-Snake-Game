package edu.wit.yeatesg.mps.network.clientserver;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.swing.Timer;

import edu.wit.yeatesg.mps.buffs.Fruit;
import edu.wit.yeatesg.mps.network.packets.DirectionChangePacket;
import edu.wit.yeatesg.mps.network.packets.FruitPickupPacket;
import edu.wit.yeatesg.mps.network.packets.FruitSpawnPacket;
import edu.wit.yeatesg.mps.network.packets.InitiateGamePacket;
import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeDeathPacket;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Direction;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeList;

public class Server implements Runnable, ActionListener 
{
	private ServerSocket ss;
	private int port;
	private boolean open;

	public Server(int port) 
	{
		this.port = port;
	}

	public boolean start() 
	{
		try
		{
			ss = new ServerSocket(port);
			Thread startServerThread = new Thread(this);
			startServerThread.setDaemon(true);
			startServerThread.setName("Server");
			open = true;
			startServerThread.start();
			return true;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void run() 
	{
		while (open)
		{
			try
			{
				final Socket s = ss.accept();
				Thread acceptConnections = new Thread(() -> 
				{
					try 
					{
						DataInputStream in = new DataInputStream(s.getInputStream());
						DataOutputStream out = new DataOutputStream(s.getOutputStream());
						if (onAttemptConnect(s, in, out))
						{
							while (open)
							{
								String data = in.readUTF();
								new Thread(() -> onReceive(data)).start();
							}
						} 
						else
							s.close();
					} 
					catch (Exception e) 
					{	
						System.out.println("Exception at Server.in.readUTF() -> " + e.getMessage());
					}
				});
				acceptConnections.start();
			} 
			catch (Exception e)
			{
				e.printStackTrace();
			}			
		}
	}
	
	private boolean gameStarted = false;

	public boolean onAttemptConnect(Socket s, DataInputStream in, DataOutputStream out) throws IOException
	{
		String rawUTF = in.readUTF();
		SnakeUpdatePacket clientRequestPacket = (SnakeUpdatePacket) Packet.parsePacket(rawUTF);
		SnakeData newClient = clientRequestPacket.getClientData();
		String clientName = newClient.getClientName();
		System.out.println("Attempt Connect From -> " + clientName + " " + s.getInetAddress());
		newClient.setSocket(s);
		newClient.setOutputStream(out);
		newClient.setDirectionBuffer(new ArrayList<>());

		MessagePacket responsePacket = new MessagePacket("Server", "CONNECTION ACCEPT");
		if (!isDuplicateClient(clientName) && !isServerFull() && !gameStarted)
		{
			send(responsePacket, newClient);

//			Client loop will start here (NetworkClient.startAutoReceiving() is called)
//			by this time, the client should be linked to a LobbyGUI so the lobby GUI is listening

//			Send join order data of all the already connected clients to the new client
			for (SnakeData client : connectedClients)
			{
				SnakeUpdatePacket pack = new SnakeUpdatePacket(client);
				send(pack, newClient);
				MessagePacket notifyJoin = new MessagePacket(client.getClientName(), "SOMEONE JOINED");
				send(notifyJoin, newClient);
			}					

			connectedClients.add(newClient);

//			Send the ClientData of the newly connected client to all of the connected clients (including the new client itself)
			SnakeUpdatePacket updatePack = new SnakeUpdatePacket(newClient);
			sendToAll(updatePack);
			
//		    Notify all clients that someone joined so the lobby can be updated properly
			MessagePacket notifyJoin = new MessagePacket(clientName, "SOMEONE JOINED");
			sendToAll(notifyJoin);
			return true;
		} 
		else if (gameStarted)
		{
			responsePacket.setMessage("GAME ACTIVE");
		}
		else if (isServerFull())
		{
			responsePacket.setMessage("SERVER FULL");
		}
		else if (isDuplicateClient(clientName))
		{
			responsePacket.setMessage("NAME TAKEN");
		}
		send(responsePacket, newClient);
		return false;
	}

	private SnakeList connectedClients = new SnakeList();

	public boolean isDuplicateClient(String name)
	{
		return connectedClients.contains(name);
	}

	public boolean isServerFull() 
	{
		return connectedClients.size() == 4;
	}
	
	public void onReceive(String data) 
	{
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
		else if (packetReceiving instanceof DirectionChangePacket)
		{
			onReceiveClientDirectionChangeRequest((DirectionChangePacket) packetReceiving);
		}
	}

	private int TICK_RATE = 83;

	private Timer timer;

	private void onGameStart()
	{		
		gameStarted = true;
		
		/*
		 * Starts InitiateGamePacket to the clients which tells them to start a timer that had
		 * numTicks ticks, is on tick 0 for initialDelay milliseconds, and is on each subsequent
		 * tick for [(gameStartDelay - initialDelay) / numTicks] milliseconds. At each tick,
		 * something different is painted, depending on how I code InitiateGameScript.class in SnakeClient.java
		 */
		final int gameStartDelay = 6000;
		final int numTicks = 3;
		final int initialDelay = 3000;
		InitiateGamePacket gameCounterPack = new InitiateGamePacket(initialDelay, gameStartDelay, numTicks);
		sendToAll(gameCounterPack);
		
		spawnRandomFruit();
		spawnRandomFruit();
		spawnRandomFruit();
		spawnRandomFruit();
		
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
		tickNum++;
	}
	
	private void doSnakeMovements()
	{		
		HashMap<SnakeData, Point> oldTailLocations = new HashMap<>();
		
		// Move each snake forward by one, store old tail location in HashMap
		for (SnakeData aClient : connectedClients)
		{
			if (aClient.isAlive())
			{
				if (!aClient.getDirectionBuffer().isEmpty())
					aClient.setDirection(aClient.getDirectionBuffer().remove(0));
				
				PointList points = aClient.getPointList();
				oldTailLocations.put(aClient, points.get(points.size() - 1));

				PointList clone = points.clone();
				for (int i = 1; i < points.size(); i++)
					points.set(i, clone.get(i - 1));
				Point head = clone.get(0);
				Point dir = aClient.getDirection().getVector();
				head = new Point(head.getX() + dir.getX(), head.getY() + dir.getY());
				if (head.getX() > GameplayClient.NUM_HORIZONTAL_UNITS - 1)
					head.setX(0);
				else if (head.getX() < 0)
					head.setX(GameplayClient.NUM_HORIZONTAL_UNITS - 1);
				else if (head.getY() > GameplayClient.NUM_VERTICAL_UNITS - 1)
					head.setY(0);
				else if (head.getY() < 0)
					head.setY(GameplayClient.NUM_VERTICAL_UNITS - 1);
				points.set(0, head);

				aClient.setPointList(points);
			}
		}
		
//		^ Update the position of all snakes before doing collision checks ^
		
//		Handle collision, fruit pickup/spawning, segment adding
		for (SnakeData aClient : connectedClients)
		{
			if (aClient.isAlive())
			{
				Point head = aClient.getPointList().get(0);
				boolean colliding = false;
				SnakeList otherClients = new SnakeList();
				for (SnakeData bClient : connectedClients)
					if (!bClient.equals(aClient) && bClient.isAlive()) // Collision upon dead snakes does not occur
						otherClients.add(bClient);
				
//				If this Snake's head location intercepts any segment on any OTHER snake, it counts as a collision
				for (SnakeData otherClient : otherClients)
					if (Server.interceptsSnakesSegment(head, otherClient))
						colliding = true;
				
//				If this Snake's head location intercepts any of its own body segments more than once, it counts as a collision
				colliding = aClient.getOccurrenceOf(head) > 2 ? true : colliding;
				
//				Set colliding to false if this Snake currently has the Translucent buff
				colliding = aClient.hasBuffTranslucent() ? false : colliding;
	
				aClient.setAlive(!colliding);			
			
//				If client still alive, handle other stuff such as adding segments and picking up fruit
				if (aClient.isAlive())
				{
//					Handle fruit pickup and the subsequent fruit spawning
					if (hasInterceptingFruit(head))
					{
						Fruit pickingUp = getInterceptingFruit(head);
						if (!pickingUp.hasAssociatedBuff() || !aClient.hasAnyBuffs())
						{
//							Remove this fruit from the fruit list, and add its segment value to snake belly
							allFruit.remove(pickingUp);
							int fruitValue = pickingUp.getFruitType().getNumSegmentsGiven();
							aClient.modifyFoodInBelly(fruitValue);
							
//							Inform all clients that a player picked up a fruit so the Fruit is no longer drawn
							FruitPickupPacket pack = new FruitPickupPacket(aClient.getClientName(), pickingUp);
							sendToAll(pack); 
							
//							If this fruit has an associated buff, grant it to the player
							if (pickingUp.hasAssociatedBuff())
								aClient.grantBuff(pickingUp.getAssociatedBuff());
							
//							Spawn in a new Fruit somewhere else, send a FruitSpawnPacket to all clients
							spawnRandomFruit();
						}	
					}
					
//					Handle the snake adding more segments as a result of recently eating a Fruit
					if (aClient.hasFoodInBelly())
					{
						Point addSegmentHere = oldTailLocations.get(aClient);
						aClient.addToPointList(addSegmentHere);
						aClient.modifyFoodInBelly(-1);
					}
				}
				else
				{
//					Collision occurred and the client is now dead, so send a SnakeDeathPacket
					SnakeDeathPacket snakeDeathPack = new SnakeDeathPacket(aClient.getClientName());
					sendToAll(snakeDeathPack);
				}
			}
		}
	}
	
	private ArrayList<Fruit> allFruit = new ArrayList<>();

	public boolean spawnRandomFruit()
	{
		Random rand = new Random();
		boolean spawned = false;
		Fruit addedFruit = null;
		while (!spawned)
		{
//			If we have greater than a 1/20 chance of random spawning one, do this
			if (getPercentCovered() < 0.95)
			{
				int randX = rand.nextInt(GameplayClient.NUM_HORIZONTAL_UNITS);
				int randY = rand.nextInt(GameplayClient.NUM_VERTICAL_UNITS);
				Point theoreticalFruitLoc = new Point(randX, randY);
				if (!hasInterceptingFruit(theoreticalFruitLoc) && !interceptsAnySnakeSegment(theoreticalFruitLoc))
				{
					addedFruit = new Fruit(theoreticalFruitLoc);
					spawned = true;
				}
			}
//			If >90% of the map is covered, it is probably more efficient to just loop through a list of available spaces and choose a random index
			else if (getPercentCovered() < 1)
			{
				ArrayList<Point> availableLocations = new ArrayList<>();
				for (int x = 0; x < GameplayClient.NUM_HORIZONTAL_UNITS; x++)
					for (int y = 0; y < GameplayClient.NUM_VERTICAL_SPACES; y++)
						availableLocations.add(new Point(x, y));
				for (SnakeData client : connectedClients)
					availableLocations.removeAll(client.getPointList());
				Point randomAvailableLoc = availableLocations.get(rand.nextInt(availableLocations.size()));
				addedFruit = new Fruit(randomAvailableLoc);
				spawned = true;
			}
			else break; // Can't spawn fruit, map is completely covered
		}
		if (addedFruit != null)
		{
			allFruit.add(addedFruit);
			FruitSpawnPacket fruitSpawnPack = new FruitSpawnPacket(addedFruit);
			sendToAll(fruitSpawnPack);
			return true;
		}
		return false;
	}
	
	
	public int getCoveredArea()
	{
		int covered = 0;
		for (SnakeData snake : connectedClients)
			covered += snake.getPointList().size();
		return covered;
	}
	
	public double getPercentCovered()
	{
		return (double) getCoveredArea() / GameplayClient.MAX_AREA;
	}
	
	public boolean hasInterceptingFruit(Point p)
	{
		return getInterceptingFruit(p) != null;
	}
	
	public Fruit getInterceptingFruit(Point p)
	{
		for (Fruit f : allFruit)
			if (f.getLocation().equals(p))
				return f;		
		return null;
	}
	
	public static boolean interceptsSnakesSegment(Point point, SnakeData theSnake)
	{
		for (Point p : theSnake.getPointList())
			if (point.equals(p))
				return true;
		return false;
	}
	
	public boolean interceptsAnySnakeSegment(Point p)
	{
		for (SnakeData snake : connectedClients)
			if (interceptsSnakesSegment(p, snake))
				return true;
		return false;
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

	private void onReceiveClientDirectionChangeRequest(DirectionChangePacket pack)
	{
		SnakeData whoSent = connectedClients.get(pack.getSender());
		ArrayList<Direction> keyBuffer = whoSent.getDirectionBuffer();

		if (keyBuffer.size() <= 20)
		{		
			boolean canAdd;
			Direction entered = pack.getDirection();
			if (keyBuffer.size() == 0)
			{
				canAdd = whoSent.getDirection() != entered && entered != whoSent.getDirection().getOpposite();
			}
			else
			{
				Direction lastEntered = keyBuffer.get(keyBuffer.size() - 1);
				canAdd = lastEntered != entered && entered != lastEntered.getOpposite();
			}
			
			if (canAdd)
			{
				keyBuffer.add(entered);
			}
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

	public synchronized void send(Packet p, SnakeData to)
	{
		p.setDataStream(to.getOutputStream());
		p.send();
	}

	public synchronized void sendToAll(Packet p)
	{
		for (SnakeData dat : connectedClients)
		{
			p.setDataStream(dat.getOutputStream());
			p.send();
		}
	}
}
