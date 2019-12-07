package edu.wit.yeatesg.mps.network.packets;

import edu.wit.yeatesg.mps.buffs.Fruit;

public class FruitPickupPacket extends Packet
{
	private String who;

	private Fruit pickedUp;

	public FruitPickupPacket(String... strings)
	{
		super(strings);
	}

	public FruitPickupPacket(String splittable)
	{
		this(splittable.split(REGEX));
	}

	public FruitPickupPacket(String who, Fruit pickedUp)
	{
		this(who, pickedUp.toString());
	}

	@Override
	protected void initFromStringArray(String[] args)
	{
		who = args[0];
		pickedUp = Fruit.fromString(args[1]);
	}

	public String getWhoPickedUp()
	{
		return who;
	}

	public void setWhoPickedUp(String who)
	{
		this.who = who;
	}

	public Fruit getFruit()
	{
		return pickedUp;
	}

	public void setFruit(Fruit fruit)
	{
		pickedUp = fruit;
	}
}