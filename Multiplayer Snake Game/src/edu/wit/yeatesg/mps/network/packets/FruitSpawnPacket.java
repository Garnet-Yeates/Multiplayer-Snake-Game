package edu.wit.yeatesg.mps.network.packets;

import edu.wit.yeatesg.mps.buffs.Fruit;

/**
 * Informs each client that a Fruit has spawned on the server so that the client can update
 * its Fruit list
 * @author yeatesg
 *
 */
public class FruitSpawnPacket extends Packet
{
	private Fruit spawned;

	public FruitSpawnPacket(String... strings)
	{
		super(strings);
	}
	
	public FruitSpawnPacket(String splittable)
	{
		super(splittable.split(REGEX));
	}
	
	public FruitSpawnPacket(Fruit fruit)
	{
		this(fruit.toString());
	}
	
	public Fruit getFruit()
	{
		return spawned;
	}
	
	public void setFruit(Fruit fruit)
	{
		spawned = fruit;
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		spawned = Fruit.fromString(args[0]);
	}
}