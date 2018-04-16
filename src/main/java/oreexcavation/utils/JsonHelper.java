package oreexcavation.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import oreexcavation.core.OreExcavation;
import org.apache.logging.log4j.Level;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonHelper
{
	private static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	public static JsonArray GetArray(JsonObject json, String id)
	{
		if(json == null)
		{
			return new JsonArray();
		}
		
		if(json.has(id) && json.get(id).isJsonArray())
		{
			return json.get(id).getAsJsonArray();
		} else
		{
			return new JsonArray();
		}
	}
	
	public static JsonObject GetObject(JsonObject json, String id)
	{
		if(json == null)
		{
			return new JsonObject();
		}
		
		if(json.has(id) && json.get(id).isJsonObject())
		{
			return json.get(id).getAsJsonObject();
		} else
		{
			return new JsonObject();
		}
	}
	
	public static String GetString(JsonObject json, String id, String def)
	{
		if(json == null)
		{
			return def;
		}
		
		if(json.has(id) && json.get(id).isJsonPrimitive() && json.get(id).getAsJsonPrimitive().isString())
		{
			return json.get(id).getAsString();
		} else
		{
			return def;
		}
	}
	
	public static Number GetNumber(JsonObject json, String id, Number def)
	{
		if(json == null)
		{
			return def;
		}
		
		if(json.has(id) && json.get(id).isJsonPrimitive())
		{
			try
			{
				return json.get(id).getAsNumber();
			} catch(Exception e)
			{
				return def;
			}
		} else
		{
			return def;
		}
	}
	
	public static boolean GetBoolean(JsonObject json, String id, boolean def)
	{
		if(json == null)
		{
			return def;
		}
		
		if(json.has(id) && json.get(id).isJsonPrimitive())
		{
			try
			{
				return json.get(id).getAsBoolean();
			} catch(Exception e)
			{
				return def;
			}
		} else
		{
			return def;
		}
	}
	
	public static JsonObject ReadFromFile(File file)
	{
		if(file == null || !file.exists())
		{
			return new JsonObject();
		}
		
		try
		{
			InputStreamReader fr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
			JsonObject json = GSON.fromJson(fr, JsonObject.class);
			fr.close();
			return json;
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.ERROR, "An error occured while loading JSON from file:", e);
			
			int i = 0;
			File bkup = new File(file.getParent(), "malformed_" + file.getName() + i + ".json");
			
			while(bkup.exists())
			{
				i++;
				bkup = new File(file.getParent(), "malformed_" + file.getName() + i + ".json");
			}
			
			OreExcavation.logger.log(Level.ERROR, "Creating backup at: " + bkup.getAbsolutePath());
			CopyPaste(file, bkup);
			
			return new JsonObject(); // Just a safety measure against NPEs
		}
	}
	
	public static void WriteToFile(File file, JsonObject jObj)
	{
		try
		{
			if(!file.exists())
			{
				if(file.getParentFile() != null)
				{
					file.getParentFile().mkdirs();
				}
				file.createNewFile();
			}
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.ERROR, "An error occured while saving JSON to file:", e);
			return;
		}
		
		try(OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
		{
			GSON.toJson(jObj, fw);
			fw.flush();
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.ERROR, "An error occured while saving JSON to file:", e);
		}
	}
	
	public static void CopyPaste(File fileIn, File fileOut)
	{
		if(!fileIn.exists())
		{
			return;
		}
		
		try
		{
			if(!fileOut.exists())
			{
				if(fileOut.getParentFile() != null)
				{
					fileOut.getParentFile().mkdirs();
				}
				
				fileOut.createNewFile();
			} else
			{
				throw new IOException("File already exists!");
			}
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.ERROR, "Failed copy paste", e);
			return;
		}
		
		try(BufferedReader fr = new BufferedReader(new InputStreamReader(new FileInputStream(fileIn), StandardCharsets.UTF_8));
			BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut), StandardCharsets.UTF_8)))
		{
			char[] buffer = new char[256];
			int read;
			while((read = fr.read(buffer)) != -1)
			{
				fw.write(buffer, 0, read);
			}
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.ERROR, "Failed copy paste", e);
		}
	}
}
