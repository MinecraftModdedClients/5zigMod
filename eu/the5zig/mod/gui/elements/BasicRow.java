package eu.the5zig.mod.gui.elements;

import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callable;
import java.util.List;

public class BasicRow
  implements Row
{
  protected int HEIGHT = 14;
  protected int LINE_HEIGHT = 11;
  protected String string;
  protected int maxWidth;
  private Callable<String> callback;
  
  public BasicRow(String string)
  {
    this.string = string;
    this.maxWidth = 95;
  }
  
  public BasicRow(String string, int maxWidth)
  {
    this.string = string;
    this.maxWidth = maxWidth;
  }
  
  public BasicRow(String string, int maxWidth, int height)
  {
    this.string = string;
    this.maxWidth = maxWidth;
    this.HEIGHT = height;
  }
  
  public BasicRow(Callable<String> callback, int maxWidth)
  {
    this.callback = callback;
    this.maxWidth = maxWidth;
  }
  
  public String getString()
  {
    return this.string;
  }
  
  public int getLineHeight()
  {
    String toDraw = this.callback != null ? (String)this.callback.call() : this.string;
    return (MinecraftFactory.getVars().splitStringToWidth(toDraw, this.maxWidth).size() - 1) * this.LINE_HEIGHT + this.HEIGHT;
  }
  
  public void draw(int x, int y)
  {
    x += 2;
    y += 2;
    String toDraw = this.callback != null ? (String)this.callback.call() : this.string;
    List<String> split = MinecraftFactory.getVars().splitStringToWidth(toDraw, this.maxWidth);
    int i = 0;
    for (int splitStringToWidthSize = split.size(); i < splitStringToWidthSize; i++)
    {
      String line = (String)split.get(i);
      MinecraftFactory.getVars().drawString(line, x, y);
      if (i < splitStringToWidthSize - 1) {
        y += this.LINE_HEIGHT;
      } else {
        y += this.HEIGHT;
      }
    }
  }
}
