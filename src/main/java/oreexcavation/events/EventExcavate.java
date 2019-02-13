package oreexcavation.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
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
        private final IBlockState state;
        private final BlockPos pos;
        
        public Break(MiningAgent agent, IBlockState state, BlockPos pos)
        {
            super(agent);
            this.state = state;
            this.pos = pos;
        }
        
        @Override
        public boolean isCancelable()
        {
            return false;
        }
        
        public IBlockState getBlockState()
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
