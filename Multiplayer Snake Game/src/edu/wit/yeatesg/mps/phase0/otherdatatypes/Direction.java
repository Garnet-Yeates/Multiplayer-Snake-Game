package edu.wit.yeatesg.mps.phase0.otherdatatypes;

import java.awt.event.KeyEvent;

public enum Direction
{
	UP(new Point(0, -1)), DOWN(new Point(0, 1)), LEFT(new Point(-1, 0)), RIGHT(new Point(1, 0));

	private Point vec;

	private Direction(Point vec)
	{
		this.vec = vec;
	}

	public Point getVector()
	{
		return Point.fromString(vec.toString());
	}
	
	public Direction getOpposite()
	{
		switch (this)
		{
		case DOWN:
			return UP;
		case LEFT:
			return RIGHT;
		case RIGHT:
			return LEFT;
		case UP:
			return DOWN;
		default:
			return null;
		}
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

	public static Direction fromKeyCode(int keyCode)
	{
		switch (keyCode)
		{
		case KeyEvent.VK_LEFT:
			return LEFT;
		case KeyEvent.VK_RIGHT:
			return RIGHT;
		case KeyEvent.VK_UP:
			return UP;
		case KeyEvent.VK_DOWN:
			return DOWN;
		default:
			return null;
		}
	}

}
