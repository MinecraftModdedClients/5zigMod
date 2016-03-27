package eu.the5zig.mod.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ChatBackgroundManager;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.config.items.ActionItem;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.config.items.ColorFormattingItem;
import eu.the5zig.mod.config.items.DisplayCategoryItem;
import eu.the5zig.mod.config.items.DisplayScreenItem;
import eu.the5zig.mod.config.items.EnumItem;
import eu.the5zig.mod.config.items.FloatItem;
import eu.the5zig.mod.config.items.IntItem;
import eu.the5zig.mod.config.items.Item;
import eu.the5zig.mod.config.items.NonConfigItem;
import eu.the5zig.mod.config.items.OnlineStatusItem;
import eu.the5zig.mod.config.items.PercentSliderItem;
import eu.the5zig.mod.config.items.PlaceholderItem;
import eu.the5zig.mod.config.items.SelectColorItem;
import eu.the5zig.mod.config.items.SliderItem;
import eu.the5zig.mod.config.items.StringItem;
import eu.the5zig.mod.config.items.ToggleServerStatsItem;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.GuiCapeSettings;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.GuiChatFilter;
import eu.the5zig.mod.gui.GuiCoordinatesClipboard;
import eu.the5zig.mod.gui.GuiFileSelector;
import eu.the5zig.mod.gui.GuiHypixelFriends;
import eu.the5zig.mod.gui.GuiHypixelGuild;
import eu.the5zig.mod.gui.GuiHypixelStats;
import eu.the5zig.mod.gui.GuiLanguage;
import eu.the5zig.mod.gui.GuiModules;
import eu.the5zig.mod.gui.GuiSettings;
import eu.the5zig.mod.gui.GuiYesNo;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.gui.YesNoCallback;
import eu.the5zig.mod.gui.ingame.IGui2ndChat;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.FileSelectorCallback;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.AsyncExecutor;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

public class ConfigNew
{
  private final Gson builder = new GsonBuilder().setPrettyPrinting().create();
  private final File file;
  private JsonObject root;
  private LinkedHashMap<String, Item> items = Maps.newLinkedHashMap();
  
  public ConfigNew(File file)
    throws IOException
  {
    this.file = file;
    addDefaultItems();
    try
    {
      load(FileUtils.readFileToString(file));
    }
    catch (Exception e)
    {
      The5zigMod.logger.warn("Error loading config! Creating new one...", e);
      this.root = new JsonObject();
    }
    save(true);
  }
  
