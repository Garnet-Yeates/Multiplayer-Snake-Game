package edu.wit.yeatesg.mps.phase0.otherdatatypes;

import java.util.ArrayList;

import edu.wit.yeatesg.mps.buffs.Fruit;

public class Point
{
	private int x;
	private int y;
	
	public static final String REGEX = ",";
	
	public Point(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public static Point fromString(String string)
	{
		try
		{
			String[] params = string.split(REGEX);
			return new Point(Integer.parseInt(params[0]), Integer.parseInt(params[1]));
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public void setXY(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void setX(int x)
	{
		this.x = x;
	}
	
	public void setY(int y)
	{
		this.y = y;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Point)
		{
			Point other = (Point) obj;
			return other.x == x && other.y == y;
		}
		return false;
	}
	
	@Override
	public Point clone()
	{
		return new Point(x, y);
	}
	
	@Override
	public String toString()
	{
		return x + REGEX + y;
	}
}
