package edu.wit.yeatesg.multiplayersnakegame.datatypes.packet;

import java.awt.EventQueue;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Random;

import javax.management.RuntimeErrorException;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.ReflectionTools;

public abstract class Packet
{
	public static final String REGEX = "`";

	private DataOutputStream outputStream;

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
		int uuidOpenIndex = utfData.indexOf("<");
		int uuidCloseIndex = utfData.indexOf(">");		

		long uuid = 0;

		Packet pack = null;
		int packetID = 0;

		if (uuidOpenIndex == 0 && uuidOpenIndex < uuidCloseIndex && uuidCloseIndex != utfData.length() - 1)
		{
			try
			{
				uuid = Long.parseLong(utfData.substring(1, uuidCloseIndex));
				utfData = utfData.substring(uuidCloseIndex + 1);

				int idOpenIndex = utfData.indexOf("<");
				int idCloseIndex = utfData.indexOf(">");		

				if (idOpenIndex == 0 && idOpenIndex < idCloseIndex && idCloseIndex != utfData.length() - 1)
				{					
					packetID = Integer.parseInt(utfData.substring(1, idCloseIndex));
					utfData = utfData.substring(idCloseIndex + 1);
					int typeOpenIndex = utfData.indexOf("<");
					int typeCloseIndex = utfData.indexOf(">");
					if (typeOpenIndex == 0 && typeOpenIndex < typeCloseIndex && typeCloseIndex != utfData.length() - 1)
					{
						String packetType = utfData.substring(1, typeCloseIndex);
						boolean hasPacketData = typeCloseIndex != utfData.length() - 1;
						String packetData = hasPacketData ? utfData.substring(typeCloseIndex + 1) : null;
						switch (packetType)
						{
						case "ErrorPacket":
							pack =  new ErrorPacket(packetData);
							break;
						case "MessagePacket":
							pack =  new MessagePacket(packetData);
							break;
						case "SnakeUpdatePacket":
							pack =  new SnakeUpdatePacket(packetData);
							break;
						case "TickPacket":
							pack = new TickPacket();
							break;
						case "InitiateGamePacket":
							pack =  new InitiateGamePacket(packetData);
							break;
						default:
							pack = null;
							break;
						}
					}
				}
			}
			catch (Exception e) {
				System.out.println("POOR AT : "  + utfData);
				e.printStackTrace();
				}
		}
		if (pack != null)
		{
			pack.setID(packetID);
			pack.setUUID(uuid);
		}
		else
		{
			pack = new MessagePacket("INFO", "PACKET_RECEIVE_ERROR");
		}
		return pack;
	}

	private long uuid;
	
	private void setUUID(long uuid)
	{
		this.uuid = uuid;		
	}
	
	public long getUUID()
	{
		return uuid;
	}

	public static final String ERR = "PACKET_RECEIVE_ERROR";

	public int getID()
	{
		return packetID;
	}

	public void setID(int id)
	{
		packetID = id;
	}

	public void sendMultiple(Collection<DataOutputStream> streams)
	{
		for (DataOutputStream os : streams)
		{
			setDataStream(os);
			send();
		}
	}

	private static int packetIDAssign = 0;
	private int packetID;

	public static final int NUM_SENT = 10;

	public void send()
	{
		try
		{
			Random r = new Random();
			long uuid = r.nextLong();
			String utf = getUTF();
			System.out.println("Packet.send(" + "<" + uuid + ">" + " " + utf + ")");
			packetID = packetIDAssign++;
			for (int i = 0; i < NUM_SENT; i++)
				outputStream.writeUTF("<" + uuid + ">" + getUTF());
		}
		catch (IOException e)
		{
			System.out.println("Something went wrong trying to write a packet in UTF format on an outputstream");
		}
	}

	public void setDataStream(DataOutputStream stream)
	{
		outputStream = stream;
	}

	public final String getUTF()
	{
		return "<" + packetID + "><" + getClass().getSimpleName() + ">" + toString();
	}

	@Override
	public String toString()
	{
		return ReflectionTools.fieldsToString(REGEX, this, this.getClass());
	}
}
