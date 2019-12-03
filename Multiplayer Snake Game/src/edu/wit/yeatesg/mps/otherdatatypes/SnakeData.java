package edu.wit.yeatesg.mps.otherdatatypes;


import java.awt.Graphics;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.Timer;

import edu.wit.yeatesg.mps.buffs.SnakeDrawScript;
import edu.wit.yeatesg.mps.buffs.TranslucentBuffDrawScript;
import edu.wit.yeatesg.mps.buffs.BuffType;
import edu.wit.yeatesg.mps.buffs.Fruit;
import edu.wit.yeatesg.mps.buffs.HungryBuffDrawScript;
import edu.wit.yeatesg.mps.network.clientserver.GameplayGUI;
import edu.wit.yeatesg.mps.network.clientserver.SocketSecurityTool;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;

public class SnakeData
{
	public static final String REGEX = ":";

	public SnakeData(String name, Color color, Direction direction, PointList pointList, boolean isHost, boolean isAlive, boolean addingSegment)
	{
		this.name = name;
		this.color = color;
		this.direction = direction;
		this.pointList = pointList;
		this.isHost = isHost;
		this.isAlive = true;
		this.isAddingSegment = addingSegment;
	}

	public SnakeData(String... params)
	{
		this.name = params[0];
		this.color = Color.fromString(params[1]);
		this.direction = Direction.fromString(params[2]);
		this.pointList = PointList.fromString(params[3]);
		this.isHost = Boolean.parseBoolean(params[4]);
		this.isAlive = Boolean.parseBoolean(params[5]);
		this.isAddingSegment = Boolean.parseBoolean(params[6]);
	}

