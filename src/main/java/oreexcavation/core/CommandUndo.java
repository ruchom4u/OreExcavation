package oreexcavation.core;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
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
	public boolean canCommandSenderUseCommand(ICommandSender sender)
	{
		return true;
	}
	
	@Override
	public void processCommand(ICommandSender sender, String[] args)
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
				sender.addChatMessage(new ChatComponentTranslation("oreexcavation.undo.failed.xp"));
				break;
			case INVALID_ITEMS:
				sender.addChatMessage(new ChatComponentTranslation("oreexcavation.undo.failed.items"));
				break;
			case NO_UNDO_HISTORY:
				sender.addChatMessage(new ChatComponentTranslation("oreexcavation.undo.failed.no_history"));
				break;
			case OBSTRUCTED:
				sender.addChatMessage(new ChatComponentTranslation("oreexcavation.undo.failed.obstructed"));
				break;
			case SUCCESS:
				sender.addChatMessage(new ChatComponentTranslation("oreexcavation.undo.success"));
				break;
		}
	}
}
