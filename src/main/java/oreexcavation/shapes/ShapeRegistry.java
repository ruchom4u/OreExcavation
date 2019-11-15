package oreexcavation.shapes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import oreexcavation.utils.JsonHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShapeRegistry
{
	public static final ShapeRegistry INSTANCE = new ShapeRegistry();
	
	private List<ExcavateShape> shapeList = new ArrayList<>();
	private int curShape = 0;
	
	private ShapeRegistry()
	{
	}

	public void toggleShape()
	{
		this.curShape = (curShape + 1)%(shapeList.size() + 1);
	}
	
	public ExcavateShape getActiveShape()
	{
		return getShapeAt(curShape);
	}
	
	public List<ExcavateShape> getShapeList()
	{
		return shapeList;
	}
	
	public ExcavateShape getShapeAt(int index)
	{
		if(index <= 0 || index > shapeList.size())
		{
			return null;
		}
		
		int idx = index - 1;
		
		return shapeList.get(idx);
	}
	
	public void loadShapes(File file)
	{
		shapeList.clear();
		
		if(file.exists())
		{
			JsonObject json = JsonHelper.ReadFromFile(file);
			
			JsonArray jary = JsonHelper.GetArray(json, "shapes");
			
			for(JsonElement je : jary)
			{
				if(je == null || !je.isJsonObject())
				{
					continue;
				}
				
				ExcavateShape shape = new ExcavateShape();
				shape.readFromJson(je.getAsJsonObject());
				shapeList.add(shape);
			}
		} else
		{
			ExcavateShape shape = new ExcavateShape();
			shape.setName("1x2");
			
			shape.setMask(2, 2, true);
			shape.setMask(2, 1, true);
			
			shapeList.add(shape);
			
			shape = new ExcavateShape();
			shape.setName("3x3");
			
			for(int i = 1; i < 4; i++)
			{
				for(int j = 1; j < 4; j++)
				{
					shape.setMask(i, j, true);
				}
			}
			
			shapeList.add(shape);
			
			shape = new ExcavateShape();
			shape.setName("5x5");
			
			for(int i = 0; i < 5; i++)
			{
				for(int j = 0; j < 5; j++)
				{
					shape.setMask(i, j, true);
				}
			}
			
			shapeList.add(shape);
			
			saveShapes(file);
		}
	}
	
	public void saveShapes(File file)
	{
		JsonObject jsonBase = new JsonObject();
		JsonArray jsonList = new JsonArray();
		
		for(ExcavateShape shape : shapeList)
		{
			jsonList.add(shape.writeToJson(new JsonObject()));
		}
		
		jsonBase.add("shapes", jsonList);
		
		JsonHelper.WriteToFile(file, jsonBase);
	}
}
