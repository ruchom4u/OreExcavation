package oreexcavation.client;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import oreexcavation.core.OreExcavation;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;
import org.lwjgl.input.Keyboard;

public class GuiEditShapes extends GuiScreen
{
	private static final ResourceLocation GUI_TEX = new ResourceLocation(OreExcavation.MODID, "textures/gui/edit_shapes.png");
	private static final ResourceLocation GUI_ICO = new ResourceLocation("minecraft:textures/gui/icons.png");
	
	private int guiLeft = 0;
	private int guiTop = 0;
	
	private int idx = 0;
	private ExcavateShape curShape = null;
	
	private GuiTextField txtField = null;
	private GuiNumberField nmbField = null;
	
	private GuiButton btnLeft = null;
	private GuiButton btnRight = null;
	private GuiButton btnAdd = null;
	private GuiButton btnRemove = null;
	
	public GuiEditShapes()
	{
	}
	
	@Override
	public void initGui()
	{
		this.guiLeft = this.width/2 - 128;
		this.guiTop = this.height/2 - 128;
		
		Keyboard.enableRepeatEvents(true);
		
		btnLeft = new GuiButton(0, guiLeft + 14, guiTop + 118, 20, 20, "<");
		btnLeft.enabled = idx > 0;
		
		btnRight = new GuiButton(1, guiLeft + 222, guiTop + 118, 20, 20, ">");
		btnRight.enabled = idx + 1 < ShapeRegistry.INSTANCE.getShapeList().size();
		
		btnAdd = new GuiButton(2, guiLeft + 150, guiTop + 222, 20, 20, "+");
		btnAdd.packedFGColour = Color.GREEN.getRGB();
		
		btnRemove = new GuiButton(3, guiLeft + 86, guiTop + 222, 20, 20, "-");
		btnRemove.packedFGColour = Color.RED.getRGB();
		
		txtField = new GuiTextField(0, mc.fontRenderer, guiLeft + 48, guiTop + 16, 112, 16);
		nmbField = new GuiNumberField(mc.fontRenderer, guiLeft + 176, guiTop + 16, 32, 16);
		
		this.buttonList.add(btnLeft);
		this.buttonList.add(btnRight);
		this.buttonList.add(btnAdd);
		this.buttonList.add(btnRemove);
		
		refreshShape();
	}
	
	@Override
	public void drawScreen(int mx, int my, float partialTick)
	{
		this.drawDefaultBackground();
		
		mc.renderEngine.bindTexture(GUI_TEX);
		this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 256);
		
		txtField.drawTextBox();
		nmbField.drawTextBox();
		
		super.drawScreen(mx, my, partialTick);
		
		GlStateManager.color(1F, 1F, 1F, 1F);
		
		if(curShape != null)
		{
			int mask = curShape.getShapeMask();
			int off = curShape.getReticle();
			
			for(int x = 0; x < 5; x++)
			{
				for(int y = 0; y < 5; y++)
				{
					int flag = ExcavateShape.posToMask(x, y);
					
					if((mask & flag) != flag)
					{
						mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
						
						// Draw stone scaled x2
						GlStateManager.pushMatrix();
						GlStateManager.translate(guiLeft + 176 - (x * 32), guiTop + 176 - (y * 32), 0F);
						GlStateManager.scale(2F, 2F, 1F);
						this.drawTexturedModalRect(0, 0,  mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/stone"), 16, 16);
						GlStateManager.popMatrix();
					}
					
					if(off == (y * 5) + x)
					{
						mc.renderEngine.bindTexture(GUI_ICO);
						
						// Draw reticle scaled x2
						GlStateManager.pushMatrix();
						GlStateManager.translate(guiLeft + 176 - (x * 32), guiTop + 176 - (y * 32), 0F);
						GlStateManager.scale(2F, 2F, 1F);
						this.drawTexturedModalRect(0, 0, 0, 0, 16, 16);
						GlStateManager.popMatrix();
					}
				}
			}
		}
	}
	
	@Override
	public void onGuiClosed()
	{
		ShapeRegistry.INSTANCE.saveShapes(new File("config/oreexcavation_shapes.json"));
		
		Keyboard.enableRepeatEvents(false);
	}
	
	public void refreshShape()
	{
		if(ShapeRegistry.INSTANCE.getShapeList().size() <= 0)
		{
			curShape = null;
			idx = 0;
		} else
		{
			idx = MathHelper.clamp(idx, 0, ShapeRegistry.INSTANCE.getShapeList().size() - 1);
			curShape = ShapeRegistry.INSTANCE.getShapeAt(idx + 1);
		}
		
		if(curShape != null)
		{
			txtField.setText(curShape.getName());
			nmbField.setText("" + curShape.getMaxDepth());
			btnRemove.enabled = true;
		} else
		{
			txtField.setText("");
			nmbField.setText("-1");
			btnRemove.enabled = false;
		}
		
		btnLeft.enabled = idx > 0;
		btnRight.enabled = idx + 1 < ShapeRegistry.INSTANCE.getShapeList().size();
	}
	
	@Override
	public void actionPerformed(GuiButton button)
	{
		if(button.id == 0 && idx > 0)
		{
			idx--;
			refreshShape();
		} else if(button.id == 1 && idx + 1 < ShapeRegistry.INSTANCE.getShapeList().size())
		{
			idx++;
			refreshShape();
		} else if(button.id == 2)
		{
			idx = ShapeRegistry.INSTANCE.getShapeList().size();
			ExcavateShape nes = new ExcavateShape();
			
			for(int i = 1; i < 4; i++)
			{
				for(int j = 1; j < 4; j++)
				{
					nes.setMask(i, j, true);
				}
			}
			
			ShapeRegistry.INSTANCE.getShapeList().add(nes);
			refreshShape();
		} else if(button.id == 3 && curShape != null)
		{
			ShapeRegistry.INSTANCE.getShapeList().remove(curShape);
			refreshShape();
		}
	}
	
	@Override
	public void keyTyped(char c, int keycode) throws IOException
	{
		super.keyTyped(c, keycode);
		
		txtField.textboxKeyTyped(c, keycode);
		nmbField.textboxKeyTyped(c, keycode);
		
		if(curShape != null)
		{
			curShape.setName(txtField.getText());
			curShape.setMaxDepth(nmbField.getNumber().intValue());
		}
	}
	
	@Override
	public void mouseClicked(int mx, int my, int click) throws IOException
	{
		super.mouseClicked(mx, my, click);
		
		txtField.mouseClicked(mx, my, click);
		nmbField.mouseClicked(mx, my, click);
		
		int x = (mx - guiLeft - 48)/32;
		int y = (my - guiTop - 48)/32;
		
		if(curShape != null && mx - guiLeft >= 48 && x < 5 && my - guiTop >= 48 && y < 5)
		{
			if(click == 0)
			{
				int flag = ExcavateShape.posToMask(4 - x, 4 - y);
				int mask = curShape.getShapeMask();
				curShape.setMask(4- x, 4 - y, (mask & flag) != flag);
			} else if(click == 1)
			{
				curShape.setReticle(4 - x, 4 - y);
			}
		}
	}
}
