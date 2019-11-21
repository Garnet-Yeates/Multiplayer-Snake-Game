package edu.wit.yeatesg.mps.buffs;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;

public class Fruit
{
	public static final String REGEX = "=";
	
	private FruitType type;
	private Point location;
	
	public Fruit(FruitType type, Point location)
	{
		this.type = type;
		this.location = location;
	}
	
	public Fruit(Point theoreticalFruitLoc)
	{
		this.location = theoreticalFruitLoc;
		Random rand = new Random();
		for (FruitType type : FruitType.values())
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
		int drawX = GameplayClient.getPixelCoord(location.getX());
		int drawY = GameplayClient.getPixelCoord(location.getY());
		int drawSize = GameplayClient.UNIT_SIZE;
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
		default:
			break;
		}
	}
}
