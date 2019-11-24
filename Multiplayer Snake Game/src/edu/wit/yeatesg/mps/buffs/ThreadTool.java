package edu.wit.yeatesg.mps.buffs;

import java.util.ArrayList;
import java.util.Iterator;

public class ThreadTool implements Iterable<Integer>
{
	private int startIndex;
	private int endIndex;

	public ThreadTool(int startIndex, int endIndex)
	{
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public static ThreadTool[] splitIntoThreads(double size)
	{
		int numThreads = ThreadTool.getSuggestedNumThreads((int) size);
		ThreadTool[] toolArr = new ThreadTool[numThreads];
		double[] fractions = new double[numThreads + 1];
		for (int i = 0; i <= numThreads; i++)
			fractions[i] = (double) i / (double) numThreads;
		for (int i = 0, j = 1; j < fractions.length; j++, i++)
			toolArr[i] = new ThreadTool((int) (fractions[i]*size), (int) (fractions[j]*size));
		return toolArr;
	}

	@Override
	public ThreadToolIterator iterator()
	{
		return new ThreadToolIterator();
	}

	class ThreadToolIterator implements Iterator<Integer>
	{
		private int currIndex = startIndex;

		@Override
		public boolean hasNext()
		{
			return currIndex < endIndex;
		}

		@Override
		public Integer next()
		{
			return currIndex++;
		}
	}
	
	public int getStartIndex()
	{
		return startIndex;
	}
	
	public int getEndIndex()
	{
		return endIndex;
	}
	
	public static void waitForThreads(ArrayList<Thread> threads)
	{
		boolean threadsDone = false;
		while (!threadsDone)
		{
			threadsDone = true;
			i: for (Thread tr : threads)
			{
				if (tr.isAlive())
				{
					threadsDone = false;
					break i;
				}
			}
		}
	}


	public static void waitForThreads(Thread[] threads)
	{
		boolean threadsDone = false;
		while (!threadsDone)
		{
			threadsDone = true;
			i: for (Thread tr : threads)
			{
				if (tr.isAlive())
				{
					threadsDone = false;
					break i;
				}
			}
		}
	}

	public static int getSuggestedNumThreads(int length)
	{
		return length / 50 > 0 ? (10) : (length / 25 > 0 ? (5) : (1));
	}
}