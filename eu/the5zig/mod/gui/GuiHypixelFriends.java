package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendRequest;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.hypixel.api.HypixelAPICallback;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIManager;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIMissingKeyException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIResponse;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIResponseException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPITooManyRequestsException;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Iterator;
import java.util.List;

public class GuiHypixelFriends
  extends Gui
{
  private IGuiList guiList;
  private List<User> friends = Lists.newArrayList();
  private String status;
  private List<String> statusSplit;
  
  public GuiHypixelFriends(Gui lastScreen)
  {
    super(lastScreen);
    
    load();
  }
  
  public void initGui()
  {
    if (this.guiList == null) {
      updateStatus(I18n.translate("server.hypixel.loading"));
    }
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 - 206, getHeight() - 38, 100, 20, I18n.translate("server.hypixel.friends.display_stats")));
    addButton(The5zigMod.getVars().createButton(101, getWidth() / 2 - 102, getHeight() - 38, 100, 20, I18n.translate("server.hypixel.friends.add_mod")));
    addButton(The5zigMod.getVars().createButton(102, getWidth() / 2 + 2, getHeight() - 38, 100, 20, I18n.translate("server.hypixel.friends.add_mod_all")));
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 + 106, getHeight() - 38, 100, 20, The5zigMod.getVars().translate("gui.done", new Object[0])));
    this.guiList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 50, getHeight() - 50, 0, getWidth(), this.friends);
    this.guiList.setRowWidth(200);
    this.guiList.setScrollX(getWidth() / 2 + 124);
    addGuiList(this.guiList);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 100)
    {
      User selectedRow = (User)this.guiList.getSelectedRow();
      if (selectedRow == null) {
        return;
      }
      The5zigMod.getVars().displayScreen(new GuiHypixelStats(this, selectedRow.getUsername()));
    }
    if (button.getId() == 101)
    {
      User selectedRow = (User)this.guiList.getSelectedRow();
      if (selectedRow == null) {
        return;
      }
      if (!The5zigMod.getNetworkManager().isConnected()) {
        return;
      }
      if (The5zigMod.getFriendManager().isFriend(selectedRow.getUniqueId())) {
        return;
      }
      The5zigMod.getNetworkManager().sendPacket(new PacketFriendRequest(selectedRow.getUsername()), new GenericFutureListener[0]);
    }
    Iterator<User> iterator;
    if (button.getId() == 102)
    {
      if (!The5zigMod.getNetworkManager().isConnected()) {
        return;
      }
      List<User> users = Lists.newArrayList(this.friends);
      for (iterator = users.iterator(); iterator.hasNext();) {
        if (The5zigMod.getFriendManager().isFriend(((User)iterator.next()).getUniqueId())) {
          iterator.remove();
        }
      }
      for (User user : users) {
        The5zigMod.getNetworkManager().sendPacket(new PacketFriendRequest(user.getUsername()), new GenericFutureListener[0]);
      }
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    int y;
    if (this.status != null)
    {
      y = getHeight() / 2 - 30;
      for (String s : this.statusSplit)
      {
        drawCenteredString(s, getWidth() / 2, y);
        y += 12;
      }
    }
  }
  
  protected void tick()
  {
    super.getButtonById(100).setEnabled(this.guiList.getSelectedRow() != null);
    super.getButtonById(101).setEnabled((this.guiList.getSelectedRow() != null) && (!The5zigMod.getFriendManager().isFriend(((User)this.guiList.getSelectedRow()).getUniqueId())) && 
      (The5zigMod.getNetworkManager().isConnected()));
    super.getButtonById(102).setEnabled(The5zigMod.getNetworkManager().isConnected());
  }
  
  private void load()
  {
    try
    {
      The5zigMod.getHypixelAPIManager().get("friends?player=" + The5zigMod.getDataManager().getUsername(), new HypixelAPICallback()
      {
        public void call(HypixelAPIResponse response)
        {
          GuiHypixelFriends.this.updateStatus(null);
          JsonArray friends = response.data().getAsJsonArray("records");
          for (JsonElement element : friends)
          {
            JsonObject friend = element.getAsJsonObject();
            String sender = friend.get("sender").getAsString();
            String senderUUID = friend.get("uuidSender").getAsString();
            String receiver = friend.get("receiver").getAsString();
            String receiverUUID = friend.get("uuidReceiver").getAsString();
            if (The5zigMod.getDataManager().getUniqueIdWithoutDashes().equals(senderUUID)) {
              GuiHypixelFriends.this.friends.add(new User(receiver, Utils.getUUID(receiverUUID)));
            } else if (The5zigMod.getDataManager().getUniqueIdWithoutDashes().equals(receiverUUID)) {
              GuiHypixelFriends.this.friends.add(new User(sender, Utils.getUUID(senderUUID)));
            }
          }
        }
        
        public void call(HypixelAPIResponseException e)
        {
          GuiHypixelFriends.this.updateStatus(e.getErrorMessage());
        }
      });
    }
    catch (HypixelAPITooManyRequestsException e)
    {
      updateStatus(I18n.translate("server.hypixel.too_many_requests"));
    }
    catch (HypixelAPIMissingKeyException e)
    {
      updateStatus(I18n.translate("server.hypixel.no_key"));
    }
    catch (HypixelAPIException e)
    {
      updateStatus(e.getMessage());
    }
  }
  
  private void updateStatus(String status)
  {
    this.status = status;
    if (status == null) {
      this.statusSplit = null;
    } else {
      this.statusSplit = The5zigMod.getVars().splitStringToWidth(status, getWidth() - 50);
    }
  }
  
  public String getTitleKey()
  {
    return "server.hypixel.friends.title";
  }
}
