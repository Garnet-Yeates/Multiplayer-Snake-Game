package edu.wit.yeatesg.multiplayersnakegame.phase1connect;

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
