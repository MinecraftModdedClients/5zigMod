package eu.the5zig.mod.server;

import java.util.List;

public class MultiPatternResult
{
  private List<String> messages;
  
  public MultiPatternResult(List<String> messages)
  {
    this.messages = messages;
  }
  
  public PatternResult parseKey(ServerListener serverListener, String key)
  {
    for (String message : this.messages)
    {
      List<String> match = serverListener.match(message, key);
      if ((match != null) && (!match.isEmpty())) {
        return new PatternResult(match);
      }
    }
    return null;
  }
}
