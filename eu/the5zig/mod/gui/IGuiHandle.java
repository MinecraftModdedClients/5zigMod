package eu.the5zig.mod.gui;

import java.util.List;

public abstract interface IGuiHandle
{
  public abstract int getWidth();
  
  public abstract int getHeight();
  
  public abstract void setResolution(int paramInt1, int paramInt2);
  
  public abstract void drawDefaultBackground();
  
  public abstract void drawMenuBackground();
  
  public abstract void drawTexturedModalRect(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6);
  
  public abstract void drawHoveringText(List<String> paramList, int paramInt1, int paramInt2);
}
