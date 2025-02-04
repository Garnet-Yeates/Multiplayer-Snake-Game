package edu.wit.yeatesg.mps.network.packets;
import edu.wit.yeatesg.mps.otherdatatypes.ReflectionTools;


public abstract class Packet
{
	public static final String REGEX = "`";

	public Packet(String... args)
	{
		if (ReflectionTools.getNumUpdatableFields(this.getClass()) == args.length)
			initFromStringArray(args);
	}

	public Packet(String splittableString)
	{
		initFromStringArray(splittableString.split(REGEX));
	}

	protected abstract void initFromStringArray(String[] args);

	public static Packet parsePacket(String utfData) 
	{
		Packet pack = null;
		int typeOpenIndex = utfData.indexOf("<");
		int typeCloseIndex = utfData.indexOf(">");
		if (typeOpenIndex == 0 && typeOpenIndex < typeCloseIndex && typeCloseIndex != utfData.length() - 1) 
		{
			String packetType = utfData.substring(1, typeCloseIndex);
			boolean hasPacketData = typeCloseIndex != utfData.length() - 1;
			String packetData = hasPacketData ? utfData.substring(typeCloseIndex + 1) : null;
			try 
			{
				if (packetType.equals("MessagePacket")) 
				{
					pack =  new MessagePacket(packetData);
				} 
				else if (packetType.equals("SnakeUpdatePacket")) 
				{
					pack =  new SnakeUpdatePacket(packetData);
				} 
				else if (packetType.equals("InitiateGamePacket"))
				{
					pack =  new InitiateGamePacket(packetData);
				}
				else if (packetType.equals("DirectionChangePacket"))
				{
					pack =  new DirectionChangePacket(packetData);
				}
				else if (packetType.equals("FruitSpawnPacket"))
				{
					pack = new FruitSpawnPacket(packetData);
				}
				else if (packetType.equals("FruitPickupPacket"))
				{
					pack = new FruitPickupPacket(packetData);
				}
				else if (packetType.equals("DebuffReceivePacket"))
				{
					pack = new DebuffReceivePacket(packetData);
				}
				else if (packetType.equals("SnakeDeathPacket"))
				{
					pack = new SnakeDeathPacket(packetData);
				}
				else if (packetType.equals("SnakeBitePacket"))
				{
					pack = new SnakeBitePacket(packetData);
				}

			} catch (Exception e) { e.printStackTrace(); }
		}
		if (pack == null) 
		{
			System.out.println("\n\n\n\n\nNULL PACKET\n\n\n\n\n\n");
		}
		return pack;
	}

	public final String getUTF()
	{
		return "<" + getClass().getSimpleName() + ">" + toString();
	}

	@Override
	public String toString()
	{
		return ReflectionTools.fieldsToString(REGEX, this, this.getClass());
	}
}