  protected void addDefaultItems()
  {
    add(new IntItem("version", null, Integer.valueOf(6)));
    add(new BoolItem("debug", null, Boolean.valueOf(false)));
    add(new StringItem("language", null, "en_US"));
    
    add(new BoolItem("showMod", "main", Boolean.valueOf(true)));
    add(new PercentSliderItem("scale", "main", Float.valueOf(1.0F), Float.valueOf(0.5F), Float.valueOf(1.5F), -1));
    add(new BoolItem("reportCrashes", "main", Boolean.valueOf(true)));
    add(new DisplayCategoryItem("display", "main", "display"));
    add(new DisplayScreenItem("modules", "main", GuiModules.class));
    add(new DisplayCategoryItem("server", "main", "server"));
    add(new DisplayScreenItem("coordinate_clipboard", "main", GuiCoordinatesClipboard.class));
    add(new DisplayScreenItem("chat", "main", GuiChat.class));
    add(new PlaceholderItem("main"));
    add(new PlaceholderItem("main"));
    add(new EnumItem("autoUpdate", "main", UpdateType.ALWAYS, UpdateType.class));
    add(new DisplayScreenItem("cape_settings", "main", GuiCapeSettings.class));
    add(new DisplayScreenItem("language_screen", "main", GuiLanguage.class));
    add(new ActionItem("reset_config", "main", new Runnable()
    {
      public void run()
      {
        The5zigMod.getVars().displayScreen(new GuiYesNo(The5zigMod.getVars().getCurrentScreen(), new YesNoCallback()
        {
          public void onDone(boolean yes)
          {
            if (!yes) {
              return;
            }
            ConfigNew.this.reset();
            The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.YELLOW + I18n.translate("config.reset"));
            The5zigMod.getVars().displayScreen(new GuiSettings(The5zigMod.getVars().getCurrentScreen().lastScreen, "main"));
          }
          
          public String title()
          {
            return I18n.translate("config.main.reset.title");
          }
        }));
      }
    }));
    add(new ColorFormattingItem("formattingPrefix", "display", ChatColor.RESET));
    add(new SelectColorItem("colorPrefix", "display", ChatColor.GOLD));
    add(new ColorFormattingItem("formattingMain", "display", ChatColor.RESET));
    add(new SelectColorItem("colorMain", "display", ChatColor.WHITE));
    add(new EnumItem("formattingBrackets", "display", BracketsFormatting.ARROW, BracketsFormatting.class)
    {
      public String translateValue()
      {
        return ((ConfigNew.BracketsFormatting)get()).getFirst() + ((ConfigNew.BracketsFormatting)get()).getLast();
      }
    });
    add(new SelectColorItem("colorBrackets", "display", ChatColor.GRAY));
    add(new SliderItem("numberPrecision", "", "display", 1.0F, 0.0F, 4.0F, 1)
    {
      public String getSuffix()
      {
        return " " + I18n.translate("config.display.digits");
      }
    });
    add(new SliderItem("maxOverlays", "", "display", 4.0F, 1.0F, 10.0F, 1)
    {
      public void action()
      {
        The5zigMod.getVars().updateOverlayCount(((Float)get()).intValue());
      }
    });
    add(new SliderItem("zoomFactor", "", "display", 4.0F, 2.0F, 12.0F, -1)
    {
      public String getCustomValue(float value)
      {
        return Utils.getShortenedFloat(((Float)get()).floatValue(), 1) + "x";
      }
    });
    add(new SliderItem("crosshairDistance", "m", "display", 0.0F, 0.0F, 150.0F, 5)
    {
      public String getCustomValue(float value)
      {
        if (value == 0.0F) {
          return The5zigMod.toBoolean(false);
        }
        return super.getCustomValue(value);
      }
    });
    add(new BoolItem("showPotionIndicator", "display", Boolean.valueOf(false))
    {
      public boolean isRestricted()
      {
        return !The5zigMod.getVars().isFancyGraphicsEnabled();
      }
    });
    add(new BoolItem("coloredEquipmentDurability", "display", Boolean.valueOf(false)));
    add(new BoolItem("showSaturation", "display", Boolean.valueOf(false)));
    add(new BoolItem("showHotbarNumbers", "display", Boolean.valueOf(false)));
    add(new BoolItem("showChatSymbols", "display", Boolean.valueOf(true)));
    add(new BoolItem("showLastServer", "display", Boolean.valueOf(true)));
    add(new BoolItem("showCustomModels", "display", Boolean.valueOf(true)));
    
    add(new DisplayCategoryItem("general", "server", "server_general"));
    add(new DisplayCategoryItem("timolia", "server", "server_timolia"));
    add(new DisplayCategoryItem("playminity", "server", "server_playminity"));
    add(new DisplayCategoryItem("gommehd", "server", "server_gommehd"));
    add(new DisplayCategoryItem("bergwerk", "server", "server_bergwerk"));
    add(new DisplayCategoryItem("mineplex", "server", "server_mineplex"));
    add(new DisplayCategoryItem("venicraft", "server", "server_venicraft"));
    add(new DisplayCategoryItem("hypixel", "server", "server_hypixel"));
    add(new DisplayCategoryItem("2nd_chat", "server", "2nd_chat"));
    add(new DisplayScreenItem("chatmessage_filter", "server", GuiChatFilter.class));
    
