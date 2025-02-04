package edu.wit.yeatesg.mps.buffs;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;

import edu.wit.yeatesg.mps.network.clientserver.GameplayGUI;
import edu.wit.yeatesg.mps.network.clientserver.MPSServer;
import edu.wit.yeatesg.mps.otherdatatypes.Point;
import edu.wit.yeatesg.mps.otherdatatypes.Snake;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

public class Fruit
{
	public static final String REGEX = "=";
	
	private FruitType type;
	private Point location;
	
	public static final int MAX_BITE_OFF = 30;
	public static final int COMPLEX_CHECK_MIN = MAX_BITE_OFF + 2;
	
	public Fruit(FruitType type, Point location)
	{
		this.type = type;
		this.location = location;
	}
	
	public Fruit(MPSServer creating, Point theoreticalFruitLoc)
	{
		this.location = theoreticalFruitLoc;
		Random rand = new Random();
		ArrayList<FruitType> possibleTypes = new ArrayList<>();
		
		for (FruitType type : FruitType.values())
			possibleTypes.add(type);
		
		if (creating.getCoveredArea() < 0)
			possibleTypes.remove(FruitType.FRUIT_HUNGRY);
		
		for (FruitType type : possibleTypes)
		{
			if (rand.nextInt(type.getRarity()) == 0)
			{
				this.type = type;
				break;
			}
		}
	}

	public Point getLocation()
	{
		return location.clone();
	}
	
	public void setLocation(Point location)
	{
		this.location = location;
	}
	
	public static Fruit fromString(String string)
	{
		String[] params = string.split(REGEX);
		return new Fruit(FruitType.fromString(params[0]), Point.fromString(params[1]));
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Fruit)
		{
			Fruit other = (Fruit) obj;
			return other.type == type && other.location.equals(location);
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return type + REGEX + location;
	}
	
	// FruitType specific methods
	
	public FruitType getFruitType()
	{
		return type;
	}

	public void setFruitType(FruitType type)
	{
		this.type = type;
	}
	
	public BuffType getAssociatedBuff()
	{
		return type.getAssociatedBuff();
	}
	
	public boolean hasAssociatedBuff()
	{
		return type.hasAssociatedBuff();
	}

	public void draw(Graphics graphics)
	{
		int drawX = GameplayGUI.getPixelCoord(location.getX());
		int drawY = GameplayGUI.getPixelCoord(location.getY());
		int drawSize = UNIT_SIZE;
		switch (type)
		{
		case FRUIT_REGULAR:
			graphics.setColor(Color.WHITE);
			graphics.fillRect(drawX, drawY, drawSize, drawSize);
			break;
		case FRUIT_TRANSLUCENT:
			graphics.setColor(Color.WHITE);
			int offset = 0;
			int outlineThickness = 2;
			for (int i = 0; i < outlineThickness; i++)
			{
				graphics.drawRect(drawX + offset, drawY + offset, drawSize - (2*offset) - 1, drawSize - (2*offset) - 1);
				offset++;
			}
			break;
		case FRUIT_HUNGRY:
			graphics.setColor(Color.DARK_GRAY);
			graphics.fillRect(drawX, drawY, drawSize, drawSize);
			break;
		default:
			break;
		}

	}
}
