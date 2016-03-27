package eu.the5zig.mod.modules.items;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ConfigNew.BracketsFormatting;
import eu.the5zig.mod.config.items.ColorFormattingItem;
import eu.the5zig.mod.config.items.EnumItem;
import eu.the5zig.mod.config.items.SelectColorItem;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.player.AFKTime;
import eu.the5zig.mod.modules.items.player.Arrows;
import eu.the5zig.mod.modules.items.player.Biome;
import eu.the5zig.mod.modules.items.player.Boots;
import eu.the5zig.mod.modules.items.player.CPS;
import eu.the5zig.mod.modules.items.player.Chestplate;
import eu.the5zig.mod.modules.items.player.Coordinates;
import eu.the5zig.mod.modules.items.player.CoordinatesClipboard;
import eu.the5zig.mod.modules.items.player.Direction;
import eu.the5zig.mod.modules.items.player.EntityCount;
import eu.the5zig.mod.modules.items.player.EntityHealth;
import eu.the5zig.mod.modules.items.player.FPS;
import eu.the5zig.mod.modules.items.player.Helmet;
import eu.the5zig.mod.modules.items.player.Leggings;
import eu.the5zig.mod.modules.items.player.Light;
import eu.the5zig.mod.modules.items.player.MainHand;
import eu.the5zig.mod.modules.items.player.OffHand;
import eu.the5zig.mod.modules.items.player.Potions;
import eu.the5zig.mod.modules.items.player.Soups;
import eu.the5zig.mod.modules.items.player.Speed;
import eu.the5zig.mod.modules.items.player.Time;
import eu.the5zig.mod.modules.items.server.Countdown;
import eu.the5zig.mod.modules.items.server.CustomServer;
import eu.the5zig.mod.modules.items.server.Deaths;
import eu.the5zig.mod.modules.items.server.Kills;
import eu.the5zig.mod.modules.items.server.Killstreak;
import eu.the5zig.mod.modules.items.server.Lobby;
import eu.the5zig.mod.modules.items.server.ServerIP;
import eu.the5zig.mod.modules.items.server.ServerPing;
import eu.the5zig.mod.modules.items.server.ServerPlayers;
import eu.the5zig.mod.modules.items.server.WinMessage;
import eu.the5zig.mod.modules.items.server.bergwerk.DuelRespawn;
import eu.the5zig.mod.modules.items.server.bergwerk.DuelTeam;
import eu.the5zig.mod.modules.items.server.bergwerk.DuelTeleportMessage;
import eu.the5zig.mod.modules.items.server.gommehd.BedWarsBeds;
import eu.the5zig.mod.modules.items.server.gommehd.BedWarsGold;
import eu.the5zig.mod.modules.items.server.gommehd.BedWarsRespawn;
import eu.the5zig.mod.modules.items.server.gommehd.BedWarsTeam;
import eu.the5zig.mod.modules.items.server.gommehd.EnderGamesCoins;
import eu.the5zig.mod.modules.items.server.gommehd.EnderGamesKit;
import eu.the5zig.mod.modules.items.server.gommehd.SGDeathmatch;
import eu.the5zig.mod.modules.items.server.gommehd.SGDeathmatchMessage;
import eu.the5zig.mod.modules.items.server.gommehd.SkyWarsCoins;
import eu.the5zig.mod.modules.items.server.gommehd.SkyWarsKit;
import eu.the5zig.mod.modules.items.server.gommehd.SkyWarsTeam;
import eu.the5zig.mod.modules.items.server.hypixel.BlitzDeathmatch;
import eu.the5zig.mod.modules.items.server.hypixel.BlitzDeathmatchMessage;
import eu.the5zig.mod.modules.items.server.hypixel.BlitzKit;
import eu.the5zig.mod.modules.items.server.hypixel.BlitzStar;
import eu.the5zig.mod.modules.items.server.hypixel.BlitzStarMessage;
import eu.the5zig.mod.modules.items.server.hypixel.PaintballTeam;
import eu.the5zig.mod.modules.items.server.playminity.JumpLeagueCheckpoints;
import eu.the5zig.mod.modules.items.server.playminity.JumpLeagueFails;
import eu.the5zig.mod.modules.items.server.playminity.JumpLeagueLives;
import eu.the5zig.mod.modules.items.server.timolia.AdventCheckpoint;
import eu.the5zig.mod.modules.items.server.timolia.AdventMedal;
import eu.the5zig.mod.modules.items.server.timolia.AdventParkour;
import eu.the5zig.mod.modules.items.server.timolia.ArcadeCurrentMinigame;
import eu.the5zig.mod.modules.items.server.timolia.ArcadeNextMinigame;
import eu.the5zig.mod.modules.items.server.timolia.ArenaRound;
import eu.the5zig.mod.modules.items.server.timolia.BrainbowScore;
import eu.the5zig.mod.modules.items.server.timolia.BrainbowTeam;
import eu.the5zig.mod.modules.items.server.timolia.DNAHeight;
import eu.the5zig.mod.modules.items.server.timolia.InTimeInvincibility;
import eu.the5zig.mod.modules.items.server.timolia.InTimeInvincibilityMessage;
import eu.the5zig.mod.modules.items.server.timolia.InTimeLoot;
import eu.the5zig.mod.modules.items.server.timolia.InTimeRegeneration;
import eu.the5zig.mod.modules.items.server.timolia.JumpWorldCheckpoint;
import eu.the5zig.mod.modules.items.server.timolia.JumpWorldFails;
import eu.the5zig.mod.modules.items.server.timolia.JumpWorldLastCheckpoint;
import eu.the5zig.mod.modules.items.server.timolia.PVPOpponent;
import eu.the5zig.mod.modules.items.server.timolia.PVPOpponentGames;
import eu.the5zig.mod.modules.items.server.timolia.PVPOpponentKDR;
import eu.the5zig.mod.modules.items.server.timolia.PVPOpponentWins;
import eu.the5zig.mod.modules.items.server.timolia.PVPWinStreak;
import eu.the5zig.mod.modules.items.server.timolia.TournamentBestOf;
import eu.the5zig.mod.modules.items.server.timolia.TournamentDeaths;
import eu.the5zig.mod.modules.items.server.timolia.TournamentKills;
import eu.the5zig.mod.modules.items.server.timolia.TournamentParticipants;
import eu.the5zig.mod.modules.items.server.timolia.TournamentQualification;
import eu.the5zig.mod.modules.items.server.timolia.TournamentRound;
import eu.the5zig.mod.modules.items.server.venicraft.MineathlonDiscipline;
import eu.the5zig.mod.modules.items.server.venicraft.MineathlonRound;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Item
  implements Row
{
  private static final List<RegisteredItem> REGISTERED_ITEMS = ;
  private static final HashMap<String, RegisteredItem> BY_KEY = Maps.newHashMap();
  private static final HashMap<Class<? extends Item>, RegisteredItem> BY_ITEM = Maps.newHashMap();
  
  static
  {
    registerItem("DUMMY", Dummy.class);
    registerItem("FPS", FPS.class, Category.GENERAL);
    registerItem("Counter", CPS.class, Category.GENERAL);
    registerItem("AFK_TIME", AFKTime.class, Category.GENERAL);
    registerItem("COORDINATES", Coordinates.class, Category.GENERAL);
    registerItem("DIRECTION", Direction.class, Category.GENERAL);
    registerItem("BIOME", Biome.class, Category.GENERAL);
    registerItem("ENTITIES", EntityCount.class, Category.GENERAL);
    registerItem("TIME", Time.class, Category.GENERAL);
    registerItem("POTIONS", Potions.class, Category.GENERAL);
    registerItem("LIGHT_LEVEL", Light.class, Category.GENERAL);
    registerItem("SPEED", Speed.class, Category.GENERAL);
    
    registerItem("COORDINATES_CLIPBOARD", CoordinatesClipboard.class, Category.OTHER);
    registerItem("CUSTOM_SERVER", CustomServer.class, Category.OTHER);
    registerItem("ENTITY_HEALTH", EntityHealth.class, Category.OTHER);
    
    registerItem("MAIN_HAND", MainHand.class, Category.EQUIP);
    registerItem("OFF_HAND", OffHand.class, Category.EQUIP);
    registerItem("HELMET", Helmet.class, Category.EQUIP);
    registerItem("CHESTPLATE", Chestplate.class, Category.EQUIP);
    registerItem("LEGGINGS", Leggings.class, Category.EQUIP);
    registerItem("BOOTS", Boots.class, Category.EQUIP);
    registerItem("ARROWS", Arrows.class, Category.EQUIP);
    registerItem("SOUPS", Soups.class, Category.EQUIP);
    
    registerItem("IP", ServerIP.class, Category.SERVER_GENERAL);
    registerItem("PLAYERS", ServerPlayers.class, Category.SERVER_GENERAL);
    registerItem("PING", ServerPing.class, Category.SERVER_GENERAL);
    registerItem("LOBBY", Lobby.class, Category.SERVER_GENERAL);
    registerItem("COUNTDOWN", Countdown.class, Category.SERVER_GENERAL);
    registerItem("KILLS", Kills.class, Category.SERVER_GENERAL);
    registerItem("KILLSTREAK", Killstreak.class, Category.SERVER_GENERAL);
    registerItem("DEATHS", Deaths.class, Category.SERVER_GENERAL);
    registerItem("WIN_MESSAGE", WinMessage.class, Category.SERVER_GENERAL);
    
    registerItem("TIMOLIA_PVP_WINSTREAK", PVPWinStreak.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_PVP_OPPONENT", PVPOpponent.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_PVP_OPPONENT_GAMES", PVPOpponentGames.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_PVP_OPPONENT_WINS", PVPOpponentWins.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_PVP_OPPONENT_KDR", PVPOpponentKDR.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_TOURNAMENT_PARTICIPANTS", TournamentParticipants.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_TOURNAMENT_ROUND", TournamentRound.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_TOURNAMENT_BEST_OF", TournamentBestOf.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_TOURNAMENT_QUALIFICATION", TournamentQualification.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_TOURNAMENT_KILLS", TournamentKills.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_TOURNAMENT_DEATHS", TournamentDeaths.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_DNA_HEIGHT", DNAHeight.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_ARCADE_CURRENT_MINIGAME", ArcadeCurrentMinigame.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_ARCADE_NEXT_MINIGAME", ArcadeNextMinigame.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_ARENA_ROUND", ArenaRound.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_BRAINBOW_TEAM", BrainbowTeam.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_BRAINBOW_SCORE", BrainbowScore.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_ADVENT_MEDAL", AdventMedal.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_ADVENT_PARKOUR", AdventParkour.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_ADVENT_CHECKPOINT", AdventCheckpoint.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_INTIME_INVINCIBILITY", InTimeInvincibility.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_INTIME_REGENERATION", InTimeRegeneration.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_INTIME_LOOT", InTimeLoot.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_INTIME_INVINCIBILITY_MESSAGE", InTimeInvincibilityMessage.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_JUMPWORLD_FAILS", JumpWorldFails.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_JUMPWORLD_CHECKPOINT", JumpWorldCheckpoint.class, Category.SERVER_TIMOLIA);
    registerItem("TIMOLIA_JUMPWORLD_LAST_CHECKPOINT", JumpWorldLastCheckpoint.class, Category.SERVER_TIMOLIA);
    
    registerItem("GOMMEHD_SG_DEATHMATCH", SGDeathmatch.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_SG_DEATHMATCH_MESSAGE", SGDeathmatchMessage.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_BEDWARS_TEAM", BedWarsTeam.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_BEDWARS_RESPAWN", BedWarsRespawn.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_BEDWARS_GOLD", BedWarsGold.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_BEDWARS_BEDS", BedWarsBeds.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_ENDERGAMES_KIT", EnderGamesKit.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_ENDERGAMES_COINS", EnderGamesCoins.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_SKYWARS_COINS", SkyWarsCoins.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_SKYWARS_TEAM", SkyWarsTeam.class, Category.SERVER_GOMMEHD);
    registerItem("GOMMEHD_SKYWARS_KIT", SkyWarsKit.class, Category.SERVER_GOMMEHD);
    
    registerItem("PLAYMINITY_JUMPLEAGUE_CHECKPOINTS", JumpLeagueCheckpoints.class, Category.SERVER_PLAYMINITY);
    registerItem("PLAYMINITY_JUMPLEAGUE_FAILS", JumpLeagueFails.class, Category.SERVER_PLAYMINITY);
    registerItem("PLAYMINITY_JUMPLEAGUE_LIVES", JumpLeagueLives.class, Category.SERVER_PLAYMINITY);
    
    registerItem("BERGWERK_DUEL_TEAM", DuelTeam.class, Category.SERVER_BERGWERK);
    registerItem("BERGWERK_DUEL_RESPAWN", DuelRespawn.class, Category.SERVER_BERGWERK);
    registerItem("BERGWERK_DUEL_TELEPORT_MESSAGE", DuelTeleportMessage.class, Category.SERVER_BERGWERK);
    
    registerItem("HYPIXEL_PAINTBALL_TEAM", PaintballTeam.class, Category.SERVER_HYPIXEL);
    registerItem("HYPIXEL_BLITZ_KIT", BlitzKit.class, Category.SERVER_HYPIXEL);
    registerItem("HYPIXEL_BLITZ_STAR", BlitzStar.class, Category.SERVER_HYPIXEL);
    registerItem("HYPIXEL_BLITZ_DEATHMATCH", BlitzDeathmatch.class, Category.SERVER_HYPIXEL);
    registerItem("HYPIXEL_BLITZ_STAR_MESSAGE", BlitzStarMessage.class, Category.SERVER_HYPIXEL);
    registerItem("HYPIXEL_BLITZ_DEATHMATCH_MESSAGE", BlitzDeathmatchMessage.class, Category.SERVER_HYPIXEL);
    
    registerItem("VENICRAFT_MINEATHLON_DISCIPLINE", MineathlonDiscipline.class, Category.SERVER_VENICRAFT);
    registerItem("VENICRAFT_MINEATHLON_ROUND", MineathlonRound.class, Category.SERVER_VENICRAFT);
  }
  
  public static void registerItem(String key, Class<? extends Item> clazz)
  {
    RegisteredItem registeredItem = new RegisteredItem(key, clazz);
    REGISTERED_ITEMS.add(registeredItem);
    BY_KEY.put(key, registeredItem);
    BY_ITEM.put(clazz, registeredItem);
  }
  
  public static void registerItem(String key, Class<? extends Item> clazz, Category category)
  {
    RegisteredItem registeredItem = new RegisteredItem(key, clazz, category);
    REGISTERED_ITEMS.add(registeredItem);
    BY_KEY.put(key, registeredItem);
    BY_ITEM.put(clazz, registeredItem);
  }
  
  public static Item create(RegisteredItem item)
    throws Exception
  {
    Class<? extends Item> clazz = item.getClazz();
    return (Item)clazz.newInstance();
  }
  
  public static RegisteredItem byItem(Class<? extends Item> clazz)
  {
    return (RegisteredItem)BY_ITEM.get(clazz);
  }
  
  public static RegisteredItem byKey(String key)
  {
    return (RegisteredItem)BY_KEY.get(key);
  }
  
  public static List<RegisteredItem> getRegisteredItems()
  {
    return REGISTERED_ITEMS;
  }
  
  private Map<String, eu.the5zig.mod.config.items.Item> settings = Maps.newHashMap();
  private Color color;
  
  public boolean shouldRender(boolean dummy)
  {
    return true;
  }
  
  public String getName()
  {
    return null;
  }
  
  public String getTranslation()
  {
    return null;
  }
  
  public Color getColor()
  {
    return this.color;
  }
  
  public void setColor(Color color)
  {
    this.color = color;
  }
  
  public String getDisplayName()
  {
    return (getTranslation() != null) && (!getTranslation().isEmpty()) ? I18n.translate(getTranslation()) : getName();
  }
  
  protected void addSetting(eu.the5zig.mod.config.items.Item item)
  {
    item.setTranslationPrefix("modules.item");
    this.settings.put(item.getKey(), item);
  }
  
  public eu.the5zig.mod.config.items.Item getSetting(String key)
  {
    return (eu.the5zig.mod.config.items.Item)this.settings.get(key);
  }
  
  public Collection<eu.the5zig.mod.config.items.Item> getSettings()
  {
    return this.settings.values();
  }
  
  public String getPrefix(String prefixText)
  {
    ChatColor bracketsColor = (ChatColor)((SelectColorItem)The5zigMod.getConfig().get("colorBrackets", SelectColorItem.class)).get();
    ConfigNew.BracketsFormatting bracketsFormatting = (ConfigNew.BracketsFormatting)((EnumItem)The5zigMod.getConfig().get("formattingBrackets", EnumItem.class)).get();
    
    ChatColor prefixFormatting = (this.color != null) && (this.color.prefixFormatting != null) ? this.color.prefixFormatting : (ChatColor)((ColorFormattingItem)The5zigMod.getConfig().get("formattingPrefix", ColorFormattingItem.class)).get();
    ChatColor prefixColor = (this.color != null) && (this.color.prefixColor != null) ? this.color.prefixColor : (ChatColor)((SelectColorItem)The5zigMod.getConfig().get("colorPrefix", SelectColorItem.class)).get();
    ChatColor mainFormatting = (this.color != null) && (this.color.mainFormatting != null) ? this.color.mainFormatting : (ChatColor)((ColorFormattingItem)The5zigMod.getConfig().get("formattingMain", ColorFormattingItem.class)).get();
    ChatColor mainColor = (this.color != null) && (this.color.mainColor != null) ? this.color.mainColor : (ChatColor)((SelectColorItem)The5zigMod.getConfig().get("colorMain", SelectColorItem.class)).get();
    
    return bracketsColor.toString() + bracketsFormatting.getFirst() + prefixColor.toString() + (prefixFormatting == ChatColor.RESET ? "" : prefixFormatting.toString()) + prefixText + bracketsColor.toString() + bracketsFormatting.getLast() + " " + mainColor.toString() + (mainFormatting == ChatColor.RESET ? "" : mainFormatting.toString());
  }
  
  public String getPrefix()
  {
    return getPrefix(getDisplayName());
  }
  
  protected String shorten(double d)
  {
    return Utils.getShortenedDouble(d, The5zigMod.getConfig().getInt("numberPrecision"));
  }
  
  protected String shorten(float f)
  {
    return Utils.getShortenedFloat(f, The5zigMod.getConfig().getInt("numberPrecision"));
  }
  
  public void draw(int x, int y)
  {
    The5zigMod.getVars().drawString(The5zigMod.getVars().shortenToWidth(I18n.translate("modules.item." + byItem(getClass()).getKey().toLowerCase()), 160), x + 2, y + 2);
  }
  
  public int getLineHeight()
  {
    return 16;
  }
  
  public abstract void render(int paramInt1, int paramInt2, RenderLocation paramRenderLocation, boolean paramBoolean);
  
  public abstract int getWidth(boolean paramBoolean);
  
  public abstract int getHeight(boolean paramBoolean);
  
  public static class Color
  {
    public ChatColor prefixFormatting;
    public ChatColor prefixColor;
    public ChatColor mainFormatting;
    public ChatColor mainColor;
    
    public Color(ChatColor prefixFormatting, ChatColor prefixColor, ChatColor mainFormatting, ChatColor mainColor)
    {
      this.prefixFormatting = prefixFormatting;
      this.prefixColor = prefixColor;
      this.mainFormatting = mainFormatting;
      this.mainColor = mainColor;
    }
  }
}
