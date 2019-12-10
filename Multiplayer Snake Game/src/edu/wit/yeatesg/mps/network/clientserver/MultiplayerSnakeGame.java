package edu.wit.yeatesg.mps.network.clientserver;

import java.awt.EventQueue;
import java.io.DataInputStream;
import java.io.IOException;

import edu.wit.yeatesg.mps.otherdatatypes.Color;
import edu.wit.yeatesg.mps.otherdatatypes.Direction;
import edu.wit.yeatesg.mps.otherdatatypes.Point;
import edu.wit.yeatesg.mps.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.otherdatatypes.Vector;

public class MultiplayerSnakeGame
{
	public static void main(String[] args)
	{
		EventQueue.invokeLater(() ->
		{
			new ConnectGUI();
		});
	}
		
	public static final int START_LENGTH = 3;

	public static final int JAR_OFFSET_X = 9;
	public static final int JAR_OFFSET_Y = 10;

	public static final int NUM_HORIZONTAL_UNITS = 86; // 95
	public static final int NUM_HORIZONTAL_SPACES = NUM_HORIZONTAL_UNITS + 1;
	public static final int NUM_VERTICAL_UNITS = 43; // 50
	public static final int NUM_VERTICAL_SPACES = NUM_VERTICAL_UNITS + 1;
	public static final int UNIT_SIZE = 18; // 18
	public static final int SPACE_SIZE = 1; 
	public static final int MAX_AREA = NUM_HORIZONTAL_UNITS * NUM_VERTICAL_UNITS;

	public static final int MAX_OUTLINE_THICKNESS = UNIT_SIZE / 2;

	public static final int WIDTH = NUM_HORIZONTAL_UNITS * UNIT_SIZE + NUM_HORIZONTAL_SPACES * SPACE_SIZE + JAR_OFFSET_X;
	public static final int HEIGHT = NUM_VERTICAL_UNITS * UNIT_SIZE + NUM_VERTICAL_SPACES * SPACE_SIZE + JAR_OFFSET_Y;

	public static final int MAX_NAME_LENGTH = 17;

	public static Color getColorFromSlotNum(int slotNum)
	{
		switch (slotNum)
		{
		case 1:
			return Color.GREEN;
		case 2:
			return Color.RED;
		case 3:
			return Color.BLUE;
		case 4:
			return Color.YELLOW;
		default:
			return null;
		}
	}

	public static PointList getStartPointListFromSlotNum(int slotNum)
	{
		Vector dirVec = getStartDirectionFromSlotNum(slotNum).getVector().multiply(-1);
		PointList list = new PointList();
		Point start = getStartPointFromSlotNum(slotNum);
		list.add(start.clone());
		for (int i = 0; i < START_LENGTH - 1; i++)
		{
			start.setXY(start.getX() + (int) dirVec.getX(), start.getY() + (int) dirVec.getY());
			list.add(start.clone());
		}
		return list;
	}

	public static Point getStartPointFromSlotNum(int slotNum)
	{
		switch (slotNum)
		{
		case 1:
			return new Point(1 + (START_LENGTH - 1), 1);
		case 2:
			return new Point(NUM_HORIZONTAL_SPACES - 3 - (START_LENGTH - 1), NUM_VERTICAL_SPACES - 3);
		case 3:
			return new Point(NUM_HORIZONTAL_SPACES - 3, 1 + (START_LENGTH - 1));
		case 4:
			return new Point(1, NUM_VERTICAL_SPACES - 3 - (START_LENGTH - 1));
		default:
			return null;
		}
	}

	public static Direction getStartDirectionFromSlotNum(int slotNum)
	{
		switch (slotNum)
		{
		case 1:
			return Direction.RIGHT;
		case 2:
			return Direction.LEFT;
		case 3:
			return Direction.DOWN;
		case 4:
			return Direction.UP;
		default:
			return null;
		}
	}
}
