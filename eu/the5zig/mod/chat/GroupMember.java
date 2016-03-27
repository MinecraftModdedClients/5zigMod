package eu.the5zig.mod.chat;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.User;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.IVariables;
import java.util.UUID;

public class GroupMember
  extends User
  implements Comparable<GroupMember>
{
  public static final int MEMBER = 0;
  public static final int ADMIN = 1;
  public static final int OWNER = 2;
  private int type;
  
  public GroupMember(String username, UUID uuid, int type)
  {
    super(username, uuid);
    this.type = type;
  }
  
  public GroupMember(User user, int type)
  {
    this(user.getUsername(), user.getUniqueId(), type);
  }
  
  public boolean isAdmin()
  {
    return this.type == 1;
  }
  
  public int getType()
  {
    return this.type;
  }
  
  public void setType(int type)
  {
    this.type = type;
  }
  
  public void draw(int x, int y)
  {
    Gui currentScreen = The5zigMod.getVars().getCurrentScreen();
    String displayName = getUsername();
    switch (getType())
    {
    case 1: 
      displayName = displayName + String.format(" (%s)", new Object[] { I18n.translate("group.admin") });
      break;
    case 2: 
      displayName = displayName + String.format(" (%s)", new Object[] { I18n.translate("group.owner") });
    }
    Gui.drawCenteredString(displayName, currentScreen.getWidth() / 2, y + 2);
  }
  
  public int compareTo(GroupMember other)
  {
    if ((getType() == 2) && (other.getType() != 2)) {
      return -1;
    }
    if ((getType() != 2) && (other.getType() == 2)) {
      return 1;
    }
    if ((getType() == 1) && (other.getType() != 1)) {
      return -1;
    }
    if ((getType() != 1) && (other.getType() == 1)) {
      return 1;
    }
    return getUsername().compareTo(other.getUsername());
  }
}
