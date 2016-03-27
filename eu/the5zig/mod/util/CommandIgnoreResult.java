package eu.the5zig.mod.util;

import com.google.common.collect.Lists;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;

public class CommandIgnoreResult
{
  private List<Result> messagesToIgnore = Lists.newArrayList();
  
  public boolean handle(String message)
  {
    if (this.messagesToIgnore.isEmpty()) {
      return false;
    }
    message = ChatColor.stripColor(message);
    if (((Result)this.messagesToIgnore.get(0)).getResult().matcher(message).matches())
    {
      The5zigMod.logger.debug("Ignored Chat Message {}!", new Object[] { message });
      this.messagesToIgnore.remove(0);
      return true;
    }
    return false;
  }
  
  public void send(String command, String ignorePattern)
  {
    send(command, Pattern.compile(ignorePattern));
  }
  
  public void send(String command, Pattern ignorePattern)
  {
    if (The5zigMod.getVars().isPlayerNull())
    {
      The5zigMod.logger.warn("Could not send command " + command);
      return;
    }
    The5zigMod.getVars().sendMessage(command);
    this.messagesToIgnore.add(new Result(ignorePattern));
  }
  
  public class Result
  {
    private final Pattern result;
    private final long time;
    
    public Result(Pattern result)
    {
      this.result = result;
      this.time = (System.currentTimeMillis() + 10000L);
    }
    
    public Pattern getResult()
    {
      return this.result;
    }
    
    public long getTime()
    {
      return this.time;
    }
    
    public String toString()
    {
      return this.result.pattern();
    }
  }
}
