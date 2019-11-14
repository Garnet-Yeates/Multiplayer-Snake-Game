package edu.wit.yeatesg.multiplayersnakegame.datatypes;

public class Snake extends ClientData
{		
	public Snake(ClientData parent)
	{
		super(parent.getClientName(),
				parent.getColor(),
				parent.getDirection(),
				parent.getHeadLocation(),
				parent.getNextHeadLocation(),
				parent.isHost());
	}
}
