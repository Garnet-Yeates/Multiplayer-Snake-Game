package edu.wit.yeatesg.multiplayersnakegame.datatypes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ClientData
{
	public static final String REGEX = ":";

	private String name;
	private Color color;
	private Direction direction;

	private Point currentHeadLoc;
	private Point nextHeadLoc;

	private boolean isHost;

	public ClientData(String name, Color color, Direction direction, Point currentHeadLoc, Point nextHeadLoc, boolean isHost)
	{
		this.name = name;
		this.color = color;
		this.direction = direction;
		this.currentHeadLoc = currentHeadLoc;
		this.nextHeadLoc = nextHeadLoc;
		this.isHost = isHost;
	}

	public ClientData(String... params)
	{
		this(params[0],
				Color.fromString(params[1]),
				Direction.fromString(params[2]),
				Point.fromString(params[3]),
				Point.fromString(params[4]),
				Boolean.parseBoolean(params[5]));
	}

	public ClientData()
	{
		this.name = "null";
		this.color = Color.BLACK;
		this.direction = Direction.DOWN;
		this.currentHeadLoc = new Point(0, 0);
		this.nextHeadLoc = new Point(0, 0);
		this.isHost = false;
	}

	public ClientData(String splittableString)
	{
		this(splittableString.split(REGEX));
	}

	public Color getColor()
	{
		return color;
	}

	public void setColor(Color c)
	{
		color = c;
	}

	public String getClientName()
	{
		return name;
	}

	public void setName(String newName)
	{
		name = newName;
	}

	public Direction getDirection()
	{
		return direction;
	}

	public void setDirection(Direction newDir)
	{
		direction = newDir;
	}

	public Point getHeadLocation()
	{
		return currentHeadLoc;
	}

	public void setHeadLocation(Point newHeadLoc)
	{
		currentHeadLoc = newHeadLoc;
	}

	public Point getNextHeadLocation()
	{
		return nextHeadLoc;
	}

	public void setNextHeadLocation(Point nextHeadLoc)
	{
		this.nextHeadLoc = nextHeadLoc;
	}

	public boolean isHost()
	{
		return isHost;
	}

	public void setIsHost(boolean isHost)
	{
		this.isHost = isHost;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof ClientData && ((ClientData) obj).getClientName().equalsIgnoreCase(name);
	}

	@Override
	public String toString()
	{
		return fieldsToString(REGEX, ClientData.class);
	}

	public String fieldsToString(String regex, Class<? extends ClientData> type)
	{
		Field[] fields = type.getDeclaredFields();

		// Determine the number of instance fields
		int numInstanceFields = 0;
		for (int i = 0; i < fields.length; i++)
		{
			if (!fields[i].isAccessible())
				fields[i].setAccessible(true);
			if (!Modifier.isStatic(fields[i].getModifiers()) && fields[i].getDeclaringClass() == type)
				numInstanceFields++;
		}		
		
		String s = "";
		int index = 0;
		for (Field f : fields)
		{
			try
			{					
				if (!Modifier.isStatic(f.getModifiers()) && f.getDeclaringClass() == ClientData.class) // Add all instance field values to the String, separated by regex
				{
					Object v = f.get(this);
					s += v + (index == numInstanceFields - 1 ? "" : regex);
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
}