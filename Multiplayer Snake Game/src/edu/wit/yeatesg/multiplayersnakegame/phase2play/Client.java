package edu.wit.yeatesg.multiplayersnakegame.phase2play;

import java.util.ArrayList;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.Point;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.PointList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.Packet;

public class Client
{	
	public static final int MAX_NAME_LENGTH = 17;
	
	private SnakeList snakeList;

	public static boolean validName(String text)
	{
		text = text.toLowerCase();
		return !text.contains("server") &&
				!text.contains("null") &&
				!text.contains(PointList.REGEX) &&
				!(text.length() > MAX_NAME_LENGTH) &&
				!text.contains(Packet.REGEX) &&
				!text.contains(SnakeData.REGEX) &&
				!text.contains(SnakeList.REGEX) &&
				!text.contains(Point.REGEX);
	}


}

