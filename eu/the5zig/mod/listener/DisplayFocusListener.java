package eu.the5zig.mod.listener;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.entity.Conversation;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.TabConversations;
import eu.the5zig.mod.util.IVariables;
import org.lwjgl.opengl.Display;

public class DisplayFocusListener
  extends Listener
{
  private boolean waitForFocus;
  
  public void onTick()
  {
    if ((this.waitForFocus) && (Display.isActive()))
    {
      if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)))
      {
        TabConversations gui = (TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
        Conversation conversation = gui.getSelectedConversation();
        if (conversation != null) {
          The5zigMod.getConversationManager().setConversationRead(conversation, true);
        }
      }
      this.waitForFocus = false;
    }
    else if ((!this.waitForFocus) && (!Display.isActive()))
    {
      this.waitForFocus = true;
    }
  }
}
