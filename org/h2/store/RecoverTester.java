package org.h2.store;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import org.h2.command.Prepared;
import org.h2.engine.ConnectionInfo;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.store.fs.FilePathRec;
import org.h2.store.fs.FileUtils;
import org.h2.store.fs.Recorder;
import org.h2.tools.Recover;
import org.h2.util.IOUtils;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class RecoverTester
  implements Recorder
{
  private static RecoverTester instance;
  private String testDatabase = "memFS:reopen";
  private int writeCount = Utils.getProperty("h2.recoverTestOffset", 0);
  private int testEvery = Utils.getProperty("h2.recoverTest", 64);
  private final long maxFileSize = Utils.getProperty("h2.recoverTestMaxFileSize", Integer.MAX_VALUE) * 1024L * 1024L;
  private int verifyCount;
  private final HashSet<String> knownErrors = New.hashSet();
  private volatile boolean testing;
  
  public static synchronized void init(String paramString)
  {
    RecoverTester localRecoverTester = getInstance();
    if (StringUtils.isNumber(paramString)) {
      localRecoverTester.setTestEvery(Integer.parseInt(paramString));
    }
    FilePathRec.setRecorder(localRecoverTester);
  }
  
  public static synchronized RecoverTester getInstance()
  {
    if (instance == null) {
      instance = new RecoverTester();
    }
    return instance;
  }
  
  public void log(int paramInt, String paramString, byte[] paramArrayOfByte, long paramLong)
  {
    if ((paramInt != 8) && (paramInt != 7)) {
      return;
    }
    if ((!paramString.endsWith(".h2.db")) && (!paramString.endsWith(".mv.db"))) {
      return;
    }
    this.writeCount += 1;
    if (this.writeCount % this.testEvery != 0) {
      return;
    }
    if (FileUtils.size(paramString) > this.maxFileSize) {
      return;
    }
    if (this.testing) {
      return;
    }
    this.testing = true;
    PrintWriter localPrintWriter = null;
    try
    {
      localPrintWriter = new PrintWriter(new OutputStreamWriter(FileUtils.newOutputStream(paramString + ".log", true)));
      
      testDatabase(paramString, localPrintWriter);
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
    finally
    {
      IOUtils.closeSilently(localPrintWriter);
      this.testing = false;
    }
  }
  
  private synchronized void testDatabase(String paramString, PrintWriter paramPrintWriter)
  {
    paramPrintWriter.println("+ write #" + this.writeCount + " verify #" + this.verifyCount);
    Object localObject3;
    try
    {
      IOUtils.copyFiles(paramString, this.testDatabase + ".h2.db");
      String str1 = paramString.substring(0, paramString.length() - ".h2.db".length()) + ".mv.db";
      if (FileUtils.exists(str1)) {
        IOUtils.copyFiles(str1, this.testDatabase + ".mv.db");
      }
      this.verifyCount += 1;
      
      localObject1 = new Properties();
      ((Properties)localObject1).setProperty("user", "");
      ((Properties)localObject1).setProperty("password", "");
      ConnectionInfo localConnectionInfo2 = new ConnectionInfo("jdbc:h2:" + this.testDatabase + ";FILE_LOCK=NO;TRACE_LEVEL_FILE=0", (Properties)localObject1);
      
      localObject3 = new Database(localConnectionInfo2, null);
      
      Session localSession = ((Database)localObject3).getSystemSession();
      localSession.prepare("script to '" + this.testDatabase + ".sql'").query(0);
      localSession.prepare("shutdown immediately").update();
      ((Database)localObject3).removeSession(null);
      
      return;
    }
    catch (DbException localDbException)
    {
      Object localObject1 = DbException.toSQLException(localDbException);
      int k = ((SQLException)localObject1).getErrorCode();
      if (k == 28000) {
        return;
      }
      if (k == 90049) {
        return;
      }
      localDbException.printStackTrace(System.out);
    }
    catch (Exception localException1)
    {
      int i = 0;
      if ((localException1 instanceof SQLException)) {
        i = ((SQLException)localException1).getErrorCode();
      }
      if (i == 28000) {
        return;
      }
      if (i == 90049) {
        return;
      }
      localException1.printStackTrace(System.out);
    }
    paramPrintWriter.println("begin ------------------------------ " + this.writeCount);
    try
    {
      Recover.execute(paramString.substring(0, paramString.lastIndexOf('/')), null);
    }
    catch (SQLException localSQLException1) {}
    this.testDatabase += "X";
    try
    {
      IOUtils.copyFiles(paramString, this.testDatabase + ".h2.db");
      
      Properties localProperties = new Properties();
      ConnectionInfo localConnectionInfo1 = new ConnectionInfo("jdbc:h2:" + this.testDatabase + ";FILE_LOCK=NO", localProperties);
      
      localObject2 = new Database(localConnectionInfo1, null);
      
      ((Database)localObject2).removeSession(null);
    }
    catch (Exception localException2)
    {
      int j = 0;
      SQLException localSQLException2;
      if ((localException2 instanceof DbException))
      {
        localSQLException2 = ((DbException)localException2).getSQLException();
        j = ((SQLException)localSQLException2).getErrorCode();
      }
      if (j == 28000) {
        return;
      }
      if (j == 90049) {
        return;
      }
      Object localObject2 = new StringBuilder();
      localObject3 = localSQLException2.getStackTrace();
      for (int m = 0; (m < 10) && (m < localObject3.length); m++) {
        ((StringBuilder)localObject2).append(localObject3[m].toString()).append('\n');
      }
      String str2 = ((StringBuilder)localObject2).toString();
      if (!this.knownErrors.contains(str2))
      {
        paramPrintWriter.println(this.writeCount + " code: " + j + " " + localSQLException2.toString());
        localSQLException2.printStackTrace(System.out);
        this.knownErrors.add(str2);
      }
      else
      {
        paramPrintWriter.println(this.writeCount + " code: " + j);
      }
    }
  }
  
  public void setTestEvery(int paramInt)
  {
    this.testEvery = paramInt;
  }
}
