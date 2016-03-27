package eu.the5zig.mod.render;

import com.google.common.collect.Maps;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ConfigNew.BracketsFormatting;
import eu.the5zig.mod.config.ConfigNew.Location;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.config.items.ColorFormattingItem;
import eu.the5zig.mod.config.items.SelectColorItem;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.opengl.Display;

public class DisplayRenderer
{
  public static final LargeTextRenderer largeTextRenderer = new LargeTextRenderer();
  private final PotionIndicatorRenderer potionIndicatorRenderer = new PotionIndicatorRenderer();
  private final ChatSymbolsRenderer chatSymbolsRenderer = new ChatSymbolsRenderer();
  private final HashMap<ConfigNew.Location, Integer> totalHeights = Maps.newHashMap();
  private final HashMap<ConfigNew.Location, Integer> offsets = Maps.newHashMap();
  private float scale = 1.0F;
  
  public void drawScreen()
  {
    if (!Display.isVisible()) {
      return;
    }
    if ((The5zigMod.getConfig().getBool("showChatSymbols")) && (The5zigMod.getVars().isChatOpened())) {
      this.chatSymbolsRenderer.render();
    }
    if (!The5zigMod.getConfig().getBool("showMod")) {
      return;
    }
    this.scale = The5zigMod.getConfig().getFloat("scale");
    GLUtil.pushMatrix();
    GLUtil.scale(this.scale, this.scale, this.scale);
    renderModules();
    GLUtil.popMatrix();
    largeTextRenderer.flush();
  }
  
  private void renderModules()
  {
    List<Module> modules = The5zigMod.getModuleMaster().getModules();
    for (ConfigNew.Location location : ConfigNew.Location.values()) {
      this.totalHeights.put(location, Integer.valueOf(0));
    }
    for (??? = modules.iterator(); ((Iterator)???).hasNext();)
    {
      Module module = (Module)((Iterator)???).next();
      if (module.isShouldRender()) {
        if (module.getLocation() != ConfigNew.Location.CUSTOM) {
          this.totalHeights.put(module.getLocation(), Integer.valueOf(((Integer)this.totalHeights.get(module.getLocation())).intValue() + module.getTotalHeight(false)));
        }
      }
    }
    this.offsets.put(ConfigNew.Location.TOP_LEFT, Integer.valueOf(2));
    this.offsets.put(ConfigNew.Location.BOTTOM_LEFT, Integer.valueOf(getHeight() - 2 - ((Integer)this.totalHeights.get(ConfigNew.Location.BOTTOM_LEFT)).intValue()));
    int leftCenter = getHeight() / 2 - ((Integer)this.totalHeights.get(ConfigNew.Location.CENTER_LEFT)).intValue() / 2;
    while (leftCenter > ((Integer)this.offsets.get(ConfigNew.Location.BOTTOM_LEFT)).intValue()) {
      leftCenter--;
    }
    while (leftCenter < ((Integer)this.offsets.get(ConfigNew.Location.TOP_LEFT)).intValue() + ((Integer)this.totalHeights.get(ConfigNew.Location.TOP_LEFT)).intValue()) {
      leftCenter++;
    }
    this.offsets.put(ConfigNew.Location.CENTER_LEFT, Integer.valueOf(leftCenter));
    
    this.offsets.put(ConfigNew.Location.TOP_RIGHT, Integer.valueOf(2 + The5zigMod.getVars().getPotionEffectIndicatorHeight()));
    this.offsets.put(ConfigNew.Location.BOTTOM_RIGHT, Integer.valueOf(getHeight() - 2 - ((Integer)this.totalHeights.get(ConfigNew.Location.BOTTOM_RIGHT)).intValue()));
    int rightCenter = getHeight() / 2 - ((Integer)this.totalHeights.get(ConfigNew.Location.CENTER_RIGHT)).intValue() / 2;
    while (rightCenter > ((Integer)this.offsets.get(ConfigNew.Location.BOTTOM_RIGHT)).intValue()) {
      rightCenter--;
    }
    while (rightCenter < ((Integer)this.offsets.get(ConfigNew.Location.TOP_RIGHT)).intValue() + ((Integer)this.totalHeights.get(ConfigNew.Location.TOP_RIGHT)).intValue()) {
      rightCenter++;
    }
    this.offsets.put(ConfigNew.Location.CENTER_RIGHT, Integer.valueOf(rightCenter));
    for (Module module : modules) {
      if (module.isShouldRender())
      {
        int y = 0;
        int x;
        int x;
        switch (module.getLocation())
        {
        case TOP_LEFT: 
        case BOTTOM_LEFT: 
        case CENTER_LEFT: 
          x = 2;
          break;
        case TOP_RIGHT: 
        case BOTTOM_RIGHT: 
        case CENTER_RIGHT: 
          x = getWidth() - module.getMaxWidth(false) - 2;
          break;
        case CUSTOM: 
          int x = (int)(module.getLocationX() * getWidth());
          y = (int)(module.getLocationY() * getHeight());
          break;
        default: 
          throw new IllegalArgumentException();
        }
        int x;
        if (module.getLocation() == ConfigNew.Location.CUSTOM)
        {
          module.render(this, x, y, false);
        }
        else
        {
          Integer yOffset = (Integer)this.offsets.get(module.getLocation());
          module.render(this, x, yOffset.intValue(), false);
          this.offsets.put(module.getLocation(), Integer.valueOf(yOffset.intValue() + module.getTotalHeight(false)));
        }
      }
    }
  }
  
