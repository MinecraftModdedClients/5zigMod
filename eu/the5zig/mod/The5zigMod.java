package eu.the5zig.mod;

import com.google.gson.Gson;
import eu.the5zig.mod.api.ServerAPIBackend;
import eu.the5zig.mod.asm.Transformer;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.filetransfer.FileTransferManager;
import eu.the5zig.mod.config.ChatFilterConfiguration;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.LastServerConfiguration;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.config.items.IntItem;
import eu.the5zig.mod.crashreport.CrashHopper;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.listener.EventListener;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.KeybindingManager;
import eu.the5zig.mod.manager.SkinManager;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.render.GuiIngame;
import eu.the5zig.mod.server.bergwerk.ServerInstanceBergwerk;
import eu.the5zig.mod.server.gomme.ServerInstanceGommeHD;
import eu.the5zig.mod.server.hypixel.ServerInstanceHypixel;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIManager;
import eu.the5zig.mod.server.mineplex.ServerInstanceMineplex;
import eu.the5zig.mod.server.playminity.ServerInstancePlayMinity;
import eu.the5zig.mod.server.timolia.ServerInstanceTimolia;
import eu.the5zig.mod.server.venicraft.ServerInstanceVenicraft;
import eu.the5zig.mod.util.ClassProxyCallbackImpl;
import eu.the5zig.mod.util.CrashReportUtil;
import eu.the5zig.mod.util.IResourceLocation;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.MojangAPIManager;
import eu.the5zig.mod.util.TrayManager;
import eu.the5zig.mod.util.Updater;
import eu.the5zig.util.AsyncExecutor;
import eu.the5zig.util.db.Database;
import eu.the5zig.util.db.DummyDatabase;
import eu.the5zig.util.db.FileDatabaseConfiguration;
import eu.the5zig.util.db.exceptions.NoConnectionException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Random;
import java.util.UUID;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.lwjgl.input.Keyboard;

public class The5zigMod
{
  public static final Logger logger = LogManager.getLogger("5zig");
  public static final Marker networkMarker = MarkerManager.getMarker("Net");
  public static final Random random = new Random();
  public static final Gson gson = new Gson();
  public static final IResourceLocation ITEMS = MinecraftFactory.getVars().createResourceLocation("the5zigmod", "textures/items.png");
  public static final IResourceLocation INVENTORY_BACKGROUND = MinecraftFactory.getVars().createResourceLocation("textures/gui/container/inventory.png");
  public static final IResourceLocation STEVE = MinecraftFactory.getVars().createResourceLocation("the5zigmod", "textures/skin.png");
  public static final IResourceLocation MINECRAFT_ICONS = MinecraftFactory.getVars().createResourceLocation("textures/gui/icons.png");
  public static final IResourceLocation DEMO_BACKGROUND = MinecraftFactory.getVars().createResourceLocation("textures/gui/demo_background.png");
  private static final AsyncExecutor asyncExecutor = new AsyncExecutor();
  public static boolean DEBUG = false;
  private static ConfigNew config;
  private static GuiIngame guiIngame;
  private static DataManager datamanager;
  private static EventListener listener;
  private static ChatFilterConfiguration chatFilterConfig;
  private static LastServerConfiguration lastServerConfig;
  private static IVariables variables;
  private static DisplayRenderer renderer;
  private static ModuleMaster moduleMaster;
  private static ServerAPIBackend serverAPIBackend;
  private static TrayManager trayManager;
  private static NetworkManager networkManager;
  private static Database conversationDatabase;
  private static ConversationManager conversationManager;
  private static GroupChatManager groupChatManager;
  private static FriendManager friendManager;
  private static SkinManager skinManager;
  private static KeybindingManager keybindingManager;
  private static HypixelAPIManager hypixelAPIManager;
  private static MojangAPIManager mojangAPIManager;
  private static boolean init = false;
  
