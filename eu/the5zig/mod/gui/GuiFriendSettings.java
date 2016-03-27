package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketAddBlockedUser;
import eu.the5zig.mod.chat.network.packets.PacketDeleteFriend;
import eu.the5zig.mod.chat.network.packets.PacketFriendStatus;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import io.netty.util.concurrent.GenericFutureListener;

public class GuiFriendSettings
  extends GuiOptions
{
  private Friend friend;
  
  public GuiFriendSettings(Gui lastScreen, Friend friend)
  {
    super(lastScreen);
    this.friend = friend;
  }
  
  public void initGui()
  {
    super.initGui();
    
    addOptionButton(!this.friend.isFavorite() ? I18n.translate("friend.settings.favorite") : I18n.translate("friend.settings.unfavorite"), new Callback()
    {
      public void call(IButton button)
      {
        button.setLabel(GuiFriendSettings.this.friend.isFavorite() ? I18n.translate("friend.settings.favorite") : I18n.translate("friend.settings.unfavorite"));
        button.setTicksDisabled(100);
        The5zigMod.getNetworkManager().sendPacket(new PacketFriendStatus(GuiFriendSettings.this.friend.getUniqueId(), !GuiFriendSettings.this.friend.isFavorite()), new GenericFutureListener[0]);
      }
    });
    addOptionButton(I18n.translate("friend.settings.delete"), new Callback()
    {
      public void call(IButton button)
      {
        The5zigMod.getNetworkManager().sendPacket(new PacketDeleteFriend(GuiFriendSettings.this.friend.getUniqueId()), new GenericFutureListener[0]);
        The5zigMod.getVars().displayScreen(GuiFriendSettings.this.lastScreen);
      }
    });
    addOptionButton(I18n.translate("friend.settings.block"), new Callback()
    {
      public void call(IButton button)
      {
        The5zigMod.getNetworkManager().sendPacket(new PacketAddBlockedUser(new User(GuiFriendSettings.this.friend.getName(), GuiFriendSettings.this.friend.getUniqueId())), new GenericFutureListener[0]);
        The5zigMod.getVars().displayScreen(GuiFriendSettings.this.lastScreen);
      }
    });
  }
  
  protected void tick()
  {
    if (this.friend == null) {
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
  }
  
  public String getTitleName()
  {
    return "The 5zig Mod - " + I18n.translate("friend.settings.title", new Object[] { this.friend.getName() });
  }
}
