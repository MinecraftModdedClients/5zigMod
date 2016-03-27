package eu.the5zig.mod.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.listener.IListener;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.CommandIgnoreResult;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import eu.the5zig.util.ExtendedCallback;
import eu.the5zig.util.io.PropertyLoader;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.logging.log4j.Logger;

public class ServerListener
  implements IListener
{
  protected final ServerInstance serverInstance;
  private final Class<? extends Server> server;
  private final List<GameListener> listeners = Lists.newArrayList();
  private final HashMap<String, Pattern> messages = Maps.newHashMap();
  private CommandIgnoreResult ignoreResult;
  private List<MultiLineIgnore> multiLineIgnores = Lists.newArrayList();
  
  public ServerListener(ServerInstance serverInstance)
  {
    this.serverInstance = serverInstance;
    this.server = serverInstance.getServer();
    this.ignoreResult = new CommandIgnoreResult();
    loadPatterns("core/messages/" + serverInstance.getConfigName() + ".properties");
  }
  
  protected void loadPatterns(String path)
  {
    if (!this.messages.isEmpty())
    {
      The5zigMod.logger.debug("Messages Property Map has been already filled with values. {}", new Object[] { this.messages });
      return;
    }
    The5zigMod.logger.debug("Loading Patterns from {}...", new Object[] { path });
    Properties properties = PropertyLoader.load(path);
    if (properties == null)
    {
      The5zigMod.logger.error("Could not load Messages from {}", new Object[] { path });
      return;
    }
    Set<Map.Entry<Object, Object>> enumeration = properties.entrySet();
    for (Map.Entry<Object, Object> entry : enumeration) {
      try
      {
        String regex = String.valueOf(entry.getValue());
        regex = regex.replace("%p", "\\w{1,16}");
        regex = regex.replace("%d", "-?[0-9]+");
        this.messages.put(String.valueOf(entry.getKey()), Pattern.compile(regex));
      }
      catch (PatternSyntaxException e)
      {
        The5zigMod.logger.error("Could not parse Pattern in " + path + "!", e);
      }
    }
  }
  
  public void registerListener(GameListener listener)
  {
    if (this.listeners.contains(listener)) {
      return;
    }
    this.listeners.add(listener);
  }
  
  public List<String> match(String message)
  {
    for (String key : this.messages.keySet())
    {
      List<String> match = match(message, key);
      if (match != null)
      {
        match.add(0, key);
        return match;
      }
    }
    return Lists.newArrayList();
  }
  
  public List<String> match(String message, String key)
  {
    if (!this.messages.containsKey(key)) {
      return null;
    }
    Pattern pattern = (Pattern)this.messages.get(key);
    Matcher matcher = pattern.matcher(message);
    if (matcher.matches())
    {
      List<String> matches = Lists.newArrayList();
      for (int i = 1; i <= matcher.groupCount(); i++) {
        matches.add(matcher.group(i));
      }
      The5zigMod.logger.debug("Pattern matched with {}", new Object[] { matches });
      return matches;
    }
    return null;
  }
  
  protected void onMatch(String key, PatternResult match) {}
  
  public Class<? extends Server> getServer()
  {
    return this.server;
  }
  
  public boolean isCurrentServerInstance()
  {
    return (The5zigMod.getDataManager().getServer() != null) && (getServer().isAssignableFrom(The5zigMod.getDataManager().getServer().getClass()));
  }
  
  public void onTick()
  {
    if (!isCurrentServerInstance()) {
      return;
    }
    if (!The5zigMod.getVars().isPlayerNull()) {
      executeAll(new Callback()
      {
        public void call(GameListener callback)
        {
          callback.onTick();
        }
      });
    }
  }
  
  public void onKeyPress(final int code)
  {
    if (!isCurrentServerInstance()) {
      return;
    }
    executeAll(new Callback()
    {
      public void call(GameListener callback)
      {
        callback.onKeyPress(code);
      }
    });
  }
  
  public void onServerJoin(String host, int port)
  {
    for (GameListener listener : this.listeners) {
      listener.onServerJoin(host, port);
    }
  }
  
  public void onServerConnect()
  {
    if (!isCurrentServerInstance()) {
      return;
    }
    executeAll(new Callback()
    {
      public void call(GameListener callback)
      {
        callback.onServerConnect();
      }
    });
  }
  
  public void onServerDisconnect()
  {
    if (!isCurrentServerInstance()) {
      return;
    }
    executeAll(new Callback()
    {
      public void call(GameListener callback)
      {
        callback.onServerDisconnect();
      }
    });
  }
  
  public void onPayloadReceive(final String channel, final ByteBuf packetData)
  {
    if (!isCurrentServerInstance()) {
      return;
    }
    executeAll(new Callback()
    {
      public void call(GameListener callback)
      {
        callback.onPayloadReceive(channel, packetData);
      }
    });
  }
  
  public boolean onServerChat(final String message)
  {
    if (!isCurrentServerInstance()) {
      return false;
    }
    boolean ignore = executeAllBool(new ExtendedCallback()
    {
      public Boolean get(GameListener<?> key)
      {
        return Boolean.valueOf(key.onServerChat(message));
      }
    });
    tryMatch(message);
    if (tryMultiIgnores(message)) {
      ignore = true;
    }
    if (this.ignoreResult.handle(message)) {
      ignore = true;
    }
    return ignore;
  }
  
  public boolean onServerChat(final String message, final Object chatComponent)
  {
    (isCurrentServerInstance()) && (executeAllBool(new ExtendedCallback()
    {
      public Boolean get(GameListener<?> key)
      {
        return Boolean.valueOf(key.onServerChat(message, chatComponent));
      }
    }));
  }
  
  public boolean onActionBar(final String message)
  {
    if (!isCurrentServerInstance()) {
      return false;
    }
    boolean ignore = executeAllBool(new ExtendedCallback()
    {
      public Boolean get(GameListener<?> key)
      {
        return Boolean.valueOf(key.onActionBar(message));
      }
    });
    tryMatch(message);
    return ignore;
  }
  
  public void onPlayerListHeaderFooter(final String header, final String footer)
  {
    if (!isCurrentServerInstance()) {
      return;
    }
    executeAll(new Callback()
    {
      public void call(GameListener callback)
      {
        callback.onPlayerListHeaderFooter(header, footer);
      }
    });
  }
  
  public void onTitle(final String title, final String subTitle)
  {
    if (!isCurrentServerInstance()) {
      return;
    }
    executeAll(new Callback()
    {
      public void call(GameListener callback)
      {
        callback.onTitle(title, subTitle);
      }
    });
  }
  
  protected void onGameModeJoin()
  {
    executeAll(new Callback()
    {
      public void call(GameListener<?> callback)
      {
        if (callback.getGameModeClass().isAssignableFrom(((GameServer)The5zigMod.getDataManager().getServer()).getGameMode().getClass())) {
          callback.onGameModeJoin();
        }
      }
    });
  }
  
  private void tryMatch(String message)
  {
    List<String> match = match(ChatColor.stripColor(message));
    if (match.isEmpty()) {
      return;
    }
    final String key = (String)match.get(0);
    match.remove(0);
    final PatternResult patternResult = new PatternResult(match);
    onMatch(key, patternResult);
    executeAll(new Callback()
    {
      public void call(GameListener callback)
      {
        callback.onMatch(key, patternResult);
      }
    });
  }
  
  private void executeAll(Callback<GameListener<?>> callback)
  {
    for (GameListener<?> listener : this.listeners) {
      if ((this.serverInstance.getServer().isAssignableFrom(The5zigMod.getDataManager().getServer().getClass())) && 
        (((GameServer)The5zigMod.getDataManager().getServer()).getGameMode() != null) && (listener.getServer().isAssignableFrom(
        ((GameServer)The5zigMod.getDataManager().getServer()).getGameMode().getClass()))) {
        try
        {
          callback.call(listener);
        }
        catch (Throwable throwable)
        {
          The5zigMod.logger.error("Could not call GameListener " + listener + "!", throwable);
        }
      }
    }
  }
  
  private boolean executeAllBool(ExtendedCallback<GameListener<?>, Boolean> callback)
  {
    boolean result = false;
    for (GameListener<?> listener : this.listeners) {
      if ((this.serverInstance.getServer().isAssignableFrom(The5zigMod.getDataManager().getServer().getClass())) && 
        (((GameServer)The5zigMod.getDataManager().getServer()).getGameMode() != null) && (listener.getServer().isAssignableFrom(
        ((GameServer)The5zigMod.getDataManager().getServer()).getGameMode().getClass()))) {
        try
        {
          if (((Boolean)callback.get(listener)).booleanValue()) {
            result = true;
          }
        }
        catch (Throwable throwable)
        {
          The5zigMod.logger.error("Could not call GameListener " + listener + "!", throwable);
        }
      }
    }
    return result;
  }
  
  public void sendAndIgnore(String message, String key)
  {
    this.ignoreResult.send(message, (Pattern)this.messages.get(key));
  }
  
  public void sendAndIgnoreMultiple(String message, String start, String end, Callback<MultiPatternResult> callback)
  {
    this.multiLineIgnores.add(new MultiLineIgnore(start, end, callback));
    The5zigMod.getVars().sendMessage(message);
  }
  
  private boolean tryMultiIgnores(String message)
  {
    boolean ignore = false;
    
    message = ChatColor.stripColor(message);
    for (Iterator<MultiLineIgnore> iterator = this.multiLineIgnores.iterator(); iterator.hasNext();)
    {
      MultiLineIgnore multiLineIgnore = (MultiLineIgnore)iterator.next();
      if ((!multiLineIgnore.hasStartedListening()) && (message.equalsIgnoreCase(multiLineIgnore.getStartMessage())))
      {
        multiLineIgnore.setStartedListening(true);
        multiLineIgnore.add(message);
        ignore = true;
      }
      else if (multiLineIgnore.hasStartedListening())
      {
        multiLineIgnore.add(message);
        ignore = true;
        if (message.equalsIgnoreCase(multiLineIgnore.getEndMessage()))
        {
          iterator.remove();
          try
          {
            multiLineIgnore.callCallback();
          }
          catch (Throwable throwable)
          {
            The5zigMod.logger.error("Could not call multi-line-ignore-callback", throwable);
          }
        }
      }
    }
    return ignore;
  }
}
