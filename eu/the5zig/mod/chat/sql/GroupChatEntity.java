package eu.the5zig.mod.chat.sql;

public class GroupChatEntity
{
  private int id;
  private int groupId;
  private String name;
  private long lastused;
  private boolean read;
  private int status;
  private int behaviour;
  
  public int getId()
  {
    return this.id;
  }
  
  public int getGroupId()
  {
    return this.groupId;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public long getLastused()
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
