package edu.wit.yeatesg.multiplayersnakegame.datatypes;

public class Point
{
	private int x;
	private int y;
	
	public Point(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public static Point fromString(String string)
	{
		String[] params = string.split(",");
		return new Point(Integer.parseInt(params[0]), Integer.parseInt(params[1]));
	}
	
	@Override
	public String toString()
	{
		return x + "," + y;
	}
}
