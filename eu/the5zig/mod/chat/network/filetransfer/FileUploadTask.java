package eu.the5zig.mod.chat.network.filetransfer;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.entity.FileMessage;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferChunk;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferStart;
import eu.the5zig.mod.manager.DataManager;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.apache.commons.io.IOUtils;

public class FileUploadTask
{
  public static final int MAX_LENGTH = 5000000;
  private final int CHUNK_SIZE = 32000;
  private final int fileId;
  private final FileMessage message;
  private File file;
  private boolean uploading = false;
  
  public FileUploadTask(int fileId, String fileName, FileMessage message)
    throws IOException
  {
    this.fileId = fileId;
    this.message = message;
    this.file = new File("the5zigmod/media/" + The5zigMod.getDataManager().getUniqueId().toString() + "/" + The5zigMod.getDataManager().getFileTransferManager().getFileName(message) + "/" + fileName);
    if (!this.file.exists()) {
      throw new FileNotFoundException(this.file.getAbsolutePath());
    }
    long fileLength = this.file.length();
    int parts = (int)Math.ceil(fileLength / 32000.0D);
    if (fileLength > 5000000L) {
      throw new IllegalArgumentException("Image too large!");
    }
    The5zigMod.getNetworkManager().sendPacket(new PacketFileTransferStart(fileId, parts, 32000), new GenericFutureListener[0]);
  }
  
  public void initSend()
    throws IOException
  {
    this.uploading = true;
    new Thread("Upload Thread")
    {
      private final Thread instance = this;
      private int index;
      private int parts;
      
      public void run()
      {
        long fileLength = FileUploadTask.this.file.length();
        this.parts = ((int)Math.ceil(fileLength / 32000.0D));
        if (fileLength > 5000000L) {
          throw new IllegalArgumentException("Image too large!");
        }
        InputStream is = null;
        try
        {
          is = new FileInputStream(FileUploadTask.this.file);
          byte[] buffer = new byte['紀'];
          int l;
          while ((l = is.read(buffer)) > 0)
          {
            if (!FileUploadTask.this.uploading) {
              throw new IllegalStateException("Upload Aborted");
            }
            The5zigMod.getNetworkManager().sendPacket(new PacketFileTransferChunk(FileUploadTask.this.fileId, this.index, new FileUploadTask.Chunk(FileUploadTask.this, (byte[])buffer.clone(), l)), new GenericFutureListener[] { new GenericFutureListener()
            {
              public void operationComplete(Future future)
                throws Exception
              {
                FileUploadTask.this.message.setPercentage((FileUploadTask.1.this.index - 1) / FileUploadTask.1.this.parts);
                synchronized (FileUploadTask.1.this.instance)
                {
                  FileUploadTask.1.this.instance.notify();
                }
              }
            } });
            this.index += 1;
            synchronized (this)
            {
              wait();
            }
          }
          The5zigMod.getConversationManager().setImageUploaded(FileUploadTask.this.message);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        finally
        {
          IOUtils.closeQuietly(is);
        }
      }
    }.start();
  }
  
  public void abortUpload()
  {
    this.uploading = false;
  }
  
  public class Chunk
  {
    private byte[] data;
    private int length;
    
    public Chunk(byte[] data, int length)
    {
      this.data = data;
      this.length = length;
    }
    
    public byte[] getData()
    {
      return this.data;
    }
    
    public int getLength()
    {
      return this.length;
    }
  }
}
