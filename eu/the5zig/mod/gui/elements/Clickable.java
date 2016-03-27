package eu.the5zig.mod.gui.elements;

public abstract interface Clickable<E extends Row>
{
  public abstract void onSelect(int paramInt, E paramE, boolean paramBoolean);
}
