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
import edu.wit.yeatesg.mps.network.clientserver.NetworkClient.NotConnectedException;
import edu.wit.yeatesg.mps.network.clientserver.NetworkClient.SocketClosedException;
import edu.wit.yeatesg.mps.network.packets.DirectionChangePacket;
import edu.wit.yeatesg.mps.network.packets.FruitPickupPacket;
import edu.wit.yeatesg.mps.network.packets.FruitSpawnPacket;
import edu.wit.yeatesg.mps.network.packets.InitiateGamePacket;
import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeBitePacket;
import edu.wit.yeatesg.mps.network.packets.SnakeDeathPacket;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.Color;
import edu.wit.yeatesg.mps.otherdatatypes.Direction;
import edu.wit.yeatesg.mps.otherdatatypes.Point;
import edu.wit.yeatesg.mps.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.otherdatatypes.ReflectionTools;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeList;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;


/**
 * Synchronization Logic:
 * 1) Since {@link #onReceive(String)} is able to be called by up to 4 different threads concurrently, it should
 * be synchronized so that only one thread calls it at once, avoiding tons of unpredictable results
 * 2) The {@link #send(Packet, SnakeData, boolean)} method should also be synchronized. This is because we do not
 * want run into the case where packets are encrypted (sent) and decrypted (received) at the same time, because this
 * can confuse the encrypt/decrypt cipher in SocketSecurityTool (I low-key already avoided this by synchronizing
 * {@link SocketSecurityTool#decryptBytes(byte[])} and {@link SocketSecurityTool#encryptBytes(byte[], SocketSecurityTool.Key)},
 * but there are other issues that can be caused from concurrently calling send() and receive...
 * 3) 
 * @author yeatesg
 *
 */
public class Server 
{	
	private ServerSocket ss;
	private SocketSecurityTool serverDecrypter;
	private int port;
	private boolean open;

	public Server(int port) 
	{
		this.port = port;
		serverDecrypter = new SocketSecurityTool();
	}

	/**
	 * Attempts to start the server. As long as new ServerSocket({@link #port}) doesn't throw
	 * an exception, then the server will have successfully started and this method will return
	 * true.
	 * @return true if the server successfully started (no exception was thrown)
	 * @throws IOException 
	 */
	public boolean start() throws ServerStartFailedException 
	{
		try
		{	
			ss = new ServerSocket(port);
			open = true;
			Thread acceptConnections = new AcceptConnectionsThread();
			acceptConnections.setName("Start-Server-Thread");
			acceptConnections.start();
			return true;
		}
		catch (IOException e)
		{
			throw new ServerStartFailedException("Couldn't create server");
		}
	}

	public static class ServerStartFailedException extends Exception
	{
		private static final long serialVersionUID = -8840417831600338886L;

		private String message;

		public ServerStartFailedException(String message)
		{
			this.message = message;
		}

		@Override
		public String getMessage()
		{
			return message;
		}
	}

	private SnakeList connectedClients = new SnakeList();
	
	private class AcceptConnectionsThread extends Thread
	{
		@Override
		public void run() 
		{
			while (open)
			{
				try
				{
					final Socket s = ss.accept();
					DataInputStream in = new DataInputStream(s.getInputStream());
					DataOutputStream out = new DataOutputStream(s.getOutputStream());
					String clientName;
					if ((clientName = onAttemptConnect(s, in, out)) != null)
					{
						ReceiveFromClientThread receiveThread = new ReceiveFromClientThread(clientName, s, in, out);
						connectedClients.get(clientName).setPacketReceiveThread(receiveThread);
						receiveThread.start();
					}
					else
						s.close();
				} 
				catch (IOException e)
				{
					System.out.println("[Server] IOException occured while waiting for a connection");
				}			
			}
		}
	}
	
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
	 * @return the client's name if this client connected successfully, or null if the connection was denied
	 * @throws IOException if in.readUTF() throws an exception
	 */
	public String onAttemptConnect(Socket s, DataInputStream in, DataOutputStream out) throws IOException
	{
		// Read request packet
		String rawUTF = new String(NetworkClient.readByteSegments(in)[0]);
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
			byte[] clientKey = NetworkClient.readByteSegments(in)[0];
			System.out.println("Server received clint key");
			// Assign a separate AsymmetricEncryptionTool to this client to send encrypted packets to them
			SocketSecurityTool clientEncrypter = new SocketSecurityTool();
			newClient.setEncrypter(clientEncrypter);
			clientEncrypter.setPartnerKey(clientKey);

			// After receiving client encryption key, send server encryption key to client to fully establish asym connection
			serverDecrypter.sendPublicKey(out);

			PlayerSlot emptySlot = slots.getNextEmptySlot();
			emptySlot.bindClient(newClient);

			connectedClients.add(newClient);	
			updateAllClients(true);
			return newClient.getClientName();
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
		return null;
	}

