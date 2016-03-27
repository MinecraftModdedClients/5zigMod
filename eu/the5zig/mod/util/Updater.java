package eu.the5zig.mod.util;

import com.google.gson.Gson;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.asm.Transformer;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ConfigNew.UpdateType;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.installer.InstallerUtils;
import eu.the5zig.mod.installer.ProcessCallback;
import eu.the5zig.mod.installer.UpdateInstaller;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

public class Updater
{
  public static void check()
  {
    final ConfigNew.UpdateType updateType = (ConfigNew.UpdateType)The5zigMod.getConfig().getEnum("autoUpdate", ConfigNew.UpdateType.class);
    if (updateType == ConfigNew.UpdateType.NEVER) {
      return;
    }
    new Thread("Update Thread")
    {
      public void run()
      {
        try
        {
          String latestVersion = IOUtils.toString(new URL("http://5zig.eu/api/update?mc=1.9&same-version=" + (updateType == ConfigNew.UpdateType.SAME_VERSION ? 1 : 0))
            .toURI());
          Updater.Download download = (Updater.Download)The5zigMod.gson.fromJson(latestVersion, Updater.Download.class);
          if ((Utils.versionCompare("3.5.3", download.name).intValue() < 0) && (Utils.versionCompare("1.9", download.mc).intValue() <= 0))
          {
            The5zigMod.logger.info("Found new update of The 5zig Mod (v" + download.name + ")!");
            boolean sameMinecraftVersion = "1.9".equals(download.mc);
            if ((updateType == ConfigNew.UpdateType.SAME_VERSION) && (!sameMinecraftVersion)) {
              return;
            }
            if (!sameMinecraftVersion)
            {
              File minecraftDir = new File("").getAbsoluteFile();
              File versionsDir = new File(minecraftDir, "versions");
              File minecraftJarDirectory = new File(versionsDir, download.mc);
              File minecraftJarFile = new File(minecraftJarDirectory, download.mc + ".jar");
              if (!minecraftJarFile.exists())
              {
                sameMinecraftVersion = true;
                The5zigMod.logger.info("New Minecraft version doesn't exist yet! Trying to download most recent mod file for Minecraft 1.9");
                latestVersion = IOUtils.toString(new URL("http://5zig.eu/api/update?mc=1.9&same-version=1").toURI());
                download = (Updater.Download)The5zigMod.gson.fromJson(latestVersion, Updater.Download.class);
                if ((download.name == null) || (Utils.versionCompare("3.5.3", download.name).intValue() >= 0))
                {
                  The5zigMod.logger.info("No new download available for this Minecraft version!");
                  return;
                }
              }
            }
            File modLibraryDirectory = new File(InstallerUtils.getMinecraftDirectory(), "libraries" + File.separator + "eu" + File.separator + "the5zig" + File.separator + "The5zigMod" + File.separator + download.mc + "_" + download.name);
            if (modLibraryDirectory.exists())
            {
              The5zigMod.logger.info("Update already has been downloaded! Aborting...");
              return;
            }
            Updater.downloadLatest(download.url, download.md5, download.name, download.mc, sameMinecraftVersion);
          }
          else
          {
            The5zigMod.logger.info("The 5zig Mod is up to date!");
          }
        }
        catch (Exception e)
        {
          The5zigMod.logger.error("Could not check for latest 5zig Mod Version!", e);
        }
      }
    }.start();
  }
  
  private static void downloadLatest(String url, String md5, String version, String mcVersion, boolean sameMinecraftVersion)
    throws Exception
  {
    File minecraftDir = InstallerUtils.getMinecraftDirectory();
    if ((Transformer.FORGE) && (sameMinecraftVersion))
    {
      File modsDir = new File(minecraftDir, "mods");
      File modsFile = new File(modsDir, "The5zigMod-" + mcVersion + "_" + version + ".jar");
      URL website = new URL(url);
      ReadableByteChannel rbc = Channels.newChannel(website.openStream());
      FileOutputStream fos = new FileOutputStream(modsFile);
      fos.getChannel().transferFrom(rbc, 0L, Long.MAX_VALUE);
      
      String downloadedMD5 = eu.the5zig.util.io.FileUtils.md5(modsFile);
      if (!downloadedMD5.equals(md5))
      {
        org.apache.commons.io.FileUtils.deleteQuietly(modsFile);
        throw new RuntimeException("Invalid Downloaded File MD5!");
      }
    }
    else
    {
      File libraryDir = new File(minecraftDir, "libraries" + File.separator + "eu" + File.separator + "the5zig" + File.separator + "The5zigMod" + File.separator + mcVersion + "_" + version);
      if ((!libraryDir.exists()) && (!libraryDir.mkdirs())) {
        throw new IOException("Could not create directory at " + libraryDir);
      }
      File libraryFile = new File(libraryDir, "The5zigMod-" + mcVersion + "_" + version + ".jar");
      URL website = new URL(url);
      ReadableByteChannel rbc = Channels.newChannel(website.openStream());
      FileOutputStream fos = new FileOutputStream(libraryFile);
      fos.getChannel().transferFrom(rbc, 0L, Long.MAX_VALUE);
      String downloadedMD5 = eu.the5zig.util.io.FileUtils.md5(libraryFile);
      if (!downloadedMD5.equals(md5))
      {
        org.apache.commons.io.FileUtils.deleteQuietly(libraryDir);
        throw new RuntimeException("Invalid Downloaded File MD5!");
      }
      UpdateInstaller installer = new UpdateInstaller(version, mcVersion, sameMinecraftVersion ? "3.5.3" : null, libraryFile);
      installer.install(new ProcessCallback()
      {
        public void progress(float percentage) {}
        
        public void message(String message)
        {
          The5zigMod.logger.debug(message);
        }
      });
    }
    The5zigMod.logger.info("Downloaded and installed the latest version of The 5zig Mod!");
    The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.GREEN + I18n.translate("new_update.1"));
    The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.GREEN + I18n.translate("new_update.2"));
  }
  
  private static class Download
  {
    public String name;
    public String mc;
    public String url;
    public String md5;
  }
}
