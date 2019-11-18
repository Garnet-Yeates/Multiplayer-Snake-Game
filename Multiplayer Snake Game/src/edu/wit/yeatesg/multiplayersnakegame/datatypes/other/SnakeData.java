package edu.wit.yeatesg.multiplayersnakegame.datatypes.other;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.Timer;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.buffs.BuffAnimationScript;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.BuffPickupPacket;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.SnakeUpdatePacket;
import edu.wit.yeatesg.multiplayersnakegame.phase2play.Client;

public class SnakeData
{
	public static void main(String[] args) {
		SnakeData juan = new SnakeData("Jahn", Color.YELLOW, Direction.RIGHT, new PointList(new Point(1,1), new Point(2,2)), true, true);
		SnakeData tooh = new SnakeData("Won", Color.GREEN, Direction.UP, new PointList(new Point(3,3), new Point(2,2)), true, true);
		tooh.$outputStream = new DataOutputStream(System.out);
		
		tooh.updateBasedOn(new SnakeUpdatePacket(juan));
		System.out.println(tooh.$outputStream + " " + tooh);
	}
	
	public static final String REGEX = ":";
	
	public SnakeData(String name, Color color, Direction direction, PointList pointList, boolean isHost, boolean isAlive)
	{
		this.name = name;
		this.color = color;
		this.direction = direction;
		this.pointList = pointList;
		this.isHost = isHost;
		this.isAlive = true;
	}

	public SnakeData(String... params)
	{
		this(params[0],
				Color.fromString(params[1]),
				Direction.fromString(params[2]),
				PointList.fromString(params[3]),
				Boolean.parseBoolean(params[4]),
				Boolean.parseBoolean(params[5]));
	}

	public SnakeData()
	{
		this.name = "null";
		this.color = Color.BLACK;
		this.pointList = new PointList();
		this.direction = Direction.DOWN;
		this.isHost = false;
		this.isAlive = true;
	}
	
	public SnakeData(String splittableString)
	{
		this(splittableString.split(REGEX));
	}
	
	
	// Fields that are shared/updated/used between client/server
	
	
	private String name;
	private Color color;
	private Direction direction;
	private PointList pointList;
	private boolean isHost;
	private boolean isAlive;

	public Color getColor()
	{
		return color;
	}

	public void setColor(Color c)
	{
		color = c;
	}

	public PointList getPointList()
	{
		return pointList.clone();
	}

	public String getClientName()
	{
		return name;
	}

	public void setName(String newName)
	{
		name = newName;
	}

	public Direction getDirection()
	{
		return direction;
	}

	public void setDirection(Direction newDir)
	{
		direction = newDir;
	}

	public boolean isHost()
	{
		return isHost;
	}

	public void setIsHost(boolean isHost)
	{
		this.isHost = isHost;
	}

	public boolean isAlive()
	{
		return isAlive;
	}

	public void setAlive(boolean isAlive)
	{
		this.isAlive = isAlive;
	}


	// Server fields and methods, not updated when updateBasedOn(SnakeUpdatePacket pack) is called


	private DataOutputStream $outputStream;
	private Socket $socket;
	
	public boolean hasOutputStream()
	{
		return $outputStream != null;
	}
	
	public DataOutputStream getOutputStream()
	{
		return $outputStream;
	}

	public void setOutputStream(DataOutputStream outputStream)
	{
		$outputStream = outputStream;
	}

	public Socket getSocket()
	{
		return $socket;
	}

	public void setSocket(Socket socket)
	{
		$socket = socket;
	}
	
	
	// Client fields and methods, not updated when updateBasedOn(SnakeUpdatePacket pack) is called
	
	
	private BuffAnimationScript $currentBuffAnimationScript;
	
	public void setBuffAnimationScript(BuffAnimationScript script)
	{
		$currentBuffAnimationScript = script;
	}
	
	public void endBuffAnimationScript()
	{
		$currentBuffAnimationScript = null;
	}
	
	public void draw(Graphics g)
	{
		if ($currentBuffAnimationScript != null)
			$currentBuffAnimationScript.drawSnake(g);
		else
			drawSnakeNormally(g);
	}
	
	private void drawSnakeNormally(Graphics g)
	{
		for (Point segmentLoc : pointList)
		{
			Point drawCoords = Client.getPixelCoords(segmentLoc);
			g.setColor(color);
			g.fillRect(drawCoords.getX(), drawCoords.getY(), Client.UNIT_SIZE, Client.UNIT_SIZE);
		}
	}

	// The buff timers are going to be ehre

	public void updateBasedOn(SnakeUpdatePacket pack)
	{
		SnakeData updated = pack.getClientData();

		ArrayList<Field> fieldsToTransfer = ReflectionTools.getFieldsThatUpdate(SnakeData.class);
		for (int i = 0; i < fieldsToTransfer.size(); i++)
		{       // Update the fields of this object with the corresponding fields of the updated
			try // SnakeData object. Don't update fields that begin with $
			{
				Field f = fieldsToTransfer.get(i);
				f.setAccessible(true);
				Object updatedValue = f.get(updated);
				f.set(this, updatedValue); 
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof SnakeData && ((SnakeData) obj).getClientName().equalsIgnoreCase(name);
	}

	@Override
	public String toString()
	{
		return ReflectionTools.fieldsToString(REGEX, this, SnakeData.class);
	}

	@Override
	public SnakeData clone()
	{
		return new SnakeData(toString());
	}

	public void setPointList(PointList pointList)
	{
		this.pointList = pointList;		
	}
}