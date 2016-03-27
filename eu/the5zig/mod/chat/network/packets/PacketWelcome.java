package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.GuiWelcome;
import eu.the5zig.mod.util.IVariables;
import java.io.IOException;

public class PacketWelcome
  implements Packet
{
  public void read(PacketBuffer buffer)
    throws IOException
  {}
  
  public void write(PacketBuffer buffer)
    throws IOException
  {}
  
  public void handle()
  {
    The5zigMod.getVars().displayScreen(new GuiWelcome());
  }
}
