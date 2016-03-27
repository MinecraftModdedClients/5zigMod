package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.Conversation;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendRequest;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.SearchEntry;
import eu.the5zig.mod.manager.SearchManager;
import eu.the5zig.mod.manager.SkinManager;
import eu.the5zig.mod.render.Base64Renderer;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TabFriends
  extends Tab
  implements Clickable<Friend>
{
  private static int lastSelected = 0;
  private final Base64Renderer base64Renderer = new Base64Renderer();
  private final GuiChat guiChat;
  protected IGuiList<Friend> friendList;
  protected IGuiList profileList;
  private final List<StaticProfileRow> profileRows = Collections.singletonList(new StaticProfileRow(null));
  
  public TabFriends(GuiChat guiChat)
  {
    this.guiChat = guiChat;
  }
  
  public void initGui()
  {
    int left = Math.max(getWidth() / 2 - 40, 190);
    int right = Math.min(left + 200, getWidth() - 10);
    addButton(The5zigMod.getVars().createButton(1, left - 60, getHeight() / 4 + 76 + 48, 150, 20, I18n.translate("friend.start_conversation")));
    addButton(The5zigMod.getVars().createButton(4, left - 60 + 10 + 140 + 4, getHeight() / 4 + 76 + 48, 90, 20, I18n.translate("friend.join_server")));
    addButton(The5zigMod.getVars().createButton(2, 2, getHeight() - 16 - 20 - 10 - 2, 98, 20, I18n.translate("friend.add_new")));
    addButton(The5zigMod.getVars().createButton(3, 2, getHeight() - 16 - 10, 98, 20, 
      I18n.translate("friend.requests", new Object[] {ChatColor.YELLOW + String.valueOf(The5zigMod.getFriendManager().getFriendRequests().size()) + ChatColor.RESET })));
    
    this.friendList = The5zigMod.getVars().createGuiList(this, 110, getHeight(), 76, getHeight() - 16 - 36, 0, 100, The5zigMod.getFriendManager().getFriends());
    this.friendList.setLeftbound(true);
    this.friendList.setScrollX(95);
    this.profileList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 56, 162, left, right, this.profileRows);
    this.profileList.setLeftbound(true);
    this.profileList.setDrawSelection(false);
    this.profileList.setScrollX(right - 5);
    
    addButton(The5zigMod.getVars().createStringButton(111, left - 80, 136, 76, 20, I18n.translate("friend.settings")));
    this.friendList.setSelectedId(lastSelected);
    Friend selected = (Friend)this.friendList.getSelectedRow();
    synchronized (The5zigMod.getFriendManager().getFriends())
    {
      this.friendList.onSelect(The5zigMod.getFriendManager().getFriends().indexOf(selected), this.friendList.getSelectedRow(), false);
    }
    ITextfield textfield = The5zigMod.getVars().createTextfield(I18n.translate("gui.search"), 9991, 2, 56, 96, 16);
    Object comparator = new Comparator()
    {
      public int compare(Friend o1, Friend o2)
      {
        return o1.compareTo(o2);
      }
    };
    SearchEntry searchEntry = new SearchEntry(textfield, The5zigMod.getFriendManager().getFriends(), (Comparator)comparator)
    {
      public boolean filter(String text, Object o)
      {
        Friend friend = (Friend)o;
        return friend.getName().toLowerCase().contains(text.toLowerCase());
      }
    };
    searchEntry.setAlwaysVisible(true);
    searchEntry.setEnterCallback(new Callback()
    {
      public void call(Object callback)
      {
        TabFriends.this.onSelect(0, (Friend)callback, true);
      }
    });
    The5zigMod.getDataManager().getSearchManager().addSearch(searchEntry, new SearchEntry[0]);
  }
  
  public void onSelect(int id, Friend friend, boolean doubleClick)
  {
    lastSelected = id;
    this.friendList.setSelectedId(lastSelected);
    if (lastSelected < 0) {
      return;
    }
    this.profileList.scrollTo(0.0F);
  }
  
  protected void mouseClicked(int x, int y, int button)
  {
    this.friendList.mouseClicked(x, y);
    this.profileList.mouseClicked(x, y);
    The5zigMod.getDataManager().getSearchManager().mouseClicked(x, y, button);
    super.mouseClicked(x, y, button);
  }
  
  public void handleMouseInput()
  {
    this.friendList.handleMouseInput();
    this.profileList.handleMouseInput();
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawMenuBackground();
    
    this.friendList.drawScreen(mouseX, mouseY, partialTicks);
    this.profileList.drawScreen(mouseX, mouseY, partialTicks);
    drawProfile();
    The5zigMod.getDataManager().getSearchManager().draw();
  }
  
  private void drawProfile()
  {
    int size = 76;
    int x = Math.max(getWidth() / 2 - 120, 110);
    int y = 56;
    
    Friend friend = (Friend)this.friendList.getSelectedRow();
    if (friend == null)
    {
      if (this.base64Renderer.getBase64String() != null) {
        this.base64Renderer.setBase64String(null, "player_skin/none");
      }
      this.base64Renderer.renderImage(x, y, size, size);
      return;
    }
    String base64EncodedSkin = The5zigMod.getSkinManager().getBase64EncodedSkin(friend.getUniqueId());
    if ((this.base64Renderer.getBase64String() != null) && (base64EncodedSkin == null)) {
      this.base64Renderer.reset();
    } else if ((base64EncodedSkin != null) && (!base64EncodedSkin.equals(this.base64Renderer.getBase64String()))) {
      this.base64Renderer.setBase64String(base64EncodedSkin, "player_skin/" + friend.getUniqueId());
    }
    this.base64Renderer.renderImage(x, y, size, size);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      Friend friend = (Friend)this.friendList.getSelectedRow();
      if (friend == null) {
        return;
      }
      Conversation conversation = The5zigMod.getConversationManager().getConversation(friend);
      TabConversations tab = new TabConversations(this.guiChat);
      this.guiChat.setCurrentTab(tab);
      tab.setCurrentConversation(conversation);
      tab.onSelect(tab.chatList.getRows().indexOf(conversation), conversation, false);
    }
    if (button.getId() == 2) {
      The5zigMod.getVars().displayScreen(new GuiCenteredTextfield(this.guiChat, new CenteredTextfieldCallback()
      {
        public void onDone(String text)
        {
          if (!The5zigMod.getNetworkManager().isConnected()) {
            return;
          }
          The5zigMod.getNetworkManager().sendPacket(new PacketFriendRequest(text), new GenericFutureListener[0]);
        }
        
        public String title()
        {
          return I18n.translate("friends.invite.title");
        }
      }, 1, 16));
    }
    if (button.getId() == 3) {
      The5zigMod.getVars().displayScreen(new GuiFriendRequests(this.guiChat));
    }
    if (button.getId() == 4)
    {
      Friend friend = (Friend)this.friendList.getSelectedRow();
      if (friend == null) {
        return;
      }
      if ((friend.getStatus() == Friend.OnlineStatus.OFFLINE) || (friend.getServer() == null) || (friend.getServer().equals("Hidden"))) {
        return;
      }
      String[] server = friend.getServer().split(":");
      if (server.length != 2) {
        return;
      }
      The5zigMod.getVars().joinServer(server[0], Integer.parseInt(server[1]));
    }
    if (button.getId() == 111)
    {
      if (this.friendList.getSelectedRow() == null) {
        return;
      }
      The5zigMod.getVars().displayScreen(new GuiFriendSettings(this.guiChat, (Friend)this.friendList.getSelectedRow()));
    }
  }
  
  protected void onKeyType(char character, int key)
  {
    The5zigMod.getDataManager().getSearchManager().keyTyped(character, key);
  }
  
  protected void tick()
  {
    enableDisableButtons();
  }
  
  public void enableDisableButtons()
  {
    Friend friend = (Friend)this.friendList.getSelectedRow();
    boolean selected = friend != null;
    getButtonById(1).setEnabled(selected);
    getButtonById(4).setEnabled(selected);
    if ((selected) && (friend.getStatus() != Friend.OnlineStatus.OFFLINE) && (friend.getServer() != null) && 
      (!friend.getServer().equals("Hidden"))) {
      getButtonById(4).setEnabled(true);
    } else {
      getButtonById(4).setEnabled(false);
    }
    getButtonById(3).setLabel(I18n.translate("friend.requests", new Object[] { ChatColor.YELLOW + String.valueOf(The5zigMod.getFriendManager().getFriendRequests().size()) + ChatColor.RESET }));
  }
  
  public String getTitleName()
  {
    return "";
  }
  
  public Friend getSelectedFriend()
  {
    return (Friend)this.friendList.getSelectedRow();
  }
  
  private class StaticProfileRow
    implements Row
  {
    private int totalHeight = 0;
    
    private StaticProfileRow() {}
    
    public void draw(int x, int y)
    {
      this.totalHeight = 0;
      Friend friend = TabFriends.this.getSelectedFriend();
      if (friend == null) {
        return;
      }
      int width = Math.min(Math.max(TabFriends.this.getWidth() / 2 - 40, 190) + 200, TabFriends.this.getWidth() - 10) - Math.max(TabFriends.this.getWidth() / 2 - 40, 190) - 10;
      
      List<String> lines = Lists.newArrayList();
      lines.add(friend.getDisplayName());
      lines.add(ChatColor.GRAY.toString() + ChatColor.ITALIC + I18n.translate("friend.info.first_join_date", new Object[] { friend.getFirstOnline() }));
      lines.add(I18n.translate("friend.info.status", new Object[] {
        friend
        .getStatus().getDisplayName() + ChatColor.RESET }));
      lines.add(friend.getStatus() != Friend.OnlineStatus.OFFLINE ? I18n.translate("friend.info.server", new Object[] {friend
        .getServer() == null ? I18n.translate("friend.info.server.none") : 
        (friend.getServer().split(":")[0] + (friend.getLobby() != null ? " (" + friend.getLobby() + ")" : "")).replace("Hidden", I18n.translate("friend.info.hidden")) }) : I18n.translate("friend.info.last_seen", new Object[] {friend
        .getLastOnline() }));
      lines.add(I18n.translate("friend.info.profile_message") + " " + friend.getStatusMessage());
      lines.add(I18n.translate("friend.info.mod_version", new Object[] { friend.getModVersion() }));
      lines.add(I18n.translate("friend.info.country", new Object[] {
        (!The5zigMod.getDataManager().getProfile().isShowCountry()) || (friend.getLocale() == null) ? I18n.translate("friend.info.hidden") : friend
        .getLocale().getDisplayCountry(I18n.getCurrentLanguage()) }));
      
      this.totalHeight = y;
      for (String text : lines)
      {
        List<String> split = MinecraftFactory.getVars().splitStringToWidth(text, width);
        int j = 0;
        for (int splitStringToWidthSize = split.size(); j < splitStringToWidthSize; j++)
        {
          String line = (String)split.get(j);
          MinecraftFactory.getVars().drawString(line, x, y);
          if (j < splitStringToWidthSize - 1) {
            y += 11;
          } else {
            y += 14;
          }
        }
      }
      this.totalHeight = (y - this.totalHeight);
    }
    
    public int getLineHeight()
    {
      return this.totalHeight;
    }
  }
}
