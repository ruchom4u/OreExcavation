package oreexcavation.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class GuiOEConfig extends Screen
{
	public GuiOEConfig(Screen parent)
	{
	    super(new StringTextComponent("Ore Excavation Config"));
		//super(parent, new ConfigElement(ConfigHandler.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), OreExcavation.MODID, false, false, OreExcavation.NAME);
	}
}
