package eu.the5zig.mod.chat;

public class Announcement
{
  private long time;
  private String message;
  
  public Announcement(long time, String message)
  {
    this.time = time;
    this.message = message;
  }
  
  public long getTime()
  {
    return this.time;
  }
  
  public void setTime(long time)
  {
    this.time = time;
  }
  
  public String getMessage()
  {
    return this.message;
  }
  
  public void setMessage(String message)
  {
    this.message = message;
  }
}
