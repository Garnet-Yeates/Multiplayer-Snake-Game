package edu.wit.yeatesg.mps.network.clientserver;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import edu.wit.yeatesg.mps.network.clientserver.MPSClient.ClientListener;
import edu.wit.yeatesg.mps.network.packets.MessagePacket;
import edu.wit.yeatesg.mps.network.packets.Packet;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.otherdatatypes.Color;
import edu.wit.yeatesg.mps.otherdatatypes.Snake;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeList;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

public class LobbyGUI extends JPanel implements ClientListener, WindowListener
{	
	private static final long serialVersionUID = 4339194739358327310L;

	private SnakeList allClients;
	private Snake thisClient;
	private String thisClientName;
	
	private MPSClient networkClient;
	
	private LobbyFrame frame;
	
	public LobbyGUI(String clientName, MPSClient internal, int serverPort)
	{
		frame = new LobbyFrame();
		this.networkClient = internal;
		networkClient.setListener(this);
		networkClient.startAutoReceiving();
		thisClientName = clientName;	
		allClients = new SnakeList();
		frame.setVisible(true);
	}

	@Override
	public void onAutoReceive(Packet packetReceiving)
	{
		System.out.println(thisClientName + " Lobby Receive -> " + packetReceiving);
		if (packetReceiving instanceof SnakeUpdatePacket)
		{
			allClients.updateBasedOn((SnakeUpdatePacket) packetReceiving);
			if (allClients.didSomeoneJoinOnLastUpdate())
			{
				Snake whoJustJoined = allClients.getWhoJoinedOnLastUpdate();
				if (whoJustJoined.getClientName().equals(thisClientName))
				{
					thisClient = whoJustJoined;
					if (thisClient.isHost())
						button_startGame.setEnabled(true);
				}
				PlayerSlot playerSlot = slotList.getPlayerSlotPanel(whoJustJoined.getPlayerNum());
				playerSlot.connectClient(whoJustJoined);
			}
		}
		else if (packetReceiving instanceof MessagePacket)
		{
			MessagePacket msgPacket = (MessagePacket) packetReceiving;
			switch (msgPacket.getMessage())
			{
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
		networkClient.send(startGamePacket);
	}
	
	private void onGameStart()
	{
		new GameplayGUI(networkClient, thisClient, allClients);
		frame.dispose();
	}

	private void onDisconnectPress()
	{
		if (canDisconnect)
		{
			canDisconnect = false;
			MessagePacket exitPacket = new MessagePacket(thisClientName, "I EXIT");
			networkClient.send(exitPacket);
			new Timer(1500, (e) -> System.exit(0)).start();
		}
	}
	
	private void onPlayerLeaveLobby(Snake whoLeft)
	{
		PlayerSlot leaversPanel = slotList.getConnectedSlot(whoLeft);
		leaversPanel.disconnectClient();
		allClients.remove(whoLeft);
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

	private static int slotNumAssign = 1;
	
	private class PlayerSlot extends JPanel
	{
		private static final int WIDTH = 150;
		private static final int HEIGHT = 105;
		private static final int GAP = 10;

		private static final long serialVersionUID = 5816367030492476277L;

		private int playerNum;

		private JLabel nameField; 

		public PlayerSlot(int x, int y, JPanel contentPane)
		{
			slotList.add(this);
			playerNum = slotNumAssign++;

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
			nameField.setBounds(5, 30, PlayerSlot.WIDTH - 5, 20);
			add(nameField);

			updatePanel();

			contentPane.add(this);
		}

		private Snake connectedClient = null;

		public void connectClient(Snake clientData)
		{
			connectedClient = clientData;
			updatePanel();
		}

		public void disconnectClient()
		{
			connectedClient = null;
			updatePanel();
		}

		public boolean hasConnectedClient()
		{
			return connectedClient != null;
		}

		public Snake getConnectedClient()
		{
			return connectedClient;
		}

		private void updatePanel()
		{
			nameField.setText(hasConnectedClient() ? connectedClient.getClientName() : "<not connected>");
			if (nameField.getText().equals(thisClientName))
			{
				Color col = MultiplayerSnakeGame.getColorFromSlotNum(playerNum);
				int r = col.getRed() + 100;
				int g = col.getGreen() + 100;
				int b = col.getBlue() + 100;
				r = r > 255 ? 255 : r;
				g = g > 255 ? 255 : g;
				b = b > 255 ? 255 : b;

				setBorder(new MatteBorder(1, 1, 1, 1, new Color(r, g, b)));
			}
			repaint();
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			g = g.create();
			super.paintComponent(g);
			g.setColor(MultiplayerSnakeGame.getColorFromSlotNum(playerNum));

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

		public int getSlotNum()
		{
			return playerNum;
		}

	}
	
	private SlotList slotList;
	
	private static class SlotList extends ArrayList<PlayerSlot>
	{
		private static final long serialVersionUID = 4989139048216644782L;
	
		public PlayerSlot getConnectedSlot(Snake client)
		{
			for (PlayerSlot panel : this)
				if (client.equals(panel.getConnectedClient()))
					return panel;
			return null;
		}

		public PlayerSlot getPlayerSlotPanel(int slotNum)
		{
			for (PlayerSlot panel : this) 
				if (panel.getSlotNum() == slotNum)
					return panel;
			return null;
		}	
	}
		
	// Frame stuff
	
	private JButton button_startGame;
	
	private boolean canDisconnect;
	
	public class LobbyFrame extends JFrame
	{
		private static final long serialVersionUID = -714233913230803955L;

		public LobbyFrame()
		{
			setTitle(thisClientName);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			LobbyGUI.this.setBorder(new EmptyBorder(5, 5, 5, 5));
			LobbyGUI.this.setLayout(null);
			setContentPane(LobbyGUI.this);
			setTitle(thisClientName);
			setBounds(0, 0, PlayerSlot.GAP*3 + PlayerSlot.WIDTH*2 + JAR_OFFSET_X + 2, PlayerSlot.GAP*3 + PlayerSlot.HEIGHT*2 + 70 + JAR_OFFSET_Y);
			setResizable(false);

			addWindowListener(LobbyGUI.this);

			LobbyGUI.this.setBackground(Color.BLACK);
			LobbyGUI.this.setForeground(Color.WHITE);
			
			int panxy = PlayerSlot.GAP;
			int xinc = PlayerSlot.WIDTH + PlayerSlot.GAP;
			int yinc = PlayerSlot.HEIGHT + PlayerSlot.GAP;
			slotList = new SlotList();
			new PlayerSlot(panxy, panxy, LobbyGUI.this);
			new PlayerSlot(panxy + xinc, panxy, LobbyGUI.this);
			new PlayerSlot(panxy, panxy + yinc, LobbyGUI.this);
			new PlayerSlot(panxy + xinc, panxy + yinc, LobbyGUI.this);

			button_startGame = new JButton("Start Game");
			button_startGame.addActionListener((e) -> onGameStartRequest());
			button_startGame.setForeground(Color.BLACK);
			button_startGame.setBackground(Color.BLACK);
			button_startGame.setBounds(PlayerSlot.GAP, PlayerSlot.GAP*2 + 10 + PlayerSlot.HEIGHT*2, PlayerSlot.WIDTH, 25);
			button_startGame.setEnabled(false);
			this.add(button_startGame);

			JButton button_disconnect = new JButton("Disconnect");
			button_disconnect.addActionListener((e) -> onDisconnectPress());
			button_disconnect.setForeground(Color.BLACK);
			button_disconnect.setBackground(Color.BLACK);
			button_disconnect.setBounds(button_startGame.getX() + PlayerSlot.GAP + button_startGame.getWidth(), button_startGame.getY(), PlayerSlot.WIDTH, 25);
			button_disconnect.setEnabled(false);
			Timer enableDisconnect = new Timer(1000, (e) ->
			{
				canDisconnect = true;
				button_disconnect.setEnabled(true);
				button_disconnect.requestFocus();
			});
			enableDisconnect.setRepeats(false);
			enableDisconnect.start();
			this.add(button_disconnect);
		}
	}
}