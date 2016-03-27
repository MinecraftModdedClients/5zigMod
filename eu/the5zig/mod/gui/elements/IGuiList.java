package eu.the5zig.mod.gui.elements;

import java.util.List;

public abstract interface IGuiList<E extends Row>
{
  public abstract void drawScreen(int paramInt1, int paramInt2, float paramFloat);
  
  public abstract void handleMouseInput();
  
  public abstract void onSelect(int paramInt, E paramE, boolean paramBoolean);
  
  public abstract void mouseClicked(int paramInt1, int paramInt2);
  
  public abstract void mouseReleased(int paramInt1, int paramInt2, int paramInt3);
  
  public abstract void scrollToBottom();
  
  public abstract float getCurrentScroll();
  
  public abstract void scrollTo(float paramFloat);
  
  public abstract boolean isSelected(int paramInt);
  
  public abstract int getContentHeight();
  
  public abstract int getRowWidth();
  
  public abstract void setRowWidth(int paramInt);
  
  public abstract int getSelectedId();
  
  public abstract int setSelectedId(int paramInt);
  
  public abstract E getSelectedRow();
  
  public abstract int getWidth();
  
  public abstract void setWidth(int paramInt);
  
  public abstract int getHeight();
  
  public abstract void setHeight(int paramInt);
  
  public abstract int getHeight(int paramInt);
  
  public abstract int getTop();
  
  public abstract void setTop(int paramInt);
  
  public abstract int getBottom();
  
  public abstract void setBottom(int paramInt);
  
  public abstract int getLeft();
  
  public abstract void setLeft(int paramInt);
  
  public abstract int getRight();
  
  public abstract void setRight(int paramInt);
  
  public abstract int getScrollX();
  
  public abstract void setScrollX(int paramInt);
  
  public abstract boolean isLeftbound();
  
  public abstract void setLeftbound(boolean paramBoolean);
  
  public abstract boolean isDrawSelection();
  
  public abstract void setDrawSelection(boolean paramBoolean);
  
  public abstract int getPadding();
  
  public abstract int getHeaderPadding();
  
  public abstract void setHeaderPadding(int paramInt);
  
  public abstract String getHeader();
  
  public abstract void setHeader(String paramString);
  
  public abstract int getBottomPadding();
  
  public abstract void setBottomPadding(int paramInt);
  
  public abstract List<E> getRows();
  
  public abstract void calculateHeightMap();
  
  public abstract E getHoverItem(int paramInt1, int paramInt2);
}
