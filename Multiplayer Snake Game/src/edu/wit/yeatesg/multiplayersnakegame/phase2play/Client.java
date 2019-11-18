package edu.wit.yeatesg.multiplayersnakegame.phase2play;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JPanel;
import javax.swing.Timer;

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

public class Client extends JPanel implements PacketListener
{	
	private static final long serialVersionUID = 5573784413946297734L;

	public static final int SSL = 3;

	public static final int NUM_HORIZONTAL_UNITS = 30;
	public static final int NUM_HORIZONTAL_SPACES = NUM_HORIZONTAL_UNITS + 1;
	public static final int NUM_VERTICAL_UNITS = 20;
	public static final int NUM_VERTICAL_SPACES = NUM_VERTICAL_UNITS + 1;
	public static final int UNIT_SIZE = 30; // Pixels
	public static final int SPACE_SIZE = 3; 

	public static final int WIDTH = NUM_HORIZONTAL_UNITS * UNIT_SIZE + NUM_HORIZONTAL_SPACES * SPACE_SIZE;
	public static final int HEIGHT = NUM_VERTICAL_UNITS * UNIT_SIZE + NUM_VERTICAL_SPACES * SPACE_SIZE;

	public static final int MAX_NAME_LENGTH = 17;

	public InitiateGamePaintScript currentInitiateScript = null;

	boolean serverPaused = false;

	private SnakeData thisClient;
	private SnakeList allClients;

	private DataInputStream in;
	private DataOutputStream out;

	private GameGUI frame;
	
	public Client(Socket s, PacketReceiver receiver, SnakeList allClients, SnakeData thisClient)
	{
		receiver.setReceiving("Clint");
		receiver.setListener(this);
		this.thisClient = thisClient;
		this.allClients = allClients;

		System.out.println("NU NU CLINT");
	}
	
	public void setFrame(GameGUI frame)
	{
		this.frame = frame;
	}

	public void onPacketReceive(Packet packetReceiving)
	{
		if (packetReceiving instanceof InitiateGamePacket)
		{
			onGameStart((InitiateGamePacket) packetReceiving);
		}
		else if (packetReceiving instanceof SnakeUpdatePacket)
		{
			//	SnakeData dat = ((SnakeUpdatePacket) packetReceiving).getClientData();
			allClients.updateBasedOn((SnakeUpdatePacket) packetReceiving);
		}
		else if (packetReceiving instanceof MessagePacket)
		{
			MessagePacket msgPacket = (MessagePacket) packetReceiving;
			switch (msgPacket.getMessage())
			{
			case "THEY_EXIT":
				onPlayerLeaveGame(allClients.get(msgPacket.getSender()));
				break;
			case "YOU_EXIT":
				onExit();
				break;
			}
		}
		else if (packetReceiving instanceof TickPacket)
		{
			System.out.println("TEEK");
			repaint();
		}
	}

	private void onPlayerLeaveGame(SnakeData leaver)
	{

	}


	private void onGameStart(InitiateGamePacket pack)
	{
		currentInitiateScript = new InitiateGamePaintScript(pack);
		// Open a GameGUI on the port. Make sure the server sends all the clientData to the newly created GameGUI's!
	}


	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		setSize(WIDTH + 100, HEIGHT + 900);
		if (currentInitiateScript != null)
		{
			System.out.println("ARBYS: WE HAVE THE INITIATION SCRIPTS");
			currentInitiateScript.draw(g);
		}

		System.out.println("DRAW TIME BABY, ALL CLIENTS LEN = " + allClients.size());
		for (SnakeData dat : allClients)
		{
			System.out.println("I MUST DRAW " + dat.getPointList());
			dat.draw(g);
		}

		// TODO Auto-generated method stub
	}

	public static final int getPixelCoord(int segmentCoord)
	{
		return SPACE_SIZE + (SPACE_SIZE + UNIT_SIZE) * segmentCoord;
	}

	public static Point getPixelCoords(Point segmentCoords)
	{
		return new Point(getPixelCoord(segmentCoords.getX()), getPixelCoord(segmentCoords.getY()));
	}

	private void onExit()
	{
		System.exit(0);
	}


	class InitiateGamePaintScript extends Timer implements ActionListener
	{
		private static final long serialVersionUID = 7481429617463613490L;

		private int currentTick;
		private int maxTicks;

		public InitiateGamePaintScript(InitiateGamePacket pack)
		{
			super(pack.getTickRate(), null);
			pack.setInitialDelay(pack.getInitialDelay());
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
				currentInitiateScript = null;
			}
		}

		public void draw(Graphics g)
		{
			g.setFont(new Font("Times New Roman", Font.BOLD, 80));
			g.setColor(Color.WHITE);
			if (currentTick == 0)
			{
				g.drawString("Game Starting Soon", 250, 250);
			}
			else
			{
				g.drawString(currentTick + "", 250, 250);
			}
		}
	}

	public static boolean validName(String text)
	{
		text = text.toLowerCase();
		return !text.contains("server") &&
				!text.contains("null") &&
				!text.contains(PointList.REGEX) &&
				!(text.length() > MAX_NAME_LENGTH) &&
				!text.contains(Packet.REGEX) &&
				!text.contains(SnakeData.REGEX) &&
				!text.contains(SnakeList.REGEX) &&
				!text.contains(Point.REGEX);
	}


}