  public static void init()
  {
    if (init) {
      throw new IllegalStateException("The 5zig Mod has been already initialized!");
    }
    init = true;
    
    long start = System.currentTimeMillis();
    MinecraftFactory.setClassProxyCallback(new ClassProxyCallbackImpl());
    CrashHopper.init();
    logger.info("Initializing the 5zig Mod!");
    if (Transformer.FORGE) {
      logger.info("Forge detected!");
    }
    try
    {
      setupDirs();
    }
    catch (IOException e)
    {
      logger.fatal("Could not create Mod directories! Exiting!", e);
      CrashReportUtil.makeCrashReport(e, "Creating Directory.");
    }
    try
    {
      loadConfig();
    }
    catch (IOException e)
    {
      logger.fatal("Could not load Main Configuration!");
      CrashReportUtil.makeCrashReport(e, "Loading Main Configuration.");
    }
    DEBUG = config.getBool("debug");
    File dir = new File("the5zigmod");
    chatFilterConfig = new ChatFilterConfiguration(dir);
    lastServerConfig = new LastServerConfiguration(dir);
    setupLogger();
    
    variables = MinecraftFactory.getVars();
    datamanager = new DataManager();
    keybindingManager = new KeybindingManager();
    listener = new EventListener();
    renderer = new DisplayRenderer();
    serverAPIBackend = new ServerAPIBackend();
    new ServerInstanceTimolia();
    new ServerInstanceGommeHD();
    new ServerInstancePlayMinity();
    new ServerInstanceBergwerk();
    new ServerInstanceMineplex();
    new ServerInstanceHypixel();
    new ServerInstanceVenicraft();
    moduleMaster = new ModuleMaster(dir);
    
    guiIngame = new GuiIngame();
    variables.updateOverlayCount(getConfig().getInt("maxOverlays"));
    
    variables.patchGamma();
    
    newConversationDatabase();
    conversationManager = new ConversationManager();
    groupChatManager = new GroupChatManager();
    friendManager = new FriendManager();
    trayManager = new TrayManager();
    newNetworkManager();
    
    skinManager = new SkinManager();
    
    hypixelAPIManager = new HypixelAPIManager();
    mojangAPIManager = new MojangAPIManager();
    
    Updater.check();
    
    logger.info("Loaded The 5zig Mod! (took {} ms)", new Object[] { Long.valueOf(System.currentTimeMillis() - start) });
  }
  
  public static void shutdown()
  {
    logger.info("Stopping The 5zig Mod!");
    try
    {
      if (networkManager != null) {
        networkManager.disconnect();
      }
      if (trayManager != null) {
        trayManager.destroy();
      }
      asyncExecutor.finish();
      if (conversationDatabase != null) {
        conversationDatabase.closeConnection();
      }
      if ((datamanager != null) && (datamanager.getFileTransferManager() != null)) {
        datamanager.getFileTransferManager().cleanUp(new File("the5zigmod/media", datamanager.getUniqueId().toString()));
      }
    }
    catch (Throwable throwable)
    {
      throwable.printStackTrace();
    }
  }
  
  private static void setupDirs()
    throws IOException
  {
    eu.the5zig.util.io.FileUtils.createDir(new File("the5zigmod/sql/chatlogs"));
    eu.the5zig.util.io.FileUtils.createDir(new File("the5zigmod/lang"));
    eu.the5zig.util.io.FileUtils.createDir(new File("the5zigmod/skins"));
    eu.the5zig.util.io.FileUtils.createDir(new File("the5zigmod/servers/hypixel"));
  }
  
  private static void loadConfig()
    throws IOException
  {
    File configFile = new File("the5zigmod", "config.json");
    if ((!configFile.exists()) && (!configFile.createNewFile())) {
      throw new IOException("Could not create Configuration!");
    }
    config = new ConfigNew(configFile);
    IntItem version = (IntItem)config.get("version", IntItem.class);
    if (!version.isDefault()) {
      config.reset();
    }
    logger.info("Loaded Configurations!");
  }
  
  public static ConfigNew getConfig()
  {
    return config;
  }
  
  private static void setupLogger()
  {
    LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig("");
    if (DEBUG) {
      loggerConfig.setLevel(Level.DEBUG);
    }
    ctx.updateLoggers();
    logger.debug("Debug Mode ENABLED!");
  }
  
  public static DataManager getDataManager()
  {
    return datamanager;
  }
  
  public static KeybindingManager getKeybindingManager()
  {
    return keybindingManager;
  }
  
  public static EventListener getListener()
  {
    return listener;
  }
  
  public static ChatFilterConfiguration getChatFilterConfig()
  {
    return chatFilterConfig;
  }
  
  public static LastServerConfiguration getLastServerConfig()
  {
    return lastServerConfig;
  }
  
  public static GuiIngame getGuiIngame()
  {
    return guiIngame;
  }
  
  public static IVariables getVars()
  {
    return variables;
  }
  
  public static DisplayRenderer getRenderer()
  {
    return renderer;
  }
  