  public String getPrefix(String name)
  {
    return getBrackets() + getBracketsLeft() + getPrefix() + name + getBrackets() + getBracketsRight() + " " + getMain();
  }
  
  public String getPrefix()
  {
    ChatColor formattingPrefix = (ChatColor)((ColorFormattingItem)The5zigMod.getConfig().get("formattingPrefix", ColorFormattingItem.class)).get();
    ChatColor colorPrefix = (ChatColor)((SelectColorItem)The5zigMod.getConfig().get("colorPrefix", SelectColorItem.class)).get();
    if (formattingPrefix != ChatColor.RESET) {
      return colorPrefix.toString() + formattingPrefix.toString();
    }
    return colorPrefix.toString();
  }
  
  public String getMain()
  {
    ChatColor formattingMain = (ChatColor)((ColorFormattingItem)The5zigMod.getConfig().get("formattingMain", ColorFormattingItem.class)).get();
    ChatColor colorMain = (ChatColor)((SelectColorItem)The5zigMod.getConfig().get("colorMain", SelectColorItem.class)).get();
    if (formattingMain != ChatColor.RESET) {
      return colorMain.toString() + formattingMain.toString();
    }
    return colorMain.toString();
  }
  
  public String getBrackets()
  {
    return ((ChatColor)((SelectColorItem)The5zigMod.getConfig().get("colorBrackets", SelectColorItem.class)).get()).toString();
  }
  
  public String getBracketsLeft()
  {
    return ((ConfigNew.BracketsFormatting)The5zigMod.getConfig().getEnum("formattingBrackets", ConfigNew.BracketsFormatting.class)).getFirst();
  }
  
  public String getBracketsRight()
  {
    return ((ConfigNew.BracketsFormatting)The5zigMod.getConfig().getEnum("formattingBrackets", ConfigNew.BracketsFormatting.class)).getLast();
  }
  
  public int getWidth()
  {
    return (int)(The5zigMod.getVars().getScaledWidth() / this.scale);
  }
  
  public int getHeight()
  {
    return (int)(The5zigMod.getVars().getScaledHeight() / this.scale);
  }
  
  public PotionIndicatorRenderer getPotionIndicatorRenderer()
  {
    return this.potionIndicatorRenderer;
  }
  
  public ChatSymbolsRenderer getChatSymbolsRenderer()
  {
    return this.chatSymbolsRenderer;
  }
}
