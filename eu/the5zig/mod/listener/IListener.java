package eu.the5zig.mod.listener;

import io.netty.buffer.ByteBuf;

public abstract interface IListener
{
  public abstract void onTick();
  
  public abstract void onKeyPress(int paramInt);
  
  public abstract void onServerJoin(String paramString, int paramInt);
  
  public abstract void onServerConnect();
  
  public abstract void onServerDisconnect();
  
  public abstract void onPayloadReceive(String paramString, ByteBuf paramByteBuf);
  
  public abstract boolean onServerChat(String paramString);
  
  public abstract boolean onServerChat(String paramString, Object paramObject);
  
  public abstract boolean onActionBar(String paramString);
  
  public abstract void onPlayerListHeaderFooter(String paramString1, String paramString2);
  
  public abstract void onTitle(String paramString1, String paramString2);
}
