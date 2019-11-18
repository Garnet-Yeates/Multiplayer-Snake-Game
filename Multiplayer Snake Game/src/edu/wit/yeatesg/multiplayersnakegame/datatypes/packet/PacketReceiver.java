package edu.wit.yeatesg.multiplayersnakegame.datatypes.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.Timer;

public class PacketReceiver
{	
	private static int idAssign = 0;
	private int id;

	private PacketListener listener;
	
	private DataInputStream in;
	private DataOutputStream out;
	
	private boolean active;

	public PacketReceiver(DataInputStream in, DataOutputStream out, PacketListener listener)
	{
		this.id = idAssign++;
		this.in = in;
		this.out = out;
		this.listener = listener;
	}

	private TreeMap<Integer, Packet> packMap = new TreeMap<Integer, Packet>((p1, p2) -> { return p1 > p2 ? 1 : p1 == p2 ? 0 : -1; });

	public Packet manualConsumePacket()
	{
		if (!packMap.isEmpty())
		{
			Packet packetReceiving = packMap.get(packMap.firstKey());
			packMap.remove(packMap.firstKey());
			return packetReceiving;
		}
		return null;
	}
	
	public DataOutputStream getOutputStream()
	{
		return out;
	}

	public void setListener(PacketListener l)
	{
		this.listener = l;
	}

	private void autoConsumePacket()
	{
		int packMapIndex = 0;

		if (!packMap.isEmpty())
		{
			Packet packetReceiving = packMap.get(packMap.firstKey());
			packMap.remove(packMap.firstKey());
			listener.onPacketReceive(packetReceiving);
		}
	}

	public void manualReceive() throws IOException
	{
		for (int i = 0; i < Packet.NUM_SENT; i++)
			onAutoReceive(Packet.parsePacket(in.readUTF()));
	}

	private String receiving;

	public void setReceiving(String name)
	{
		this.receiving = name;
	}

	private Timer consumeTimer;

	public void startAutoReceiving()
	{
		active = true;
		consumeTimer = new Timer(3, (e) -> autoConsumePacket());
		Thread receiveThread = new Thread(() ->
		{
			while (active)
			{
				try
				{
					if (!paused)
					{
						String rawUTF = in.readUTF();
						System.out.println(receiving + " rawReceive -> " + rawUTF);
						Packet rawReceive = Packet.parsePacket(rawUTF);
						onAutoReceive(rawReceive);
						autoConsumePacket();
						autoConsumePacket();
					}
				}
				catch (IOException e1)
				{
					System.out.println("AUTO RECEIVE BROKE, BREAK");
					break;
				}
			}
		});
		receiveThread.start();
		consumeTimer.start();
	}
	
	private boolean paused = false;
	
	public void pause()
	{
		paused = true;
	}
	
	public void unPause()
	{
		paused = false;
	}

	private void onAutoReceive(Packet packetReceiving)
	{
		if (packetReceiving != null && !(packetReceiving instanceof MessagePacket && ((MessagePacket) packetReceiving).getMessage().equals(Packet.ERR)))
		{
			long uuid = packetReceiving.getUUID();
			if (!alreadyReceivedUUID(uuid))
			{
				receivedUUIDs.add(uuid);
			/*	System.out.println("------ PACK MAP --------");
				for (Packet p : packMap.values())
					System.out.println(p.getUUID() + " " + p);*/
		//		System.out.println(receiving + "'s packMap Doesn't contain " + packetReceiving.getID() + " " + packetReceiving.getUUID() + " so add " + packetReceiving);
				packMap.put(packetReceiving.getID(), packetReceiving);	
			}
		}

	}

	private ArrayList<Long> receivedUUIDs = new ArrayList<Long>();

	boolean alreadyReceivedUUID(long uuid)
	{
		boolean ret = receivedUUIDs.contains(uuid);
		if (receivedUUIDs.size() == 60)
		{
			ArrayList<Long> last10 = new ArrayList<>();
			for (int i = receivedUUIDs.size() - 10; i < last10.size(); i++)
				last10.add(receivedUUIDs.get(i));
			receivedUUIDs.clear();
			receivedUUIDs.addAll(last10);
		}
		return ret;
	}
}
