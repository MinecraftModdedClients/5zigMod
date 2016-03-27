package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.ingame.resource.IResourceManager;
import eu.the5zig.mod.util.IVariables;
import java.io.IOException;

public class PacketCapeSettings
  implements Packet
{
  private Action action;
  private String image;
  private boolean enabled;
  private Cape cape;
  
  public PacketCapeSettings(Action action, String image)
  {
    this.action = action;
    this.image = image;
  }
  
  public PacketCapeSettings(Action action, boolean enabled)
  {
    this.action = action;
    this.enabled = enabled;
  }
  
  public PacketCapeSettings(Action action, Cape cape)
  {
    this.action = action;
    this.cape = cape;
  }
  
  public PacketCapeSettings() {}
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int ordinal = buffer.readVarIntFromBuffer();
    if ((ordinal < 0) || (ordinal >= Action.values().length)) {
      throw new IllegalArgumentException("Received Integer is out of enum range");
    }
    this.action = Action.values()[ordinal];
    if (this.action == Action.SETTINGS) {
      this.enabled = buffer.readBoolean();
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {
    buffer.writeVarIntToBuffer(this.action.ordinal());
    if (this.action == Action.UPLOAD_CUSTOM) {
      buffer.writeString(this.image);
    }
    if (this.action == Action.UPLOAD_DEFAULT) {
      buffer.writeVarIntToBuffer(this.cape.ordinal());
    }
    if (this.action == Action.SETTINGS) {
      buffer.writeBoolean(this.enabled);
    }
  }
  
  public void handle()
  {
    if (this.action == Action.UPDATE) {
      The5zigMod.getVars().getResourceManager().updateOwnPlayerTextures();
    }
  }
  
  public static enum Action
  {
    SETTINGS,  UPLOAD_CUSTOM,  UPLOAD_DEFAULT,  UPDATE;
    
    private Action() {}
  }
  
  public static enum Cape
  {
    GREEN,  RED,  BLUE,  YELLOW;
    
    private Cape() {}
  }
}
