package edu.wit.yeatesg.multiplayersnakegame.datatypes;

public enum Direction
{
	UP, DOWN, LEFT, RIGHT;
	
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
}
