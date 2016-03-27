package eu.the5zig.mod.server;

import java.util.List;

public class PatternResult
{
  private final List<String> result;
  
  public PatternResult(List<String> result)
  {
    this.result = result;
  }
  
  public int size()
  {
    return this.result.size();
  }
  
  public String get(int index)
  {
    if ((index < 0) || (index >= size())) {
      return "";
    }
    return (String)this.result.get(index);
  }
  
  public String toString()
  {
    return this.result.toString();
  }
}
