package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.util.IVariables;
import java.util.List;

public class GuiFriendRequests
  extends Gui
{
  private IGuiList friendRequestList;
  private int selected = 0;
  
  public GuiFriendRequests(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, 8, 6, 50, 20, I18n.translate("gui.back")));
    
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 152, getHeight() - 38, 150, 20, I18n.translate("friend_requests.accept")));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 + 2, getHeight() - 38, 150, 20, I18n.translate("friend_requests.deny")));
    
    initRowList();
  }
  
  protected void initRowList()
  {
    this.friendRequestList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 50, getHeight() - 50, 0, getWidth(), The5zigMod.getFriendManager().getFriendRequests());
    this.friendRequestList.setRowWidth(200);
    this.friendRequestList.setScrollX(getWidth() - 15);
    addGuiList(this.friendRequestList);
    if (this.selected < 0) {
      this.selected = 0;
    }
    this.friendRequestList.setSelectedId(this.selected);
  }
  
  private void enableDisableButtons()
  {
    boolean enabled = !The5zigMod.getFriendManager().getFriendRequests().isEmpty();
    
    getButtonById(1).setEnabled(enabled);
    getButtonById(2).setEnabled(enabled);
  }
  
  protected void tick()
  {
    enableDisableButtons();
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    if (The5zigMod.getFriendManager().getFriendRequests().isEmpty()) {
      drawCenteredString(I18n.translate("friend_requests.none"), getWidth() / 2, getHeight() / 2 - 20);
    }
  }
  
  protected void actionPerformed(IButton button)
  {
    if ((button.getId() == 1) || (button.getId() == 2))
    {
      User selectedRow = (User)this.friendRequestList.getSelectedRow();
      if (selectedRow == null) {
        return;
      }
      The5zigMod.getFriendManager().handleFriendRequestResponse(selectedRow.getUniqueId(), button.getId() == 1);
      enableDisableButtons();
    }
  }
  
  public String getTitleKey()
  {
    return "friend_requests.title";
  }
}
