package eu.the5zig.mod.util;

import eu.the5zig.mod.gui.elements.Row;

public abstract interface GuiListChatCallback
{
  public abstract boolean drawDefaultBackground();
  
  public abstract Object getResourceLocation();
  
  public abstract int getImageWidth();
  
  public abstract int getImageHeight();
  
  public abstract void chatLineClicked(Row paramRow, int paramInt1, int paramInt2, int paramInt3, int paramInt4);
}