	public class ReceiveFromClientThread extends Thread
	{
		private boolean receiving = true;
		
		private String clientName;
		private Socket clientSocket;
		private DataInputStream in;
		private DataOutputStream out;
		
		public ReceiveFromClientThread(String clientName, Socket clientSocket, DataInputStream in, DataOutputStream out)
		{
			this.clientName = clientName;
			this.clientSocket = clientSocket;
			this.in = in;
			this.out = out;
		}
		
		@Override
		public void run()
		{
			while (receiving)
			{
				try 
				{
					byte[][] blocks = NetworkClient.readByteSegments(in);
					String received = serverDecrypter.decryptString(blocks);
					onReceive(received);
				}
				catch (Exception e)
				{
					receiving = false;
					e.printStackTrace();
				}
			}
		}
		
		public void end()
		{
			receiving = false;
		}
	}

	private boolean gameStarted = false;

	
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

	/** Used to synchronize send() and onReceive() because we do not want any those methods running concurrently */
	private final Object SEND_REC_LOCK = new Object();

	/**
	 * This method is used for when the Server is sending a packet to a single client. This method
	 * sets the data stream of the packet to the data stream associated with the given client
	 * @param p the Packet that is being sent from the server
	 * @param to the Client that this packet is being sent to
	 */
	public void send(Packet p, SnakeData to, boolean encrypting)
	{
		synchronized (SEND_REC_LOCK)
		{
			p.setDataStream(to.getOutputStream());
			p.write(encrypting ? to.getEncrypter() : null);
		}
	}

