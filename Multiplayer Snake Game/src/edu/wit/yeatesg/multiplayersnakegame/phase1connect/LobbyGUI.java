package edu.wit.yeatesg.multiplayersnakegame.phase1connect;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.Color;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.Direction;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.Point;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.PointList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.MessagePacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.Packet;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.SnakeUpdatePacket;
import edu.wit.yeatesg.multiplayersnakegame.phase2play.GameGUI;
import edu.wit.yeatesg.multiplayersnakegame.server.Server;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JButton;

public class LobbyGUI extends JFrame
{
	private static final long serialVersionUID = 4339194739358327310L;

	private JPanel contentPane;	

	private boolean gameStarted = false;
	
	private String thisClientName;
	private SnakeList allClients;
	private DataOutputStream outputStream;
	
	private int x;
	private int y;
	
	public LobbyGUI(String clientName, Socket clientSocket, DataInputStream inputStream, DataOutputStream outputStream, String serverName, String serverPort, int x, int y)
	{
		this.x = x;
		this.y = y;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		thisClientName = clientName;
		this.outputStream = outputStream;
		allClients = new SnakeList();
		initFrame();
		ConnectGUI.setLookAndFeel();
		setVisible(true);
		
		Thread inputThread = new Thread(() ->
		{
			try
			{
				while (!gameStarted)
				{
					System.out.println("waiting for packet");
					Packet packetReceiving = Packet.parsePacket(inputStream.readUTF());
					System.out.println(packetReceiving.getUTF());
					onPacketReceive(packetReceiving);
				}						
			}
			catch (IOException e)
			{
				System.out.println(e.getStackTrace());
			}
		});		

		inputThread.start();
	}

	boolean firstUpdatePacketReceived = false;
	
	private void onPacketReceive(Packet packetReceiving)
	{
		if (packetReceiving instanceof SnakeUpdatePacket)
		{
			allClients.updateBasedOn((SnakeUpdatePacket) packetReceiving);
		}
		else if (packetReceiving instanceof MessagePacket)
		{
			MessagePacket msgPacket = (MessagePacket) packetReceiving;
			switch (msgPacket.getMessage())
			{
			case "SOMEONE_JOIN":
				onPlayerJoinLobby(allClients.get(allClients.size() - 1)); // The client at size - 1 is the one who just joined
				break;
			case "THEY_EXIT":
				onPlayerLeaveLobby(allClients.get(msgPacket.getSender()));
				break;
			case "YOU_EXIT":
				onExit();
				break;
			case "GAME_START":
				onGameStart();
				break;
			}
		}
	}
	
	private void onStartGamePress()
	{
		
	}
	
	private void onDisconnectPress()
	{
		MessagePacket exitPacket = new MessagePacket(thisClientName, "I_EXIT");
		exitPacket.setDataStream(outputStream);
		exitPacket.send();
	}

	private void onGameStart()
	{
		// Open a GameGUI on the port. Make sure the server sends all the clientData to the newly created GameGUI's!
	}

	private void onExit()
	{
		System.exit(0);
	}

	private void onPlayerJoinLobby(SnakeData whoJoinedOnLastUpdate)
	{
		if (whoJoinedOnLastUpdate.isHost() && whoJoinedOnLastUpdate.getClientName().equals(thisClientName))
			button_startGame.setEnabled(true);			
		PlayerPanel emptyPanel = getEmptyPlayerPanel();
		emptyPanel.connectClient(whoJoinedOnLastUpdate);
	}

	private void onPlayerLeaveLobby(SnakeData whoLeftOnLastUpdate)
	{
		PlayerPanel leaversPanel = getConnectedPlayerPanel(whoLeftOnLastUpdate);
		leaversPanel.disconnectClient();
		allClients.remove(whoLeftOnLastUpdate);
	}
	
	private ArrayList<PlayerPanel> playerPanelList;

	private PlayerPanel getConnectedPlayerPanel(SnakeData client)
	{
		for (PlayerPanel panel : playerPanelList)
			if (client.equals(panel.getConnectedClient()))
				return panel;
		return null;
	}
	
