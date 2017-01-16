package oreexcavation.client;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class UpdateNotification
{
	public static Future<String> checkThread;
	
	private static final String MOD_NAME = OreExcavation.NAME;
	private static final String GIT_BRANCH = OreExcavation.BRANCH;
	private static final String CUR_HASH = OreExcavation.HASH;
	private static final String DEB_HASH = "CI_MOD_" + "HASH";
	private static final String URL_UPDATE = "https://goo.gl/q9VC9j";
	private static final String URL_DOWNLOAD = "http://minecraft.curseforge.com/projects/ore-excavation";
	
	private boolean hasChecked = false;
	private final Logger logger;
	private final boolean hidden;
	
	public static void startUpdateCheck()
	{
		if(CUR_HASH.equalsIgnoreCase(DEB_HASH))
		{
			return;
		}
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		
		checkThread = executor.submit(new Callable<String>()
		{
			@Override
			public String call() throws Exception
			{
				return getNotification(URL_UPDATE, true);
			}
		});
	}
	
	public UpdateNotification()
	{
		this.logger = OreExcavation.logger;
		this.hidden = ExcavationSettings.hideUpdates;
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(CUR_HASH.equalsIgnoreCase(DEB_HASH))
		{
			event.player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "THIS COPY OF " + MOD_NAME.toUpperCase() + " IS NOT FOR PUBLIC USE!"));
			return;
		}
		
		if(hasChecked || checkThread == null || !checkThread.isDone())
		{
			return;
		}
		
		hasChecked = true;
		
		if(hidden)
		{
			return;
		}
		
		try
		{
			String[] data = checkThread.get().split("\\n");
			
			ArrayList<String> changelog = new ArrayList<String>();
			boolean hasLog = false;
			
			for(String s : data)
			{
				if(s.equalsIgnoreCase("git_branch:" + GIT_BRANCH))
				{
					if(!hasLog)
					{
						hasLog = true;
						changelog.add(s);
						continue;
					} else
					{
						break;
					}
				} else if(s.toLowerCase().startsWith("git_branch:"))
				{
					if(hasLog)
					{
						break;
					} else
					{
						continue;
					}
				} else if(hasLog)
				{
					changelog.add(s);
				}
			}
			
			if(!hasLog || data.length < 2)
			{
				event.player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "An error has occured while checking " + MOD_NAME + " version!"));
				logger.log(Level.ERROR, "An error has occured while checking " + MOD_NAME + " version! (hasLog: " + hasLog + ", data: " + data.length + ")");
				return;
			} else
			{
				// Only the relevant portion of the changelog is preserved
				data = changelog.toArray(new String[0]);
			}
			
			String hash = data[1].trim();
			
			boolean hasUpdate = !CUR_HASH.equalsIgnoreCase(hash);
			
			if(hasUpdate)
			{
				event.player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Update for " + MOD_NAME + " available!"));
				ChatComponentText dlUrl = new ChatComponentText("Download: " + URL_DOWNLOAD);
				dlUrl.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, URL_DOWNLOAD));
				event.player.addChatMessage(dlUrl);
				
				for(int i = 2; i < data.length; i++)
				{
					if(i > 5)
					{
						event.player.addChatMessage(new ChatComponentText("and " + (data.length - 5) + " more..."));
						break;
					} else
					{
						event.player.addChatMessage(new ChatComponentText("- " + data[i].trim()));
					}
				}
			}
			
		} catch(Exception e)
		{
			event.player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "An error has occured while checking " + MOD_NAME + " version!"));
			logger.log(Level.ERROR, "An error has occured while checking " + MOD_NAME + " version!", e);
			return;
		}
	}
	
	public static String getNotification(String link, boolean doRedirect) throws Exception
	{
		URL url = new URL(link);
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(false);
		con.setReadTimeout(20000);
		con.setRequestProperty("Connection", "keep-alive");
		
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
		((HttpURLConnection)con).setRequestMethod("GET");
		con.setConnectTimeout(5000);
		BufferedInputStream in = new BufferedInputStream(con.getInputStream());
		int responseCode = con.getResponseCode();
		HttpURLConnection.setFollowRedirects(true);
		if(responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_MOVED_PERM)
		{
			System.out.println("Update request returned response code: " + responseCode + " " + con.getResponseMessage());
		} else if(responseCode == HttpURLConnection.HTTP_MOVED_PERM)
		{
			if(doRedirect)
			{
				try
				{
					return getNotification(con.getHeaderField("location"), false);
				} catch(Exception e)
				{
					throw e;
				}
			} else
			{
				throw new Exception();
			}
		}
		StringBuffer buffer = new StringBuffer();
		int chars_read;
		while((chars_read = in.read()) != -1)
		{
			char g = (char)chars_read;
			buffer.append(g);
		}
		return buffer.toString();
	}
}