  public static ModuleMaster getModuleMaster()
  {
    return moduleMaster;
  }
  
  public static IOverlay getOverlayMessage()
  {
    return getVars().newOverlay();
  }
  
  public static void newNetworkManager()
  {
    networkManager = NetworkManager.connect();
  }
  
  public static NetworkManager getNetworkManager()
  {
    return networkManager;
  }
  
  private static void newConversationDatabase()
  {
    File file = new File("the5zigmod/sql/chatlogs/" + getDataManager().getUniqueId().toString());
    File dbFile = new File(file.getAbsolutePath() + ".mv.db");
    File backupFile = new File(file.getAbsolutePath() + "_backup.mv.db");
    FileDatabaseConfiguration configuration = new FileDatabaseConfiguration(file, new String[] { "DATABASE_TO_UPPER=FALSE" });
    try
    {
      if ((dbFile.exists()) && (isDBLocked(configuration.getFile().getAbsolutePath() + ".mv.db")))
      {
        logger.info("Found locked database! Using dummy database!");
        conversationDatabase = new DummyDatabase();
      }
      else
      {
        conversationDatabase = new Database(configuration);
        try
        {
          conversationDatabase.closeConnection();
          org.apache.commons.io.FileUtils.copyFile(dbFile, backupFile);
          logger.debug("Created db backup!");
        }
        catch (Exception e)
        {
          logger.warn("Could not create backup of conversations!", e);
        }
      }
    }
    catch (Throwable throwable)
    {
      logger.info("Could not load Conversations!", throwable);
      try
      {
        logger.info("Trying to load backup!");
        org.apache.commons.io.FileUtils.copyFile(backupFile, dbFile);
        conversationDatabase = new Database(configuration);
      }
      catch (Throwable e2)
      {
        logger.info("Could not load backup! Deleting...");
        try
        {
          if (((backupFile.exists()) && (!backupFile.delete())) || ((dbFile.exists()) && (!dbFile.delete()))) {
            throw new IOException("Could not delete db file...");
          }
          try
          {
            conversationDatabase = new DummyDatabase();
          }
          catch (NoConnectionException localNoConnectionException) {}
        }
        catch (Exception e1)
        {
          try
          {
            conversationDatabase = new DummyDatabase();
          }
          catch (NoConnectionException localNoConnectionException1) {}
        }
      }
    }
  }
  
  private static boolean isDBLocked(String fileName)
  {
    try
    {
      RandomAccessFile f = new RandomAccessFile(fileName, "r");
      try
      {
        FileLock lock = f.getChannel().tryLock(0L, Long.MAX_VALUE, true);
        if (lock != null)
        {
          lock.release();
          return false;
        }
      }
      finally
      {
        f.close();
      }
    }
    catch (IOException localIOException) {}
    return true;
  }
  
  public static Database getConversationDatabase()
  {
    return conversationDatabase;
  }
  
  public static ConversationManager getConversationManager()
  {
    return conversationManager;
  }
  
  public static GroupChatManager getGroupChatManager()
  {
    return groupChatManager;
  }
  
  public static FriendManager getFriendManager()
  {
    return friendManager;
  }
  
  public static SkinManager getSkinManager()
  {
    return skinManager;
  }
  
  public static ServerAPIBackend getServerAPIBackend()
  {
    return serverAPIBackend;
  }
  
  public static AsyncExecutor getAsyncExecutor()
  {
    return asyncExecutor;
  }
  
  public static TrayManager getTrayManager()
  {
    return trayManager;
  }
  
  public static HypixelAPIManager getHypixelAPIManager()
  {
    return hypixelAPIManager;
  }
  
  public static MojangAPIManager getMojangAPIManager()
  {
    return mojangAPIManager;
  }
  
  public static String toBoolean(boolean b)
  {
    return b ? I18n.translate("gui.on") : I18n.translate("gui.off");
  }
  
  public static String getKeyDisplayString(int key)
  {
    return key < 256 ? Keyboard.getKeyName(key) : key < 0 ? getVars().translate("key.mouseButton", new Object[] { Integer.valueOf(key + 101) }) : String.format("%c", new Object[] { Character.valueOf((char)(key - 256)) }).toUpperCase();
  }
  
  public static String getKeyDisplayStringShort(int key)
  {
    return key < 256 ? Keyboard.getKeyName(key) : key < 0 ? "M" + (key + 101) : String.format("%c", new Object[] { Character.valueOf((char)(key - 256)) }).toUpperCase();
  }
}