	private PlayerPanel getEmptyPlayerPanel()
	{
		for (PlayerPanel playerPanel : playerPanelList)
			if (!playerPanel.hasConnectedClient())
				return playerPanel;
		return null;
	}
	
	private JButton button_startGame;
	
	private void initFrame()
	{
		setTitle(thisClientName);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(x, y, PlayerPanel.GAP*3 + PlayerPanel.WIDTH*2 + 20, PlayerPanel.GAP*3 + PlayerPanel.HEIGHT*2 + 80);
		setResizable(false);
		
		addWindowListener(new WindowListener()
		{	
			@Override
			public void windowClosing(WindowEvent e)
			{
				onDisconnectPress();
			}
			
			public void windowOpened(WindowEvent e) { }
			public void windowIconified(WindowEvent e) { }
			public void windowDeiconified(WindowEvent e) { }
			public void windowDeactivated(WindowEvent e) { }
			public void windowClosed(WindowEvent e) { }
			public void windowActivated(WindowEvent e) { }
		});
		
		contentPane = new JPanel();
		contentPane.setBackground(Color.BLACK);
		contentPane.setForeground(Color.WHITE);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(null);
		setContentPane(contentPane);
		
		int panxy = PlayerPanel.GAP;
		int xinc = PlayerPanel.WIDTH + PlayerPanel.GAP;
		int yinc = PlayerPanel.HEIGHT + PlayerPanel.GAP;
		playerPanelList = new ArrayList<>();
		playerPanelList.add(new PlayerPanel(panxy, panxy, contentPane));
		playerPanelList.add(new PlayerPanel(panxy + xinc, panxy, contentPane));
		playerPanelList.add(new PlayerPanel(panxy, panxy + yinc, contentPane));
		playerPanelList.add(new PlayerPanel(panxy + xinc, panxy + yinc, contentPane));
		
		button_startGame = new JButton("Start Game");
		button_startGame.addActionListener((e) -> onStartGamePress());
		button_startGame.setForeground(Color.BLACK);
		button_startGame.setBackground(Color.BLACK);
		button_startGame.setBounds(PlayerPanel.GAP, PlayerPanel.GAP*2 + 10 + PlayerPanel.HEIGHT*2, PlayerPanel.WIDTH, 25);
		button_startGame.setEnabled(false);
		contentPane.add(button_startGame);
		
		JButton button_disconnect = new JButton("Disconnect");
		button_disconnect.addActionListener((e) -> onDisconnectPress());
		button_disconnect.setForeground(Color.BLACK);
		button_disconnect.setBackground(Color.BLACK);
		button_disconnect.setBounds(button_startGame.getX() + PlayerPanel.GAP + button_startGame.getWidth(), button_startGame.getY(), PlayerPanel.WIDTH, 25);
		contentPane.add(button_disconnect);
		
		setVisible(true);
	}
	
	private static int playerPanelNumAssign = 1;

	class PlayerPanel extends JPanel
	{
		private static final int WIDTH = 150;
		private static final int HEIGHT = 105;
		private static final int GAP = 10;
		
		private static final long serialVersionUID = 5816367030492476277L;
		
		private int playerNum;
		
		private JLabel nameField; 
		
		public PlayerPanel(int x, int y, JPanel contentPane)
		{
			playerNum = playerPanelNumAssign++;

			setBackground(new Color(15, 15, 15));
			setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(255, 255, 255)));
			setBounds(x, y, WIDTH, HEIGHT);
			setLayout(null);

			JLabel playerNumField = new JLabel("Player " + playerNum);
			playerNumField.setForeground(Color.WHITE);
			playerNumField.setFont(new Font("Tahoma", Font.BOLD, 14));
			playerNumField.setBounds(5, 5, 64, 20);
			add(playerNumField);

