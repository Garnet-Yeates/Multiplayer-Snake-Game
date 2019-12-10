package edu.wit.yeatesg.mps.otherdatatypes;

import java.awt.Graphics;
import java.awt.PointerInfo;
import java.io.BufferedOutputStream;
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
import edu.wit.yeatesg.mps.network.clientserver.MPSServer.ClientInformation;
import edu.wit.yeatesg.mps.network.clientserver.MPSServer.ClientThread;
import edu.wit.yeatesg.mps.network.clientserver.MPSServer.PlayerSlot;
import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

public class Snake
{
	public static final String REGEX = ":";

	public Snake(String name, Color color, Direction direction, PointList pointList, boolean isHost, boolean isAlive, boolean addingSegment, int playerNum)
	{
		this.name = name;
		this.color = color;
		this.direction = direction;
		this.pointList = pointList;
		this.isHost = isHost;
		this.isAlive = true;
		this.isAddingSegment = addingSegment;
		this.playerNum = playerNum;
	}

	public Snake(String... params)
	{
		name = params[0];
		color = Color.fromString(params[1]);
		direction = Direction.fromString(params[2]);
		pointList = PointList.fromString(params[3]);
		isHost = Boolean.parseBoolean(params[4]);
		isAlive = Boolean.parseBoolean(params[5]);
		isAddingSegment = Boolean.parseBoolean(params[6]);
		playerNum = Integer.parseInt(params[7]);
	}

	public Snake()
	{
		name = "null";
		color = Color.BLACK;
		pointList = new PointList();
		direction = Direction.DOWN;
		isHost = false;
		isAlive = true;
		isAddingSegment = false;
		playerNum = -1;
	}

	public Snake(String splittableString)
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
	private int playerNum;

	private PointList $multipleOccurances = new PointList();
	
	public PointList getMultipleOccurancesList()
	{
		return $multipleOccurances;
	}
	
	// Remember snakes keep getting updated after they die. might wanna do smthing abt that (maybe) (potentially)
	public void updateBasedOn(SnakeUpdatePacket pack)
	{
		Snake updated = pack.getClientData();

		boolean receivingPointList = true;

		ArrayList<Field> fieldsToTransfer = ReflectionTools.getFieldsThatUpdate(Snake.class);
		for (int i = 0; i < fieldsToTransfer.size(); i++)
		{       // Update the fields of this object with the corresponding fields of the updated
			try // SnakeData object. Don't update fields that begin with $
			{
				Field f = fieldsToTransfer.get(i);
				f.setAccessible(true);
				Object updatedValue = f.get(updated);
				if (f.getName().equals("pointList") && (updatedValue.toString().equals("")))
					receivingPointList = false;
				else
					f.set(this, updatedValue);		
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		if (!receivingPointList && isAlive) // Move the snake if the pointList is null (move it on the client for efficiency)
		{
			PointList pointList = this.pointList.clone(); 
			Point oldHead = pointList.get(0);
			Point head = oldHead.addVector(direction.getVector());
			head = GameplayGUI.keepInBounds(head);

			if (pointList.contains(head))
				$multipleOccurances.add(head);
			pointList.add(0, head);

			if (isAddingSegment)
				isAddingSegment = false;
			else
			{
				// Remove from multiple occurrences just in case it is in there
				$multipleOccurances.remove(pointList.get(pointList.size() - 1));
				pointList.remove(pointList.size() - 1);	
			}
				
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

	public int getPlayerNum()
	{
		return playerNum;
	}

	public void setPlayerNum(int playerNum)
	{
		this.playerNum = playerNum;
	}

	///

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Snake && ((Snake) obj).getClientName().equalsIgnoreCase(name);
	}

	@Override
	public String toString()
	{
		return ReflectionTools.fieldsToString(REGEX, this, Snake.class);
	}

	@Override
	public Snake clone()
	{
		return new Snake(toString());
	}

	public void setPointList(PointList pointList)
	{
		this.pointList = pointList;		
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

	// Server side fields and methods, not updated when updateBasedOn(SnakeUpdatePacket pack) is called
	// on the client side of this SnakeData, all of these fields will be null

	private ClientInformation $socketInfo;
	private ArrayList<Direction> $directionBuffer;
	private boolean $buffTranslucentActive;
	private boolean $buffHungryActive;
	private int $foodInBelly;
	private PlayerSlot $playerSlot;
	private ClientThread $threadForServer;

	public void setSocketInfo(ClientInformation info)
	{
		$socketInfo = info;
	}
	
	public ClientInformation getSocketInfo()
	{
		return $socketInfo;
	}
	
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
	
	public int getFoodInBelly()
	{
		return $foodInBelly;
	}

	public void setDirectionBuffer(ArrayList<Direction> buffer)
	{
		$directionBuffer = buffer;
	}

	public ArrayList<Direction> getDirectionBuffer()
	{
		return $directionBuffer;
	}

	public void onSlotBind(PlayerSlot playerSlot)
	{
		$playerSlot = playerSlot;
	}

	public void onSlotUnbind()
	{
		$playerSlot = null;
		setPlayerNum(-1);
	}

	public PlayerSlot getPlayerSlot()
	{
		return $playerSlot;
	}
	
	public void setPacketReceiveThread(ClientThread receiveThread)
	{
		$threadForServer = receiveThread;
	}
	
	public ClientThread getPacketReceiveThread()
	{
		return $threadForServer;
	}

	// Client side fields and methods, not updated when updateBasedOn(SnakeUpdatePacket pack) is called
	// on the server side of this SnakeData, these fields will be null


	private SnakeDrawScript $currentDrawScript;

	public void setDrawScript(SnakeDrawScript script)
	{
		if (script == null)
			throw new IllegalArgumentException();
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
		if ($currentDrawScript != null)
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
	
	private boolean occursMoreThanOnce(Point p)
	{
		return $multipleOccurances.contains(p);
	}

	public void drawSnakeNormally(Graphics g, GameplayGUI drawingOn)
	{
		boolean drawingSelf = this.equals(drawingOn.getClient());
		int drawSize = UNIT_SIZE;
		int index = 0;
		for (Point segmentLoc : pointList)
		{
			boolean drawingOnNotHungry = !drawingOn.getClient().hasHungryBuffDrawScript();
			boolean drawingHead = index == 0;
			boolean highlightableLocation = getLength() > Fruit.COMPLEX_CHECK_MIN && index < getLength() - Fruit.MAX_BITE_OFF;
			if (drawingSelf || drawingOnNotHungry || drawingHead || highlightableLocation)
			{
				Point drawCoords = GameplayGUI.getPixelCoords(segmentLoc);
				g.setColor(color);
				if (occursMoreThanOnce(segmentLoc))
				{
					int outlineThickness = (int) (MAX_OUTLINE_THICKNESS*0.65);
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