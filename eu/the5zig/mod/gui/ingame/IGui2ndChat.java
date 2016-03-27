package eu.the5zig.mod.gui.ingame;

public abstract interface IGui2ndChat
{
  public abstract void draw(int paramInt);
  
  public abstract void printChatMessage(String paramString);
  
  public abstract void printChatMessage(Object paramObject);
  
  public abstract void clear();
  
  public abstract void refreshChat();
  
  public abstract int getLineCount();
  
  public abstract void scroll(int paramInt);
  
  public abstract void resetScroll();
  
  public abstract void drawComponentHover(int paramInt1, int paramInt2);
  
  public abstract boolean mouseClicked(int paramInt);
  
  public abstract void keyTyped(int paramInt);
}
