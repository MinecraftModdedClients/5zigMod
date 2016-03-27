package eu.the5zig.mod.chat;

import com.google.common.collect.Lists;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.ConversationGroupChat;
import eu.the5zig.mod.chat.entity.Group;
import java.util.List;

public class GroupChatManager
{
  private List<Group> groups = Lists.newArrayList();
  
  public void addGroup(Group group)
  {
    this.groups.add(group);
  }
  
  public void removeGroup(Group group)
  {
    this.groups.remove(group);
  }
  
  public Group getGroup(int id)
  {
    for (Group group : this.groups) {
      if (group.getId() == id) {
        return group;
      }
    }
    return null;
  }
  
  public void setGroups(List<Group> groups)
  {
    this.groups = groups;
    manageGroups();
  }
  
  private void manageGroups()
  {
    for (Group group : this.groups)
    {
      ConversationGroupChat conversationGroupChat = The5zigMod.getConversationManager().getConversation(group);
      if (!group.getName().equals(conversationGroupChat.getName())) {
        The5zigMod.getConversationManager().setConversationName(conversationGroupChat, group.getName());
      }
    }
  }
}
