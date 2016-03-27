package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketCreateGroupChat;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GuiCreateGroupChatSelectFriends
  extends Gui
{
  private String name;
  private IGuiList guiListAllFriends;
  private IGuiList guiListInvitedFriends;
  private List<Friend> allFriends = Lists.newArrayList(The5zigMod.getFriendManager().getFriends());
  private List<Friend> invitedFriends = Lists.newArrayList();
  
  public GuiCreateGroupChatSelectFriends(Gui lastScreen, String name)
  {
    super(lastScreen);
    this.name = name;
  }
  
  public void initGui()
  {
    this.guiListAllFriends = The5zigMod.getVars().createGuiList(null, getWidth() / 2, getHeight(), 48, getHeight() - 64, getWidth() / 2 - 130, getWidth() / 2 - 30, this.allFriends);
    this.guiListAllFriends.setLeftbound(true);
    this.guiListAllFriends.setScrollX(getWidth() / 2 - 35);
    this.guiListInvitedFriends = The5zigMod.getVars().createGuiList(null, getWidth() / 2, getHeight(), 48, getHeight() - 64, getWidth() / 2 + 30, getWidth() / 2 + 130, this.invitedFriends);
    this.guiListInvitedFriends.setLeftbound(true);
    this.guiListInvitedFriends.setScrollX(getWidth() / 2 + 125);
    addGuiList(this.guiListAllFriends);
    addGuiList(this.guiListInvitedFriends);
    
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 - 155, getHeight() - 30, 150, 20, I18n.translate("gui.back")));
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 + 5, getHeight() - 30, 150, 20, I18n.translate("group.invite.create"), false));
    
    addButton(The5zigMod.getVars().createButton(5, getWidth() / 2 - 20, getHeight() / 2 - 40, 40, 20, ">>"));
    addButton(The5zigMod.getVars().createButton(6, getWidth() / 2 - 20, getHeight() / 2 - 15, 40, 20, "<<"));
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    The5zigMod.getVars().drawString(I18n.translate("group.invite.friends.all", new Object[] { Integer.valueOf(this.allFriends.size()) }), getWidth() / 2 - 130, 36);
    The5zigMod.getVars().drawString(I18n.translate("group.invite.friends.invited", new Object[] {
      (this.invitedFriends.size() >= 49 ? ChatColor.YELLOW.toString() : ChatColor.RESET.toString()) + this.invitedFriends.size() + ChatColor.RESET }), getWidth() / 2 + 30, 36);
  }
  
  protected void tick()
  {
    getButtonById(1).setEnabled(!this.invitedFriends.isEmpty());
    
    getButtonById(5).setEnabled((this.guiListAllFriends.getSelectedRow() != null) && (this.invitedFriends.size() < 49));
    getButtonById(6).setEnabled(this.guiListInvitedFriends.getSelectedRow() != null);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      if (this.invitedFriends.isEmpty()) {
        return;
      }
      List<UUID> players = Lists.newArrayList();
      for (Friend invitedFriend : this.invitedFriends) {
        players.add(invitedFriend.getUniqueId());
      }
      The5zigMod.getNetworkManager().sendPacket(new PacketCreateGroupChat(players, this.name), new GenericFutureListener[0]);
      The5zigMod.getVars().displayScreen(this.lastScreen.lastScreen);
    }
    if (button.getId() == 5)
    {
      Friend selectedRow = (Friend)this.guiListAllFriends.getSelectedRow();
      if (selectedRow != null)
      {
        this.allFriends.remove(selectedRow);
        this.invitedFriends.add(selectedRow);
        Collections.sort(this.allFriends);
        Collections.sort(this.invitedFriends);
      }
    }
    if (button.getId() == 6)
    {
      Friend selectedRow = (Friend)this.guiListInvitedFriends.getSelectedRow();
      if (selectedRow != null)
      {
        this.allFriends.add(selectedRow);
        this.invitedFriends.remove(selectedRow);
        Collections.sort(this.allFriends);
        Collections.sort(this.invitedFriends);
      }
    }
  }
  
  public String getTitleName()
  {
    return "The 5zig Mod - " + I18n.translate("group.invite.title", new Object[] { this.name });
  }
}
