package edu.wit.yeatesg.mps.buffs;

public enum FruitType
{
//	SORT BY RARITY (highest rarity should be at the left, lowest rarity should be at the right)
	FRUIT_HUNGRY(BuffType.BUFF_HUNGRY, 10, 5), FRUIT_TRANSLUCENT(BuffType.BUFF_TRANSLUCENT, 10, 15), FRUIT_REGULAR(null, 200, 1);
		
	private BuffType connectedBuff;
	private int segmentsGiven;
	private int rarity;
	
	private FruitType(BuffType givesBuff, int segmentsGiven, int rarity)
	{
		connectedBuff = givesBuff;
		this.segmentsGiven = segmentsGiven;
		this.rarity = rarity;
	}
	
	public int getRarity()
	{
		return rarity;
	}
	
	public int getNumSegmentsGiven()
	{
		return segmentsGiven;
	}
	
	public BuffType getAssociatedBuff()
	{
		return connectedBuff;
	}
	
	public boolean hasAssociatedBuff()
	{
		return connectedBuff != null;
	}
	
	public static FruitType fromString(String string)
	{
		for (FruitType type : FruitType.values())
			if (string.equals(type.toString()))
				return type;
		return null;
	}
}
