package edu.wit.yeatesg.mps.phase0.otherdatatypes;


import java.awt.Graphics;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.Timer;

import edu.wit.yeatesg.mps.buffs.BuffDrawScript;
import edu.wit.yeatesg.mps.buffs.BuffType;
import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;

public class SnakeData
{
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
		this.name = params[0];
		this.color = Color.fromString(params[1]);
		this.direction = Direction.fromString(params[2]);
		this.pointList =PointList.fromString(params[3]);
		this.isHost = Boolean.parseBoolean(params[4]);
		this.isAlive = Boolean.parseBoolean(params[5]);
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


	// Server side fields and methods, not updated when updateBasedOn(SnakeUpdatePacket pack) is called
    // on the client side of this SnakeData, all of these fields will be null
	
	
	private DataOutputStream $outputStream;
	private Socket $socket;
	private ArrayList<Direction> $directionBuffer;
	private boolean $buffTranslucentActive;
	private int $foodInBelly;
	
	public void grantBuff(BuffType buff)
	{
		Timer removeBuffTimer = new Timer(buff.getDuration(), null);
		removeBuffTimer.setRepeats(false);
		switch (buff)
		{
		case BUFF_TRANSLUCENT:
			$buffTranslucentActive = true;
			removeBuffTimer.addActionListener((e) -> $buffTranslucentActive = false);
			break;
		default:
			break;
		}
		removeBuffTimer.start();
	}
	
	public boolean hasBuffTranslucent()
	{
		return $buffTranslucentActive;
	}
	
	public boolean hasAnyBuffs()
	{
		return $buffTranslucentActive; // || otherBuffActive || otherBuff2Active..
	}
	
	public boolean hasFoodInBelly()
	{
		return $foodInBelly > 0;
	}	
	
	public void modifyFoodInBelly(int byHowMuch)
	{
		$foodInBelly += byHowMuch;
	}

	public void setDirectionBuffer(ArrayList<Direction> buffer)
	{
		$directionBuffer = buffer;
	}
	
	public ArrayList<Direction> getDirectionBuffer()
	{
		return $directionBuffer;
	}

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
	
	public void addToPointList(Point adding)
	{
		pointList.add(adding);
	}
	

	// Client side fields and methods, not updated when updateBasedOn(SnakeUpdatePacket pack) is called
	// on the server side of this SnakeData, these fields will be null


	private BuffDrawScript $currentBuffDrawScript;

	public void setBuffDrawScript(BuffDrawScript script)
	{
		$currentBuffDrawScript = script;
	}

	public void endBuffDrawScript()
	{
		$currentBuffDrawScript = null;
	}
	
	public boolean hasBuffDrawScript()
	{
		return $currentBuffDrawScript != null;
	}
	
	public BuffDrawScript getBuffDrawScript()
	{
		return $currentBuffDrawScript;
	}

	public void draw(Graphics g)
	{
		if ($currentBuffDrawScript != null)
			$currentBuffDrawScript.drawSnake(g);
		else
			drawSnakeNormally(g);
	}

	private void drawSnakeNormally(Graphics g)
	{
		for (Point segmentLoc : pointList)
		{
			Point drawCoords = GameplayClient.getPixelCoords(segmentLoc);
			g.setColor(color);
			g.fillRect(drawCoords.getX(), drawCoords.getY(), GameplayClient.UNIT_SIZE, GameplayClient.UNIT_SIZE);
		}
	}
}