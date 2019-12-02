package edu.wit.yeatesg.mps.network.clientserver;
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
import edu.wit.yeatesg.mps.network.packets.SnakeBitePacket;
import edu.wit.yeatesg.mps.network.packets.SnakeDeathPacket;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.Direction;
import edu.wit.yeatesg.mps.otherdatatypes.Point;
import edu.wit.yeatesg.mps.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.otherdatatypes.ReflectionTools;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeList;

public class Server implements Runnable 
{	
	private ServerSocket ss;
	private SocketSecurityTool serverDecrypter;
	private int port;
	private boolean open;
	
	public Server(int port) 
	{
		this.port = port;
		serverDecrypter = new SocketSecurityTool(1024);
	}

	/**
	 * Attempts to start the server. As long as new ServerSocket({@link #port}) doesn't throw
	 * an exception, then the server will have successfully started and this method will return
	 * true.
	 * @return true if the server successfully started (no exception was thrown)
	 */
	public boolean start() 
	{
		try
		{
			ss = new ServerSocket(port);
			open = true;
			Thread startServerThread = new Thread(this);
			startServerThread.setName("Start-Server-Thread");
			startServerThread.start();
			return true;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 
	 */
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
								byte[] bytes = new byte[in.readInt()];
								in.read(bytes);
								String received = serverDecrypter.decryptStringBytes(bytes);
								onReceiveEncrypted(received);
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

	/**
	 * This method is called right after the server socket accepts a connection from a new client. The protocol for
	 * when a client attempts to connect to this server is that the client will send a SnakeUpdatePacket containing
	 * their desired name to the server. The server will then determine whether or not the connection is accepted based
	 * on a few factors. The determining factors for a client connecting are that the server cannot be full, a game
	 * cannot currently be running, and {@link #isDuplicateClient(String)} needs to return false for the newly connected
	 * client's name.
	 * @param s the socket for the client that is trying to connect.
	 * @param in the DataInputStream for this client, to receive packets.
	 * @param out the DataOutputStream for this client, to send packets.
	 * @return true if this client connected successfully
	 * @throws IOException if in.readUTF() throws an exception
	 */
	public synchronized boolean onAttemptConnect(Socket s, DataInputStream in, DataOutputStream out) throws IOException
	{
		// Read request packet
		byte[] bytes = new byte[in.readInt()]; in.read(bytes);
		String rawUTF = new String(bytes);
		SnakeUpdatePacket clientRequestPacket = (SnakeUpdatePacket) Packet.parsePacket(rawUTF);
		System.out.println("Server Received -> " + clientRequestPacket);
		
		// Process request packet
		SnakeData newClient = clientRequestPacket.getClientData();
		String clientName = newClient.getClientName();
		System.out.println("Attempt Connect From -> " + clientName + " " + s.getInetAddress());
		newClient.setSocket(s);
		newClient.setOutputStream(out);
		newClient.setDirectionBuffer(new ArrayList<>());

		// Determine what the response will be
		MessagePacket responsePacket = new MessagePacket("Server", "CONNECTION ACCEPT");
		if (!isDuplicateClient(clientName) && !isServerFull() && !gameStarted)
		{
			System.out.println("Server -> CONNECTION ACCEPT" );
			// Send "CONNECTION ACCEPT" response
			send(responsePacket, newClient, false);
			
			// After client gets CONNECTION ACCEPT it sends its encryption key, so process key below
			byte[] clientKey = new byte[in.readInt()]; in.read(clientKey);
			// Assign a separate AsymmetricEncryptionTool to this client to send encrypted packets to them
			SocketSecurityTool clientEncrypter = new SocketSecurityTool(1024);
			newClient.setEncrypter(clientEncrypter);
			clientEncrypter.setPartnerKey(clientKey);
			
			// After receiving client encryption key, send server encryption key to client to fully establish asym connection
			serverDecrypter.sendPublicKey(out);
			
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

	/**
	 * Determines whether or not there is already a client on this server with the same name as the one given in
	 * the parameter. Since clients are identified by their name, client equality is also determined based on
	 * name. Therefore two clients on the server cannot have the same name.
	 * @param name the name of the client attempting to connect
	 * @return true if there is already a client on this server with the same name
	 */
	public boolean isDuplicateClient(String name)
	{
		return connectedClients.contains(name);
	}

	/**
	 * Determines whether or not the server is full. The maximum amount of players that can be on the server at
	 * once is 4
	 * @return true if there are already 4 connected clients
	 */
	public boolean isServerFull() 
	{
		return connectedClients.size() == 4;
	}
	
	
	/**
	 * This method is used for when the Server is sending a packet to a single client. This method
	 * sets the data stream of the packet to the data stream associated with the given client
	 * @param p the Packet that is being sent from the server
	 * @param to the Client that this packet is being sent to
	 */
	public void send(Packet p, SnakeData to)
	{
		send(p, to, true);

	}
	
	public void send(Packet p, SnakeData to, boolean encrypting)
	{
		p.setDataStream(to.getOutputStream());
		p.send(encrypting ? to.getEncrypter() : null);
	}

	public void sendToAll(Packet p)
	{
		for (SnakeData dat : connectedClients)
			send(p, dat);
	}

	/**
	 * For each connected client on the server, there is a separate thread for them where, in a while loop,
	 * {@link DataInputStream#readUTF()} is called. Each time that method is called for a specific client,
	 * the UTF data is passed to this onReceive() method. This method is synchronized because we do not want
	 * the server to receive two packets of data concurrently, because that can result in a concurrent modification
	 * exception in the {@link #connectedClients} list, among other unpredictable things. The {@link #onTick()} method
	 * is also synchronized for the same reason ({@link #onTick()} and {@link #onReceiveEncrypted(String)} are the only two
	 * methods in the server that interact with other threads, so they are the only two that should be synchronized)
	 * @param data the raw UTF data received from the client socket on another thread
	 */
	public synchronized void onReceiveEncrypted(String data) 
	{
		Packet packetReceiving = Packet.parsePacket(data);
		switch (packetReceiving.getClass().getSimpleName())
		{
		case "MessagePacket":
			onReceiveMessagePacket((MessagePacket) packetReceiving);
			break;
		case "SnakeUpdatePacket":
			onReceiveClientDataUpdate((SnakeUpdatePacket) packetReceiving);
			break;
		case "DirectionChangePacket":
			onReceiveClientDirectionChangeRequest((DirectionChangePacket) packetReceiving);
			break;
		}
	}
	
	private void onReceiveMessagePacket(MessagePacket messagePacket)
	{
		SnakeData sender = connectedClients.get(messagePacket.getSender());
		switch (messagePacket.getMessage())
		{
		case "GAME START":
			if (sender.isHost()) 
			{
				MessagePacket gameStartResponse = new MessagePacket("Server", "GAME START"); 
				sendToAll(gameStartResponse);
				onReceiveGameStartPacket();
			}
			break;
		case "I EXIT":
			onReceiveClientQuitPacket(sender);
			break;
		}
	}
	
	private void onReceiveClientQuitPacket(SnakeData quitter)
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

	private Timer tickTimer; 
	private int tickRate = 70; // 14.28 squares/sec

	private void onReceiveGameStartPacket()
	{		
		gameStarted = true;

		final int gameStartDelay = 6000; // 6000
		final int numTicks = 3; // 3
		final int initialDelay = 3000; // 3000
		InitiateGamePacket gameCounterPack = new InitiateGamePacket(initialDelay, gameStartDelay, numTicks);
		sendToAll(gameCounterPack);

		for (SnakeData client : connectedClients)
		{
			client.modifyFoodInBelly(37);
		}

		spawnRandomFruit();
		spawnRandomFruit();
		spawnRandomFruit();
		spawnRandomFruit();

		// Start the timer after gameStartDelay and start the game ticks
		tickTimer = new Timer(tickRate, (e) -> onTick());
		tickTimer.setInitialDelay(gameStartDelay);
		tickTimer.start();
	}
	
	/**
	 * This method is called whenever the Server's tick timer is called. This method will be called every
	 * {@link #tickRate} milliseconds. Since the tick timer is on a different thread, and {@link #onReceiveEncrypted(String)}
	 * is called in a different thread, both of these methods are synchronized so that there won't be any
	 * ConcurrentModificationException thrown as a result of 2 or more threads looping through {@link #connectedClients}
	 */
	private synchronized void onTick()
	{
		doSnakeMovements();
		updateAllClients();
		MessagePacket tickPacket = new MessagePacket("Server", "SERVER TICK");
		sendToAll(tickPacket);
	}

	private void doSnakeMovements()
	{		
		HashMap<SnakeData, Point> oldTailLocations = new HashMap<>();

//		Move each snake forward by one, store old tail location in HashMap. This is in a separate loop because ALL client
//		positions must be updated before doing collision checks
		for (SnakeData aClient : connectedClients)
		{
			if (aClient.isAlive())
			{
				if (!aClient.getDirectionBuffer().isEmpty())
					aClient.setDirection(aClient.getDirectionBuffer().remove(0));

				PointList points = aClient.getPointList(true);
//				Save the old tail location, because later in this method it will be added back if the snake 
				oldTailLocations.put(aClient, points.get(points.size() - 1));

				Point oldHead = points.get(0);
				Point head = oldHead.addVector(aClient.getDirection().getVector());
				head = GameplayClient.keepInBounds(head);

				points.add(0, head);
				points.remove(points.size() - 1);
				aClient.setPointList(points);
			}
		}

//		Handle collision, fruit pickup/spawning, segment adding
		for (SnakeData aClient : connectedClients)
		{
			if (aClient.isAlive())
			{
				Point head = aClient.getPointList(false).get(0);
				boolean colliding = false;
				SnakeList otherClients = new SnakeList();
				for (SnakeData bClient : connectedClients)
					if (!bClient.equals(aClient) && bClient.isAlive()) // Collision upon dead snakes does not occur
						otherClients.add(bClient);

//				If this Snake's head location intercepts any segment on any OTHER snake, it counts as a collision
				SnakeData interceptingSnake = aClient;
				int interceptingIndex = -1;
				for (SnakeData otherClient : otherClients)
				{
					interceptingIndex = Server.getInterceptingIndexSpecificSnake(head, otherClient);
					if (interceptingIndex != -1)
					{
						interceptingSnake = otherClient;
						colliding = true;
					}
				}

				int headOccurance = aClient.getOccurrenceOf(head);
//				If this Snake's head location intercepts any of its own body segments more than once, it counts as a collision
				colliding = headOccurance > 2 || headOccurance > 1 && aClient.hasBuffHungry() ? true : colliding;

//				Set colliding to false if this Snake currently has the Translucent buff
				colliding = aClient.hasBuffTranslucent() ? false : colliding;

//				Special collision case if the user has the Hungry buff, which allows them to eat small portions of other snakes
				if (colliding && aClient.hasBuffHungry())
				{
					colliding = false;
					if (interceptingSnake == aClient)
						interceptingIndex = Server.getInterceptingIndexSpecificSnake(head, aClient);

					int howManyBitOff = interceptingSnake.getLength() - interceptingIndex;
					if ((interceptingIndex != 0 && (interceptingSnake.getLength() < Fruit.MIN_FRUIT_HUNGRY_LENGTH || howManyBitOff <= Fruit.MAX_BITE_OFF)) || interceptingSnake == aClient)
					{
						int length = interceptingSnake.getLength();
						PointList clone = interceptingSnake.getPointList(true);
						for (int i = length - 1; i >= interceptingIndex; clone.remove(i), i--);
						interceptingSnake.setPointList(clone);
						if (interceptingSnake != aClient)
							aClient.modifyFoodInBelly(howManyBitOff);
						aClient.removeHungryBuffEarly();
						if (!interceptingSnake.equals(aClient))
							interceptingSnake.removeAllBuffsEarly();
						SnakeBitePacket snakeBite = new SnakeBitePacket(aClient.getClientName(), interceptingSnake.getClientName(), howManyBitOff, interceptingIndex);
						sendToAll(snakeBite);
					}
					else // Bit off more than they can chew
					{
						colliding = true;
					}
				}

				aClient.setIsAlive(!colliding);			

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
						aClient.setAddingSegment(true);
					}
					else
						aClient.setAddingSegment(false);
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
	
	private void updateAllClients()
	{
		for (SnakeData client : connectedClients)
		{
			SnakeData clientClone = new SnakeData(ReflectionTools.fieldsToString(SnakeData.REGEX, client, SnakeData.class, new String[] { "pointList" }));
			SnakeUpdatePacket updatePack = new SnakeUpdatePacket(clientClone);
			sendToAll(updatePack);
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
				if (!hasInterceptingFruit(theoreticalFruitLoc) && !hasInterceptingSnake(theoreticalFruitLoc))
				{
					addedFruit = new Fruit(this, theoreticalFruitLoc);
					spawned = true;
				}
			}
//		    If >90% of the map is covered, it is probably more efficient to just loop through a list of available spaces and choose a random index
			else if (getPercentCovered() < 1)
			{
				ArrayList<Point> availableLocations = new ArrayList<>();
				for (int x = 0; x < GameplayClient.NUM_HORIZONTAL_UNITS; x++)
					for (int y = 0; y < GameplayClient.NUM_VERTICAL_SPACES; y++)
						availableLocations.add(new Point(x, y));
				for (SnakeData client : connectedClients)
					availableLocations.removeAll(client.getPointList(false));
				Point randomAvailableLoc = availableLocations.get(rand.nextInt(availableLocations.size()));
				addedFruit = new Fruit(this, randomAvailableLoc);
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
			covered += snake.getLength();
		return covered;
	}

	/**
	 * Determines how many spaces on the map are empty spaces (a space is empty if there isn't a
	 * @return
	 */
	public double getPercentCovered()
	{
		return (double) getCoveredArea() / GameplayClient.MAX_AREA;
	}

	/**
	 * Determines whether or not there is a Fruit in the {@link #allFruit} list that exists at the given
	 * Point.
	 * @param p the Point that is being checked for an intercepting fruit.
	 * @return true if there is a Fruit on this server that exists at the given Point.
	 */
	public boolean hasInterceptingFruit(Point p)
	{
		return getInterceptingFruit(p) != null;
	}

	/**
	 * Obtains the Fruit in this Server's {@link #allFruit} list that exists at the given point, if any.
	 * @param p the Point where a Fruit may be intercepting.
	 * @return a reference to the Fruit that exists at the given Point, or null if there is no Fruit that exists here.
	 */
	public Fruit getInterceptingFruit(Point p)
	{
		for (Fruit f : allFruit)
			if (f.getLocation().equals(p))
				return f;		
		return null;
	}
	
	/**
	 * Determines whether or not the given Point intercepts any of the snakes that are connected
	 * to this server.
	 * @param p the Point that may be intercepting any of the Snake's connected to this server
	 * @return true if this Point intercepts any client on the server.
	 */
	public boolean hasInterceptingSnake(Point p)
	{
		for (SnakeData snake : connectedClients)
			if (interceptsSpecificSnake(p, snake))
				return true;
		return false;
	}

	/**
	 * Determines whether or not the given Point intercepts the given client.
	 * @param point the point that may potentially intercept the given Snake.
	 * @param theSnake the client that the given point may be intercepting.
	 * @return true if the given point exists in theSnake.getPointList().
	 */
	public static boolean interceptsSpecificSnake(Point point, SnakeData theSnake)
	{
		return getInterceptingIndexSpecificSnake(point, theSnake) != -1;
	}

	/**
	 * Gets the last index of the given Point in the given SnakeData's point list.
	 * @param point the point that potentially exists in the given SnakeData's point list.
	 * @param theSnake the SnakeData (client) that the given point may potentially be intercepting.
	 * @return -1 if this Point does not exist in theSnake.getPointList().
	 */
	public static int getInterceptingIndexSpecificSnake(Point point, SnakeData theSnake)
	{
		int theIndex = -1;
		int i = 0;
		for (Point possible : theSnake.getPointList(false))
		{
			if (possible.equals(point))
			{
				theIndex = i;
			}
			i++;
		}
		return theIndex;
	}
	
	/**
	 * Obtains a reference to the list of all of the clients that are connected to the server.
	 * @return a SnakeList containing all of the clients connected to this server.
	 */
	public SnakeList getConnectedClients()
	{
		return connectedClients;
	}

	/**
	 * Closes the connection between the server and the given client, and
	 * removes the client from the {@link #connectedClients} list.
	 * @param exiting the client that is exiting the game.
	 */
	private void closeConnection(SnakeData exiting)
	{
		try
		{
			exiting.getSocket().close();
		}
		catch (IOException e) { }

		connectedClients.remove(exiting);
	}

	/**
	 * Closes the connection between this server and all of the clients
	 * that are connected to it.
	 */
	private void closeAllConnections()
	{
		ArrayList<SnakeData> copy = new ArrayList<>();
		for (SnakeData dat : connectedClients) // Avoid ConcurrentModificationException here
			copy.add(dat);
		for (SnakeData dat : copy)
			closeConnection(dat);
	}
}