			nameField = new JLabel();
			nameField.setForeground(Color.WHITE);
			nameField.setFont(new Font("Tahoma", Font.PLAIN, 14));
			nameField.setBounds(5, 30, PlayerPanel.WIDTH - 5, 20);
			add(nameField);
			
			updateConnectedClientInfo();
			
			contentPane.add(this);
		}
		
		public Color getColor()
		{
			switch (playerNum)
			{
			case 1:
				return Color.RED;
			case 2:
				return Color.BLUE;
			case 3:
				return Color.GREEN;
			case 4:
				return Color.YELLOW;
			default:
				return null;
			}
		}
		
		public Direction getStartDirection()
		{
			switch (playerNum)
			{
			case 1:
				return Direction.RIGHT;
			case 2:
				return Direction.LEFT;
			case 3:
				return Direction.DOWN;
			case 4:
				return Direction.UP;
			default:
				return null;
			}
		}
		
		public Point getStartPoint()
		{
			switch (playerNum)
			{
			case 1:
				return new Point(2 + (GameGUI.SSL - 1), 2);
			case 2:
				return new Point(GameGUI.NUM_HORIZONTAL_SPACES - 3 - (GameGUI.SSL - 1), GameGUI.NUM_VERTICAL_SPACES - 3);
			case 3:
				return new Point(GameGUI.NUM_HORIZONTAL_SPACES - 3, 2 + (GameGUI.SSL - 1));
			case 4:
				return new Point(2, GameGUI.NUM_VERTICAL_SPACES - 3 - (GameGUI.SSL - 1));
			default:
				return null;
			}
		}
		
		private int start_length = 3;
		
		public PointList getPointList()
		{
			int xMod = 0, yMod = 0;
			switch (getStartDirection())
			{
			case DOWN:
				yMod = -1;
				break;
			case LEFT:
				xMod = 1;
				break;
			case RIGHT:
				xMod = -1;
				break;
			case UP:
				yMod = 1; 
				break;
			}
			PointList list = new PointList();
			Point start = getStartPoint();
			list.add(start);
			for (int i = 0; i < start_length; i++, list.add(start));
				start.setXY(start.getX() + xMod, start.getY() + yMod);
			return list;
		}
		
		private SnakeData connectedClient = null;
		
		public void connectClient(SnakeData clientData)
		{
			connectedClient = clientData;
			updateConnectedClientInfo();
		}
		
		public void disconnectClient()
		{
			connectedClient = null;
			updateConnectedClientInfo();
		}

		public boolean hasConnectedClient()
		{
			return connectedClient != null;
		}
		
		public SnakeData getConnectedClient()
		{
			return connectedClient;
		}
		
		private void updateConnectedClientInfo()
		{
			nameField.setText(hasConnectedClient() ? connectedClient.getClientName() : "<not connected>");
			if (nameField.getText().equals(thisClientName))
			{
				int r = getColor().getRed() + 100;
				int g = getColor().getGreen() + 100;
				int b = getColor().getBlue() + 100;
				r = r > 255 ? 255 : r;
				g = g > 255 ? 255 : g;
				b = b > 255 ? 255 : b;

				setBorder(new MatteBorder(1, 1, 1, 1, new Color(r, g, b)));
	
			}
			if (hasConnectedClient())
			{
				SnakeData clonedData = this.connectedClient.clone(); // Remember, we don't want the data to actually change
				clonedData.setColor(getColor());                      // until the server approves it, so modify a clone
				clonedData.setDirection(getStartDirection());
				clonedData.setPointList(getPointList());
				SnakeUpdatePacket pack = new SnakeUpdatePacket(clonedData);
				pack.setDataStream(outputStream);
				pack.send();
			}
			repaint();
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			g = g.create();
			super.paintComponent(g);
			g.setColor(getColor());
			
			if (hasConnectedClient())
			{
				int gap = 6;
				int width = 22;
				int x = 36;
				int y = 64;
				for (int i = 0; i < 3; i++)
				{
					g.fillRect(x, y, width, width);
					x += gap + width;
				}
			}
		}

	}
}