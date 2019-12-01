package edu.wit.yeatesg.mps.otherdatatypes;

public class Vector
{
	private double x;
	private double y;
	
	public static final String REGEX = ",";
	
	public Vector(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Vector(Point initial, Point terminal)
	{
		this.x = terminal.getX() - initial.getX();
		this.y = terminal.getY() - initial.getY();
	}
	
	public Vector add(Vector other)
	{
		return new Vector(x + other.x, y + other.y);
	}
	
	public Vector multiply(double scalar)
	{
		return new Vector(x * scalar, y * scalar);
	}
	
	public Vector divide(double scalar)
	{
		return new Vector(x / scalar, y / scalar);
	}
	
	public double getNorm()
	{
		return Math.sqrt(x*x + y*y);
	}
	
	public Vector normalize()
	{
		return normalize(1);
	}
	
	public Vector normalize(int length)
	{
		return divide(getNorm()).multiply(length);
	}

	public static Vector fromString(String string)
	{
		try
		{
			String[] params = string.split(REGEX);
			return new Vector(Double.parseDouble(params[0]), Double.parseDouble(params[1]));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public void setXY(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void setX(double x)
	{
		this.x = x;
	}
	
	public void setY(double y)
	{
		this.y = y;
	}
	
	public double getX()
	{
		return x;
	}
	
	public double getY()
	{
		return y;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Vector)
		{
			Vector other = (Vector) obj;
			return other.x == x && other.y == y;
		}
		return false;
	}
	
	@Override
	public Vector clone()
	{
		return new Vector(x, y);
	}
	
	@Override
	public String toString()
	{
		return x + REGEX + y;
	}
}
