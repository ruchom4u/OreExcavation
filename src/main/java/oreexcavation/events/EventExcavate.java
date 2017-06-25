package oreexcavation.events;

import cpw.mods.fml.common.eventhandler.Event;
import oreexcavation.handlers.MiningAgent;

public abstract class EventExcavate extends Event
{
	private final MiningAgent agent;
	
	public EventExcavate(MiningAgent agent)
	{
		this.agent = agent;
	}
	
	public MiningAgent getAgent()
	{
		return agent;
	}
	
	public static class Pre extends EventExcavate
	{
		public Pre(MiningAgent agent)
		{
			super(agent);
		}
		
		@Override
		public boolean isCancelable()
		{
			return true;
		}
		
		@Override
		public boolean hasResult()
		{
			return true;
		}
	}
	
	public static class Post extends EventExcavate
	{
		public Post(MiningAgent agent)
		{
			super(agent);
		}
	}
}
