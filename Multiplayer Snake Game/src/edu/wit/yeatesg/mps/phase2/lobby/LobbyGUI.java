package edu.wit.yeatesg.mps.phase2.lobby;


import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.border.MatteBorder;

import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Direction;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeList;
import edu.wit.yeatesg.mps.phase0.packets.MessagePacket;
import edu.wit.yeatesg.mps.phase0.packets.Packet;
import edu.wit.yeatesg.mps.phase0.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.phase1.connect.Client;
import edu.wit.yeatesg.mps.phase1.connect.ClientListener;
import edu.wit.yeatesg.mps.phase1.connect.ConnectGUI;
import edu.wit.yeatesg.mps.phase3.play.SnakeClient;
import edu.wit.yeatesg.mps.phase3.play.SnakeGameGUI;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataOutputStream;
import java.util.ArrayList;
import javax.swing.JButton;

public class LobbyGUI extends JFrame implements ClientListener
{
	private static final long serialVersionUID = 4339194739358327310L;

	private JPanel contentPane;	

	private SnakeList allClients;
	private SnakeData thisClient;
	private String thisClientName;
	
	private Client internalClient;

	private DataOutputStream outputStream;

	public LobbyGUI(String clientName, Client internal, int serverPort)
	{
		internal.setListener(this);
		initFrame();
		this.internalClient = internal;
		thisClientName = clientName;	
		allClients = new SnakeList();
		ConnectGUI.setLookAndFeel();
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}

	private JButton button_startGame;

	private void initFrame()
	{
		setTitle(thisClientName);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(0, 0, PlayerDisplayPanel.GAP*3 + PlayerDisplayPanel.WIDTH*2 + 20, PlayerDisplayPanel.GAP*3 + PlayerDisplayPanel.HEIGHT*2 + 80);
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

		int panxy = PlayerDisplayPanel.GAP;
		int xinc = PlayerDisplayPanel.WIDTH + PlayerDisplayPanel.GAP;
		int yinc = PlayerDisplayPanel.HEIGHT + PlayerDisplayPanel.GAP;
		playerPanelList = new ArrayList<>();
		playerPanelList.add(new PlayerDisplayPanel(panxy, panxy, contentPane));
		playerPanelList.add(new PlayerDisplayPanel(panxy + xinc, panxy, contentPane));
		playerPanelList.add(new PlayerDisplayPanel(panxy, panxy + yinc, contentPane));
		playerPanelList.add(new PlayerDisplayPanel(panxy + xinc, panxy + yinc, contentPane));

		button_startGame = new JButton("Start Game");
		button_startGame.addActionListener((e) -> onStartGamePress());
		button_startGame.setForeground(Color.BLACK);
		button_startGame.setBackground(Color.BLACK);
		button_startGame.setBounds(PlayerDisplayPanel.GAP, PlayerDisplayPanel.GAP*2 + 10 + PlayerDisplayPanel.HEIGHT*2, PlayerDisplayPanel.WIDTH, 25);
		button_startGame.setEnabled(false);
		contentPane.add(button_startGame);

		JButton button_disconnect = new JButton("Disconnect");
		button_disconnect.addActionListener((e) -> onDisconnectPress());
		button_disconnect.setForeground(Color.BLACK);
		button_disconnect.setBackground(Color.BLACK);
		button_disconnect.setBounds(button_startGame.getX() + PlayerDisplayPanel.GAP + button_startGame.getWidth(), button_startGame.getY(), PlayerDisplayPanel.WIDTH, 25);
		contentPane.add(button_disconnect);

		setVisible(true);
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
				onExit();
				break;
			case "GAME START":
				onGameStart();
				break;
			}
		}		
	}
	
	
	private void onStartGamePress()
	{
		System.out.println("HEE HEE STAR PRESS");
		MessagePacket startGamePacket = new MessagePacket(thisClientName, "GAME START");
		startGamePacket.setDataStream(outputStream);
		startGamePacket.send();
	}

	private void onDisconnectPress()
	{
		MessagePacket exitPacket = new MessagePacket(thisClientName, "I EXIT");
		exitPacket.setDataStream(outputStream);
		exitPacket.send();
	}

	private void onGameStart()
	{
		SnakeClient client = new SnakeClient(internalClient, thisClient, allClients);
		new SnakeGameGUI(client).setVisible(true);
		dispose();
	}

	private void onExit()
	{
		System.exit(0);
	}

	private void onPlayerJoinLobby(SnakeData whoJoinedOnLastUpdate)
	{
		System.out.println(whoJoinedOnLastUpdate.getClientName());
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
	public void setOutputStream(DataOutputStream out) {
		this.outputStream = out;
	}
	
	
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
				return new Point(1 + (SnakeClient.SSL - 1), 1);
			case 2:
				return new Point(SnakeClient.NUM_HORIZONTAL_SPACES - 3 - (SnakeClient.SSL - 1), SnakeClient.NUM_VERTICAL_SPACES - 3);
			case 3:
				return new Point(SnakeClient.NUM_HORIZONTAL_SPACES - 3, 1 + (SnakeClient.SSL - 1));
			case 4:
				return new Point(1, SnakeClient.NUM_VERTICAL_SPACES - 3 - (SnakeClient.SSL - 1));
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
}