package autosaveworld.threads.purge;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import autosaveworld.core.AutoSaveWorld;

public class Datfilepurge {

	private AutoSaveWorld plugin;

	public Datfilepurge(AutoSaveWorld plugin, long awaytime)
	{
		this.plugin = plugin;
		DelPlayerDatFileTask(awaytime);
	}
	
	private void DelPlayerDatFileTask(long awaytime) {
		int deleted = 0;
		OfflinePlayer[] checkPlayers = Bukkit.getServer().getOfflinePlayers();
		for (OfflinePlayer pl : checkPlayers) {
			
			boolean remove = false;
			//check is the player is inactive
			if (!pl.hasPlayedBefore()) {remove = true;}
			else if (System.currentTimeMillis() - pl.getLastPlayed() >= awaytime) {remove = true;}
			//rare occasion when player just joined server, then hasPlayedBefore will return false for this player
			if (pl.isOnline()) {remove = false;}
			
				if (remove) {

					try {
					String worldfoldername = Bukkit.getWorlds().get(0).getWorldFolder().getCanonicalPath();
							File pldatFile = new File(
											worldfoldername
											+ File.separator + "players"
											+ File.separator + pl.getName()
											+ ".dat");
							pldatFile.delete();
							plugin.debug(pl.getName()
									+ " is inactive. Removing dat file");
							deleted += 1;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
		}
		
		plugin.debug("Player .dat purge finished, deleted "+deleted+" player .dat files");
		
	}
	
}