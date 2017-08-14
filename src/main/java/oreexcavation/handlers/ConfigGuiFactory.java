package oreexcavation.handlers;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import oreexcavation.client.GuiOEConfig;

public class ConfigGuiFactory implements IModGuiFactory
{
	@Override
	public void initialize(Minecraft minecraftInstance)
	{
	}
	
	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
	{
		return null;
	}
	
	@Override
	public GuiScreen createConfigGui(GuiScreen arg0)
	{
		return new GuiOEConfig(arg0);
	}
	
	@Override
	public boolean hasConfigGui()
	{
		return true;
	}
}
