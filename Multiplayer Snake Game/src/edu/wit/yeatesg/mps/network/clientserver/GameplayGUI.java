package edu.wit.yeatesg.mps.network.clientserver;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataOutputStream;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import edu.wit.yeatesg.mps.buffs.BuffType;
import edu.wit.yeatesg.mps.buffs.DeadSnakeDrawScript;
import edu.wit.yeatesg.mps.buffs.Fruit;
import edu.wit.yeatesg.mps.buffs.SnakeBiteDrawScript;
import edu.wit.yeatesg.mps.buffs.TickListener;
import edu.wit.yeatesg.mps.network.clientserver.MPSClient.ClientListener;
import edu.wit.yeatesg.mps.network.packets.DebuffReceivePacket;
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
import edu.wit.yeatesg.mps.otherdatatypes.Snake;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeList;
import edu.wit.yeatesg.mps.otherdatatypes.Vector;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

public class GameplayGUI extends JPanel implements ClientListener, KeyListener, WindowListener
{	
	private static final long serialVersionUID = 5573784413946297734L;

	public static final int WIDTH = MultiplayerSnakeGame.WIDTH;
	public static final int HEIGHT = MultiplayerSnakeGame.HEIGHT;

	private MPSClient networkClient;

	public GameplayGUI(MPSClient networkClient, Snake thisClient, SnakeList allClients)
	{
		this.thisClient = thisClient;
		this.allClients = allClients;
		this.networkClient = networkClient;
		networkClient.setListener(this);
		new GameplayFrame();
	}

	@Override
	public void onAutoReceive(Packet packetReceiving)
	{
		System.out.println(thisClient.getClientName() + " Receive -> " + packetReceiving);		
		switch (packetReceiving.getClass().getSimpleName())
		{
		case "InitiateGamePacket":
			onReceiveInitiateGamePacket((InitiateGamePacket) packetReceiving);
			break;
		case "SnakeUpdatePacket":
			onReceiveSnakeUpdatePacket((SnakeUpdatePacket) packetReceiving);
			break;
		case "DebuffReceivePacket":
			onReceiveDebuffPacket((DebuffReceivePacket) packetReceiving);
			break;
		case "FruitSpawnPacket":
			onReceiveFruitSpawnPacket((FruitSpawnPacket) packetReceiving);
			break;
		case "FruitPickupPacket":
			onReceiveFruitPickupPacket((FruitPickupPacket) packetReceiving);
			break;
		case "SnakeDeathPacket":
			onSnakeDeathPacketReceive((SnakeDeathPacket) packetReceiving);
			break;
		case "MessagePacket":
			onMessagePacketReceive((MessagePacket) packetReceiving);
			break;
		case "SnakeBitePacket":
			onSnakeBitePacketReceive((SnakeBitePacket) packetReceiving);
			break;
		}
	}

	public GameStartDrawScript gameStartScript = null;

	private void onReceiveInitiateGamePacket(InitiateGamePacket pack)
	{
		gameStartScript = new GameStartDrawScript(pack);
	}

	private Snake thisClient;
	private SnakeList allClients;

	private void onReceiveSnakeUpdatePacket(SnakeUpdatePacket pack)
	{
		allClients.updateBasedOn(pack);
	}

	private void onReceiveDebuffPacket(DebuffReceivePacket debuffPacket)
	{
		BuffType buff = debuffPacket.getBuff();
		buff.startDrawScript(this, allClients.get(debuffPacket.getReceiver()));
	}

	private ArrayList<Fruit> fruitList = new ArrayList<>();

	private void onReceiveFruitSpawnPacket(FruitSpawnPacket packetReceiving)
	{
		fruitList.add(packetReceiving.getFruit());
	}

	private void onReceiveFruitPickupPacket(FruitPickupPacket packetReceiving)
	{
		Fruit theFruit = packetReceiving.getFruit();

		Snake whoPickedUp = allClients.get(packetReceiving.getWhoPickedUp());
		fruitList.remove(packetReceiving.getFruit());
		if (theFruit.hasAssociatedBuff())
		{
			BuffType buffToGrant = theFruit.getAssociatedBuff();
			buffToGrant.startDrawScript(this, whoPickedUp);
		}
	}

