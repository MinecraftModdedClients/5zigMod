package eu.the5zig.mod.modules.items;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.util.IVariables;

public abstract class StringItem
  extends Item
{
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    String text = getText(dummy);
    
    The5zigMod.getVars().drawString(text, x, y);
  }
  
  public boolean shouldRender(boolean dummy)
  {
    return getValue(dummy) != null;
  }
  
  private String getText(boolean dummy)
  {
    return getPrefix() + String.valueOf(getValue(dummy));
  }
  
  public int getWidth(boolean dummy)
  {
    return The5zigMod.getVars().getStringWidth(getText(dummy));
  }
  
  public int getHeight(boolean dummy)
  {
    return 10;
  }
  
  protected abstract Object getValue(boolean paramBoolean);
}
