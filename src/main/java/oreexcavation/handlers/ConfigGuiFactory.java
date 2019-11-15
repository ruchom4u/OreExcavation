package oreexcavation.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.fml.client.IModGuiFactory;
import oreexcavation.client.GuiOEConfig;

import java.util.Set;

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
	public Screen createConfigGui(Screen arg0)
	{
		return new GuiOEConfig(arg0);
	}
	
	@Override
	public boolean hasConfigGui()
	{
		return true;
	}
}
