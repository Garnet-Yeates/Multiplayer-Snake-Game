package edu.wit.yeatesg.multiplayersnakegame.datatypes.other;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SnakeData
{
	public static final String REGEX = ":";

	private String name;
	private Color color;
	private Direction direction;
	private PointList pointList;
	private boolean isHost;
	private boolean isAlive;

	public SnakeData(String name, Color color, Direction direction, PointList pointList, boolean isHost, boolean isAlive)
	{
		this.name = name;
		this.color = color;
		this.direction = direction;
		this.pointList = pointList;
		this.isHost = isHost;
		this.isAlive = true;
	}

	public SnakeData(String... params)
	{
		this(params[0],
				Color.fromString(params[1]),
				Direction.fromString(params[2]),
				PointList.fromString(params[3]),
				Boolean.parseBoolean(params[4]),
				Boolean.parseBoolean(params[5]));
	}

	public SnakeData()
	{
		this.name = "null";
		this.color = Color.BLACK;
		this.pointList = new PointList();
		this.direction = Direction.DOWN;
		this.isHost = false;
		this.isAlive = true;
	}

	public SnakeData(String splittableString)
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
	
	public PointList getPointList()
	{
		return pointList.clone();
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

	public boolean isHost()
	{
		return isHost;
	}

	public void setIsHost(boolean isHost)
	{
		this.isHost = isHost;
	}
	
	public boolean isAlive()
	{
		return isAlive;
	}

	public void setAlive(boolean isAlive)
	{
		this.isAlive = isAlive;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof SnakeData && ((SnakeData) obj).getClientName().equalsIgnoreCase(name);
	}

	@Override
	public String toString()
	{
		return fieldsToString(REGEX, SnakeData.class);
	}
	
	@Override
	public SnakeData clone()
	{
		return new SnakeData(toString());
	}

	public String fieldsToString(String regex, Class<? extends SnakeData> type)
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
				if (!Modifier.isStatic(f.getModifiers()) && f.getDeclaringClass() == SnakeData.class) // Add all instance field values to the String, separated by regex
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

	public void setPointList(PointList pointList)
	{
		this.pointList = pointList;		
	}
}