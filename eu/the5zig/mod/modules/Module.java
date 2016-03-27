package eu.the5zig.mod.modules;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew.Location;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.PvP;
import eu.the5zig.mod.server.timolia.ServerTimolia.PvPTournament;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;

public class Module
  implements Row
{
  private String id;
  private String name;
  private String translation;
  private String server;
  private ServerInstance serverInstance;
  private boolean showLabel;
  private ConfigNew.Location location;
  private float locationX;
  private float locationY;
  private List<Item> items = Lists.newArrayList();
  private boolean shouldRender = true;
  private RenderType renderType;
  
  public Module(String id, String name, String translation, String server, boolean showLabel, ConfigNew.Location location, float locationX, float locationY)
  {
    this.id = id;
    this.name = name;
    this.translation = translation;
    setServer(server);
    this.showLabel = showLabel;
    this.location = location;
    this.locationX = locationX;
    this.locationY = locationY;
  }
  
  public void render(DisplayRenderer renderer, int x, int y, boolean dummy)
  {
    if (getItemRenderCount(dummy) == 0) {
      return;
    }
    int maxWidth = getMaxWidth(dummy);
    RenderLocation renderLocation;
    RenderLocation renderLocation;
    if ((this.location == ConfigNew.Location.TOP_LEFT) || (this.location == ConfigNew.Location.CENTER_LEFT) || (this.location == ConfigNew.Location.BOTTOM_LEFT))
    {
      renderLocation = RenderLocation.LEFT;
    }
    else
    {
      RenderLocation renderLocation;
      if ((this.location == ConfigNew.Location.TOP_RIGHT) || (this.location == ConfigNew.Location.CENTER_RIGHT) || (this.location == ConfigNew.Location.BOTTOM_RIGHT))
      {
        renderLocation = RenderLocation.RIGHT;
      }
      else
      {
        RenderLocation renderLocation;
        if (this.locationX == 0.5D)
        {
          renderLocation = RenderLocation.CENTERED;
        }
        else
        {
          RenderLocation renderLocation;
          if (((this.location == ConfigNew.Location.CUSTOM) && (this.locationX + maxWidth / renderer.getWidth() / 2.0F < 0.5D)) || (this.locationX < 0.5D)) {
            renderLocation = RenderLocation.LEFT;
          } else {
            renderLocation = RenderLocation.RIGHT;
          }
        }
      }
    }
    String displayName;
    if (isShowLabel())
    {
      displayName = getDisplayName();
      if (renderLocation == RenderLocation.CENTERED) {
        The5zigMod.getVars().drawString(renderer.getPrefix() + displayName, x - The5zigMod.getVars().getStringWidth(displayName) / 2, y);
      } else if (renderLocation == RenderLocation.RIGHT)
      {
        if (this.location == ConfigNew.Location.CUSTOM) {
          The5zigMod.getVars().drawString(renderer.getPrefix() + displayName, x - The5zigMod.getVars().getStringWidth(displayName), y);
        } else {
          The5zigMod.getVars().drawString(renderer.getPrefix() + displayName, x + (maxWidth - The5zigMod.getVars().getStringWidth(displayName)), y);
        }
      }
      else {
        The5zigMod.getVars().drawString(renderer.getPrefix() + displayName, x, y);
      }
      y += 12;
    }
    for (Item item : this.items) {
      if (item.shouldRender(dummy))
      {
        if (renderLocation == RenderLocation.CENTERED) {
          item.render(x - item.getWidth(dummy) / 2, y, renderLocation, dummy);
        } else if (renderLocation == RenderLocation.RIGHT)
        {
          if (this.location == ConfigNew.Location.CUSTOM) {
            item.render(x - item.getWidth(dummy), y, renderLocation, dummy);
          } else {
            item.render(x + (maxWidth - item.getWidth(dummy)), y, renderLocation, dummy);
          }
        }
        else {
          item.render(x, y, renderLocation, dummy);
        }
        y += item.getHeight(dummy);
      }
    }
  }
  
  public int getMaxWidth(boolean dummy)
  {
    int max = 0;
    for (Item item : this.items) {
      if (item.shouldRender(dummy))
      {
        int width = item.getWidth(dummy);
        if (width > max) {
          max = width;
        }
      }
    }
    if (isShowLabel())
    {
      int width = The5zigMod.getVars().getStringWidth(getDisplayName());
      if (width > max) {
        max = width;
      }
    }
    return max;
  }
  
  public int getTotalHeight(boolean dummy)
  {
    int height = 0;
    for (Item item : this.items) {
      if (item.shouldRender(dummy)) {
        height += item.getHeight(dummy);
      }
    }
    if (isShowLabel()) {
      height += 12;
    }
    return height + 6;
  }
  
  private int getItemRenderCount(boolean dummy)
  {
    int count = 0;
    for (Item item : this.items) {
      if (item.shouldRender(dummy)) {
        count++;
      }
    }
    return count;
  }
  
  public String getId()
  {
    return this.id;
  }
  
  public void setId(String id)
  {
    this.id = id;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public String getTranslation()
  {
    return this.translation;
  }
  
  public void setTranslation(String translation)
  {
    this.translation = translation;
  }
  
  public String getServer()
  {
    return this.server;
  }
  
  public void setServer(String server)
  {
    this.server = server;
    if (server != null) {
      this.serverInstance = ServerInstance.byConfigName(server);
    } else {
      this.serverInstance = null;
    }
  }
  
  public String getDisplayName()
  {
    if (this.renderType == RenderType.TIMOLIA_TOURNAMENT)
    {
      if (((The5zigMod.getDataManager().getServer() instanceof ServerTimolia)) && (((ServerTimolia)The5zigMod.getDataManager().getServer()).getGameMode() != null) && 
        (((ServerTimolia.PvP)((ServerTimolia)The5zigMod.getDataManager().getServer()).getGameMode()).getTournament() != null)) {
        return ChatColor.UNDERLINE + I18n.translate("ingame.tournament", new Object[] {
          ((ServerTimolia.PvP)((ServerTimolia)The5zigMod.getDataManager().getServer()).getGameMode()).getTournament().getHost() });
      }
      return ChatColor.UNDERLINE + I18n.translate("ingame.tournament", new Object[] { "/" });
    }
    if (this.serverInstance != null)
    {
      String name = getName();
      if ((The5zigMod.getDataManager().getServer() instanceof GameServer))
      {
        GameMode gameMode = ((GameServer)The5zigMod.getDataManager().getServer()).getGameMode();
        name = name.replace("%gamemode%", gameMode == null ? this.serverInstance.getName() : gameMode.getName());
      }
      else
      {
        name = this.serverInstance.getName();
      }
      return ChatColor.UNDERLINE + name;
    }
    if ((getTranslation() != null) && (!getTranslation().isEmpty())) {
      return ChatColor.UNDERLINE + I18n.translate(getTranslation());
    }
    if ((getName() == null) || (getName().isEmpty())) {
      return ChatColor.UNDERLINE + getId();
    }
    if (getName().contains("%version%")) {
      return getName().replace("%version%", "3.5.3");
    }
    return ChatColor.UNDERLINE + getName();
  }
  
  public boolean isShowLabel()
  {
    return this.showLabel;
  }
  
  public void setShowLabel(boolean showLabel)
  {
    this.showLabel = showLabel;
  }
  
  public ConfigNew.Location getLocation()
  {
    return this.location;
  }
  
  public void setLocation(ConfigNew.Location location)
  {
    this.location = location;
  }
  
  public float getLocationX()
  {
    return this.locationX;
  }
  
  public void setLocationX(float locationX)
  {
    this.locationX = locationX;
  }
  
  public float getLocationY()
  {
    return this.locationY;
  }
  
  public void setLocationY(float locationY)
  {
    this.locationY = locationY;
  }
  
  public void addItem(Item item)
  {
    this.items.add(item);
  }
  
  public List<Item> getItems()
  {
    return this.items;
  }
  
  public boolean isShouldRender()
  {
    if (this.renderType == RenderType.TIMOLIA_TOURNAMENT) {
      return ((The5zigMod.getDataManager().getServer() instanceof ServerTimolia)) && ((((ServerTimolia)The5zigMod.getDataManager().getServer()).getGameMode() instanceof ServerTimolia.PvP)) && (((ServerTimolia.PvP)((ServerTimolia)The5zigMod.getDataManager().getServer()).getGameMode()).getTournament() != null);
    }
    return (this.serverInstance.isConnectedTo()) && (getItemRenderCount(false) != 0);
  }
  
  public RenderType getRenderType()
  {
    return this.renderType;
  }
  
  public void setRenderType(RenderType renderType)
  {
    this.renderType = renderType;
  }
  
  public void draw(int x, int y)
  {
    The5zigMod.getVars().drawString(The5zigMod.getVars().shortenToWidth(getId(), 145), x + 2, y + 2);
  }
  
  public int getLineHeight()
  {
    return 16;
  }
  
  public static enum RenderType
  {
    TIMOLIA_TOURNAMENT,  CUSTOM_SERVER;
    
    private RenderType() {}
  }
}
