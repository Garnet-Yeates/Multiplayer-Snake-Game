package edu.wit.yeatesg.mps.phase0.otherdatatypes;



public enum Direction
{
	UP(new Point(0, -1)), DOWN(new Point(0, 1)), LEFT(new Point(-1, 0)), RIGHT(new Point(1, 0));
	
	private Point vec;
	
	private Direction(Point vec)
	{
		this.vec = vec;
	}
	
	public static Direction fromString(String string)
	{
		switch (string.toUpperCase())
		{
		case "UP":
			return UP;
		case "DOWN":
			return DOWN;
		case "LEFT":
			return LEFT;
		case "RIGHT":
			return RIGHT;
		default:
			return null;
		}
	}
	
	public Point getVector()
	{
		return Point.fromString(vec.toString());
	}
	
}
