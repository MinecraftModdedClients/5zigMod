package eu.the5zig.mod.chat.entity;

import com.google.gson.Gson;
import eu.the5zig.mod.The5zigMod;

public abstract class FileMessage
  extends Message
{
  private FileData fileData;
  private float percentage;
  
  public FileMessage(Conversation conversation, int id, String username, String message, long time, Message.MessageType type)
  {
    super(conversation, id, username, message, time, type);
    loadData();
  }
  
  public FileMessage(Conversation conversation, int id, String username, FileData fileData, long time, Message.MessageType type)
  {
    super(conversation, id, username, "", time, type);
    this.fileData = fileData;
    this.percentage = ((fileData.getStatus() == Status.UPLOADED) || (fileData.getStatus() == Status.DOWNLOADED) ? 1.0F : 0.0F);
    saveData();
  }
  
  private void loadData()
  {
    this.fileData = ((FileData)The5zigMod.gson.fromJson(getMessage(), getDataClass()));
    this.percentage = ((this.fileData.getStatus() == Status.UPLOADED) || (this.fileData.getStatus() == Status.DOWNLOADED) ? 1.0F : 0.0F);
  }
  
  public void saveData()
  {
    setMessage(The5zigMod.gson.toJson(this.fileData));
  }
  
  public FileData getFileData()
  {
    return this.fileData;
  }
  
  public float getPercentage()
  {
    return this.percentage;
  }
  
  public void setPercentage(float percentage)
  {
    this.percentage = percentage;
  }
  
  protected abstract Class<? extends FileData> getDataClass();
  
  public static abstract class FileData
  {
    private int fileId;
    private String hash;
    private int status;
    private long length;
    
    public FileData() {}
    
    public FileData(FileMessage.Status status)
    {
      this.status = status.ordinal();
    }
    
    public void setFileId(int fileId)
    {
      this.fileId = fileId;
    }
    
    public int getFileId()
    {
      return this.fileId;
    }
    
    public String getHash()
    {
      return this.hash;
    }
    
    public void setHash(String hash)
    {
      this.hash = hash;
    }
    
    public FileMessage.Status getStatus()
    {
      return FileMessage.Status.values()[this.status];
    }
    
    public void setStatus(FileMessage.Status status)
    {
      this.status = status.ordinal();
    }
    
    public long getLength()
    {
      return this.length;
    }
    
    public void setLength(long length)
    {
      this.length = length;
    }
    
    public boolean isOwn()
    {
      return this.status >= FileMessage.Status.WAITING.ordinal();
    }
  }
  
  public static enum Status
  {
    REQUEST,  REQUEST_ACCEPTED,  REQUEST_DENIED,  DOWNLOADING,  DOWNLOADED,  DOWNLOAD_FAILED,  WAITING,  DENIED,  ACCEPTED,  UPLOADING,  UPLOADED,  UPLOAD_FAILED;
    
    private Status() {}
  }
}
