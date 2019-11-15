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
	
	JButton button_startGame;
	
	private void initFrame()
	{
		setTitle(thisClientName);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(x, y, 304, 284);
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
		
		playerPanelList = new ArrayList<>();
		playerPanelList.add(new PlayerPanel(10, 10, contentPane));
		playerPanelList.add(new PlayerPanel(150, 10, contentPane));
		playerPanelList.add(new PlayerPanel(10, 110, contentPane));
		playerPanelList.add(new PlayerPanel(150, 110, contentPane));
		
		JButton button_disconnect = new JButton("Disconnect");
		button_disconnect.addActionListener((e) -> onDisconnectPress());
		button_disconnect.setForeground(Color.BLACK);
		button_disconnect.setBackground(Color.BLACK);
		button_disconnect.setBounds(10, 211, 130, 23);
		contentPane.add(button_disconnect);
		
		button_startGame = new JButton("Start Game");
		button_startGame.addActionListener((e) -> onStartGamePress());
		button_startGame.setForeground(Color.BLACK);
		button_startGame.setBackground(Color.BLACK);
		button_startGame.setBounds(150, 211, 130, 23);
		button_startGame.setEnabled(false);
		contentPane.add(button_startGame);
		
		setVisible(true);
	}
	
	private static int playerPanelNumAssign = 1;

	class PlayerPanel extends JPanel
	{
		private static final long serialVersionUID = 5816367030492476277L;
		
		private int playerNum;
		
		private JLabel nameField; 
		
		public PlayerPanel(int x, int y, JPanel contentPane)
		{
			playerNum = playerPanelNumAssign++;

			setBackground(new Color(15, 15, 15));
			setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(255, 255, 255)));
			setBounds(x, y, 130, 90);
			setLayout(null);

			JLabel playerNumField = new JLabel("Player " + playerNum);
			playerNumField.setForeground(Color.WHITE);
			playerNumField.setFont(new Font("Tahoma", Font.BOLD, 14));
			playerNumField.setBounds(5, 5, 64, 20);
			add(playerNumField);

			nameField = new JLabel();
			nameField.setForeground(Color.WHITE);
			nameField.setFont(new Font("Tahoma", Font.PLAIN, 14));
			nameField.setBounds(5, 30, 115, 20);
			add(nameField);
			
			updateConnectedClientInfo();
			
			setVisible(true);
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
			if (hasConnectedClient())
			{
				SnakeData clonedData = this.connectedClient.clone(); // Remember, we don't want the data to actually change
				clonedData.setColor(getColor());                      // until the server approves it, so modify a clone
				clonedData.setDirection(getStartDirection());
// TODO fix fix fix implement implement implement				clonedData.setHeadLocation(getStartPoint());
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
				int gap = 5;
				int width = 18;
				int x = 32;
				int y = 62;
				for (int i = 0; i < 3; i++)
				{
					g.fillRect(x, y, width, width);
					x += gap + width;
				}
			}
		}

	}
}