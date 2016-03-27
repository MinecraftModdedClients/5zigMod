package eu.the5zig.mod.listener;

import io.netty.buffer.ByteBuf;

public class Listener
  implements IListener
{
  public void onTick() {}
  
  public void onKeyPress(int code) {}
  
  public void onServerJoin(String host, int port) {}
  
  public void onServerConnect() {}
  
  public void onServerDisconnect() {}
  
  public void onPayloadReceive(String channel, ByteBuf packetData) {}
  
  public boolean onServerChat(String message)
  {
    return false;
  }
  
  public boolean onServerChat(String message, Object chatComponent)
  {
    return false;
  }
  
  public boolean onActionBar(String message)
  {
    return false;
  }
  
  public void onPlayerListHeaderFooter(String header, String footer) {}
  
  public void onTitle(String title, String subTitle) {}
}
