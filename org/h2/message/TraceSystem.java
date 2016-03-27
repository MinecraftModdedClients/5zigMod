package org.h2.message;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import org.h2.api.ErrorCode;
import org.h2.jdbc.JdbcSQLException;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.New;

public class TraceSystem
  implements TraceWriter
{
  public static final int PARENT = -1;
  public static final int OFF = 0;
  public static final int ERROR = 1;
  public static final int INFO = 2;
  public static final int DEBUG = 3;
  public static final int ADAPTER = 4;
  public static final int DEFAULT_TRACE_LEVEL_SYSTEM_OUT = 0;
  public static final int DEFAULT_TRACE_LEVEL_FILE = 1;
  private static final int DEFAULT_MAX_FILE_SIZE = 67108864;
  private static final int CHECK_SIZE_EACH_WRITES = 4096;
  private int levelSystemOut = 0;
  private int levelFile = 1;
  private int levelMax;
  private int maxFileSize = 67108864;
  private String fileName;
  private HashMap<String, Trace> traces;
  private SimpleDateFormat dateFormat;
  private Writer fileWriter;
  private PrintWriter printWriter;
  private int checkSize;
  private boolean closed;
  private boolean writingErrorLogged;
  private TraceWriter writer = this;
  private PrintStream sysOut = System.out;
  
  public TraceSystem(String paramString)
  {
    this.fileName = paramString;
    updateLevel();
  }
  
  private void updateLevel()
  {
    this.levelMax = Math.max(this.levelSystemOut, this.levelFile);
  }
  
  public void setSysOut(PrintStream paramPrintStream)
  {
    this.sysOut = paramPrintStream;
  }
  
  public synchronized Trace getTrace(String paramString)
  {
    if (paramString.endsWith("]")) {
      return new Trace(this.writer, paramString);
    }
    if (this.traces == null) {
      this.traces = New.hashMap(16);
    }
    Trace localTrace = (Trace)this.traces.get(paramString);
    if (localTrace == null)
    {
      localTrace = new Trace(this.writer, paramString);
      this.traces.put(paramString, localTrace);
    }
    return localTrace;
  }
  
  public boolean isEnabled(int paramInt)
  {
    return paramInt <= this.levelMax;
  }
  
  public void setFileName(String paramString)
  {
    this.fileName = paramString;
  }
  
  public void setMaxFileSize(int paramInt)
  {
    this.maxFileSize = paramInt;
  }
  
  public void setLevelSystemOut(int paramInt)
  {
    this.levelSystemOut = paramInt;
    updateLevel();
  }
  
  public void setLevelFile(int paramInt)
  {
    if (paramInt == 4)
    {
      String str = "org.h2.message.TraceWriterAdapter";
      try
      {
        this.writer = ((TraceWriter)Class.forName(str).newInstance());
      }
      catch (Throwable localThrowable)
      {
        localObject = DbException.get(90086, localThrowable, new String[] { str });
        write(1, "database", str, (Throwable)localObject);
        return;
      }
      Object localObject = this.fileName;
      if (localObject != null)
      {
        if (((String)localObject).endsWith(".trace.db")) {
          localObject = ((String)localObject).substring(0, ((String)localObject).length() - ".trace.db".length());
        }
        int i = Math.max(((String)localObject).lastIndexOf('/'), ((String)localObject).lastIndexOf('\\'));
        if (i >= 0) {
          localObject = ((String)localObject).substring(i + 1);
        }
        this.writer.setName((String)localObject);
      }
    }
    this.levelFile = paramInt;
    updateLevel();
  }
  
  public int getLevelFile()
  {
    return this.levelFile;
  }
  
  private synchronized String format(String paramString1, String paramString2)
  {
    if (this.dateFormat == null) {
      this.dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss ");
    }
    return this.dateFormat.format(Long.valueOf(System.currentTimeMillis())) + paramString1 + ": " + paramString2;
  }
  
  public void write(int paramInt, String paramString1, String paramString2, Throwable paramThrowable)
  {
    if ((paramInt <= this.levelSystemOut) || (paramInt > this.levelMax))
    {
      this.sysOut.println(format(paramString1, paramString2));
      if ((paramThrowable != null) && (this.levelSystemOut == 3)) {
        paramThrowable.printStackTrace(this.sysOut);
      }
    }
    if ((this.fileName != null) && 
      (paramInt <= this.levelFile)) {
      writeFile(format(paramString1, paramString2), paramThrowable);
    }
  }
  
  private synchronized void writeFile(String paramString, Throwable paramThrowable)
  {
    try
    {
      Object localObject;
      if (this.checkSize++ >= 4096)
      {
        this.checkSize = 0;
        closeWriter();
        if ((this.maxFileSize > 0) && (FileUtils.size(this.fileName) > this.maxFileSize))
        {
          localObject = this.fileName + ".old";
          FileUtils.delete((String)localObject);
          FileUtils.move(this.fileName, (String)localObject);
        }
      }
      if (!openWriter()) {
        return;
      }
      this.printWriter.println(paramString);
      if (paramThrowable != null) {
        if ((this.levelFile == 1) && ((paramThrowable instanceof JdbcSQLException)))
        {
          localObject = (JdbcSQLException)paramThrowable;
          int i = ((JdbcSQLException)localObject).getErrorCode();
          if (ErrorCode.isCommon(i)) {
            this.printWriter.println(paramThrowable.toString());
          } else {
            paramThrowable.printStackTrace(this.printWriter);
          }
        }
        else
        {
          paramThrowable.printStackTrace(this.printWriter);
        }
      }
      this.printWriter.flush();
      if (this.closed) {
        closeWriter();
      }
    }
    catch (Exception localException)
    {
      logWritingError(localException);
    }
  }
  
  private void logWritingError(Exception paramException)
  {
    if (this.writingErrorLogged) {
      return;
    }
    this.writingErrorLogged = true;
    DbException localDbException = DbException.get(90034, paramException, new String[] { this.fileName, paramException.toString() });
    
    this.fileName = null;
    this.sysOut.println(localDbException);
    localDbException.printStackTrace();
  }
  
  private boolean openWriter()
  {
    if (this.printWriter == null) {
      try
      {
        FileUtils.createDirectories(FileUtils.getParent(this.fileName));
        if ((FileUtils.exists(this.fileName)) && (!FileUtils.canWrite(this.fileName))) {
          return false;
        }
        this.fileWriter = IOUtils.getBufferedWriter(FileUtils.newOutputStream(this.fileName, true));
        
        this.printWriter = new PrintWriter(this.fileWriter, true);
      }
      catch (Exception localException)
      {
        logWritingError(localException);
        return false;
      }
    }
    return true;
  }
  
  private synchronized void closeWriter()
  {
    if (this.printWriter != null)
    {
      this.printWriter.flush();
      this.printWriter.close();
      this.printWriter = null;
    }
    if (this.fileWriter != null)
    {
      try
      {
        this.fileWriter.close();
      }
      catch (IOException localIOException) {}
      this.fileWriter = null;
    }
  }
  
  public void close()
  {
    closeWriter();
    this.closed = true;
  }
  
  public void setName(String paramString) {}
}
