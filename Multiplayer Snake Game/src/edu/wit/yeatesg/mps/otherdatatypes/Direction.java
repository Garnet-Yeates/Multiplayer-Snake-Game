package edu.wit.yeatesg.mps.otherdatatypes;

import java.awt.event.KeyEvent;

public enum Direction
{
	UP(new Vector(0, -1)), DOWN(new Vector(0, 1)), LEFT(new Vector(-1, 0)), RIGHT(new Vector(1, 0));

	private Vector vec;

	private Direction(Vector vec)
	{
		this.vec = vec;
	}

	public Vector getVector()
	{
		return vec.clone();
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
		case KeyEvent.VK_A:
			return LEFT;
		case KeyEvent.VK_D:
			return RIGHT;
		case KeyEvent.VK_W:
			return UP;
		case KeyEvent.VK_S:
			return DOWN;
		default:
			return null;
		}
	}

}
