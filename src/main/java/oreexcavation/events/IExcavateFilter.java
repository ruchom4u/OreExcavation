package oreexcavation.events;

import net.minecraft.entity.player.EntityPlayerMP;
import oreexcavation.handlers.MiningAgent;
import oreexcavation.utils.BlockPos;

public interface IExcavateFilter
{
	boolean canHarvest(EntityPlayerMP player, MiningAgent agent, BlockPos pos);
}
