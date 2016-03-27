package eu.the5zig.mod.gui.elements;

public abstract interface IButton
{
  public abstract int getId();
  
  public abstract String getLabel();
  
  public abstract void setLabel(String paramString);
  
  public abstract int getWidth();
  
  public abstract void setWidth(int paramInt);
  
  public abstract int getHeight();
  
  public abstract void setHeight(int paramInt);
  
  public abstract boolean isEnabled();
  
  public abstract void setEnabled(boolean paramBoolean);
  
  public abstract boolean isVisible();
  
  public abstract void setVisible(boolean paramBoolean);
  
  public abstract boolean isHovered();
  
  public abstract void setHovered(boolean paramBoolean);
  
  public abstract int getX();
  
  public abstract void setX(int paramInt);
  
  public abstract int getY();
  
  public abstract void setY(int paramInt);
  
  public abstract void draw(int paramInt1, int paramInt2);
  
  public abstract void tick();
  
  public abstract boolean mouseClicked(int paramInt1, int paramInt2);
  
  public abstract void mouseReleased(int paramInt1, int paramInt2);
  
  public abstract void playClickSound();
  
  public abstract void setTicksDisabled(int paramInt);
  
  public abstract void guiClosed();
}
