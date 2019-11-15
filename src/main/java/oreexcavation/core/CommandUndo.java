package oreexcavation.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import oreexcavation.handlers.MiningScheduler;
import oreexcavation.undo.RestoreResult;

public class CommandUndo
{
    public static void register(CommandDispatcher<CommandSource> dispatch)
    {
        LiteralArgumentBuilder<CommandSource> argBuilder = Commands.literal("undo_excavation");
        argBuilder.requires((src) -> src.hasPermissionLevel(0));
        argBuilder.executes(CommandUndo::execute);
        dispatch.register(argBuilder);
    }
    
    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException
    {
		PlayerEntity player = context.getSource().asPlayer();
		
		if(ExcavationSettings.maxUndos <= 0)
		{
			throw new CommandException(new TranslationTextComponent("oreexcavation.undo.failed.disabled"));
		}
		
		RestoreResult result = MiningScheduler.INSTANCE.attemptUndo(player);
		
		switch(result)
		{
			case INVALID_XP:
				player.sendMessage(new TranslationTextComponent("oreexcavation.undo.failed.xp"));
				break;
			case INVALID_ITEMS:
				player.sendMessage(new TranslationTextComponent("oreexcavation.undo.failed.items"));
				break;
			case NO_UNDO_HISTORY:
				player.sendMessage(new TranslationTextComponent("oreexcavation.undo.failed.no_history"));
				break;
			case OBSTRUCTED:
				player.sendMessage(new TranslationTextComponent("oreexcavation.undo.failed.obstructed"));
				break;
			case SUCCESS:
				player.sendMessage(new TranslationTextComponent("oreexcavation.undo.success"));
				break;
		}
		
		return 1; // Number of operations/players
	}
}