    add(new BoolItem("showServerStats", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("showIP", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("showPlayers", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("showLargeStartCountdown", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("showInvincibilityWearsOffTime", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("showLargeDeathmatchCountdown", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("showWinMessage", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("showCompassTarget", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("showLargeKillstreaks", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("notifyOnName", "server_general", Boolean.valueOf(true)));
    add(new BoolItem("autoReconnect", "server_general", Boolean.valueOf(true)));
    
    add(new ToggleServerStatsItem("showServerStats_timolia", "server_timolia", Boolean.valueOf(true)));
    add(new BoolItem("showTournaments", "server_timolia", Boolean.valueOf(true)));
    add(new BoolItem("showOpponentStats", "server_timolia", Boolean.valueOf(true)));
    
    add(new ToggleServerStatsItem("showServerStats_playminity", "server_playminity", Boolean.valueOf(true)));
    
    add(new ToggleServerStatsItem("showServerStats_gommehd", "server_gommehd", Boolean.valueOf(true)));
    
    add(new ToggleServerStatsItem("showServerStats_bergwerk", "server_bergwerk", Boolean.valueOf(true)));
    
    add(new ToggleServerStatsItem("showServerStats_mineplex", "server_mineplex", Boolean.valueOf(true)));
    
    add(new ToggleServerStatsItem("showServerStats_venicraft", "server_venicraft", Boolean.valueOf(true)));
    
    add(new ToggleServerStatsItem("showServerStats_hypixel", "server_hypixel", Boolean.valueOf(true)));
    add(new DisplayScreenItem("stats", "server_hypixel", GuiHypixelStats.class));
    add(new DisplayScreenItem("guild", "server_hypixel", GuiHypixelGuild.class));
    add(new DisplayScreenItem("friends", "server_hypixel", GuiHypixelFriends.class));
    
    add(new BoolItem("2ndChatVisible", "2nd_chat", Boolean.valueOf(true))
    {
      public void action()
      {
        if (!((Boolean)get()).booleanValue()) {
          The5zigMod.getVars().get2ndChat().clear();
        }
      }
    });
    add(new PercentSliderItem("2ndChatOpacity", "2nd_chat", Float.valueOf(1.0F), Float.valueOf(0.1F), Float.valueOf(1.0F), -1));
    add(new PercentSliderItem("2ndChatScale", "2nd_chat", Float.valueOf(1.0F), Float.valueOf(0.0F), Float.valueOf(1.0F), -1)
    {
      public String getCustomValue(float value)
      {
        return value == 0.0F ? The5zigMod.toBoolean(false) : super.getCustomValue(value);
      }
      
      public void action()
      {
        The5zigMod.getVars().get2ndChat().refreshChat();
      }
    });
    add(new SliderItem("2ndChatHeightFocused", "px", "2nd_chat", 180.0F, 20.0F, 180.0F, 1)
    {
      public void action()
      {
        The5zigMod.getVars().get2ndChat().refreshChat();
      }
    });
    add(new SliderItem("2ndChatHeightUnfocused", "px", "2nd_chat", 90.0F, 20.0F, 180.0F, 1)
    {
      public void action()
      {
        The5zigMod.getVars().get2ndChat().refreshChat();
      }
    });
    add(new SliderItem("2ndChatWidth", "px", "2nd_chat", 170.0F, 40.0F, 320.0F, 1)
    {
      public void action()
      {
        The5zigMod.getVars().get2ndChat().refreshChat();
      }
    });
    add(new BoolItem("2ndChatTextLeftbound", "2nd_chat", Boolean.valueOf(true)));
    
    add(new BoolItem("connectToServer", "profile_settings", Boolean.valueOf(true)));
    add(new BoolItem("showConnecting", "profile_settings", Boolean.valueOf(false)));
    add(new OnlineStatusItem("onlineStatus", "profile_settings", Float.valueOf(Friend.OnlineStatus.ONLINE.ordinal())));
    add(new SliderItem("afkTime", "", "profile_settings", 10.0F, 0.0F, 60.0F, 5)
    {
      public String getCustomValue(float value)
      {
        if (value > 0.0F) {
          return super.getCustomValue(value);
        }
        return I18n.translate("config.profile_settings.never");
      }
      
      public String getSuffix()
      {
        return " " + I18n.translate("config.profile_settings.afk_time.min");
      }
    });
    add(new NonConfigItem("show_server", "profile_settings")
    {
      public void action()
      {
        The5zigMod.getDataManager().getProfile().setShowServer(!The5zigMod.getDataManager().getProfile().isShowServer());
      }
      
      public String translate()
      {
        return I18n.translate(new StringBuilder().append(getTranslationPrefix()).append(".").append(getCategory()).append(".").append(getKey()).toString()) + ": " + The5zigMod.toBoolean(The5zigMod.getDataManager().getProfile().isShowServer());
      }
    });
    add(new NonConfigItem("show_messages_read", "profile_settings")
    {
      public void action()
      {
        The5zigMod.getDataManager().getProfile().setShowMessageRead(!The5zigMod.getDataManager().getProfile().isShowMessageRead());
      }
      
      public String translate()
      {
        return I18n.translate(new StringBuilder().append(getTranslationPrefix()).append(".").append(getCategory()).append(".").append(getKey()).toString()) + ": " + The5zigMod.toBoolean(
          The5zigMod.getDataManager().getProfile().isShowMessageRead());
      }
    });
    add(new NonConfigItem("show_friend_requests", "profile_settings")
    {
      public void action()
      {
        The5zigMod.getDataManager().getProfile().setShowFriendRequests(!The5zigMod.getDataManager().getProfile().isShowFriendRequests());
      }
      
      public String translate()
      {
        return I18n.translate(new StringBuilder().append(getTranslationPrefix()).append(".").append(getCategory()).append(".").append(getKey()).toString()) + ": " + The5zigMod.toBoolean(
          The5zigMod.getDataManager().getProfile().isShowFriendRequests());
      }
    });
    add(new NonConfigItem("show_country", "profile_settings")
    {
      public void action()
      {
        The5zigMod.getDataManager().getProfile().setShowCountry(!The5zigMod.getDataManager().getProfile().isShowCountry());
      }
      
      public String translate()
      {
        return I18n.translate(new StringBuilder().append(getTranslationPrefix()).append(".").append(getCategory()).append(".").append(getKey()).toString()) + ": " + The5zigMod.toBoolean(The5zigMod.getDataManager().getProfile().isShowCountry());
      }
    });
    add(new BoolItem("showMessages", "chat_settings", Boolean.valueOf(true)));
    add(new BoolItem("showGroupMessages", "chat_settings", Boolean.valueOf(false)));
    add(new BoolItem("showOnlineMessages", "chat_settings", Boolean.valueOf(true)));
    add(new BoolItem("playMessageSounds", "chat_settings", Boolean.valueOf(true)));
    add(new BoolItem("showTrayNotifications", "chat_settings", Boolean.valueOf(true)));
    add(new EnumItem("friendSortation", "chat_settings", FriendSortation.NAME, FriendSortation.class)
    {
      public void action()
      {
        The5zigMod.getFriendManager().sortFriends();
      }
    });
    add(new EnumItem("chatBackgroundType", "chat_settings", BackgroundType.DEFAULT, BackgroundType.class)
    {
      public void action()
      {
        if (get() == ConfigNew.BackgroundType.DEFAULT)
        {
          The5zigMod.getDataManager().getChatBackgroundManager().resetBackgroundImage();
          ((Item)ConfigNew.this.items.get("chatBackgroundLocation")).set(null);
          ConfigNew.this.save();
        }
      }
    });
    add(new StringItem("chatBackgroundLocation", "chat_settings", null)
    {
      public void action()
      {
        The5zigMod.getVars().displayScreen(new GuiFileSelector(The5zigMod.getVars().getCurrentScreen(), new FileSelectorCallback()
        {
          public void onDone(File file)
          {
            if (file == null)
            {
              The5zigMod.getDataManager().getChatBackgroundManager().resetBackgroundImage();
            }
            else
            {
              ConfigNew.20.this.set(file.getAbsolutePath());
              The5zigMod.getDataManager().getChatBackgroundManager().reloadBackgroundImage();
            }
            The5zigMod.getOverlayMessage().displayMessage(I18n.translate("config.chat_settings.background.selected"));
          }
          
          public String getTitle()
          {
            return "The 5zig Mod - " + I18n.translate("config.chat_settings.title");
          }
        }, new String[] { "png", "jpg" }));
      }
      
      public String translate()
      {
        return I18n.translate(getTranslationPrefix() + "." + getCategory() + "." + Utils.upperToDash(getKey()));
      }
      
      public boolean isRestricted()
      {
        return ConfigNew.this.getEnum("chatBackgroundType", ConfigNew.BackgroundType.class) != ConfigNew.BackgroundType.IMAGE;
      }
    });
  }
  
  public void add(Item item)
  {
    if (this.items.containsKey(item.getKey())) {
      throw new IllegalArgumentException("Config registry already contains key " + item.getKey());
    }
    this.items.put(item.getKey(), item);
  }
  
  public Item get(String key)
  {
    Item item = (Item)this.items.get(key);
    if (item == null) {
      The5zigMod.logger.warn("Could not find " + key + " in config!");
    }
    return item;
  }
  
  public <T extends Item> T get(String key, Class<T> classOfT)
  {
    Item item = (Item)this.items.get(key);
    if ((item == null) || (!classOfT.isAssignableFrom(item.getClass()))) {
      The5zigMod.logger.warn("Could not find " + key + " in config!");
    }
    return (Item)classOfT.cast(item);
  }
  
  public int getInt(String key)
  {
    Item item = (Item)this.items.get(key);
    if (item == null) {
      The5zigMod.logger.warn("Could not find " + key + " in config!");
    }
    return (item instanceof SliderItem) ? ((Float)((SliderItem)item).get()).intValue() : (item instanceof IntItem) ? ((Integer)((IntItem)item).get()).intValue() : 0;
  }
  
  public float getFloat(String key)
  {
    Item item = (Item)this.items.get(key);
    if (item == null) {
      The5zigMod.logger.warn("Could not find " + key + " in config!");
    }
    return (item instanceof FloatItem) ? ((Float)((FloatItem)item).get()).floatValue() : 0.0F;
  }
  
  public boolean getBool(String key)
  {
    Item item = (Item)this.items.get(key);
    if (item == null) {
      The5zigMod.logger.warn("Could not find " + key + " in config!");
    }
    return (item instanceof BoolItem) ? ((Boolean)((BoolItem)item).get()).booleanValue() : false;
  }
  
  public String getString(String key)
  {
    Item item = (Item)this.items.get(key);
    if (item == null) {
      The5zigMod.logger.warn("Could not find " + key + " in config!");
    }
    return (item instanceof StringItem) ? (String)((StringItem)item).get() : null;
  }
  
  public <T extends Enum> T getEnum(String key, Class<T> classOfT)
  {
    Item item = (Item)this.items.get(key);
    if (item == null) {
      The5zigMod.logger.warn("Could not find " + key + " in config!");
    }
    return item == null ? null : (Enum)classOfT.cast(item.get());
  }
  
  private void load(String json)
  {
    if (this.root != null) {
      throw new IllegalStateException("Config already loaded!");
    }
    JsonParser parser = new JsonParser();
    JsonElement parse = parser.parse(json);
    if ((parse == null) || (parse.isJsonNull())) {
      throw new RuntimeException("Config not found!");
    }
    this.root = parse.getAsJsonObject();
    for (Item item : this.items.values()) {
      try
      {
        item.deserialize(this.root);
      }
      catch (Exception e)
      {
        The5zigMod.logger.debug("Error deserializing item " + item, e);
        item.reset();
      }
    }
    The5zigMod.logger.debug("Loaded {} config items!", new Object[] { Integer.valueOf(this.items.size()) });
  }
  
  public void save()
  {
    save(false);
  }
  
  public void save(boolean sync)
  {
    boolean changed = false;
    for (Item item : this.items.values()) {
      if (item.hasChanged())
      {
        item.serialize(this.root);
        changed = true;
      }
    }
    if (!changed) {
      return;
    }
    if (sync) {
      doSave();
    } else {
      The5zigMod.getAsyncExecutor().execute(new Runnable()
      {
        public void run()
        {
          ConfigNew.this.doSave();
        }
      });
    }
  }
  
  private void doSave()
  {
    try
    {
      String json = this.builder.toJson(this.root);
      FileWriter writer = new FileWriter(this.file);
      writer.write(json);
      writer.close();
    }
    catch (IOException e)
    {
      The5zigMod.logger.warn("Could not update Config File!", e);
    }
  }
  
  public void reset()
  {
    this.root = new JsonObject();
    for (Item item : this.items.values()) {
      item.reset();
    }
    save();
  }
  
  public List<Item> getItems(String category)
  {
    List<Item> result = Lists.newArrayList();
    for (Item item : this.items.values()) {
      if (category.equals(item.getCategory())) {
        result.add(item);
      }
    }
    return result;
  }
  
  public List<Item> getItems()
  {
    List<Item> result = Lists.newArrayList();
    for (Item item : this.items.values()) {
      result.add(item);
    }
    return result;
  }
  
  public List<String> getCategories()
  {
    List<String> result = Lists.newArrayList();
    for (Item item : this.items.values()) {
      if (!result.contains(item.getCategory())) {
        result.add(item.getCategory());
      }
    }
    return result;
  }
  
  public static enum BracketsFormatting
  {
    BRACKETS("[", "]"),  BRACKETS_ROUND("(", ")"),  COLON("", ":"),  ARROW("", ">"),  DASH("", " -");
    
    private String first;
    private String last;
    
    private BracketsFormatting(String first, String last)
    {
      this.first = first;
      this.last = last;
    }
    
    public BracketsFormatting getNext()
    {
      return values()[((ordinal() + 1) % values().length)];
    }
    
    public String getFirst()
    {
      return this.first;
    }
    
    public String getLast()
    {
      return this.last;
    }
    
    public boolean hasFirst()
    {
      return !this.first.isEmpty();
    }
  }
  
  public static enum UpdateType
  {
    ALWAYS,  SAME_VERSION,  NEVER;
    
    private UpdateType() {}
  }
  
  public static enum Location
  {
    TOP_LEFT("modules.location.top_left"),  TOP_RIGHT("modules.location.top_right"),  CENTER_LEFT("modules.location.center_left"),  CENTER_RIGHT("modules.location.center_right"),  BOTTOM_LEFT("modules.location.bottom_left"),  BOTTOM_RIGHT("modules.location.bottom_right"),  CUSTOM("modules.location.custom");
    
    private String name;
    
    private Location(String name)
    {
      this.name = name;
    }
    
    public Location getNext()
    {
      return values()[((ordinal() + 1) % values().length)];
    }
    
    public String getName()
    {
      return this.name;
    }
  }
  
  public static enum CoordStyle
  {
    BELOW_OTHER,  SIDE_BY_SIDE;
    
    private CoordStyle() {}
  }
  
  public static enum DirectionStyle
  {
    STRING,  NUMBER,  BOTH,  DEGREE;
    
    private DirectionStyle() {}
  }
  
  public static enum FriendSortation
  {
    NAME,  STATUS;
    
    private FriendSortation() {}
  }
  
  public static enum BackgroundType
  {
    DEFAULT,  TRANSPARENT,  IMAGE;
    
    private BackgroundType() {}
  }
  
  public static enum TemperatureUnit
  {
    CELSIUS,  FAHRENHEIT;
    
    private TemperatureUnit() {}
  }
}
