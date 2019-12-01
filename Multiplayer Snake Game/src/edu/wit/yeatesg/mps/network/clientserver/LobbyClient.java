package edu.wit.yeatesg.mps.network.clientserver;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.Color;
import edu.wit.yeatesg.mps.otherdatatypes.Direction;
import edu.wit.yeatesg.mps.otherdatatypes.Point;
import edu.wit.yeatesg.mps.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeList;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataOutputStream;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;

public class LobbyClient extends JPanel implements ClientListener, WindowListener
{	
	private static final long serialVersionUID = 4339194739358327310L;

	private SnakeList allClients;
	private SnakeData thisClient;
	private String thisClientName;
	
	private NetworkClient internalClient;

	private DataOutputStream outputStream;
	
	private LobbyGUI frame;
	
	public LobbyClient(String clientName, NetworkClient internal, int serverPort)
	{
		frame = new LobbyGUI();
		internal.setListener(this);
		this.internalClient = internal;
		thisClientName = clientName;	
		allClients = new SnakeList();
		ConnectClient.setLookAndFeel();
	}
	
	private void onPlayerJoinLobby(SnakeData whoJoinedOnLastUpdate)
	{
		if (whoJoinedOnLastUpdate.isHost() && whoJoinedOnLastUpdate.getClientName().equals(thisClientName))
			button_startGame.setEnabled(true);			
		PlayerDisplayPanel emptyPanel = getEmptyPlayerPanel();
		emptyPanel.connectClient(whoJoinedOnLastUpdate);
	}

	private void onPlayerLeaveLobby(SnakeData whoLeftOnLastUpdate)
	{
		PlayerDisplayPanel leaversPanel = getConnectedPlayerPanel(whoLeftOnLastUpdate);
		leaversPanel.disconnectClient();
		allClients.remove(whoLeftOnLastUpdate);
	}

	@Override
	public void onReceive(String data)
	{
		Packet packetReceiving = Packet.parsePacket(data);
		System.out.println(thisClientName + " Lobby Receive -> " + packetReceiving);
		if (packetReceiving instanceof SnakeUpdatePacket)
		{
			allClients.updateBasedOn((SnakeUpdatePacket) packetReceiving);
			for (SnakeData dat : allClients)
				if (thisClient == null && dat.getClientName().equals(thisClientName))
					thisClient = dat;
		}
		else if (packetReceiving instanceof MessagePacket)
		{
			MessagePacket msgPacket = (MessagePacket) packetReceiving;
			switch (msgPacket.getMessage())
			{
			case "SOMEONE JOINED":
				onPlayerJoinLobby(allClients.get(allClients.size() - 1)); // The client at size - 1 is the one who just joined
				break;
			case "THEY EXIT":
				onPlayerLeaveLobby(allClients.get(msgPacket.getSender()));
				break;
			case "YOU EXIT":
				System.exit(0);
				break;
			case "GAME START":
				onGameStart();
				break;
			}
		}		
	}
	
	private void onGameStartRequest()
	{
		MessagePacket startGamePacket = new MessagePacket(thisClientName, "GAME START");
		startGamePacket.setDataStream(outputStream);
		startGamePacket.send();
	}
	
	private void onGameStart()
	{
		new GameplayClient(internalClient, thisClient, allClients);
		frame.dispose();
	}

	private void onDisconnectPress()
	{
		MessagePacket exitPacket = new MessagePacket(thisClientName, "I EXIT");
		exitPacket.setDataStream(outputStream);
		exitPacket.send();
	}

	@Override
	public void setOutputStream(DataOutputStream out)
	{
		this.outputStream = out;
	}
	
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
	
	// Less Relevant Stuff (in terms of debugging)

	private ArrayList<PlayerDisplayPanel> playerPanelList;
	private static int playerPanelNumAssign = 1;
	
	class PlayerDisplayPanel extends JPanel
	{
		private static final int WIDTH = 150;
		private static final int HEIGHT = 105;
		private static final int GAP = 10;

		private static final long serialVersionUID = 5816367030492476277L;

		private int playerNum;

		private JLabel nameField; 

