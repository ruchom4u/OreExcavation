package oreexcavation.events;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import net.minecraft.block.Block;
import oreexcavation.handlers.MiningAgent;
import oreexcavation.utils.BlockPos;

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
        private final Block block;
        private final int meta;
        private final BlockPos pos;
        
        public Break(MiningAgent agent, Block block, int meta, BlockPos pos)
        {
            super(agent);
            this.block = block;
            this.meta = meta;
            this.pos = pos;
        }
        
        @Override
        public boolean isCancelable()
        {
            return false;
        }
        
        public Block getBlock()
        {
            return this.block;
        }
        
        public int getMeta()
        {
            return this.meta;
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
