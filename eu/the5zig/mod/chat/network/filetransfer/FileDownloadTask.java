package eu.the5zig.mod.chat.network.filetransfer;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.FileMessage;
import eu.the5zig.mod.chat.entity.FileMessage.FileData;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.util.io.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.apache.commons.io.IOUtils;

public class FileDownloadTask
{
  private final File file;
  private final int parts;
  private final int chunkSize;
  private final FileMessage message;
  private final long totalLength;
  private int partCount;
  private long lengthCount;
  private OutputStream out;
  
  public FileDownloadTask(int parts, int chunkSize, FileMessage message)
    throws IOException
  {
    this.parts = parts;
    this.chunkSize = chunkSize;
    this.totalLength = message.getFileData().getLength();
    this.message = message;
    File dir = FileUtils.createDir(new File("the5zigmod/media/" + 
      The5zigMod.getDataManager().getUniqueId().toString() + "/" + The5zigMod.getDataManager().getFileTransferManager().getFileName(message)));
    this.file = new File(dir, message.getFileData().getFileId() + ".part");
    this.file.deleteOnExit();
    try
    {
      this.out = new FileOutputStream(this.file);
    }
    catch (IOException e)
    {
      close();
      throw e;
    }
  }
  
  public void handle(int partId, byte[] data)
    throws IOException, FileTransferException, NoSuchAlgorithmException
  {
    if (this.partCount != partId) {
      throw new FileTransferException("Illegal part received (out of order?!)");
    }
    if (data.length > this.chunkSize) {
      throw new FileTransferException("Illegal chunk length!");
    }
    this.out.write(data, 0, data.length);
    
    this.partCount += 1;
    this.lengthCount += data.length;
    if ((this.partCount == this.parts) && (this.lengthCount != this.totalLength)) {
      throw new FileTransferException();
    }
    if (!hasFinished())
    {
      this.message.setPercentage(partId / this.parts);
    }
    else
    {
      close();
      this.message.setPercentage(1.0F);
      this.message.getFileData().setHash(FileTransferManager.sha1(this.file));
      this.message.saveData();
    }
  }
  
  public boolean hasFinished()
  {
    return (this.partCount == this.parts) && (this.lengthCount == this.totalLength);
  }
  
  public FileMessage getMessage()
  {
    return this.message;
  }
  
  private void close()
  {
    IOUtils.closeQuietly(this.out);
  }
}
