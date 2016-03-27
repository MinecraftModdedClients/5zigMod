package eu.the5zig.mod.chat.network.filetransfer;

import com.google.common.collect.Maps;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.ConversationChat;
import eu.the5zig.mod.chat.entity.ConversationGroupChat;
import eu.the5zig.mod.chat.entity.FileMessage;
import eu.the5zig.mod.chat.entity.FileMessage.FileData;
import eu.the5zig.mod.manager.DataManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class FileTransferManager
{
  private HashMap<Integer, FileUploadTask> uploadTasks = Maps.newHashMap();
  private HashMap<Integer, FileDownloadTask> downloadTasks = Maps.newHashMap();
  
  public void initFileUpload(FileMessage message)
    throws IOException
  {
    this.uploadTasks.put(Integer.valueOf(message.getFileData().getFileId()), new FileUploadTask(message.getFileData().getFileId(), message.getFileData().getHash(), message));
  }
  
  public boolean isUploading(int fileId)
  {
    return this.uploadTasks.containsKey(Integer.valueOf(fileId));
  }
  
  public boolean isDownloading(int fileId)
  {
    return this.downloadTasks.containsKey(Integer.valueOf(fileId));
  }
  
  public void startUpload(int fileId)
  {
    if (!this.uploadTasks.containsKey(Integer.valueOf(fileId))) {
      return;
    }
    try
    {
      ((FileUploadTask)this.uploadTasks.get(Integer.valueOf(fileId))).initSend();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  public void abortUpload(int fileId)
  {
    if (!this.uploadTasks.containsKey(Integer.valueOf(fileId))) {
      return;
    }
    ((FileUploadTask)this.uploadTasks.get(Integer.valueOf(fileId))).abortUpload();
  }
  
  public void initFileDownload(int fileId, int parts, int chunkSize, FileMessage message)
    throws IOException
  {
    this.downloadTasks.put(Integer.valueOf(fileId), new FileDownloadTask(parts, chunkSize, message));
  }
  
  public boolean handleChunkDownload(Integer fileId, int partId, byte[] data, FileMessage message)
    throws IOException, FileTransferException, NoSuchAlgorithmException
  {
    if (!this.downloadTasks.containsKey(fileId)) {
      return false;
    }
    FileDownloadTask task = (FileDownloadTask)this.downloadTasks.get(fileId);
    task.handle(partId, data);
    if (task.hasFinished())
    {
      this.downloadTasks.remove(fileId);
      File partFile = new File("the5zigmod/media/" + The5zigMod.getDataManager().getUniqueId().toString() + "/" + getFileName(message), fileId + ".part");
      File mediaFile = new File("the5zigmod/media/" + The5zigMod.getDataManager().getUniqueId().toString() + "/" + getFileName(message) + "/" + sha1(partFile));
      if (!mediaFile.exists()) {
        FileUtils.moveFile(partFile, mediaFile);
      } else {
        FileUtils.deleteQuietly(partFile);
      }
      return true;
    }
    return false;
  }
  
  public void abortDownload(Integer fileId)
  {
    if (!this.downloadTasks.containsKey(fileId)) {
      return;
    }
    File partFile = new File("the5zigmod/media/" + The5zigMod.getDataManager().getUniqueId().toString() + "/" + getFileName(((FileDownloadTask)this.downloadTasks.get(fileId)).getMessage()), fileId + ".part");
    FileUtils.deleteQuietly(partFile);
    this.downloadTasks.remove(fileId);
  }
  
  public String getFileName(FileMessage message)
  {
    if ((message.getConversation() instanceof ConversationChat)) {
      return ((ConversationChat)message.getConversation()).getFriendUUID().toString();
    }
    if ((message.getConversation() instanceof ConversationGroupChat)) {
      return String.valueOf(((ConversationGroupChat)message.getConversation()).getGroupId());
    }
    return "";
  }
  
  public void cleanUp(File dir)
  {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        cleanUp(file);
      } else if (file.getName().endsWith(".part")) {
        FileUtils.deleteQuietly(file);
      }
    }
  }
  
  public static String sha1(File file)
  {
    InputStream is = null;
    try
    {
      is = new FileInputStream(file);
      return DigestUtils.sha1Hex(is);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
    finally
    {
      IOUtils.closeQuietly(is);
    }
  }
}
