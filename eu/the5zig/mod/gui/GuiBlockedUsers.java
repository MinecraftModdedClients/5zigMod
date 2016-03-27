package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketDeleteBlockedUser;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.util.IVariables;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.List;

public class GuiBlockedUsers
  extends Gui
{
  private int selected = 0;
  private IGuiList guiList;
  
  public GuiBlockedUsers(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, 8, 6, 50, 20, I18n.translate("gui.back")));
    
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 100, getHeight() - 38, 200, 20, I18n.translate("blocked_users.unblock")));
    
    initRowList();
  }
  
  protected void initRowList()
  {
    this.guiList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 50, getHeight() - 50, 0, getWidth(), The5zigMod.getFriendManager().getBlockedUsers());
    this.guiList.setRowWidth(200);
    this.guiList.setScrollX(getWidth() - 15);
    addGuiList(this.guiList);
    if (this.selected < 0) {
      this.selected = 0;
    }
    this.guiList.setSelectedId(this.selected);
  }
  
  protected void tick()
  {
    getButtonById(1).setEnabled(!The5zigMod.getFriendManager().getBlockedUsers().isEmpty());
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    if (The5zigMod.getFriendManager().getBlockedUsers().isEmpty()) {
      drawCenteredString(I18n.translate("blocked_users.none"), getWidth() / 2, getHeight() / 2 - 20);
    }
  }
  
  public String getTitleKey()
  {
    return "blocked_users.title";
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      if ((this.selected < 0) || (this.selected >= The5zigMod.getFriendManager().getBlockedUsers().size())) {
        return;
      }
      User selectedUser = (User)this.guiList.getSelectedRow();
      The5zigMod.getFriendManager().removeBlockedUser(selectedUser.getUniqueId());
      The5zigMod.getNetworkManager().sendPacket(new PacketDeleteBlockedUser(selectedUser.getUniqueId()), new GenericFutureListener[0]);
    }
  }
}
