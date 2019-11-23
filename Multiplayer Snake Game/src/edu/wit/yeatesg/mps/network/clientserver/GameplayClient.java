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
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Direction;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Vector;

public class GameplayClient extends JPanel implements ClientListener, KeyListener, WindowListener
{	
	private static final long serialVersionUID = 5573784413946297734L;

	public static final int SSL = 3;

	public static final int JAR_OFFSET_X = 9;
	public static final int JAR_OFFSET_Y = 10;

	public static final int NUM_HORIZONTAL_UNITS = 80;
	public static final int NUM_HORIZONTAL_SPACES = NUM_HORIZONTAL_UNITS + 1;
	public static final int NUM_VERTICAL_UNITS = 40;
	public static final int NUM_VERTICAL_SPACES = NUM_VERTICAL_UNITS + 1;
	public static final int UNIT_SIZE = 20; // Pixels
	public static final int SPACE_SIZE = 3; 
	public static final int MAX_AREA = GameplayClient.NUM_HORIZONTAL_UNITS*GameplayClient.NUM_VERTICAL_UNITS;

	public static final int MAX_OUTLINE_THICKNESS = UNIT_SIZE / 2;

	public static final int WIDTH = NUM_HORIZONTAL_UNITS * UNIT_SIZE + NUM_HORIZONTAL_SPACES * SPACE_SIZE + JAR_OFFSET_X;
	public static final int HEIGHT = NUM_VERTICAL_UNITS * UNIT_SIZE + NUM_VERTICAL_SPACES * SPACE_SIZE + JAR_OFFSET_Y;

	public static final int MAX_NAME_LENGTH = 17;

	public GameplayClient(NetworkClient internal, SnakeData thisClient, SnakeList allClients)
	{
		this.thisClient = thisClient;
		this.allClients = allClients;
		new SnakeGameGUI();
		internal.setListener(this);
	}

	private DataOutputStream out;

	@Override
	public void setOutputStream(DataOutputStream out)
	{
		this.out = out;
	}

	@Override
	public void onReceive(String data)
	{
		Packet packetReceiving = Packet.parsePacket(data);
//		System.out.println(thisClient.getClientName() + " received -> " + packetReceiving);

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

	private SnakeData thisClient;
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
		SnakeData whoPickedUp = allClients.get(packetReceiving.getWhoPickedUp());
		fruitList.remove(packetReceiving.getFruit());
		if (theFruit.hasAssociatedBuff())
		{
			BuffType buffToGrant = theFruit.getAssociatedBuff();
			buffToGrant.startDrawScript(this, whoPickedUp);
		}
	}
	
	private void onSnakeDeathPacketReceive(SnakeDeathPacket packetReceiving)
	{
		SnakeData whoDied = allClients.get(packetReceiving.getSnakeName());
		new DeadSnakeDrawScript(this, whoDied);
	}
	
	private void onSnakeBitePacketReceive(SnakeBitePacket packetReceiving)
	{
		SnakeData bitten = allClients.get(packetReceiving.getBitten());
		SnakeData biter = allClients.get(packetReceiving.getBiting());
		biter.endBuffDrawScript();
		int interceptingIndex = packetReceiving.getInterceptingIndex();
		PointList clone = bitten.getPointList();
		PointList bitOff = new PointList();
		for (int i = clone.size() - 1; i >= interceptingIndex; bitOff.add(clone.get(i)), clone.remove(i), i--);
		bitten.setPointList(clone);
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

	private void onOtherClientDisconnect(SnakeData leaver)
	{
		new DeadSnakeDrawScript(this, leaver);
		Timer removeTimer = new Timer(DeadSnakeDrawScript.DURATION, null);
		removeTimer.setRepeats(false);
		removeTimer.addActionListener((e) -> allClients.remove(allClients.indexOf(leaver)));
		removeTimer.start();
	}

	private void onThisClientDisconnect()
	{
		System.exit(0);
	}
	
	public ArrayList<TickListener> tickListeners = new ArrayList<TickListener>();

	public void onServerTick()
	{
		repaint();
		for (TickListener listener : tickListeners)
			listener.onReceiveTick();
	}
	
	public synchronized void addTickListener(TickListener listener)
	{
		tickListeners.add(listener);
	}
	
	public synchronized void removeTickListener(TickListener beingRemoved)
	{
		tickListeners.remove(beingRemoved);
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

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, WIDTH, HEIGHT);

		if (gameStartScript == null)
			for (Fruit f : fruitList)
				f.draw(g);

		if (gameStartScript != null)
			gameStartScript.draw(g);

		for (SnakeData dat : allClients)
			dat.draw(g);
	}

	public static final int getPixelCoord(int segmentCoord)
	{
		return SPACE_SIZE + (SPACE_SIZE + UNIT_SIZE) * segmentCoord;
	}

	public static Point getPixelCoords(Point segmentCoords)
	{
		return new Point(getPixelCoord(segmentCoords.getX()), getPixelCoord(segmentCoords.getY()));
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (Direction.fromKeyCode(e.getKeyCode()) != null)
		{
			Direction entered = Direction.fromKeyCode(e.getKeyCode());
			DirectionChangePacket pack = new DirectionChangePacket(thisClient.getClientName(), entered);
			pack.setDataStream(out);
			pack.send();
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
		exitPack.setDataStream(out);
		exitPack.send();
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
				!text.contains(Packet.REGEX) &&
				!text.contains(SnakeData.REGEX) &&
				!text.contains(SnakeList.REGEX) &&
				!text.contains(Point.REGEX) &&
				!text.contains(Fruit.REGEX) &&
				!text.contains(Vector.REGEX) &&
				!text.equals("");
	}
	
	public static Point keepInBounds(Point head)
	{
		head = head.clone();
		if (head.getX() > GameplayClient.NUM_HORIZONTAL_UNITS - 1)
			head.setX(0);
		else if (head.getX() < 0)
			head.setX(GameplayClient.NUM_HORIZONTAL_UNITS - 1);
		else if (head.getY() > GameplayClient.NUM_VERTICAL_UNITS - 1)
			head.setY(0);
		else if (head.getY() < 0)
			head.setY(GameplayClient.NUM_VERTICAL_UNITS - 1);
		return head;
	}

	// Frame

	public class SnakeGameGUI extends JFrame
	{	
		private static final long serialVersionUID = -1155890718213904522L;

		public SnakeGameGUI()
		{
			setTitle(thisClient.getClientName());
			setContentPane(GameplayClient.this);
			ConnectClient.setLookAndFeel();
			setBounds(0, 0, GameplayClient.WIDTH + 6, GameplayClient.HEIGHT + 29);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setResizable(false);
			addWindowListener(GameplayClient.this);
			addKeyListener(GameplayClient.this);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setVisible(true);
		}
	}
}