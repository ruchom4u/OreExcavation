package oreexcavation.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import oreexcavation.core.OreExcavation;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;

import java.io.File;

public class GuiEditShapes extends Screen
{
	private static final ResourceLocation GUI_TEX = new ResourceLocation(OreExcavation.MODID, "textures/gui/edit_shapes.png");
	private static final ResourceLocation GUI_ICO = new ResourceLocation("minecraft:textures/gui/icons.png");
	
	private int guiLeft = 0;
	private int guiTop = 0;
	
	private int idx = 0;
	private ExcavateShape curShape = null;
	
	private TextFieldWidget txtField = null;
	private GuiNumberField nmbField = null;
	
	private Button btnLeft = null;
	private Button btnRight = null;
	private Button btnAdd = null;
	private Button btnRemove = null;
	
	public GuiEditShapes()
	{
	    super(new StringTextComponent("Edit Shapes"));
	}
	
	@Override
    public void tick()
    {
        txtField.tick();
        nmbField.tick();
    }
	
	@Override
	public void init()
	{
		this.guiLeft = this.width/2 - 128;
		this.guiTop = this.height/2 - 128;
		
		this.minecraft.keyboardListener.enableRepeatEvents(true);
		
		btnLeft = new Button(guiLeft + 14, guiTop + 118, 20, 20, "<", (btn) -> this.actionPerformed(0));
		btnLeft.active = idx > 0;
		
		btnRight = new Button(guiLeft + 222, guiTop + 118, 20, 20, ">", (btn) -> this.actionPerformed(1));
		btnRight.active = idx + 1 < ShapeRegistry.INSTANCE.getShapeList().size();
		
		btnAdd = new Button(guiLeft + 150, guiTop + 222, 20, 20, "+", (btn) -> this.actionPerformed(2));
		//btnAdd.packedFGColor = Color.GREEN.getRGB();
		
		btnRemove = new Button(guiLeft + 86, guiTop + 222, 20, 20, "-", (btn) -> this.actionPerformed(3));
		//btnRemove.packedFGColor = Color.RED.getRGB();
		
		txtField = new TextFieldWidget(minecraft.fontRenderer, guiLeft + 48, guiTop + 16, 112, 16, "");
		nmbField = new GuiNumberField(minecraft.fontRenderer, guiLeft + 176, guiTop + 16, 32, 16);
		
		this.children.add(txtField);
		this.children.add(nmbField);
		
		this.addButton(btnLeft);
		this.addButton(btnRight);
		this.addButton(btnAdd);
		this.addButton(btnRemove);
		
		refreshShape();
	}
	
	@Override
	public void render(int mx, int my, float partialTick)
	{
		this.renderBackground();
		
		minecraft.getTextureManager().bindTexture(GUI_TEX);
		blit(guiLeft, guiTop, 0, 0, 256, 256);
		
		txtField.render(mx, my, partialTick);
		nmbField.render(mx, my, partialTick);
		
		super.render(mx, my, partialTick);
		
		GlStateManager.color4f(1F, 1F, 1F, 1F);
		
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
						minecraft.getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
						
						// Draw stone scaled x2
						GlStateManager.pushMatrix();
						GlStateManager.translatef(guiLeft + 176 - (x * 32), guiTop + 176 - (y * 32), 0F);
						GlStateManager.scalef(2F, 2F, 1F);
                        TextureAtlasSprite spr = minecraft.getTextureMap().getAtlasSprite("minecraft:block/stone");
						blit(0, 0, 0, 16, 16, spr);
						GlStateManager.popMatrix();
					}
					
					if(off == (y * 5) + x)
					{
						minecraft.getTextureManager().bindTexture(GUI_ICO);
						
						// Draw reticle scaled x2
						GlStateManager.pushMatrix();
						GlStateManager.translatef(guiLeft + 176 - (x * 32), guiTop + 176 - (y * 32), 0F);
						GlStateManager.scalef(2F, 2F, 1F);
						blit(0, 0, 0, 0, 16, 16);
						GlStateManager.popMatrix();
					}
				}
			}
		}
	}
	
	@Override
	public void onClose()
	{
		ShapeRegistry.INSTANCE.saveShapes(new File("config/oreexcavation_shapes.json"));
		
		this.minecraft.keyboardListener.enableRepeatEvents(false);
		super.onClose();
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
			btnRemove.active = true;
		} else
		{
			txtField.setText("");
			nmbField.setText("-1");
			btnRemove.active = false;
		}
		
		btnLeft.active = idx > 0;
		btnRight.active = idx + 1 < ShapeRegistry.INSTANCE.getShapeList().size();
	}
	
	private void actionPerformed(int id)
	{
		if(id == 0 && idx > 0)
		{
			idx--;
			refreshShape();
		} else if(id == 1 && idx + 1 < ShapeRegistry.INSTANCE.getShapeList().size())
		{
			idx++;
			refreshShape();
		} else if(id == 2)
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
		} else if(id == 3 && curShape != null)
		{
			ShapeRegistry.INSTANCE.getShapeList().remove(curShape);
			refreshShape();
		}
	}
	
	@Override
	public boolean keyPressed(int k1, int k2, int k3)
    {
		boolean flag = super.keyPressed(k1, k2, k3);
		
		//txtField.keyPressed(k1, k2, k3);
		//nmbField.keyPressed(k1, k2, k3);
		
		if(curShape != null)
		{
			curShape.setName(txtField.getText());
			curShape.setMaxDepth(nmbField.getNumber().intValue());
		}
		
		return flag;
	}
	
	@Override
	public boolean mouseClicked(double mx, double my, int click)
	{
		boolean used = super.mouseClicked(mx, my, click);
		
		//txtField.mouseClicked(mx, my, click);
		//nmbField.mouseClicked(mx, my, click);
		
		int x = (int)Math.floor((mx - guiLeft - 48)/32D);
		int y = (int)Math.floor((my - guiTop - 48)/32D);
		
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
		
		return used;
	}
	
	@Override
    public boolean isPauseScreen()
    {
      return false;
    }
}
