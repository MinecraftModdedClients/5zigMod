package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Group;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketDeleteGroupChat;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatStatus;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatStatus.GroupAction;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.UUID;

public class GuiGroupChatSettings
  extends GuiOptions
{
  private Group group;
  private int ADD_PLAYER;
  private int EDIT_NAME;
  private int DELETE;
  
  public GuiGroupChatSettings(Gui lastScreen, Group group)
  {
    super(lastScreen);
    this.group = group;
  }
  
  public void initGui()
  {
    super.initGui();
    this.ADD_PLAYER = addOptionButton(I18n.translate("group.settings.add_player"), new Callback()
    {
      public void call(IButton callback)
      {
        The5zigMod.getVars().displayScreen(new GuiCenteredTextfield(GuiGroupChatSettings.this, new CenteredTextfieldCallback()
        {
          public void onDone(String text)
          {
            The5zigMod.getNetworkManager().sendPacket(new PacketGroupChatStatus(GuiGroupChatSettings.this.group.getId(), PacketGroupChatStatus.GroupAction.ADD_PLAYER, text), new GenericFutureListener[0]);
          }
          
          public String title()
          {
            return I18n.translate("group.settings.confirm.add_player");
          }
        }, 1, 16));
      }
    });
    this.EDIT_NAME = addOptionButton(I18n.translate("group.settings.edit_name"), new Callback()
    {
      public void call(IButton callback)
      {
        The5zigMod.getVars().displayScreen(new GuiCenteredTextfield(GuiGroupChatSettings.this, new CenteredTextfieldCallback()
        {
          public void onDone(String text)
          {
            The5zigMod.getNetworkManager().sendPacket(new PacketGroupChatStatus(GuiGroupChatSettings.this.group.getId(), PacketGroupChatStatus.GroupAction.CHANGE_NAME, text), new GenericFutureListener[0]);
          }
          
          public String title()
          {
            return I18n.translate("group.settings.confirm.change_name");
          }
        }, 2, 50));
      }
    });
    this.DELETE = addOptionButton(I18n.translate("group.settings.delete"), new Callback()
    {
      public void call(IButton callback)
      {
        The5zigMod.getVars().displayScreen(new GuiYesNo(GuiGroupChatSettings.this, new YesNoCallback()
        {
          public void onDone(boolean yes)
          {
            if (yes)
            {
              The5zigMod.getNetworkManager().sendPacket(new PacketDeleteGroupChat(GuiGroupChatSettings.this.group.getId()), new GenericFutureListener[0]);
              The5zigMod.getVars().displayScreen(GuiGroupChatSettings.this.lastScreen.lastScreen);
            }
          }
          
          public String title()
          {
            return I18n.translate("group.settings.confirm.delete");
          }
        }));
      }
    });
  }
  
  protected void tick()
  {
    boolean isOwner = this.group.getOwner().getUniqueId().equals(The5zigMod.getDataManager().getUniqueId());
    boolean isAdmin = this.group.isAdmin(The5zigMod.getDataManager().getUniqueId());
    
    getButtonById(this.ADD_PLAYER).setEnabled((isAdmin) || (isOwner));
    getButtonById(this.EDIT_NAME).setEnabled((isAdmin) || (isOwner));
    getButtonById(this.DELETE).setEnabled(isOwner);
  }
  
  public String getTitleKey()
  {
    return "group.settings.title";
  }
}
