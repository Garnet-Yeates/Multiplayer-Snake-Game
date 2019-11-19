package edu.wit.yeatesg.mps.phase0.otherdatatypes;


import java.awt.Graphics;

import javax.swing.Timer;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;

public abstract class BuffAnimationScript
{
	private GameplayClient container;

	protected SnakeData whoGotTheBuff;

	private int currentTick;
	private int currentAnimationTick;

	private int maxTicks;

	private int tickRate;
	private int animationRate;

	private AnimationScriptTimer tickTimer;
	private AnimationScriptTimer animationTimer;

	public BuffAnimationScript(GameplayClient container, SnakeData whoGotTheBuff, int tickRate, int animationRate, long duration)
	{
		this.maxTicks = (int) (duration / tickRate);
		this.container = container;
		this.whoGotTheBuff = whoGotTheBuff;
		this.whoGotTheBuff.setBuffAnimationScript(this);
		tickTimer = new AnimationScriptTimer(TimerType.TICK_TIMER);
		animationTimer = new AnimationScriptTimer(TimerType.ANIMATION_TIMER);
	}

	public BuffAnimationScript(GameplayClient container, SnakeData whoGotTheBuff, int tickRate, int animationRate, int numTicks)
	{
		this(container, whoGotTheBuff, tickRate, animationRate, (long) numTicks*tickRate);
	}

	public abstract void drawSnake(Graphics g);

	enum TimerType { TICK_TIMER, ANIMATION_TIMER };

	private class AnimationScriptTimer extends Timer
	{
		private static final long serialVersionUID = -9066122771752189425L;

		public AnimationScriptTimer(TimerType type)
		{
			super(type == TimerType.TICK_TIMER ? tickRate : animationRate, null);
			addActionListener((e) ->
			{
				container.repaint();
				if (type == TimerType.TICK_TIMER)
				{
					currentTick++;
					if (currentTick == maxTicks)
					{
						tickTimer.stop();
						animationTimer.stop();
						tickTimer = null;
						animationTimer = null;
						whoGotTheBuff.endBuffAnimationScript();
					}
				}
				else currentAnimationTick++;
			});
			this.start();
		}
	}
}