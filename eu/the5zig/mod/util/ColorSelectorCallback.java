package eu.the5zig.mod.util;

import eu.the5zig.util.minecraft.ChatColor;

public abstract interface ColorSelectorCallback
{
  public abstract ChatColor getColor();
  
  public abstract void setColor(ChatColor paramChatColor);
}
