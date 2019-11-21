package edu.wit.yeatesg.mps.phase0.otherdatatypes;


import java.util.ArrayList;

public class PointList extends ArrayList<Point>
{
	private static final long serialVersionUID = 5798845699085599767L;
	
	public static final String REGEX = ";";
	
	public PointList(Point... pointArgs)
	{
		for (Point point : pointArgs)
		{
			add(point);
		}
	}
	
	public PointList(String... pointArgs)
	{
		Point p;
		for (String pointString : pointArgs)
			if ((p = Point.fromString(pointString)) != null)
				add(p);
	}
	
	public PointList(String splittableString)
	{
		this(splittableString.split(REGEX));
	}
	
	public PointList()
	{
		super(3000);
	}
	
	public static PointList fromString(String splittable)
	{
		return new PointList(splittable);
	}
	
	@Override
	public PointList clone()
	{
		return new PointList(toString());
	}
	
	@Override
	public String toString()
	{
		String s = "";
		int index = 0;
		for (Point pt : this)
		{
			s += pt + (index == size() - 1 ? "" : REGEX);
			index++;
		}
		return s;
	}	
}