	private void onSnakeDeathPacketReceive(SnakeDeathPacket packetReceiving)
	{
		Snake whoDied = allClients.get(packetReceiving.getSnakeName());
		new DeadSnakeDrawScript(this, whoDied);
	}

	private void onSnakeBitePacketReceive(SnakeBitePacket packetReceiving)
	{
		Snake bitten = allClients.get(packetReceiving.getBitten());
		Snake bitBy = allClients.get(packetReceiving.getBiting());
		bitBy.endBuffDrawScriptEarly();
		bitten.endBuffDrawScriptEarly();
		PointList bitOff = packetReceiving.getBitOff();	
		new SnakeBiteDrawScript(this, bitten, bitOff);
	}

	private void onMessagePacketReceive(MessagePacket msgPacket)
	{
		switch (msgPacket.getMessage())
		{
		case "THEY EXIT":
			onOtherClientDisconnect(allClients.get(msgPacket.getSender()));
			break;
		case "YOU EXIT":
			onThisClientDisconnect();
			break;
		case "SERVER TICK":
			onServerTick();
			break;
		}
	}

	private void onOtherClientDisconnect(Snake leaver)
	{
		new DeadSnakeDrawScript(this, leaver);
		Timer removeTimer = new Timer(DeadSnakeDrawScript.DURATION, null);
		removeTimer.setRepeats(false);
		removeTimer.addActionListener((e) ->
		{
			allClients.remove(leaver);
		});

		removeTimer.start();
	}

	private void onThisClientDisconnect()
	{
		System.exit(0);
	}

	public ArrayList<TickListener> tickListeners = new ArrayList<TickListener>();

	public void onServerTick()
	{
		synchronized (tickListeners)
		{
			repaint();
			for (TickListener listener : tickListeners)
				listener.onReceiveTick();
		}
	}

	// Synchronized because these methods are called by timer threads in SnakeDrawScript if two scripts start at once, cmod will happen
	public void addTickListener(TickListener listener)
	{
		synchronized (tickListeners)
		{
			tickListeners.add(listener);
		}
	}

	// Synchronized because these methods are called by timer threads in SnakeDrawScript if two scripts end at once, cmod will happen
	public void removeTickListener(TickListener beingRemoved)
	{
		synchronized (tickListeners)
		{
			tickListeners.remove(beingRemoved);
		}
	}

	class GameStartDrawScript extends Timer implements ActionListener
	{
		private static final long serialVersionUID = 7481429617463613490L;

		private int currentTick;
		private int maxTicks;

		public GameStartDrawScript(InitiateGamePacket pack)
		{
			super(pack.getTickRate(), null);
			setInitialDelay(pack.getInitialDelay());
			addActionListener(this);
			currentTick = 0;
			maxTicks = pack.getNumTicks();
			repaint();
			start();
		}

		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			currentTick++;
			repaint();
			if (currentTick > maxTicks)
			{
				this.stop();
				gameStartScript = null;
			}
		}

