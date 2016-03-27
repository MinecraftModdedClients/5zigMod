package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.util.minecraft.ChatColor;
import java.io.IOException;

public class PacketOverlay
  implements Packet
{
  private Type type;
  private String message;
  private String[] format;
  
  public void read(PacketBuffer buffer)
    throws IOException
  {
    int ordinal = buffer.readVarIntFromBuffer();
    if ((ordinal < 0) || (ordinal >= Type.values().length)) {
      throw new IllegalArgumentException("Received Integer is out of enum range");
    }
    this.type = Type.values()[ordinal];
    this.message = buffer.readString();
    if (this.type != Type.NONE)
    {
      int formatLength = buffer.readVarIntFromBuffer();
      this.format = new String[formatLength];
      for (int i = 0; i < formatLength; i++) {
        this.format[i] = buffer.readString();
      }
    }
    else
    {
      this.format = new String[0];
    }
  }
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    switch (this.type)
    {
    case INFO: 
      The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.YELLOW + I18n.translate(this.message, this.format));
      break;
    case SUCCESS: 
      The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.GREEN + I18n.translate(this.message, this.format));
      break;
    case ERROR: 
      The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.RED + I18n.translate(this.message, this.format));
      break;
    default: 
      The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.YELLOW + this.message);
    }
  }
  
  public static enum Type
  {
    NONE,  INFO,  SUCCESS,  ERROR;
    
    private Type() {}
  }
}
