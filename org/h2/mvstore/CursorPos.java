package org.h2.mvstore;

public class CursorPos
{
  public Page page;
  public int index;
  public final CursorPos parent;
  
  public CursorPos(Page paramPage, int paramInt, CursorPos paramCursorPos)
  {
    this.page = paramPage;
    this.index = paramInt;
    this.parent = paramCursorPos;
  }
}
