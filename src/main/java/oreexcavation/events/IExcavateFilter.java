package oreexcavation.events;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import oreexcavation.handlers.MiningAgent;

public interface IExcavateFilter
{
	boolean canHarvest(ServerPlayerEntity player, MiningAgent agent, BlockPos pos);
}
