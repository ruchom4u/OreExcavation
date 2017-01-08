package oreexcavation.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.undo.ExcavateHistory;
import oreexcavation.undo.RestoreResult;
import com.google.common.base.Stopwatch;

public class MiningScheduler
{
	public static final MiningScheduler INSTANCE = new MiningScheduler();
	
	private HashMap<UUID,MiningAgent> agents = new HashMap<UUID,MiningAgent>();
	private HashMap<UUID,ExcavateHistory> undoing = new HashMap<UUID,ExcavateHistory>();
	private HashMap<UUID,List<ExcavateHistory>> undoHistory = new HashMap<UUID,List<ExcavateHistory>>();
	
	private Stopwatch timer;
	
	private MiningScheduler()
	{
		timer = Stopwatch.createStarted();
	}
	
	public MiningAgent getActiveAgent(UUID uuid)
	{
		return agents.get(uuid);
	}
	
	public void stopMining(EntityPlayerMP player)
	{
		MiningAgent a = agents.get(player.getUniqueID());
		
		if(a != null)
		{
			a.dropEverything();
		}
		
		agents.remove(player.getUniqueID());
	}
	
	public MiningAgent startMining(EntityPlayerMP player, BlockPos pos, IBlockState state, ExcavateShape shape)
	{
		MiningAgent existing = agents.get(player.getUniqueID());
		
		if(existing != null)
		{
			existing.appendBlock(pos);
		} else
		{
			existing = new MiningAgent(player, pos, state);
			agents.put(player.getUniqueID(), existing);
			
			if(shape != null)
			{
				existing.setShape(shape, ExcavateShape.getFacing(player));
			}
			
			existing.init();
		}
		
		return existing;
	}
	
	public RestoreResult attemptUndo(EntityPlayer player)
	{
		RestoreResult result = RestoreResult.NO_UNDO_HISTORY;
		List<ExcavateHistory> list = undoHistory.get(player.getUniqueID());
		list = list != null? list : new ArrayList<ExcavateHistory>();
		
		if(list.size() <= 0)
		{
			return RestoreResult.NO_UNDO_HISTORY;
		} else
		{
			result = list.get(list.size() - 1).canRestore(player.getServer(), player);
		}
		
		if(result == RestoreResult.SUCCESS)
		{
			undoing.put(player.getUniqueID(), list.remove(list.size() - 1));
		}
		
		return result;
	}
	
	public void appendHistory(UUID uuid, ExcavateHistory history)
	{
		List<ExcavateHistory> list = undoHistory.get(uuid);
		list = list != null? list : new ArrayList<ExcavateHistory>();
		
		list.add(history);
		
		while(list.size() > ExcavationSettings.maxUndos)
		{
			list.remove(0);
		}
		
		undoHistory.put(uuid, list);
	}
	
	public void tickAgents(MinecraftServer server)
	{
		timer.reset();
		timer.start();
		
		Iterator<Entry<UUID,MiningAgent>> iterAgents = agents.entrySet().iterator();
		
		while(iterAgents.hasNext())
		{
			if(ExcavationSettings.tpsGuard && timer.elapsed(TimeUnit.MILLISECONDS) > 40)
			{
				EventHandler.skipNext = true;
				break;
			}
			
			Entry<UUID,MiningAgent> entry = iterAgents.next();
			
			MiningAgent a = entry.getValue();
			
			EventHandler.captureAgent = a;
			boolean complete = a.tickMiner();
			EventHandler.captureAgent = null;
			
			if(complete)
			{
				a.dropEverything();
				appendHistory(entry.getKey(), a.getHistory());
				iterAgents.remove();
			}
		}
		
		Iterator<Entry<UUID,ExcavateHistory>> iterUndo = undoing.entrySet().iterator();
		
		while(iterUndo.hasNext())
		{
			if(ExcavationSettings.tpsGuard && timer.elapsed(TimeUnit.MILLISECONDS) > 40)
			{
				EventHandler.skipNext = true;
				break;
			}
			
			Entry<UUID,ExcavateHistory> entry = iterUndo.next();
			
			boolean complete = entry.getValue().tickRestore(server, server.getPlayerList().getPlayerByUUID(entry.getKey()));
			
			if(complete)
			{
				iterUndo.remove();
			}
		}
		
		timer.stop();
	}
	
	public void resetAll()
	{
		agents.clear();
		undoing.clear();
		undoHistory.clear();
	}
}
