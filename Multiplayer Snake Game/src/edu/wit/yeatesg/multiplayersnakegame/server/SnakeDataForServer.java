package edu.wit.yeatesg.multiplayersnakegame.server;

import java.io.DataOutputStream;
import java.net.Socket;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;

class SnakeDataForServer extends SnakeData
{
	private Socket socket;
	private DataOutputStream outputStream;

	public SnakeDataForServer(SnakeData parent, Socket socket, DataOutputStream outputStream)
	{
		super(parent.getClientName(),
				parent.getColor(),
				parent.getDirection(),
				parent.getHeadLocation(),
				parent.getNextHeadLocation(),
				parent.isHost());
		this.socket = socket;
		this.outputStream = outputStream;
	}

	public SnakeDataForServer(SnakeData parent)
	{
		this(parent, null, null);
	}

	public Socket getSocket()
	{
		return socket;
	}

	public void setSocket(Socket s)
	{
		socket = s;
	}

	public DataOutputStream getOutputStream()
	{
		return outputStream;
	}

	public void setOutputStream(DataOutputStream os)
	{
		outputStream = os;
	}
}