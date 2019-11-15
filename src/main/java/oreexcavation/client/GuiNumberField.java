package oreexcavation.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.text.NumberFormat;

@OnlyIn(Dist.CLIENT)
public class GuiNumberField extends TextFieldWidget
{
	public static final String REGEX_NUMBER = "[^.0123456789-]";
	
	public GuiNumberField(FontRenderer renderer, int posX, int posY, int sizeX, int sizeY)
	{
		super(renderer, posX, posY, sizeX, sizeY, "");
		this.setMaxStringLength(Integer.MAX_VALUE);
	}
	
	@Override
	public void writeText(@Nonnull String text)
	{
		super.writeText(text.replaceAll(REGEX_NUMBER, "")); // Type new text stripping out illegal characters
	}
	
	@Override
	public void setText(@Nonnull String text)
	{
		super.setText(text.replaceAll(REGEX_NUMBER, ""));
	}
	
	@Override
	public boolean mouseClicked(double mx, double my, int click)
	{
		boolean flag = super.mouseClicked(mx, my, click);
		
		if(!isFocused())
		{
			String txt = super.getText().replaceAll(REGEX_NUMBER, "");
			txt = txt.length() <= 0? "0" : txt;
			setText(txt);
		}
		
		return flag;
    }
	
	public Number getNumber()
	{
		try
		{
			return NumberFormat.getInstance().parse(super.getText());
		} catch(Exception e)
		{
			return 0;
		}
	}
	
	@Nonnull
	@Override
	public String getText()
	{
		return getNumber().toString();
	}
}
