package eu.the5zig.mod.manager;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ChatFilter;
import eu.the5zig.mod.config.ChatFilter.Action;
import eu.the5zig.mod.config.ChatFilter.ChatFilterMessage;
import eu.the5zig.mod.config.ChatFilterConfiguration;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.gui.ingame.IGui2ndChat;
import eu.the5zig.mod.listener.Listener;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.TrayManager;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public class ChatFilterManager
  extends Listener
{
  public boolean onServerChat(String message, Object chatComponent)
  {
    message = ChatColor.stripColor(message);
    Server currentServer = The5zigMod.getDataManager().getServer();
    for (Iterator localIterator1 = ((ChatFilter)The5zigMod.getChatFilterConfig().getConfigInstance()).getChatMessages().iterator(); localIterator1.hasNext();)
    {
      chatMessage = (ChatFilter.ChatFilterMessage)localIterator1.next();
      List<Pattern> servers = chatMessage.getServerPatterns();
      if (servers.isEmpty())
      {
        if (tryIgnoreMessage(chatMessage, message, chatComponent)) {
          return true;
        }
      }
      else if (currentServer != null) {
        for (Pattern server : servers) {
          if (server.matcher(currentServer.getHost().toLowerCase()).matches()) {
            if (tryIgnoreMessage(chatMessage, message, chatComponent)) {
              return true;
            }
          }
        }
      }
    }
    ChatFilter.ChatFilterMessage chatMessage;
    return false;
  }
  
  private boolean tryIgnoreMessage(ChatFilter.ChatFilterMessage chatMessage, String message, Object chatComponent)
  {
    String toLower = message.toLowerCase();
    if (!chatMessage.getPattern().matcher(toLower).matches()) {
      return false;
    }
    if ((chatMessage.getExceptPattern() != null) && (chatMessage.getExceptPattern().matcher(toLower).find())) {
      return false;
    }
    if (chatMessage.getAction() == ChatFilter.Action.IGNORE)
    {
      The5zigMod.logger.debug("Ignored Chat Message {}!", new Object[] { message });
      return true;
    }
    if (chatMessage.getAction() == ChatFilter.Action.SECOND_CHAT)
    {
      if (!The5zigMod.getConfig().getBool("2ndChatVisible")) {
        return false;
      }
      The5zigMod.getVars().get2ndChat().printChatMessage(chatComponent);
      return true;
    }
    if (chatMessage.getAction() == ChatFilter.Action.NOTIFY)
    {
      if (Display.isActive()) {
        return false;
      }
      The5zigMod.getTrayManager().displayMessage("The 5zig Mod - " + I18n.translate("ingame_chat.new_message"), message);
      return false;
    }
    return true;
  }
}