	public SnakeData()
	{
		this.name = "null";
		this.color = Color.BLACK;
		this.pointList = new PointList();
		this.direction = Direction.DOWN;
		this.isHost = false;
		this.isAlive = true;
		this.isAddingSegment = false;
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
	private boolean isAddingSegment;	

	// Remember snakes keep getting updated after they die. might wanna do smthing abt that (maybe) (potentially)
	public void updateBasedOn(SnakeUpdatePacket pack)
	{
		SnakeData updated = pack.getClientData();

		boolean initPointList = true;

		ArrayList<Field> fieldsToTransfer = ReflectionTools.getFieldsThatUpdate(SnakeData.class);
		for (int i = 0; i < fieldsToTransfer.size(); i++)
		{       // Update the fields of this object with the corresponding fields of the updated
			try // SnakeData object. Don't update fields that begin with $
			{
				Field f = fieldsToTransfer.get(i);
				f.setAccessible(true);
				Object updatedValue = f.get(updated);
				if (!(f.getName().equals("pointList") && (updatedValue.toString() == "" || updatedValue.toString().equals("") || updatedValue.toString() == null))) 
				{
					f.set(this, updatedValue); 
				}
				else
					initPointList = false;				
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}

		if (!initPointList && isAlive) // Move the snake if the pointList is null (move it on the client for efficiency)
		{
			PointList pointList = this.pointList.clone(); 
			Point oldHead = pointList.get(0);
			Point head = oldHead.addVector(direction.getVector());
			head = GameplayGUI.keepInBounds(head);

			pointList.add(0, head);

			if (isAddingSegment)
				isAddingSegment = false;
			else
				pointList.remove(pointList.size() - 1);
			this.pointList = pointList;
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

	public PointList getPointList(boolean clone)
	{
		return clone ? pointList.clone() : pointList;
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

	public void setIsAlive(boolean isAlive)
	{
		this.isAlive = isAlive;
	}

	public boolean isAddingSegment()
	{
		return isAddingSegment;
	}

	public void setAddingSegment(boolean addingSegment)
	{
		isAddingSegment = addingSegment;
	}

	public int getLength()
	{
		return pointList.size();
	}
	
	
	///

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

	private SocketSecurityTool $encrypter;
	private DataOutputStream $outputStream;
	private Socket $socket;
	private ArrayList<Direction> $directionBuffer;
	private boolean $buffTranslucentActive;
	private boolean $buffHungryActive;
	private int $foodInBelly;

	private Timer $removeBuffTimer;
	
	public void grantBuff(BuffType buff)
	{	
		$removeBuffTimer = new Timer(buff.getDuration(), null);
		$removeBuffTimer.setRepeats(false);
		switch (buff)
		{
		case BUFF_TRANSLUCENT:
			$buffTranslucentActive = true;
			$removeBuffTimer.addActionListener((e) -> $buffTranslucentActive = false );
			break;
		case BUFF_HUNGRY:
			$buffHungryActive = true;
			$removeBuffTimer.addActionListener((e) -> { $buffHungryActive = false; System.out.println("buff ran out"); } );
			break;
		default:
			break;
		}
		$removeBuffTimer.start();
	}

	public void removeAllBuffsEarly()
	{
		removeHungryBuffEarly();
		removeTranslucentBuffEarly();
	}

	public void removeTranslucentBuffEarly()
	{
		if (hasBuffTranslucent())
		{
			$buffTranslucentActive = false;
			if ($removeBuffTimer != null)
				$removeBuffTimer.stop();
		}
	}

	public void removeHungryBuffEarly()
	{
		if (hasBuffHungry())
		{
			$buffHungryActive = false;
			if ($removeBuffTimer != null)
				$removeBuffTimer.stop();
		}
	}

	public boolean hasBuffTranslucent()
	{
		return $buffTranslucentActive;
	}

	public boolean hasBuffHungry()
	{
		return $buffHungryActive;
	}

	public boolean hasAnyBuffs()
	{
		return $buffTranslucentActive || $buffHungryActive; // || otherBuffActive || otherBuff2Active..
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
	
	public SocketSecurityTool getEncrypter()
	{
		return $encrypter;
	}
	
	public void setEncrypter(SocketSecurityTool encrypter)
	{
		$encrypter = encrypter;
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

	public int getOccurrenceOf(Point p)
	{
		int occurrences = 0;
		for (Point p2 : pointList)
			if (p.equals(p2))
				occurrences++;
		return occurrences;
	}

	// Client side fields and methods, not updated when updateBasedOn(SnakeUpdatePacket pack) is called
	// on the server side of this SnakeData, these fields will be null


	private SnakeDrawScript $currentDrawScript;

	public void setDrawScript(SnakeDrawScript script)
	{
		if (script == null)
			throw new RuntimeException();
		if ($currentDrawScript != null)
			endBuffDrawScriptEarly();
		$currentDrawScript = script;
	}

	/**
	 * Called when a draw script ends early 
	 */
	public void onDrawScriptEnd()
	{
		$currentDrawScript = null;
	}

	public void endBuffDrawScriptEarly()
	{
		$currentDrawScript.endEarly();
	}

	public boolean hasBuffDrawScript()
	{
		return $currentDrawScript != null;
	}

	public SnakeDrawScript getBuffDrawScript()
	{
		return $currentDrawScript;
	}
	
	public boolean hasTranslucentBuffDrawScript()
	{
		return $currentDrawScript instanceof TranslucentBuffDrawScript;
	}
	
	public boolean hasHungryBuffDrawScript()
	{
		return $currentDrawScript instanceof HungryBuffDrawScript;
	}

	public void drawScriptIfApplicable(Graphics g)
	{
		if ($currentDrawScript != null && $currentDrawScript.hasStarted())
			$currentDrawScript.drawSnake(g);
	}
	
	public void drawNormallyIfApplicable(Graphics g, GameplayGUI drawingOn)
	{
		if ($currentDrawScript == null && isAlive)
			drawSnakeNormally(g, drawingOn);
	}
	
	public void drawSnakeNormally(Graphics g, GameplayGUI drawingOn)
	{
		{
			boolean drawingSelf = this.equals(drawingOn.getClient());
			int drawSize = GameplayGUI.UNIT_SIZE;
			int index = 0;
			for (Point segmentLoc : pointList)
			{
				if (drawingSelf
						|| !drawingOn.getClient().hasHungryBuffDrawScript()
						|| index == 0
						|| (getLength() > Fruit.MIN_FRUIT_HUNGRY_LENGTH && index < getLength() - Fruit.MAX_BITE_OFF))
				{
					Point drawCoords = GameplayGUI.getPixelCoords(segmentLoc);
					g.setColor(color);
					if (getOccurrenceOf(segmentLoc) > 1)
					{
						int outlineThickness = (int) (GameplayGUI.MAX_OUTLINE_THICKNESS*0.65);
						int offset = 0;
						for (int i = 0; i < outlineThickness; i++)
						{
							g.drawRect(drawCoords.getX() + offset, drawCoords.getY() + offset, drawSize - 2*offset - 1, drawSize - 2*offset - 1);
							offset++;
						}
					}
					else
					{
						g.fillRect(drawCoords.getX(), drawCoords.getY(), drawSize, drawSize);
					}
				}
				index++;
			}
		}
	}
}