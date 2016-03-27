package eu.the5zig.mod.chat;

import com.google.common.collect.Lists;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFriendRequestResponse;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.TabFriends;
import eu.the5zig.mod.util.IVariables;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class FriendManager
{
  private List<Friend> friends = Lists.newArrayList();
  private List<User> friendRequests = Lists.newArrayList();
  private List<User> blockedUsers = Lists.newArrayList();
  
  public List<Friend> getFriends()
  {
    return this.friends;
  }
  
  public void setFriends(List<Friend> friends)
  {
    TabFriends guiFriends;
    if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabFriends)))
    {
      guiFriends = (TabFriends)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
      Friend selected = guiFriends.getSelectedFriend();
      int id = this.friends.indexOf(selected);
      
      this.friends.clear();
      this.friends.addAll(friends);
      for (Friend friend : this.friends) {
        The5zigMod.getConversationManager().updateConversationNames(friend);
      }
      guiFriends.onSelect(id, selected, false);
      sortFriends();
      return;
    }
    this.friends.clear();
    this.friends.addAll(friends);
    for (Friend friend : this.friends) {
      The5zigMod.getConversationManager().updateConversationNames(friend);
    }
    sortFriends();
  }
  
  public List<User> getFriendRequests()
  {
    return this.friendRequests;
  }
  
  public void setFriendRequests(List<User> friendRequests)
  {
    this.friendRequests.clear();
    this.friendRequests.addAll(friendRequests);
  }
  
  public List<User> getBlockedUsers()
  {
    return this.blockedUsers;
  }
  
  public void setBlockedUsers(List<User> blockedUsers)
  {
    this.blockedUsers.clear();
    this.blockedUsers.addAll(blockedUsers);
  }
  
  public Friend getFriend(UUID uuid)
  {
    for (Friend friend : this.friends) {
      if (friend.getUniqueId().equals(uuid)) {
        return friend;
      }
    }
    return null;
  }
  
  public User getBlocked(UUID friendUUID)
  {
    for (User blockedUser : this.blockedUsers) {
      if (friendUUID.equals(blockedUser.getUniqueId())) {
        return blockedUser;
      }
    }
    return null;
  }
  
  public void handleFriendRequestResponse(UUID friend, boolean accepted)
  {
    The5zigMod.getNetworkManager().sendPacket(new PacketFriendRequestResponse(friend, accepted), new GenericFutureListener[0]);
    for (Iterator<User> iterator = this.friendRequests.iterator(); iterator.hasNext();)
    {
      User user = (User)iterator.next();
      if (user.getUniqueId().equals(friend)) {
        iterator.remove();
      }
    }
  }
  
  public void addFriend(Friend friend)
  {
    this.friends.add(friend);
    The5zigMod.getConversationManager().updateConversationNames(friend);
    sortFriends();
  }
  
  public void addFriendRequest(User friendRequest)
  {
    this.friendRequests.add(friendRequest);
  }
  
  public void removeBlockedUser(UUID blockedUser)
  {
    for (Iterator<User> iterator = this.blockedUsers.iterator(); iterator.hasNext();)
    {
      User user = (User)iterator.next();
      if (user.getUniqueId().equals(blockedUser)) {
        iterator.remove();
      }
    }
  }
  
  public void addBlockedUser(User blockedUser)
  {
    this.blockedUsers.add(blockedUser);
  }
  
  public void removeFriend(UUID friend)
  {
    for (Iterator<Friend> iterator = this.friends.iterator(); iterator.hasNext();)
    {
      Friend f = (Friend)iterator.next();
      if (f.getUniqueId().equals(friend))
      {
        iterator.remove();
        break;
      }
    }
    if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabFriends)))
    {
      TabFriends gui = (TabFriends)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
      gui.getButtonList().clear();
      gui.initGui();
    }
    sortFriends();
  }
  
  public boolean isBlocked(UUID user)
  {
    return getBlocked(user) != null;
  }
  
  public boolean isFriend(UUID friendUUID)
  {
    return getFriend(friendUUID) != null;
  }
  
  public void sortFriends()
  {
    if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabFriends)))
    {
      TabFriends gui = (TabFriends)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
      Friend selected = gui.getSelectedFriend();
      int id = this.friends.indexOf(selected);
      
      Collections.sort(this.friends);
      
      gui.onSelect(id, selected, false);
      return;
    }
    Collections.sort(this.friends);
  }
}
