package org.h2.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;

public class TempFileDeleter
{
  private final ReferenceQueue<Object> queue = new ReferenceQueue();
  private final HashMap<PhantomReference<?>, String> refMap = New.hashMap();
  
  public static TempFileDeleter getInstance()
  {
    return new TempFileDeleter();
  }
  
  public synchronized Reference<?> addFile(String paramString, Object paramObject)
  {
    IOUtils.trace("TempFileDeleter.addFile", paramString, paramObject);
    PhantomReference localPhantomReference = new PhantomReference(paramObject, this.queue);
    this.refMap.put(localPhantomReference, paramString);
    deleteUnused();
    return localPhantomReference;
  }
  
  public synchronized void deleteFile(Reference<?> paramReference, String paramString)
  {
    if (paramReference != null)
    {
      String str = (String)this.refMap.remove(paramReference);
      if (str != null)
      {
        if ((SysProperties.CHECK) && 
          (paramString != null) && (!str.equals(paramString))) {
          DbException.throwInternalError("f2:" + str + " f:" + paramString);
        }
        paramString = str;
      }
    }
    if ((paramString != null) && (FileUtils.exists(paramString))) {
      try
      {
        IOUtils.trace("TempFileDeleter.deleteFile", paramString, null);
        FileUtils.tryDelete(paramString);
      }
      catch (Exception localException) {}
    }
  }
  
  public void deleteAll()
  {
    for (String str : New.arrayList(this.refMap.values())) {
      deleteFile(null, str);
    }
    deleteUnused();
  }
  
  public void deleteUnused()
  {
    while (this.queue != null)
    {
      Reference localReference = this.queue.poll();
      if (localReference == null) {
        break;
      }
      deleteFile(localReference, null);
    }
  }
  
  public void stopAutoDelete(Reference<?> paramReference, String paramString)
  {
    IOUtils.trace("TempFileDeleter.stopAutoDelete", paramString, paramReference);
    if (paramReference != null)
    {
      String str = (String)this.refMap.remove(paramReference);
      if ((SysProperties.CHECK) && (
        (str == null) || (!str.equals(paramString)))) {
        DbException.throwInternalError("f2:" + str + " " + (str == null ? "" : str) + " f:" + paramString);
      }
    }
    deleteUnused();
  }
}