	public void send(Packet p, SnakeData to)
	{
		send(p, to, true);
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
	 * is also synchronized for the same reason ({@link #onTick()} and {@link #onReceive(String)} are the only two
	 * methods in the server that interact with other threads, so they are the only two that should be synchronized)
	 * @param data the raw UTF data received from the client socket on another thread
	 */
	public void onReceive(String data) 
	{
		synchronized (SEND_REC_LOCK)
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
			onReceiveClientDisconnectRequest(sender);
			break;
		}
	}

	private void onReceiveClientDisconnectRequest(SnakeData quitter)
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

			quitter.getPlayerSlot().unbindClient();

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
	 * {@link #tickRate} milliseconds. Since the tick timer is on a different thread, and {@link #onReceive(String)}
	 * is called in a different thread, both of these methods are synchronized so that there won't be any
	 * ConcurrentModificationException thrown as a result of 2 or more threads looping through {@link #connectedClients}
	 */
	private void onTick()
	{
		doSnakeMovements();
		updateAllClients(false);
		MessagePacket tickPacket = new MessagePacket("Server", "SERVER TICK");
		sendToAll(tickPacket);
	}

	private void doSnakeMovements()
	{		
		HashMap<SnakeData, Point> oldTailLocations = new HashMap<>();

		// Move each snake forward by one, store old tail location in HashMap. This is in a separate loop because ALL client
		// positions must be updated before doing collision checks
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
				head = GameplayGUI.keepInBounds(head);

				points.add(0, head);
				points.remove(points.size() - 1);
				aClient.setPointList(points);
			}
		}

		// Handle collision, fruit pickup/spawning, segment adding
		for (SnakeData thisSnake : connectedClients)
		{
			if (thisSnake.isAlive())
			{
				Point head = thisSnake.getPointList(false).get(0);
				boolean colliding = false;
				SnakeList otherClients = new SnakeList();
				for (SnakeData bClient : connectedClients)
					if (!bClient.equals(thisSnake) && bClient.isAlive()) // Collision upon dead snakes does not occur
						otherClients.add(bClient);

				// If this Snake's head location intercepts any segment on any OTHER snake, it counts as a collision
				SnakeData interceptingSnake = thisSnake;
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

				int headOccurance = thisSnake.getOccurrenceOf(head);
				// If this Snake's head location intercepts any of its own body segments more than once, it counts as a collision
				colliding = headOccurance > 2 || headOccurance > 1 && thisSnake.hasBuffHungry() ? true : colliding;

				// Set colliding to false if this Snake currently has the Translucent buff
				colliding = thisSnake.hasBuffTranslucent() ? false : colliding;

				// Special collision case if the user has the Hungry buff, which allows them to eat small portions of other snakes
				if (colliding && thisSnake.hasBuffHungry())
				{
					System.out.println("Collided but has hungry buffy");
					colliding = false;
					if (interceptingSnake == thisSnake)
						interceptingIndex = Server.getInterceptingIndexSpecificSnake(head, thisSnake);

					int howManyBitOff = interceptingSnake.getLength() - interceptingIndex;
					System.out.println("howManyBitOff = " + howManyBitOff);

					boolean selfBite = interceptingSnake.equals(thisSnake);
					System.out.println("selfBite = " + selfBite);

					// You cannot hit an enemy Snake's head, even if you have the hungry buff
					boolean notInterceptingHead = interceptingIndex != 0;
					System.out.println("notInterceptingHead = " + notInterceptingHead);

					// If this boolean is true, the intercepting snake is short enough that they can get bit at any non-head index
					boolean shortEnough = interceptingSnake.getLength() < Fruit.COMPLEX_CHECK_MIN;
					System.out.println("shortEnough = " + shortEnough);

					boolean didntBiteTooMuch = howManyBitOff <= Fruit.MAX_BITE_OFF;
					System.out.println("dintBiteTooMuch = " + didntBiteTooMuch);

					if (notInterceptingHead && (selfBite || shortEnough || didntBiteTooMuch))
					{
						int length = interceptingSnake.getLength();
						PointList clone = interceptingSnake.getPointList(true);
						PointList bitOff = new PointList();
						for (int i = length - 1; i >= interceptingIndex; bitOff.add(clone.get(i)), clone.remove(i), i--);
						interceptingSnake.setPointList(clone);
						thisSnake.modifyFoodInBelly(howManyBitOff);
						interceptingSnake.modifyFoodInBelly(-1*interceptingSnake.getFoodInBelly());
						thisSnake.removeHungryBuffEarly();
						interceptingSnake.removeAllBuffsEarly();
						sendPointListNextUpdate();
						SnakeBitePacket snakeBite = new SnakeBitePacket(thisSnake.getClientName(), interceptingSnake.getClientName(), bitOff);
						sendToAll(snakeBite);
					}
					else // Bit off more than they can chew
					{
						colliding = true;
					}
				}

				thisSnake.setIsAlive(!colliding);			

				// If client still alive, handle other stuff such as adding segments and picking up fruit
				if (thisSnake.isAlive())
				{
					// Handle fruit pickup and the subsequent fruit spawning
					if (hasInterceptingFruit(head))
					{
						Fruit pickingUp = getInterceptingFruit(head);
						if (!pickingUp.hasAssociatedBuff() || !thisSnake.hasAnyBuffs())
						{
							// Remove this fruit from the fruit list, and add its segment value to snake belly
							allFruit.remove(pickingUp);
							int fruitValue = pickingUp.getFruitType().getNumSegmentsGiven();
							thisSnake.modifyFoodInBelly(fruitValue);

							// Inform all clients that a player picked up a fruit so the Fruit is no longer drawn
							FruitPickupPacket pack = new FruitPickupPacket(thisSnake.getClientName(), pickingUp);
							sendToAll(pack); 

							// If this fruit has an associated buff, grant it to the player
							if (pickingUp.hasAssociatedBuff())
								thisSnake.grantBuff(pickingUp.getAssociatedBuff());

							// Spawn in a new Fruit somewhere else, send a FruitSpawnPacket to all clients
							spawnRandomFruit();	
						}	
					}

					// Handle the snake adding more segments as a result of recently eating a Fruit
					if (thisSnake.hasFoodInBelly())
					{
						Point addSegmentHere = oldTailLocations.get(thisSnake);
						thisSnake.addToPointList(addSegmentHere);
						thisSnake.modifyFoodInBelly(-1);
						thisSnake.setAddingSegment(true);
					}
					else
						thisSnake.setAddingSegment(false);
				}
				else
				{
					// Collision occurred and the client is now dead, so send a SnakeDeathPacket
					SnakeDeathPacket snakeDeathPack = new SnakeDeathPacket(thisSnake.getClientName());
					sendToAll(snakeDeathPack);
				}
			}
		}
	}
	
	private boolean forceSendPointList = false;

	private void sendPointListNextUpdate()
	{
		forceSendPointList = true;
	}
	
	private void updateAllClients(boolean sendPointList)
	{
		if (forceSendPointList == true)
		{
			forceSendPointList = false;
			sendPointList = true;
		}
		
		for (SnakeData client : connectedClients)
		{
			String[] excluding = sendPointList ? null : new String[] { "pointList" };
			SnakeData clientClone = new SnakeData(ReflectionTools.fieldsToString(SnakeData.REGEX, client, SnakeData.class, excluding));
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
			// If we have greater than a 1/20 chance of random spawning one, do this
			if (getPercentCovered() < 0.95)
			{
				int randX = rand.nextInt(NUM_HORIZONTAL_UNITS);
				int randY = rand.nextInt(NUM_VERTICAL_UNITS);
				Point theoreticalFruitLoc = new Point(randX, randY);
				if (!hasInterceptingFruit(theoreticalFruitLoc) && !hasInterceptingSnake(theoreticalFruitLoc))
				{
					addedFruit = new Fruit(this, theoreticalFruitLoc);
					spawned = true;
				}
			}
			// If >95% of the map is covered, it is probably more efficient to just loop through a list of available spaces and choose a random index
			else if (getPercentCovered() < 1)
			{
				ArrayList<Point> availableLocations = new ArrayList<>();
				for (int x = 0; x < NUM_HORIZONTAL_UNITS; x++)
					for (int y = 0; y < NUM_VERTICAL_SPACES; y++)
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

	/**
	 * Obtains the total amount of squares occupied by all 4 Snakes combined.
	 * @return the number of covered squares.
	 */
	public int getCoveredArea()
	{
		int covered = 0;
		for (SnakeData snake : connectedClients)
			covered += snake.getLength();
		return covered;
	}

	/**
	 * Determines the percent of the map that is covered by Snake segments.
	 * on it).
	 * @return the covered squares / total squares.
	 */
	public double getPercentCovered()
	{
		return (double) getCoveredArea() / MAX_AREA;
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
		exiting.getPacketReceiveThread().end();
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

	private SlotList slots = new SlotList();

	{
		new PlayerSlot(1);
		new PlayerSlot(2);
		new PlayerSlot(3);
		new PlayerSlot(4);
	}

	public class PlayerSlot
	{
		private int slotNum;
		private SnakeData connectedPlayer;
		private SnakeData originalConfiguration;

		public PlayerSlot(int playerNum)
		{
			this.slotNum = playerNum;
			slots.add(this);
		}

		public void bindClient(SnakeData client)
		{
			client.onSlotBind(this);
			client.setColor(this.getSlotColor());
			client.setPointList(this.getStartPointList());
			client.setDirection(this.getStartDirection());
			client.setPlayerNum(slotNum);
			originalConfiguration = client.clone();
			connectedPlayer = client;
		}

		public void unbindClient()
		{
			connectedPlayer.onSlotUnbind();
			connectedPlayer = null;
		}

		public SnakeData getConnectedPlayer()
		{
			return connectedPlayer;
		}

		public int getPlayerNum()
		{
			return slotNum;
		}

		public boolean isEmpty()
		{
			return connectedPlayer == null;
		}

		public Color getSlotColor()
		{
			return MultiplayerSnakeGame.getColorFromSlotNum(slotNum);
		}

		public PointList getStartPointList()
		{
			return MultiplayerSnakeGame.getStartPointListFromSlotNum(slotNum);
		}

		public Direction getStartDirection()
		{
			return MultiplayerSnakeGame.getStartDirectionFromSlotNum(slotNum);
		}

		public SnakeData getOriginalConfiguration()
		{
			return originalConfiguration;
		}
	}

	public static class SlotList extends ArrayList<PlayerSlot>
	{
		private static final long serialVersionUID = 6090296200333287802L;

		public PlayerSlot getNextEmptySlot()
		{
			for (PlayerSlot slot : this)
				if (slot.isEmpty())
					return slot;
			return null;
		}

		public ArrayList<PlayerSlot> getOccupiedSlots()
		{
			ArrayList<PlayerSlot> occupiedSlots = new ArrayList<>();
			for (PlayerSlot slot : this)
				if (!slot.isEmpty())
					occupiedSlots.add(slot);
			return occupiedSlots;
		}
	}

	private void rollBackClients()
	{
		for (PlayerSlot slot : slots.getOccupiedSlots())
		{
			SnakeUpdatePacket toResetSharedFieldsToDefault = new SnakeUpdatePacket(slot.getOriginalConfiguration());
			slot.getConnectedPlayer().updateBasedOn(toResetSharedFieldsToDefault);
		}
	}
}