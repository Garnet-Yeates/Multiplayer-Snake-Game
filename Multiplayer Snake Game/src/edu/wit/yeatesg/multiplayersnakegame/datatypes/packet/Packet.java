package edu.wit.yeatesg.multiplayersnakegame.datatypes.packet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

public abstract class Packet
{
	public static final String REGEX = "`";
	
	private DataOutputStream outputStream;
	
	public Packet(String... args)
	{
		initFromStringArray(args);
	}
	
	public Packet(String splittableString)
	{
		initFromStringArray(splittableString.split(REGEX));
	}
		
	protected abstract void initFromStringArray(String[] args);
	
	public static Packet parsePacket(String utfData)
	{
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
				 return new ErrorPacket(packetData);
			 case "MessagePacket":
				 return new MessagePacket(packetData);
			 case "SnakeUpdatePacket":
				 return new SnakeUpdatePacket(packetData);
			 case "TickPacket":
				 return new ErrorPacket(packetData);
			 default:
				 throw new RuntimeException("Invalid packet data, packet type cannot be determined");
			 }
		}
		return null;
	}
	
	public void sendMultiple(Collection<DataOutputStream> streams)
	{
		for (DataOutputStream os : streams)
		{
			setDataStream(os);
			send();
		}
	}

	public void send()
	{
		try
		{
			outputStream.writeUTF(getUTF());
		}
		catch (IOException e)
		{
			System.out.println("Something went wrong trying to write a packet in UTF format on an outputstream");
		}
	}
	
	protected String fieldsToString(String regex)
	{	
		Field[] fields;
		fields = this.getClass().getDeclaredFields();
		int numWritableFields = 0;
		for (int i = 0; i < fields.length; i++)
		{
			if (!fields[i].isAccessible())
				fields[i].setAccessible(true);
			if (!Modifier.isStatic(fields[i].getModifiers()))
				numWritableFields++;
		}		

		String s = "";
		int index = 0;
		for (Field f : fields)
		{
			try
			{					
				if (!Modifier.isStatic(f.getModifiers()))
				{
					Object v = f.get(this);
					s += v + (index == numWritableFields - 1 ? "" : regex);
					index++;
				}
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}
		return s;
	}
	
	public void setDataStream(DataOutputStream stream)
	{
		outputStream = stream;
	}
			
	public final String getUTF()
	{
		return "<" + getClass().getSimpleName() + ">" + toString();
	}
	
	@Override
	public String toString()
	{
		return fieldsToString(REGEX);
	}
}
