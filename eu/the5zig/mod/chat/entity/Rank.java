package eu.the5zig.mod.chat.entity;

public enum Rank
{
  NONE('r'),  DEFAULT('a'),  CUSTOM('6'),  SPECIAL('5');
  
  private final char colorCode;
  
  private Rank(char colorCode)
  {
    this.colorCode = colorCode;
  }
  
  public String getColorCode()
  {
    return new String(new char[] { '§', this.colorCode });
  }
}
