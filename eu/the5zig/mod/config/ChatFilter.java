package eu.the5zig.mod.config;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;

public class ChatFilter
{
  private List<ChatFilterMessage> chatMessages = Lists.newArrayList();
  
  public ChatFilter()
  {
    this.chatMessages.add(new ChatFilterMessage("│ MSG» Du ➟ *: *", Action.SECOND_CHAT, new String[] { "*timolia.de" }));
    this.chatMessages.add(new ChatFilterMessage("│ MSG» * ➟ Du: *", Action.SECOND_CHAT, new String[] { "*timolia.de" }));
    this.chatMessages.add(new ChatFilterMessage("[EnderGames] Spieler * getrackt: * Blöcke", Action.IGNORE, new String[] { "*gommehd.net", "*gommehd.tk", "*gommehd.de" }));
    this.chatMessages.add(new ChatFilterMessage("[Freunde] Du -> *: *", Action.SECOND_CHAT, new String[] { "*gommehd.net", "*gommehd.tk", "*gommehd.de" }));
    this.chatMessages.add(new ChatFilterMessage("[Freunde] * -> Dir: *", Action.SECOND_CHAT, new String[] { "*gommehd.net", "*gommehd.tk", "*gommehd.de" }));
  }
  
  public List<ChatFilterMessage> getChatMessages()
  {
    return this.chatMessages;
  }
  
  public class ChatFilterMessage
    implements Row
  {
    private transient Pattern pattern;
    private transient Pattern exceptPattern;
    private transient List<Pattern> serverPatterns;
    private String message;
    private String except;
    private List<String> servers = Lists.newArrayList();
    private ChatFilter.Action action = ChatFilter.Action.IGNORE;
    private boolean useRegex = false;
    
    public ChatFilterMessage() {}
    
    public ChatFilterMessage(String message, ChatFilter.Action action, String... servers)
    {
      this(message, null, action, servers);
    }
    
    public ChatFilterMessage(String message, String except, ChatFilter.Action action, String... servers)
    {
      this(message, except, action, false, servers);
    }
    
    public ChatFilterMessage(String message, String except, ChatFilter.Action action, boolean useRegex, String... servers)
    {
      this.message = message;
      this.except = except;
      this.action = action;
      this.useRegex = useRegex;
      Collections.addAll(this.servers, servers);
      updatePatterns();
    }
    
    public Pattern getPattern()
    {
      return this.pattern;
    }
    
    public Pattern getExceptPattern()
    {
      return this.exceptPattern;
    }
    
    public List<Pattern> getServerPatterns()
    {
      return this.serverPatterns;
    }
    
    public String getMessage()
    {
      return this.message;
    }
    
    public void setMessage(String message)
    {
      this.message = message;
      try
      {
        if (useRegex()) {
          this.pattern = Pattern.compile(message);
        } else {
          this.pattern = Utils.compileMatchPattern(message);
        }
      }
      catch (Exception e)
      {
        The5zigMod.logger.error("Could not compile pattern: " + message + "!", e);
      }
    }
    
    public String getExcept()
    {
      return this.except;
    }
    
    public void setExcept(String except)
    {
      this.except = except;
      if ((except == null) || (except.isEmpty())) {
        this.exceptPattern = null;
      } else {
        try
        {
          String escapedString = Utils.escapeStringForRegex(except);
          String replacedString = escapedString.replace(", ", ",").replace(",", "|");
          this.exceptPattern = Pattern.compile(replacedString);
        }
        catch (Exception e)
        {
          The5zigMod.logger.error("Could not compile pattern: " + this.message + "!", e);
        }
      }
    }
    
    public void clearServers()
    {
      this.servers.clear();
    }
    
    public void addServer(String server)
    {
      this.servers.add(server);
      try
      {
        this.serverPatterns.add(Utils.compileMatchPattern(server));
      }
      catch (Exception e)
      {
        The5zigMod.logger.error("Could not compile pattern: " + server + "!", e);
      }
    }
    
    public String[] getServers()
    {
      return (String[])this.servers.toArray(new String[this.servers.size()]);
    }
    
    public ChatFilter.Action getAction()
    {
      return this.action;
    }
    
    public void setAction(ChatFilter.Action action)
    {
      this.action = action;
    }
    
    public boolean useRegex()
    {
      return this.useRegex;
    }
    
    public void setUseRegex(boolean useRegex)
    {
      this.useRegex = useRegex;
    }
    
    public void updatePatterns()
    {
      this.serverPatterns = Lists.newArrayList();
      List<String> servers = this.servers;
      clearServers();
      for (String server : servers) {
        addServer(server);
      }
      setMessage(this.message);
      setExcept(this.except);
    }
    
    public ChatFilterMessage clone()
    {
      ChatFilterMessage clone = new ChatFilterMessage(ChatFilter.this);
      clone.pattern = this.pattern;
      clone.exceptPattern = this.exceptPattern;
      clone.serverPatterns = this.serverPatterns;
      clone.message = this.message;
      clone.except = this.except;
      clone.servers = this.servers;
      clone.action = this.action;
      clone.useRegex = this.useRegex;
      return clone;
    }
    
    public int getLineHeight()
    {
      return 16;
    }
    
    public void draw(int x, int y)
    {
      Gui gui = The5zigMod.getVars().getCurrentScreen();
      
      int chatMessageX = x + 2;
      int maxChatMessageX = gui.getWidth() / 2 + 50;
      int serverX = maxChatMessageX + 20;
      int maxServerX = gui.getWidth() - 30;
      
      The5zigMod.getVars().drawString(The5zigMod.getVars().shortenToWidth(this.message, Math.max(1, maxChatMessageX - chatMessageX)), chatMessageX, y + 2);
      The5zigMod.getVars().drawString("|", maxChatMessageX + 10, y + 2);
      String s = Arrays.toString(this.servers.toArray());
      if (!s.isEmpty()) {
        s = s.substring(1, s.length() - 1);
      }
      The5zigMod.getVars().drawString(The5zigMod.getVars().shortenToWidth(s, Math.max(1, maxServerX - serverX)), serverX, y + 2);
    }
  }
  
  public static enum Action
  {
    IGNORE("chat_filter.edit.action.ignore"),  SECOND_CHAT("chat_filter.edit.action.2nd_chat"),  NOTIFY("chat_filter.edit.action.notify");
    
    private String key;
    
    private Action(String key)
    {
      this.key = key;
    }
    
    public String getName()
    {
      return I18n.translate(this.key);
    }
    
    public Action getNext()
    {
      return values()[((ordinal() + 1) % values().length)];
    }
  }
}
