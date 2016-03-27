package eu.the5zig.mod.gui.ingame;

import java.util.HashMap;

public class Scoreboard
{
  private final String title;
  private final HashMap<Integer, String> lines;
  
  public Scoreboard(String title, HashMap<Integer, String> lines)
  {
    this.title = title;
    this.lines = lines;
  }
  
  public String getTitle()
  {
    return this.title;
  }
  
  public HashMap<Integer, String> getLines()
  {
    return this.lines;
  }
}
