package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.GroupMember;
import eu.the5zig.mod.chat.entity.Group;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatStatus;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatStatus.GroupAction;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class GuiGroupChatInfo
  extends Gui
{
  private Group group;
  private IGuiList<GroupMember> groupMemberList;
  private int lastSelected;
  
  public GuiGroupChatInfo(Gui lastScreen, Group group)
  {
    super(lastScreen);
    this.group = group;
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    List split = The5zigMod.getVars().splitStringToWidth(I18n.translate("group.info.title", new Object[] { this.group.getName(), this.group.getOwner().getUsername() }), getWidth() / 3 * 2);
    int y = 5;
    for (Iterator localIterator = split.iterator(); localIterator.hasNext(); drawCenteredString(String.valueOf(o), getWidth() / 2, y))
    {
      Object o = localIterator.next();
      y += 10;
    }
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, 8, 6, 50, 20, I18n.translate("gui.back")));
    
    this.groupMemberList = The5zigMod.getVars().createGuiList(new Clickable()
    {
      public void onSelect(int id, GroupMember row, boolean doubleClick)
      {
        GuiGroupChatInfo.this.lastSelected = GuiGroupChatInfo.this.group.getMembers().indexOf(row);
      }
    }, getWidth(), getHeight(), 50, getHeight() - 50, 0, getWidth(), this.group.getMembers());
    this.groupMemberList.setRowWidth(200);
    this.groupMemberList.setScrollX(getWidth() - 15);
    addGuiList(this.groupMemberList);
    
    this.groupMemberList.setSelectedId(this.lastSelected);
    GroupMember selected = (GroupMember)this.groupMemberList.getSelectedRow();
    this.groupMemberList.onSelect(this.group.getMembers().indexOf(selected), this.groupMemberList.getSelectedRow(), false);
    
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 202, getHeight() - 38, 98, 20, I18n.translate("group.info.kick_player")));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 - 100, getHeight() - 38, 98, 20, I18n.translate("group.info.admin.set")));
    addButton(The5zigMod.getVars().createButton(3, getWidth() / 2 + 2, getHeight() - 38, 98, 20, I18n.translate("group.info.transfer_owner")));
    addButton(The5zigMod.getVars().createButton(4, getWidth() / 2 + 104, getHeight() - 38, 98, 20, I18n.translate("group.info.settings")));
  }
  
  protected void tick()
  {
    GroupMember selectedRow = (GroupMember)this.groupMemberList.getSelectedRow();
    boolean enableAdmin = this.group.isAdmin(The5zigMod.getDataManager().getUniqueId());
    boolean enableOwner = this.group.getOwner().getUniqueId().equals(The5zigMod.getDataManager().getUniqueId());
    boolean isNotSelf = (selectedRow != null) && (!selectedRow.getUniqueId().equals(The5zigMod.getDataManager().getUniqueId()));
    
    getButtonById(1).setEnabled((enableAdmin) && (isNotSelf) && (selectedRow.getType() == 0));
    getButtonById(2).setEnabled((isNotSelf) && (enableOwner));
    getButtonById(2).setLabel((selectedRow != null) && ((selectedRow.isAdmin()) || (selectedRow.getType() == 2)) ? I18n.translate("group.info.admin.remove") : 
      I18n.translate("group.info.admin.set"));
    getButtonById(3).setEnabled((isNotSelf) && (enableOwner));
    getButtonById(4).setEnabled((enableAdmin) || (enableOwner));
  }
  
  public String getTitleName()
  {
    return "";
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 4) {
      The5zigMod.getVars().displayScreen(new GuiGroupChatSettings(this, this.group));
    }
    final GroupMember selectedRow = (GroupMember)this.groupMemberList.getSelectedRow();
    if (selectedRow == null) {
      return;
    }
    if (button.getId() == 1) {
      The5zigMod.getNetworkManager().sendPacket(new PacketGroupChatStatus(this.group.getId(), PacketGroupChatStatus.GroupAction.REMOVE_PLAYER, selectedRow.getUniqueId()), new GenericFutureListener[0]);
    }
    if (button.getId() == 2) {
      The5zigMod.getNetworkManager().sendPacket(new PacketGroupChatStatus(this.group.getId(), PacketGroupChatStatus.GroupAction.ADMIN, selectedRow.getUniqueId(), !selectedRow.isAdmin()), new GenericFutureListener[0]);
    }
    if (button.getId() == 3) {
      The5zigMod.getVars().displayScreen(new GuiYesNo(this, new YesNoCallback()
      {
        public void onDone(boolean yes)
        {
          if (yes)
          {
            The5zigMod.getNetworkManager().sendPacket(new PacketGroupChatStatus(GuiGroupChatInfo.this.group.getId(), PacketGroupChatStatus.GroupAction.OWNER, selectedRow.getUniqueId()), new GenericFutureListener[0]);
            The5zigMod.getVars().displayScreen(GuiGroupChatInfo.this.lastScreen);
          }
        }
        
        public String title()
        {
          return I18n.translate("group.info.confirm.owner");
        }
      }));
    }
  }
}
