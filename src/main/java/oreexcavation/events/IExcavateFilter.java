package oreexcavation.events;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import oreexcavation.handlers.MiningAgent;

public interface IExcavateFilter
{
	public boolean canHarvest(EntityPlayerMP player, MiningAgent agent, BlockPos pos);
}
