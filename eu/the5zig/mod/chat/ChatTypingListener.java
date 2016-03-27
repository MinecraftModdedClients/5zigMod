package eu.the5zig.mod.chat;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.ConversationChat;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketTyping;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.TabConversations;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.listener.Listener;
import eu.the5zig.mod.manager.ChatTypingManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.UUID;
import org.lwjgl.opengl.Display;

public class ChatTypingListener
  extends Listener
{
  public void onTick()
  {
    ChatTypingManager manager = The5zigMod.getDataManager().getChatTypingManager();
    if (!The5zigMod.getNetworkManager().isConnected())
    {
      unType();
      return;
    }
    if ((!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) || (!(((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)))
    {
      unType();
      return;
    }
    TabConversations gui = (TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
    if ((!(gui.getSelectedConversation() instanceof ConversationChat)) || ((manager.getTypingTo() != null) && (!((ConversationChat)gui.getSelectedConversation()).getFriendUUID().equals(manager
      .getTypingTo()))))
    {
      unType();
      return;
    }
    if (!Display.isActive())
    {
      unType();
      return;
    }
    ConversationChat conversation = (ConversationChat)gui.getSelectedConversation();
    boolean isTextfieldEmpty = gui.getTextfieldById(300).getText().isEmpty();
    if ((manager.getTypingTo() == null) && (!isTextfieldEmpty)) {
      type(conversation.getFriendUUID());
    } else if ((manager.getTypingTo() != null) && (isTextfieldEmpty)) {
      unType();
    }
  }
  
  private void unType()
  {
    ChatTypingManager manager = The5zigMod.getDataManager().getChatTypingManager();
    if (manager.getTypingTo() != null)
    {
      The5zigMod.getNetworkManager().sendPacket(new PacketTyping(manager.getTypingTo(), false), new GenericFutureListener[0]);
      manager.setTypingTo(null);
    }
  }
  
  private void type(UUID friend)
  {
    ChatTypingManager manager = The5zigMod.getDataManager().getChatTypingManager();
    if (manager.getTypingTo() == null)
    {
      manager.setTypingTo(friend);
      The5zigMod.getNetworkManager().sendPacket(new PacketTyping(manager.getTypingTo(), true), new GenericFutureListener[0]);
    }
  }
}
