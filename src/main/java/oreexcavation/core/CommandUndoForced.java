package oreexcavation.core;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import oreexcavation.handlers.MiningScheduler;
import oreexcavation.undo.RestoreResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandUndoForced extends CommandBase
{
	@Nonnull
	@Override
	public String getName()
	{
		return "undo_excavation_forced";
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull ICommandSender sender)
	{
		return "/undo_excavation_forced <player>";
	}
	
	@Override
    public int getRequiredPermissionLevel()
    {
        return 4;
    }
	
	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return true;
	}
	
	@Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }
    
    @Nonnull
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if(args.length == 1) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        return Collections.emptyList();
    }
	
	@Override
	public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException
	{
		if(args.length != 1 || !(sender instanceof EntityPlayer))
		{
			throw new CommandException(getUsage(sender));
		}
		
		if(ExcavationSettings.maxUndos <= 0)
		{
			throw new CommandException("oreexcavation.undo.failed.disabled");
		}
		
		EntityPlayer player = getPlayer(server, sender, args[0]);
		RestoreResult result = MiningScheduler.INSTANCE.attemptUndo(player, true);
		
		switch(result)
		{
			case INVALID_XP:
				sender.sendMessage(new TextComponentTranslation("oreexcavation.undo.failed.xp"));
				break;
			case INVALID_ITEMS:
				sender.sendMessage(new TextComponentTranslation("oreexcavation.undo.failed.items"));
				break;
			case NO_UNDO_HISTORY:
				sender.sendMessage(new TextComponentTranslation("oreexcavation.undo.failed.no_history"));
				break;
			case OBSTRUCTED:
				sender.sendMessage(new TextComponentTranslation("oreexcavation.undo.failed.obstructed"));
				break;
			case SUCCESS:
				sender.sendMessage(new TextComponentTranslation("oreexcavation.undo.success"));
				break;
		}
	}
}
