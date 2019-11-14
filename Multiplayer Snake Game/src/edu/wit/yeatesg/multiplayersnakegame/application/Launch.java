package edu.wit.yeatesg.multiplayersnakegame.application;

import edu.wit.yeatesg.multiplayersnakegame.gui.ConnectGUI;

public class Launch
{
	public static void main(String[] args)
	{
		Thread launchThread = new Thread(() ->
		{
			new ConnectGUI();
		});
		launchThread.start();
	}
}
