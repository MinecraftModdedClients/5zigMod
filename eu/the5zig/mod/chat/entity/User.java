package eu.the5zig.mod.chat.entity;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.IVariables;
import java.util.UUID;

public class User
  implements Row
{
  private final String username;
  private final UUID uuid;
  
  public User(String username, UUID uuid)
  {
    this.username = username;
    this.uuid = uuid;
  }
  
  public String getUsername()
  {
    return this.username;
  }
  
  public UUID getUniqueId()
  {
    return this.uuid;
  }
  
  public int getLineHeight()
  {
    return 18;
  }
  
  public void draw(int x, int y)
  {
    Gui currentScreen = The5zigMod.getVars().getCurrentScreen();
    Gui.drawCenteredString(getUsername(), currentScreen.getWidth() / 2, y + 2);
  }
  
  public boolean equals(Object obj)
  {
    return ((obj instanceof User)) && (((User)obj).getUniqueId().equals(getUniqueId()));
  }
  
  public String toString()
  {
    return "User{username='" + this.username + '\'' + ", uuid=" + this.uuid + '}';
  }
}
