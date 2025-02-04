package edu.wit.yeatesg.mps.otherdatatypes;

public class Color extends java.awt.Color
{	
	private static final long serialVersionUID = 7345839733841193315L;

	public static final Color RED = new Color(java.awt.Color.RED);
	public static final Color ORANGE = new Color(java.awt.Color.ORANGE);
	public static final Color YELLOW = new Color(255, 246, 71);
	public static final Color GREEN = new Color(java.awt.Color.GREEN);
	public static final Color BLUE = new Color(0, 85, 255);
	public static final Color MAGENTA = new Color(java.awt.Color.MAGENTA);
	public static final Color PINK = new Color(java.awt.Color.PINK);
	public static final Color CYAN = new Color(java.awt.Color.CYAN);
	public static final Color WHITE = new Color(java.awt.Color.WHITE);
	public static final Color BLACK = new Color(java.awt.Color.BLACK);
	public static final Color GRAY = new Color(java.awt.Color.GRAY);
	
	public Color(int r, int g, int b)
	{
		super(r, g, b);
	}
	
	public Color(java.awt.Color awtColor)
	{
		this(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
	}

	@Override
	public String toString()
	{
		String r = Integer.toString(getRed(), 16);
		String g = Integer.toString(getGreen(), 16);
		String b = Integer.toString(getBlue(), 16);
		r = r.length() < 2 ? "0" + r : r;
		g = g.length() < 2 ? "0" + g : g;
		b = b.length() < 2 ? "0" + b : b;
		return "#" + r + g + b;
	}
	
	public static Color fromString(String colorString)
	{
		colorString = colorString.substring(1);
		int r = Integer.parseInt(colorString.substring(0, 2), 16);
		int g = Integer.parseInt(colorString.substring(2, 4), 16);
		int b = Integer.parseInt(colorString.substring(4, 6), 16);
		return new Color(r, g, b);
	}
	
	public static double[] getChangeRate(Color start, Color end, int length)
	{
		double startR = start.getRed(), startG = start.getGreen(), startB = start.getBlue();
		double endR = end.getRed(), endG = end.getGreen(), endB = end.getBlue();
		double rDiff = endR - startR, gDiff = endG - startG, bDiff = endB - startB;
		return new double[] { rDiff / (length - 1),  gDiff / (length - 1), bDiff / (length - 1) };
	}
	
	public static Color[] getBlendArray(Color start, Color end, int length)
	{
		Color[] arr = new Color[length];
		double[] changeRate = getChangeRate(start, end, length);
		double currR = start.getRed(), currG = start.getGreen(), currB = start.getBlue();
		for (int i = 0; i < length; i++)
		{
			arr[i] = new Color((int) currR, (int) currG, (int) currB);
			currR += changeRate[0]; currG += changeRate[1]; currB += changeRate[2];
		}
		return arr;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Color)
		{
			Color other = (Color) obj;
			return other.getRed() == getRed() && other.getGreen() == getGreen() && other.getBlue() == getBlue();
		}
		return false;
	}
}
