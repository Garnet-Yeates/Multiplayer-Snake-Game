package edu.wit.yeatesg.mps.buffs;

import java.util.ArrayList;

public class ThreadList
{
	private ArrayList<Thread> internal = new ArrayList<>();
	
	public void startAndWait(int timeout)
	{
		for (Thread t : internal)
		{
			t.start();
			try
			{
				t.join(timeout);
			}
			catch (InterruptedException e)
			{
				System.out.println("Root thread was interrupted while waiting for " + internal.size() + " threads to finish.");
			}
		}
	}

	public void joinThread(Thread adding)
	{
		internal.add(adding);
	}
}
