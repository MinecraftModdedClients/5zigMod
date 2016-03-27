package eu.the5zig.mod.chat.sql;

public class AnnouncementEntity
{
  private int id;
  private long lastused;
  private boolean read;
  private int behaviour;
  
  public int getId()
  {
    return this.id;
  }
  
  public long getLastused()
  {
    return this.lastused;
  }
  
  public boolean isRead()
  {
    return this.read;
  }
  
  public int getBehaviour()
  {
    return this.behaviour;
  }
}
