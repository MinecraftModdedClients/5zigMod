package eu.the5zig.mod.gui.elements;

public abstract interface RowExtended
  extends Row
{
  public abstract void draw(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5);
  
  public abstract IButton mousePressed(int paramInt1, int paramInt2);
}
