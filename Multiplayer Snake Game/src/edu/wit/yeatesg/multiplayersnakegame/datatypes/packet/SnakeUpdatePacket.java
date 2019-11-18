package edu.wit.yeatesg.multiplayersnakegame.datatypes.packet;

import java.io.DataOutputStream;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.Color;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.Point;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.PointList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
 
public class SnakeUpdatePacket extends Packet
{
	protected SnakeData data;
	
	public static void main(String[] args) {
		SnakeData dat = new SnakeData();
		dat.setColor(Color.GREEN);
		dat.setName("Fuckry");
		dat.setPointList(new PointList(new Point(1,2), new Point(2,3)));
		dat.setIsHost(true);
		dat.setOutputStream(new DataOutputStream(null));
		SnakeUpdatePacket pack = new SnakeUpdatePacket(dat);
		String utf = pack.getUTF();
		System.out.println(utf);
		
		SnakeUpdatePacket rec = (SnakeUpdatePacket) Packet.parsePacket(utf);
		System.out.println(rec.getUTF());
	
	}
	
	public SnakeUpdatePacket(SnakeData data)
	{
		super(data.toString());
	}
	
	public SnakeUpdatePacket(String splittableString)
	{
		super(splittableString);
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		data = new SnakeData(args[0]);
	}
	
	public SnakeData getClientData()
	{
		return data;
	}
}
