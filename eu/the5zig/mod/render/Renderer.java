package eu.the5zig.mod.render;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ConfigNew.Location;
import eu.the5zig.mod.config.items.LocationItem;
import eu.the5zig.util.Condition;

public abstract class Renderer
{
  protected Condition condition;
  protected LargeTextRenderer largeTextRenderer;
  
  public Renderer()
  {
    this.largeTextRenderer = DisplayRenderer.largeTextRenderer;
  }
  
  public Renderer(Condition condition)
  {
    this.condition = condition;
  }
  
  public Condition getCondition()
  {
    return this.condition;
  }
  
  public abstract void render(DisplayRenderer paramDisplayRenderer);
  
  public ConfigNew.Location getLocation()
  {
    return (ConfigNew.Location)The5zigMod.getConfig().getEnum(getLocationKey(), ConfigNew.Location.class);
  }
  
  public abstract String getLocationKey();
  
  public boolean isDummy()
  {
    return false;
  }
  
  protected void renderKillstreak(DisplayRenderer renderer, int killstreak)
  {
    String text = null;
    if (killstreak == 2) {
      text = I18n.translate("ingame.killstreak.double");
    } else if (killstreak == 3) {
      text = I18n.translate("ingame.killstreak.triple");
    } else if (killstreak == 4) {
      text = I18n.translate("ingame.killstreak.quadruple");
    } else if (killstreak >= 5) {
      text = I18n.translate("ingame.killstreak.multi");
    }
    if (text == null) {
      return;
    }
    this.largeTextRenderer.render(renderer.getPrefix() + text);
  }
  
  protected boolean isLeft(DisplayRenderer renderer, int maxWidth)
  {
    return (getLocation() == ConfigNew.Location.TOP_LEFT) || (getLocation() == ConfigNew.Location.CENTER_LEFT) || (getLocation() == ConfigNew.Location.BOTTOM_LEFT) || ((getLocation() == ConfigNew.Location.CUSTOM) && ((((LocationItem)The5zigMod.getConfig().get(getLocationKey())).isCentered()) || (((LocationItem)The5zigMod.getConfig().get(getLocationKey())).getXOffset() <= 0.5D - maxWidth / renderer.getWidth() / 2.0F)));
  }
}
