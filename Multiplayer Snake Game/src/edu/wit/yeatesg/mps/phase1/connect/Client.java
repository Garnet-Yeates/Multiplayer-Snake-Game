package edu.wit.yeatesg.mps.phase1.connect;
import java.awt.EventQueue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JTextField;

import edu.wit.yeatesg.mps.phase0.otherdatatypes.DuplicateClientException;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.ServerFullException;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.phase0.packets.MessagePacket;
import edu.wit.yeatesg.mps.phase0.packets.Packet;
import edu.wit.yeatesg.mps.phase0.packets.SnakeUpdatePacket;
import edu.wit.yeatesg.mps.phase2.lobby.LobbyGUI;

public class Client implements Runnable {

	private String name;
	private ClientListener listener;
	private DataInputStream in;
	private DataOutputStream out;
	private Socket cs;

	public Client(ClientListener listener, String name) {
		this.name = name;
		this.listener = listener;
	}

	public boolean connect(String serverIP, int serverPort, boolean isHost) throws UnknownHostException, IOException, ServerFullException, DuplicateClientException {
		cs = new Socket(serverIP, serverPort);
		in = new DataInputStream(cs.getInputStream());
		out = new DataOutputStream(cs.getOutputStream());
				
		SnakeData thisClientsData = new SnakeData();
		thisClientsData.setName(name);
		thisClientsData.setIsHost(isHost);
		
		SnakeUpdatePacket request = new SnakeUpdatePacket(thisClientsData);
		request.setDataStream(out);
		request.send();
		
		String data = in.readUTF();
		MessagePacket resp = (MessagePacket) Packet.parsePacket(data);
		
		if (resp.getMessage().equals("CONNECTION ACCEPT")) {
			System.out.println("CLIENT GET YES");
		//	listener.setOutputStream(out);
			new LobbyGUI(name, this, serverPort);
			System.out.println("wut");
			Thread clientThread = new Thread(this);
			clientThread.start();
			return true;
		} else if (resp.getMessage().equals("SERVER FULL")) {
			throw new ServerFullException();
		} else throw new DuplicateClientException();
	}
	
	public void setListener(ClientListener newListener) {
		listener = newListener;
		listener.setOutputStream(out);
	}

	@Override
	public void run() {
		while (true) {
			try {
				String data = in.readUTF();
				listener.onReceive(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void setName(String name)
	{
		this.name = name;		
	}

}
