package eu.the5zig.mod.util;

import java.util.List;

public abstract interface ItemStack
{
  public abstract int getMaxDurability();
  
  public abstract int getCurrentDurability();
  
  public abstract String getKey();
  
  public abstract String getDisplayName();
  
  public abstract List<String> getLore();
  
  public abstract void render(int paramInt1, int paramInt2, boolean paramBoolean);
}
