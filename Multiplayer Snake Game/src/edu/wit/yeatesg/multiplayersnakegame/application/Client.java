package edu.wit.yeatesg.multiplayersnakegame.application;

import java.util.ArrayList;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientDataSet;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.Snake;
import edu.wit.yeatesg.multiplayersnakegame.packets.ErrorPacket;
import edu.wit.yeatesg.multiplayersnakegame.packets.Packet;
import edu.wit.yeatesg.multiplayersnakegame.packets.UpdateAllClientsPacket;

public class Client
{	
	private ClientDataSet snakeList;


	public void onClientConnect(Snake joined)
	{
		
	}

	public void onClientDisconnect(Snake exiter)
	{

	}

	public static boolean validName(String text)
	{
		text = text.toLowerCase();
		return !text.contains("server") &&
				!(text.length() > 12) &&
				!text.contains(Packet.REGEX) &&
				!text.contains(ClientData.REGEX) &&
				!text.contains(ClientDataSet.REGEX);
	}


}

