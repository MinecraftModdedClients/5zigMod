package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.entity.Conversation;
import eu.the5zig.mod.chat.entity.Conversation.Behaviour;
import eu.the5zig.mod.chat.entity.ConversationGroupChat;
import eu.the5zig.mod.chat.entity.Group;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketLeaveGroupChat;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.UUID;

public class GuiConversationSettings
  extends GuiOptions
{
  private Conversation conversation;
  private int BEHAVIOUR;
  private int DELETE;
  
  public GuiConversationSettings(Gui lastScreen, Conversation conversation)
  {
    super(lastScreen);
    this.conversation = conversation;
  }
  
  public void initGui()
  {
    super.initGui();
    
    this.BEHAVIOUR = addOptionButton(I18n.translate("chat.conversation_settings.behaviour", new Object[] { this.conversation.getBehaviour().getName() }), new Callback()
    {
      public void call(IButton button)
      {
        Conversation.Behaviour next = GuiConversationSettings.this.conversation.getBehaviour().getNext();
        The5zigMod.getConversationManager().setBehaviour(GuiConversationSettings.this.conversation, next);
        button.setLabel(I18n.translate("chat.conversation_settings.behaviour", new Object[] { next.getName() }));
      }
    });
    this.DELETE = addOptionButton(I18n.translate("chat.conversation_settings.delete"), new Callback()
    {
      public void call(IButton button)
      {
        The5zigMod.getVars().displayScreen(GuiConversationSettings.this.lastScreen);
        if ((GuiConversationSettings.this.conversation instanceof ConversationGroupChat))
        {
          if (!The5zigMod.getNetworkManager().isConnected()) {
            return;
          }
          ConversationGroupChat c = (ConversationGroupChat)GuiConversationSettings.this.conversation;
          Group group = The5zigMod.getGroupChatManager().getGroup(c.getGroupId());
          if (group != null)
          {
            if (!group.getOwner().getUniqueId().equals(The5zigMod.getDataManager().getUniqueId())) {
              The5zigMod.getNetworkManager().sendPacket(new PacketLeaveGroupChat(c.getGroupId()), new GenericFutureListener[0]);
            } else {
              The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.RED + I18n.translate("chat.conversation_settings.transfer_ownership"));
            }
          }
          else {
            The5zigMod.getConversationManager().deleteConversation(c);
          }
        }
        else
        {
          The5zigMod.getConversationManager().deleteConversation(GuiConversationSettings.this.conversation);
        }
      }
    });
  }
  
  protected void tick()
  {
    getButtonById(this.BEHAVIOUR).setEnabled((!(this.conversation instanceof ConversationGroupChat)) || 
      (The5zigMod.getGroupChatManager().getGroup(((ConversationGroupChat)this.conversation).getGroupId()) != null));
    getButtonById(this.DELETE).setLabel(((this.conversation instanceof ConversationGroupChat)) && 
      (The5zigMod.getGroupChatManager().getGroup(((ConversationGroupChat)this.conversation).getGroupId()) != null) ? 
      I18n.translate("chat.conversation_settings.leave") : I18n.translate("chat.conversation_settings.delete"));
  }
  
  public String getTitleKey()
  {
    return "chat.conversation_settings.title";
  }
}
