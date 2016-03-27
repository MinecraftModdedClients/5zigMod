package eu.the5zig.mod.chat.sql;

import java.util.UUID;

public class ChatEntity
{
  private int id;
  private UUID uuid;
  private String friend;
  private long lastused;
  private boolean read;
  private int status;
  private int behaviour;
  
  public int getId()
  {
    return this.id;
  }
  
  public UUID getUuid()
  {
    return this.uuid;
  }
  
  public String getFriend()
  {
    return this.friend;
  }
  
  public void setFriend(String friend)
  {
    this.friend = friend;
  }
  
  public long getLastUsed()
  {
    return this.lastused;
  }
  
  public boolean isRead()
  {
    return this.read;
  }
  
  public int getStatus()
  {
    return this.status;
  }
  
  public int getBehaviour()
  {
    return this.behaviour;
  }
}
