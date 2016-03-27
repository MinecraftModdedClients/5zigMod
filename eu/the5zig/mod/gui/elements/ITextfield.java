package eu.the5zig.mod.gui.elements;

public abstract interface ITextfield
{
  public abstract int getId();
  
  public abstract void setSelected(boolean paramBoolean);
  
  public abstract boolean isFocused();
  
  public abstract void setFocused(boolean paramBoolean);
  
  public abstract boolean isBackgroundDrawing();
  
  public abstract int getX();
  
  public abstract void setX(int paramInt);
  
  public abstract int getY();
  
  public abstract void setY(int paramInt);
  
  public abstract int getWidth();
  
  public abstract int getHeight();
  
  public abstract int getMaxStringLength();
  
  public abstract void setMaxStringLength(int paramInt);
  
  public abstract String getText();
  
  public abstract void setText(String paramString);
  
  public abstract void mouseClicked(int paramInt1, int paramInt2, int paramInt3);
  
  public abstract boolean keyTyped(char paramChar, int paramInt);
  
  public abstract void tick();
  
  public abstract void draw();
}
