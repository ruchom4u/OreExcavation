package oreexcavation.core;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import oreexcavation.handlers.MiningScheduler;
import oreexcavation.undo.RestoreResult;

public class CommandUndo extends CommandBase
{
	
	@Override
	public String getCommandName()
	{
		return "undo_excavation";
	}
	
	@Override
	public String getCommandUsage(ICommandSender sender)
	{
		return "/undo_excavation";
	}
	
	@Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
	
	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return true;
	}
	
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		if(args.length != 0 || !(sender instanceof EntityPlayer))
		{
			throw new CommandException(getCommandUsage(sender));
		}
		
		if(ExcavationSettings.maxUndos <= 0)
		{
			throw new CommandException("oreexcavation.undo.failed.disabled");
		}
		
		EntityPlayer player = (EntityPlayer)sender;
		RestoreResult result = MiningScheduler.INSTANCE.attemptUndo(player);
		
		switch(result)
		{
			case INVALID_XP:
				sender.addChatMessage(new TextComponentTranslation("oreexcavation.undo.failed.xp"));
				break;
			case INVALID_ITEMS:
				sender.addChatMessage(new TextComponentTranslation("oreexcavation.undo.failed.items"));
				break;
			case NO_UNDO_HISTORY:
				sender.addChatMessage(new TextComponentTranslation("oreexcavation.undo.failed.no_history"));
				break;
			case OBSTRUCTED:
				sender.addChatMessage(new TextComponentTranslation("oreexcavation.undo.failed.obstructed"));
				break;
			case SUCCESS:
				sender.addChatMessage(new TextComponentTranslation("oreexcavation.undo.success"));
				break;
		}
	}
}
