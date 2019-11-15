package edu.wit.yeatesg.multiplayersnakegame.phase2play;

import java.util.ArrayList;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.Packet;

public class Client
{	
	private SnakeList snakeList;
	

	public static boolean validName(String text)
	{
		text = text.toLowerCase();
		return !text.contains("server") &&
				!(text.length() > 12) &&
				!text.contains(Packet.REGEX) &&
				!text.contains(SnakeData.REGEX) &&
				!text.contains(SnakeList.REGEX);
	}


}