		public void draw(Graphics g)
		{
			int drawX, drawY;
			g.setFont(new Font("Times New Roman", Font.BOLD, 80));
			g.setColor(Color.WHITE);
			if (currentTick == 0)
			{
				drawX = WIDTH / 2 - 375;
				drawY = HEIGHT / 2;
				g.drawString("Game Starting Soon", drawX, drawY);
			}
			else
			{
				drawX = WIDTH / 2 - 60;
				drawY = HEIGHT / 2 + 50;
				g.setFont(new Font("Times New Roman", Font.BOLD, 200));
				switch (currentTick)
				{
				case 1:
					g.drawString(3 + "", drawX, drawY);
					break;
				case 2:
					g.drawString(2 + "", drawX, drawY);
					break;
				case 3:
					g.drawString(1 + "", drawX, drawY);
					break;
				}
			}
		}
	}

	// Synchronized because multiple threads can call paintComponent (draw script timer threads and this thread). Probs not necessary
	// to synchronize because the threads that call paintComponent will never make any
	// modifications to the allClients list, which is the only thread-unsafe thing we are
	// dealing with
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, WIDTH, HEIGHT);

		for (Snake client : allClients)
			client.drawNormallyIfApplicable(g, this);

		for (Snake client : allClients)
			client.drawScriptIfApplicable(g);

		if (gameStartScript == null)
			for (Fruit f : fruitList)
				f.draw(g);

		if (gameStartScript != null)
			gameStartScript.draw(g);
	}

	public static final int getPixelCoord(int segmentCoord)
	{
		return SPACE_SIZE + (SPACE_SIZE + UNIT_SIZE) * segmentCoord;
	}

	public static Point getPixelCoords(Point segmentCoords)
	{
		return new Point(getPixelCoord(segmentCoords.getX()), getPixelCoord(segmentCoords.getY()));
	}

	/**
	 * Obtains a reference to the Client that is connected to this GameplayClient JPanel
	 * @return a SnakeData representing the client
	 */
	public Snake getClient()
	{
		return thisClient;
	}

	/**
	 * Obtains a list of the Clients in this game that are not equal to the Client that this GameplayClient
	 * is connected to
	 * @return a SnakeList of the other clients
	 */
	public SnakeList getOtherClients()
	{
		SnakeList clone = getAllClients();
		clone.remove(getClient());
		return clone;
	}

	/**
	 * Obtains a list of all of the clients that this client is connected to, including itself
	 * @return a SnakeList containing these clients
	 */
	public SnakeList getAllClients()
	{
		return allClients.clone();	
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (Direction.fromKeyCode(e.getKeyCode()) != null)
		{
			Direction entered = Direction.fromKeyCode(e.getKeyCode());
			DirectionChangePacket pack = new DirectionChangePacket(thisClient.getClientName(), entered);
			networkClient.send(pack);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) { }

	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void windowClosing(WindowEvent e)
	{
		MessagePacket exitPack = new MessagePacket(thisClient.getClientName(), "I EXIT");
		networkClient.send(exitPack);
		new Timer(1500, (a) -> System.exit(0)).start();
	}

	@Override
	public void windowActivated(WindowEvent arg0) { }

	@Override
	public void windowClosed(WindowEvent e) { }

	@Override
	public void windowDeactivated(WindowEvent e) { }

	@Override
	public void windowDeiconified(WindowEvent e) { }

	@Override
	public void windowIconified(WindowEvent e) { }

	@Override
	public void windowOpened(WindowEvent e) { }

	public static boolean validName(String text)
	{
		text = text.toUpperCase();
		return !text.contains("SERVER") &&
				!text.contains("NULL") &&
				!text.contains(PointList.REGEX) &&
				!(text.length() > MAX_NAME_LENGTH) &&
				!(text.length() < 2) &&
				!text.contains(Packet.REGEX) &&
				!text.contains(Snake.REGEX) &&
				!text.contains(SnakeList.REGEX) &&
				!text.contains(Point.REGEX) &&
				!text.contains(Fruit.REGEX) &&
				!text.contains(Vector.REGEX) &&
				!text.equals("");
				
	}

	public static Point keepInBounds(Point head)
	{
		head = head.clone();
		if (head.getX() > NUM_HORIZONTAL_UNITS - 1)
			head.setX(0);
		else if (head.getX() < 0)
			head.setX(NUM_HORIZONTAL_UNITS - 1);
		else if (head.getY() > NUM_VERTICAL_UNITS - 1)
			head.setY(0);
		else if (head.getY() < 0)
			head.setY(NUM_VERTICAL_UNITS - 1);
		return head;
	}

	// Frame

	public class GameplayFrame extends JFrame
	{	
		private static final long serialVersionUID = -1155890718213904522L;

		public GameplayFrame()
		{
			setTitle(thisClient.getClientName());
			setContentPane(GameplayGUI.this);
			setBounds(0, 0, MultiplayerSnakeGame.WIDTH + 6, MultiplayerSnakeGame.HEIGHT + 29);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setResizable(false);
			addWindowListener(GameplayGUI.this);
			addKeyListener(GameplayGUI.this);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setVisible(true);
		}
	}
}