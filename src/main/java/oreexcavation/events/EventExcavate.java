package oreexcavation.events;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.Event;
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
	
	public static class Break extends EventExcavate
    {
        private final BlockState state;
        private final BlockPos pos;
        
        public Break(MiningAgent agent, BlockState state, BlockPos pos)
        {
            super(agent);
            this.state = state;
            this.pos = pos;
        }
        
        public BlockState getBlockState()
        {
            return this.state;
        }
        
        public BlockPos getPos()
        {
            return this.pos;
        }
    }
    
    public static class Pass extends EventExcavate
    {
        private final Phase phase;
        
        public Pass(MiningAgent agent, Phase phase)
        {
            super(agent);
            this.phase = phase;
        }
        
        public Phase getPassPhase()
        {
            return phase;
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
}