		public PlayerDisplayPanel(int x, int y, JPanel contentPane)
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
			nameField.setBounds(5, 30, PlayerDisplayPanel.WIDTH - 5, 20);
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
				return new Point(1 + (GameplayClient.SSL - 1), 1);
			case 2:
				return new Point(GameplayClient.NUM_HORIZONTAL_SPACES - 3 - (GameplayClient.SSL - 1), GameplayClient.NUM_VERTICAL_SPACES - 3);
			case 3:
				return new Point(GameplayClient.NUM_HORIZONTAL_SPACES - 3, 1 + (GameplayClient.SSL - 1));
			case 4:
				return new Point(1, GameplayClient.NUM_VERTICAL_SPACES - 3 - (GameplayClient.SSL - 1));
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
			list.add(start.clone());
			for (int i = 0; i < start_length - 1; i++)
			{
				start.setXY(start.getX() + xMod, start.getY() + yMod);
				list.add(start.clone());
			}
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
				SnakeData clonedData = connectedClient.clone(); // Remember, we don't want the data to actually change
				clonedData.setColor(getColor());                // until the server approves it, so modify a clone
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

	private PlayerDisplayPanel getConnectedPlayerPanel(SnakeData client)
	{
		for (PlayerDisplayPanel panel : playerPanelList)
			if (client.equals(panel.getConnectedClient()))
				return panel;
		return null;
	}

	private PlayerDisplayPanel getEmptyPlayerPanel()
	{
		for (PlayerDisplayPanel playerPanel : playerPanelList)
			if (!playerPanel.hasConnectedClient())
				return playerPanel;
		return null;
	}
	
	// Frame stuff
	
	private JButton button_startGame;
	
	public class LobbyGUI extends JFrame
	{
		private static final long serialVersionUID = -714233913230803955L;

		public LobbyGUI()
		{
			setTitle(thisClientName);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			LobbyClient.this.setBorder(new EmptyBorder(5, 5, 5, 5));
			LobbyClient.this.setLayout(null);
			setContentPane(LobbyClient.this);
			setTitle(thisClientName);
			setBounds(0, 0, PlayerDisplayPanel.GAP*3 + PlayerDisplayPanel.WIDTH*2 + GameplayClient.JAR_OFFSET_X + 2, PlayerDisplayPanel.GAP*3 + PlayerDisplayPanel.HEIGHT*2 + 70 + GameplayClient.JAR_OFFSET_Y);
			setResizable(false);

			addWindowListener(LobbyClient.this);

			LobbyClient.this.setBackground(Color.BLACK);
			LobbyClient.this.setForeground(Color.WHITE);
			
			int panxy = PlayerDisplayPanel.GAP;
			int xinc = PlayerDisplayPanel.WIDTH + PlayerDisplayPanel.GAP;
			int yinc = PlayerDisplayPanel.HEIGHT + PlayerDisplayPanel.GAP;
			playerPanelList = new ArrayList<>();
			playerPanelList.add(new PlayerDisplayPanel(panxy, panxy, LobbyClient.this));
			playerPanelList.add(new PlayerDisplayPanel(panxy + xinc, panxy, LobbyClient.this));
			playerPanelList.add(new PlayerDisplayPanel(panxy, panxy + yinc, LobbyClient.this));
			playerPanelList.add(new PlayerDisplayPanel(panxy + xinc, panxy + yinc, LobbyClient.this));

			button_startGame = new JButton("Start Game");
			button_startGame.addActionListener((e) -> onGameStartRequest());
			button_startGame.setForeground(Color.BLACK);
			button_startGame.setBackground(Color.BLACK);
			button_startGame.setBounds(PlayerDisplayPanel.GAP, PlayerDisplayPanel.GAP*2 + 10 + PlayerDisplayPanel.HEIGHT*2, PlayerDisplayPanel.WIDTH, 25);
			button_startGame.setEnabled(false);
			this.add(button_startGame);

			JButton button_disconnect = new JButton("Disconnect");
			button_disconnect.addActionListener((e) -> onDisconnectPress());
			button_disconnect.setForeground(Color.BLACK);
			button_disconnect.setBackground(Color.BLACK);
			button_disconnect.setBounds(button_startGame.getX() + PlayerDisplayPanel.GAP + button_startGame.getWidth(), button_startGame.getY(), PlayerDisplayPanel.WIDTH, 25);
			this.add(button_disconnect);
			setVisible(true);
		}
	}
}