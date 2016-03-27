package org.h2.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.CRC32;
import org.h2.api.JavaObjectSerializer;
import org.h2.compress.CompressLZF;
import org.h2.engine.Constants;
import org.h2.engine.MetaRecord;
import org.h2.engine.SessionInterface;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStore.Builder;
import org.h2.mvstore.MVStoreTool;
import org.h2.mvstore.StreamStore;
import org.h2.mvstore.db.TransactionStore;
import org.h2.mvstore.db.TransactionStore.Transaction;
import org.h2.mvstore.db.TransactionStore.TransactionMap;
import org.h2.mvstore.db.ValueDataType;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SimpleRow;
import org.h2.security.SHA256;
import org.h2.store.Data;
import org.h2.store.DataHandler;
import org.h2.store.DataReader;
import org.h2.store.FileLister;
import org.h2.store.FileStore;
import org.h2.store.FileStoreInputStream;
import org.h2.store.LobStorageBackend;
import org.h2.store.PageFreeList;
import org.h2.store.PageLog;
import org.h2.store.PageStore;
import org.h2.store.fs.FileUtils;
import org.h2.util.BitField;
import org.h2.util.IOUtils;
import org.h2.util.IntArray;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.SmallLRUCache;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.TempFileDeleter;
import org.h2.util.Tool;
import org.h2.util.Utils;
import org.h2.value.Value;
import org.h2.value.Value.ValueBlob;
import org.h2.value.Value.ValueClob;
import org.h2.value.ValueArray;
import org.h2.value.ValueLob;
import org.h2.value.ValueLobDb;
import org.h2.value.ValueLong;

public class Recover
  extends Tool
  implements DataHandler
{
  private String databaseName;
  private int storageId;
  private String storageName;
  private int recordLength;
  private int valueId;
  private boolean trace;
  private boolean transactionLog;
  private ArrayList<MetaRecord> schema;
  private HashSet<Integer> objectIdSet;
  private HashMap<Integer, String> tableMap;
  private HashMap<String, String> columnTypeMap;
  private boolean remove;
  private int pageSize;
  private FileStore store;
  private int[] parents;
  private Stats stat;
  private boolean lobMaps;
  
  static class Stats
  {
    long pageDataEmpty;
    long pageDataRows;
    long pageDataHead;
    final int[] pageTypeCount = new int[10];
    int free;
  }
  
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new Recover().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = ".";
    String str2 = null;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str3 = paramVarArgs[i];
      if ("-dir".equals(str3))
      {
        str1 = paramVarArgs[(++i)];
      }
      else if ("-db".equals(str3))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if ("-removePassword".equals(str3))
      {
        this.remove = true;
      }
      else if ("-trace".equals(str3))
      {
        this.trace = true;
      }
      else if ("-transactionLog".equals(str3))
      {
        this.transactionLog = true;
      }
      else
      {
        if ((str3.equals("-help")) || (str3.equals("-?")))
        {
          showUsage();
          return;
        }
        showUsageAndThrowUnsupportedOption(str3);
      }
    }
    process(str1, str2);
  }
  
  public static Reader readClob(String paramString)
    throws IOException
  {
    return new BufferedReader(new InputStreamReader(readBlob(paramString), Constants.UTF8));
  }
  
  public static InputStream readBlob(String paramString)
    throws IOException
  {
    return new BufferedInputStream(FileUtils.newInputStream(paramString));
  }
  
  public static Value.ValueBlob readBlobDb(Connection paramConnection, long paramLong1, long paramLong2)
  {
    DataHandler localDataHandler = ((JdbcConnection)paramConnection).getSession().getDataHandler();
    return ValueLobDb.create(15, localDataHandler, -2, paramLong1, null, paramLong2);
  }
  
  public static Value.ValueClob readClobDb(Connection paramConnection, long paramLong1, long paramLong2)
  {
    DataHandler localDataHandler = ((JdbcConnection)paramConnection).getSession().getDataHandler();
    return ValueLobDb.create(16, localDataHandler, -2, paramLong1, null, paramLong2);
  }
  
  public static InputStream readBlobMap(Connection paramConnection, long paramLong1, long paramLong2)
    throws SQLException
  {
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("SELECT DATA FROM INFORMATION_SCHEMA.LOB_BLOCKS WHERE LOB_ID = ? AND SEQ = ? AND ? > 0");
    
    localPreparedStatement.setLong(1, paramLong1);
    
    localPreparedStatement.setLong(3, paramLong2);
    new SequenceInputStream(new Enumeration()
    {
      private int seq;
      private byte[] data = fetch();
      
      private byte[] fetch()
      {
        try
        {
          this.val$prep.setInt(2, this.seq++);
          ResultSet localResultSet = this.val$prep.executeQuery();
          if (localResultSet.next()) {
            return localResultSet.getBytes(1);
          }
          return null;
        }
        catch (SQLException localSQLException)
        {
          throw DbException.convert(localSQLException);
        }
      }
      
      public boolean hasMoreElements()
      {
        return this.data != null;
      }
      
      public InputStream nextElement()
      {
        ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(this.data);
        this.data = fetch();
        return localByteArrayInputStream;
      }
    });
  }
  
  public static Reader readClobMap(Connection paramConnection, long paramLong1, long paramLong2)
    throws Exception
  {
    InputStream localInputStream = readBlobMap(paramConnection, paramLong1, paramLong2);
    return new BufferedReader(new InputStreamReader(localInputStream, Constants.UTF8));
  }
  
  private void trace(String paramString)
  {
    if (this.trace) {
      this.out.println(paramString);
    }
  }
  
  private void traceError(String paramString, Throwable paramThrowable)
  {
    this.out.println(paramString + ": " + paramThrowable.toString());
    if (this.trace) {
      paramThrowable.printStackTrace(this.out);
    }
  }
  
  public static void execute(String paramString1, String paramString2)
    throws SQLException
  {
    try
    {
      new Recover().process(paramString1, paramString2);
    }
    catch (DbException localDbException)
    {
      throw DbException.toSQLException(localDbException);
    }
  }
  
  private void process(String paramString1, String paramString2)
  {
    ArrayList localArrayList = FileLister.getDatabaseFiles(paramString1, paramString2, true);
    if (localArrayList.size() == 0) {
      printNoDatabaseFilesFound(paramString1, paramString2);
    }
    for (String str1 : localArrayList) {
      if (str1.endsWith(".h2.db"))
      {
        dumpPageStore(str1);
      }
      else if (str1.endsWith(".lob.db"))
      {
        dumpLob(str1, false);
      }
      else if (str1.endsWith(".mv.db"))
      {
        String str2 = str1.substring(0, str1.length() - ".h2.db".length());
        
        PrintWriter localPrintWriter = getWriter(str1, ".txt");
        MVStoreTool.dump(str1, localPrintWriter, true);
        MVStoreTool.info(str1, localPrintWriter);
        localPrintWriter.close();
        localPrintWriter = getWriter(str2 + ".h2.db", ".sql");
        dumpMVStoreFile(localPrintWriter, str1);
        localPrintWriter.close();
      }
    }
  }
  
  private PrintWriter getWriter(String paramString1, String paramString2)
  {
    paramString1 = paramString1.substring(0, paramString1.length() - 3);
    String str = paramString1 + paramString2;
    trace("Created file: " + str);
    try
    {
      return new PrintWriter(IOUtils.getBufferedWriter(FileUtils.newOutputStream(str, false)));
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  private void writeDataError(PrintWriter paramPrintWriter, String paramString, byte[] paramArrayOfByte)
  {
    paramPrintWriter.println("-- ERROR: " + paramString + " storageId: " + this.storageId + " recordLength: " + this.recordLength + " valueId: " + this.valueId);
    
    StringBuilder localStringBuilder = new StringBuilder();
    int j;
    for (int i = 0; i < paramArrayOfByte.length; i++)
    {
      j = paramArrayOfByte[i] & 0xFF;
      if ((j >= 32) && (j < 128)) {
        localStringBuilder.append((char)j);
      } else {
        localStringBuilder.append('?');
      }
    }
    paramPrintWriter.println("-- dump: " + localStringBuilder.toString());
    localStringBuilder = new StringBuilder();
    for (i = 0; i < paramArrayOfByte.length; i++)
    {
      j = paramArrayOfByte[i] & 0xFF;
      localStringBuilder.append(' ');
      if (j < 16) {
        localStringBuilder.append('0');
      }
      localStringBuilder.append(Integer.toHexString(j));
    }
    paramPrintWriter.println("-- dump: " + localStringBuilder.toString());
  }
  
  private void dumpLob(String paramString, boolean paramBoolean)
  {
    OutputStream localOutputStream = null;
    FileStore localFileStore = null;
    long l = 0L;
    String str = paramString + (paramBoolean ? ".comp" : "") + ".txt";
    FileStoreInputStream localFileStoreInputStream = null;
    try
    {
      localOutputStream = FileUtils.newOutputStream(str, false);
      localFileStore = FileStore.open(null, paramString, "r");
      localFileStore.init();
      localFileStoreInputStream = new FileStoreInputStream(localFileStore, this, paramBoolean, false);
      l = IOUtils.copy(localFileStoreInputStream, localOutputStream);
    }
    catch (Throwable localThrowable) {}finally
    {
      IOUtils.closeSilently(localOutputStream);
      IOUtils.closeSilently(localFileStoreInputStream);
      closeSilently(localFileStore);
    }
    if (l == 0L) {
      try
      {
        FileUtils.delete(str);
      }
      catch (Exception localException)
      {
        traceError(str, localException);
      }
    }
  }
  
  private String getSQL(String paramString, Value paramValue)
  {
    Object localObject;
    byte[] arrayOfByte;
    if ((paramValue instanceof ValueLob))
    {
      localObject = (ValueLob)paramValue;
      arrayOfByte = ((ValueLob)localObject).getSmall();
      if (arrayOfByte == null)
      {
        String str1 = ((ValueLob)localObject).getFileName();
        String str2 = ((ValueLob)localObject).getType() == 15 ? "BLOB" : "CLOB";
        if (((ValueLob)localObject).isCompressed())
        {
          dumpLob(str1, true);
          str1 = str1 + ".comp";
        }
        return "READ_" + str2 + "('" + str1 + ".txt')";
      }
    }
    else if ((paramValue instanceof ValueLobDb))
    {
      localObject = (ValueLobDb)paramValue;
      arrayOfByte = ((ValueLobDb)localObject).getSmall();
      if (arrayOfByte == null)
      {
        int i = ((ValueLobDb)localObject).getType();
        long l1 = ((ValueLobDb)localObject).getLobId();
        long l2 = ((ValueLobDb)localObject).getPrecision();
        String str4;
        String str3;
        if (i == 15)
        {
          str4 = "BLOB";
          str3 = "READ_BLOB";
        }
        else
        {
          str4 = "CLOB";
          str3 = "READ_CLOB";
        }
        if (this.lobMaps) {
          str3 = str3 + "_MAP";
        } else {
          str3 = str3 + "_DB";
        }
        this.columnTypeMap.put(paramString, str4);
        return str3 + "(" + l1 + ", " + l2 + ")";
      }
    }
    return paramValue.getSQL();
  }
  
  private void setDatabaseName(String paramString)
  {
    this.databaseName = paramString;
  }
  
  private void dumpPageStore(String paramString)
  {
    setDatabaseName(paramString.substring(0, paramString.length() - ".h2.db".length()));
    
    PrintWriter localPrintWriter1 = null;
    this.stat = new Stats();
    try
    {
      localPrintWriter1 = getWriter(paramString, ".sql");
      localPrintWriter1.println("CREATE ALIAS IF NOT EXISTS READ_BLOB FOR \"" + getClass().getName() + ".readBlob\";");
      
      localPrintWriter1.println("CREATE ALIAS IF NOT EXISTS READ_CLOB FOR \"" + getClass().getName() + ".readClob\";");
      
      localPrintWriter1.println("CREATE ALIAS IF NOT EXISTS READ_BLOB_DB FOR \"" + getClass().getName() + ".readBlobDb\";");
      
      localPrintWriter1.println("CREATE ALIAS IF NOT EXISTS READ_CLOB_DB FOR \"" + getClass().getName() + ".readClobDb\";");
      
      resetSchema();
      this.store = FileStore.open(null, paramString, this.remove ? "rw" : "r");
      long l1 = this.store.length();
      try
      {
        this.store.init();
      }
      catch (Exception localException)
      {
        writeError(localPrintWriter1, localException);
      }
      Data localData = Data.create(this, 128);
      seek(0L);
      this.store.readFully(localData.getBytes(), 0, 128);
      localData.setPos(48);
      this.pageSize = localData.readInt();
      int i = localData.readByte();
      int j = localData.readByte();
      localPrintWriter1.println("-- pageSize: " + this.pageSize + " writeVersion: " + i + " readVersion: " + j);
      if ((this.pageSize < 64) || (this.pageSize > 32768))
      {
        this.pageSize = 4096;
        localPrintWriter1.println("-- ERROR: page size; using " + this.pageSize);
      }
      long l2 = l1 / this.pageSize;
      this.parents = new int[(int)l2];
      localData = Data.create(this, this.pageSize);
      for (long l3 = 3L; l3 < l2; l3 += 1L)
      {
        localData.reset();
        seek(l3);
        this.store.readFully(localData.getBytes(), 0, 32);
        localData.readByte();
        localData.readShortInt();
        this.parents[((int)l3)] = localData.readInt();
      }
      int k = 0;int m = 0;int n = 0;
      localData = Data.create(this, this.pageSize);
      int i2;
      for (long l4 = 1L; l4 != 3L; l4 += 1L)
      {
        localData.reset();
        seek(l4);
        this.store.readFully(localData.getBytes(), 0, this.pageSize);
        CRC32 localCRC32 = new CRC32();
        localCRC32.update(localData.getBytes(), 4, this.pageSize - 4);
        i1 = (int)localCRC32.getValue();
        i2 = localData.readInt();
        long l6 = localData.readLong();
        int i3 = localData.readInt();
        int i4 = localData.readInt();
        int i5 = localData.readInt();
        if (i1 == i2)
        {
          k = i3;
          m = i4;
          n = i5;
        }
        localPrintWriter1.println("-- head " + l4 + ": writeCounter: " + l6 + " log " + i3 + ":" + i4 + "/" + i5 + " crc " + i2 + " (" + (i1 == i2 ? "ok" : new StringBuilder().append("expected: ").append(i1).toString()) + ")");
      }
      localPrintWriter1.println("-- log " + k + ":" + m + "/" + n);
      
      PrintWriter localPrintWriter2 = new PrintWriter(new OutputStream()
      {
        public void write(int paramAnonymousInt) {}
      });
      dumpPageStore(localPrintWriter2, l2);
      this.stat = new Stats();
      this.schema.clear();
      this.objectIdSet = New.hashSet();
      dumpPageStore(localPrintWriter1, l2);
      writeSchema(localPrintWriter1);
      try
      {
        dumpPageLogStream(localPrintWriter1, k, m, n, l2);
      }
      catch (IOException localIOException) {}
      localPrintWriter1.println("---- Statistics ----");
      localPrintWriter1.println("-- page count: " + l2 + ", free: " + this.stat.free);
      long l5 = Math.max(1L, this.stat.pageDataRows + this.stat.pageDataEmpty + this.stat.pageDataHead);
      
      localPrintWriter1.println("-- page data bytes: head " + this.stat.pageDataHead + ", empty " + this.stat.pageDataEmpty + ", rows " + this.stat.pageDataRows + " (" + (100L - 100L * this.stat.pageDataEmpty / l5) + "% full)");
      for (int i1 = 0; i1 < this.stat.pageTypeCount.length; i1++)
      {
        i2 = this.stat.pageTypeCount[i1];
        if (i2 > 0) {
          localPrintWriter1.println("-- " + getPageType(i1) + " " + 100 * i2 / l2 + "%, " + i2 + " page(s)");
        }
      }
      localPrintWriter1.close();
    }
    catch (Throwable localThrowable)
    {
      writeError(localPrintWriter1, localThrowable);
    }
    finally
    {
      IOUtils.closeSilently(localPrintWriter1);
      closeSilently(this.store);
    }
  }
  
  private void dumpMVStoreFile(PrintWriter paramPrintWriter, String paramString)
  {
    paramPrintWriter.println("-- MVStore");
    paramPrintWriter.println("CREATE ALIAS IF NOT EXISTS READ_BLOB FOR \"" + getClass().getName() + ".readBlob\";");
    
    paramPrintWriter.println("CREATE ALIAS IF NOT EXISTS READ_CLOB FOR \"" + getClass().getName() + ".readClob\";");
    
    paramPrintWriter.println("CREATE ALIAS IF NOT EXISTS READ_BLOB_DB FOR \"" + getClass().getName() + ".readBlobDb\";");
    
    paramPrintWriter.println("CREATE ALIAS IF NOT EXISTS READ_CLOB_DB FOR \"" + getClass().getName() + ".readClobDb\";");
    
    paramPrintWriter.println("CREATE ALIAS IF NOT EXISTS READ_BLOB_MAP FOR \"" + getClass().getName() + ".readBlobMap\";");
    
    paramPrintWriter.println("CREATE ALIAS IF NOT EXISTS READ_CLOB_MAP FOR \"" + getClass().getName() + ".readClobMap\";");
    
    resetSchema();
    setDatabaseName(paramString.substring(0, paramString.length() - ".mv.db".length()));
    
    MVStore localMVStore = new MVStore.Builder().fileName(paramString).readOnly().open();
    
    dumpLobMaps(paramPrintWriter, localMVStore);
    paramPrintWriter.println("-- Meta");
    dumpMeta(paramPrintWriter, localMVStore);
    paramPrintWriter.println("-- Tables");
    TransactionStore localTransactionStore = new TransactionStore(localMVStore);
    try
    {
      localTransactionStore.init();
    }
    catch (Throwable localThrowable1)
    {
      writeError(paramPrintWriter, localThrowable1);
    }
    try
    {
      for (String str1 : localMVStore.getMapNames()) {
        if (str1.startsWith("table."))
        {
          String str2 = str1.substring("table.".length());
          ValueDataType localValueDataType1 = new ValueDataType(null, this, null);
          
          ValueDataType localValueDataType2 = new ValueDataType(null, this, null);
          
          TransactionStore.TransactionMap localTransactionMap = localTransactionStore.begin().openMap(str1, localValueDataType1, localValueDataType2);
          
          Iterator localIterator2 = localTransactionMap.keyIterator(null);
          int i = 0;
          while (localIterator2.hasNext())
          {
            Value localValue = (Value)localIterator2.next();
            Value[] arrayOfValue = ((ValueArray)localTransactionMap.get(localValue)).getList();
            this.recordLength = arrayOfValue.length;
            if (i == 0)
            {
              setStorage(Integer.parseInt(str2));
              for (this.valueId = 0; this.valueId < this.recordLength; this.valueId += 1)
              {
                localObject1 = this.storageName + "." + this.valueId;
                getSQL((String)localObject1, arrayOfValue[this.valueId]);
              }
              createTemporaryTable(paramPrintWriter);
              i = 1;
            }
            Object localObject1 = new StringBuilder();
            ((StringBuilder)localObject1).append("INSERT INTO O_").append(str2).append(" VALUES(");
            Object localObject2;
            for (this.valueId = 0; this.valueId < this.recordLength; this.valueId += 1)
            {
              if (this.valueId > 0) {
                ((StringBuilder)localObject1).append(", ");
              }
              localObject2 = this.storageName + "." + this.valueId;
              ((StringBuilder)localObject1).append(getSQL((String)localObject2, arrayOfValue[this.valueId]));
            }
            ((StringBuilder)localObject1).append(");");
            paramPrintWriter.println(((StringBuilder)localObject1).toString());
            if (this.storageId == 0) {
              try
              {
                localObject2 = new SimpleRow(arrayOfValue);
                MetaRecord localMetaRecord = new MetaRecord((SearchRow)localObject2);
                this.schema.add(localMetaRecord);
                if (localMetaRecord.getObjectType() == 0)
                {
                  String str3 = arrayOfValue[3].getString();
                  String str4 = extractTableOrViewName(str3);
                  this.tableMap.put(Integer.valueOf(localMetaRecord.getId()), str4);
                }
              }
              catch (Throwable localThrowable3)
              {
                writeError(paramPrintWriter, localThrowable3);
              }
            }
          }
        }
      }
      writeSchema(paramPrintWriter);
      paramPrintWriter.println("DROP ALIAS READ_BLOB_MAP;");
      paramPrintWriter.println("DROP ALIAS READ_CLOB_MAP;");
      paramPrintWriter.println("DROP TABLE IF EXISTS INFORMATION_SCHEMA.LOB_BLOCKS;");
    }
    catch (Throwable localThrowable2)
    {
      writeError(paramPrintWriter, localThrowable2);
    }
    finally
    {
      localMVStore.close();
    }
  }
  
  private static void dumpMeta(PrintWriter paramPrintWriter, MVStore paramMVStore)
  {
    MVMap localMVMap = paramMVStore.getMetaMap();
    for (Map.Entry localEntry : localMVMap.entrySet()) {
      paramPrintWriter.println("-- " + (String)localEntry.getKey() + " = " + (String)localEntry.getValue());
    }
  }
  
  private void dumpLobMaps(PrintWriter paramPrintWriter, MVStore paramMVStore)
  {
    this.lobMaps = paramMVStore.hasMap("lobData");
    if (!this.lobMaps) {
      return;
    }
    MVMap localMVMap1 = paramMVStore.openMap("lobData");
    StreamStore localStreamStore = new StreamStore(localMVMap1);
    MVMap localMVMap2 = paramMVStore.openMap("lobMap");
    paramPrintWriter.println("-- LOB");
    paramPrintWriter.println("CREATE TABLE IF NOT EXISTS INFORMATION_SCHEMA.LOB_BLOCKS(LOB_ID BIGINT, SEQ INT, DATA BINARY);");
    for (Map.Entry localEntry : localMVMap2.entrySet())
    {
      long l = ((Long)localEntry.getKey()).longValue();
      Object[] arrayOfObject = (Object[])localEntry.getValue();
      byte[] arrayOfByte1 = (byte[])arrayOfObject[0];
      InputStream localInputStream = localStreamStore.get(arrayOfByte1);
      int i = 8192;
      byte[] arrayOfByte2 = new byte[i];
      try
      {
        for (int j = 0;; j++)
        {
          int k = IOUtils.readFully(localInputStream, arrayOfByte2, arrayOfByte2.length);
          String str = StringUtils.convertBytesToHex(arrayOfByte2, k);
          if (k > 0) {
            paramPrintWriter.println("INSERT INTO INFORMATION_SCHEMA.LOB_BLOCKS VALUES(" + l + ", " + j + ", '" + str + "');");
          }
          if (k != i) {
            break;
          }
        }
      }
      catch (IOException localIOException)
      {
        writeError(paramPrintWriter, localIOException);
      }
    }
  }
  
  private static String getPageType(int paramInt)
  {
    switch (paramInt)
    {
    case 0: 
      return "free";
    case 1: 
      return "data leaf";
    case 2: 
      return "data node";
    case 3: 
      return "data overflow";
    case 4: 
      return "btree leaf";
    case 5: 
      return "btree node";
    case 6: 
      return "free list";
    case 7: 
      return "stream trunk";
    case 8: 
      return "stream data";
    }
    return "[" + paramInt + "]";
  }
  
  private void dumpPageStore(PrintWriter paramPrintWriter, long paramLong)
  {
    Data localData = Data.create(this, this.pageSize);
    for (long l = 3L; l < paramLong; l += 1L)
    {
      localData = Data.create(this, this.pageSize);
      seek(l);
      this.store.readFully(localData.getBytes(), 0, this.pageSize);
      dumpPage(paramPrintWriter, localData, l, paramLong);
    }
  }
  
  private void dumpPage(PrintWriter paramPrintWriter, Data paramData, long paramLong1, long paramLong2)
  {
    try
    {
      int i = paramData.readByte();
      switch (i)
      {
      case 0: 
        this.stat.pageTypeCount[i] += 1;
        return;
      }
      boolean bool = (i & 0x10) != 0;
      i &= 0xFFFFFFEF;
      if (!PageStore.checksumTest(paramData.getBytes(), (int)paramLong1, this.pageSize)) {
        writeDataError(paramPrintWriter, "checksum mismatch type: " + i, paramData.getBytes());
      }
      paramData.readShortInt();
      int j;
      int k;
      int m;
      switch (i)
      {
      case 1: 
        this.stat.pageTypeCount[i] += 1;
        j = paramData.readInt();
        setStorage(paramData.readVarInt());
        k = paramData.readVarInt();
        m = paramData.readShortInt();
        paramPrintWriter.println("-- page " + paramLong1 + ": data leaf " + (bool ? "(last) " : "") + "parent: " + j + " table: " + this.storageId + " entries: " + m + " columns: " + k);
        
        dumpPageDataLeaf(paramPrintWriter, paramData, bool, paramLong1, k, m);
        break;
      case 2: 
        this.stat.pageTypeCount[i] += 1;
        j = paramData.readInt();
        setStorage(paramData.readVarInt());
        k = paramData.readInt();
        m = paramData.readShortInt();
        paramPrintWriter.println("-- page " + paramLong1 + ": data node " + (bool ? "(last) " : "") + "parent: " + j + " table: " + this.storageId + " entries: " + m + " rowCount: " + k);
        
        dumpPageDataNode(paramPrintWriter, paramData, paramLong1, m);
        break;
      case 3: 
        this.stat.pageTypeCount[i] += 1;
        paramPrintWriter.println("-- page " + paramLong1 + ": data overflow " + (bool ? "(last) " : ""));
        
        break;
      case 4: 
        this.stat.pageTypeCount[i] += 1;
        j = paramData.readInt();
        setStorage(paramData.readVarInt());
        k = paramData.readShortInt();
        paramPrintWriter.println("-- page " + paramLong1 + ": b-tree leaf " + (bool ? "(last) " : "") + "parent: " + j + " index: " + this.storageId + " entries: " + k);
        if (this.trace) {
          dumpPageBtreeLeaf(paramPrintWriter, paramData, k, !bool);
        }
        break;
      case 5: 
        this.stat.pageTypeCount[i] += 1;
        j = paramData.readInt();
        setStorage(paramData.readVarInt());
        paramPrintWriter.println("-- page " + paramLong1 + ": b-tree node " + (bool ? "(last) " : "") + "parent: " + j + " index: " + this.storageId);
        
        dumpPageBtreeNode(paramPrintWriter, paramData, paramLong1, !bool);
        break;
      case 6: 
        this.stat.pageTypeCount[i] += 1;
        paramPrintWriter.println("-- page " + paramLong1 + ": free list " + (bool ? "(last)" : ""));
        this.stat.free += dumpPageFreeList(paramPrintWriter, paramData, paramLong1, paramLong2);
        break;
      case 7: 
        this.stat.pageTypeCount[i] += 1;
        paramPrintWriter.println("-- page " + paramLong1 + ": log trunk");
        break;
      case 8: 
        this.stat.pageTypeCount[i] += 1;
        paramPrintWriter.println("-- page " + paramLong1 + ": log data");
        break;
      default: 
        paramPrintWriter.println("-- ERROR page " + paramLong1 + " unknown type " + i);
      }
    }
    catch (Exception localException)
    {
      writeError(paramPrintWriter, localException);
    }
  }
  
  private void dumpPageLogStream(PrintWriter paramPrintWriter, int paramInt1, int paramInt2, int paramInt3, long paramLong)
    throws IOException
  {
    Data localData1 = Data.create(this, this.pageSize);
    DataReader localDataReader = new DataReader(new PageInputStream(paramPrintWriter, this, this.store, paramInt1, paramInt2, paramInt3, this.pageSize));
    
    paramPrintWriter.println("---- Transaction log ----");
    CompressLZF localCompressLZF = new CompressLZF();
    for (;;)
    {
      int i = localDataReader.readByte();
      if (i < 0) {
        break;
      }
      if (i != 0)
      {
        int j;
        Object localObject2;
        if (i == 1)
        {
          j = localDataReader.readVarInt();
          int k = localDataReader.readVarInt();
          byte[] arrayOfByte = new byte[this.pageSize];
          if (k == 0)
          {
            localDataReader.readFully(arrayOfByte, this.pageSize);
          }
          else if (k != 1)
          {
            localObject2 = new byte[k];
            localDataReader.readFully((byte[])localObject2, k);
            try
            {
              localCompressLZF.expand((byte[])localObject2, 0, k, arrayOfByte, 0, this.pageSize);
            }
            catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException)
            {
              throw DbException.convertToIOException(localArrayIndexOutOfBoundsException);
            }
          }
          localObject2 = "";
          int i2 = arrayOfByte[0];
          int i4 = (i2 & 0x10) != 0 ? 1 : 0;
          i2 &= 0xFFFFFFEF;
          switch (i2)
          {
          case 0: 
            localObject2 = "empty";
            break;
          case 1: 
            localObject2 = "data leaf " + (i4 != 0 ? "(last)" : "");
            break;
          case 2: 
            localObject2 = "data node " + (i4 != 0 ? "(last)" : "");
            break;
          case 3: 
            localObject2 = "data overflow " + (i4 != 0 ? "(last)" : "");
            break;
          case 4: 
            localObject2 = "b-tree leaf " + (i4 != 0 ? "(last)" : "");
            break;
          case 5: 
            localObject2 = "b-tree node " + (i4 != 0 ? "(last)" : "");
            break;
          case 6: 
            localObject2 = "free list " + (i4 != 0 ? "(last)" : "");
            break;
          case 7: 
            localObject2 = "log trunk";
            break;
          case 8: 
            localObject2 = "log data";
            break;
          default: 
            localObject2 = "ERROR: unknown type " + i2;
          }
          paramPrintWriter.println("-- undo page " + j + " " + (String)localObject2);
          if (this.trace)
          {
            Data localData2 = Data.create(null, arrayOfByte);
            dumpPage(paramPrintWriter, localData2, j, paramLong);
          }
        }
        else if (i == 5)
        {
          j = localDataReader.readVarInt();
          setStorage(localDataReader.readVarInt());
          Row localRow = PageLog.readRow(localDataReader, localData1);
          paramPrintWriter.println("-- session " + j + " table " + this.storageId + " + " + localRow.toString());
          if (this.transactionLog) {
            if ((this.storageId == 0) && (localRow.getColumnCount() >= 4))
            {
              int m = (int)localRow.getKey();
              localObject2 = localRow.getValue(3).getString();
              String str3 = extractTableOrViewName((String)localObject2);
              if (localRow.getValue(2).getInt() == 0) {
                this.tableMap.put(Integer.valueOf(m), str3);
              }
              paramPrintWriter.println((String)localObject2 + ";");
            }
            else
            {
              String str1 = (String)this.tableMap.get(Integer.valueOf(this.storageId));
              if (str1 != null)
              {
                localObject2 = new StatementBuilder();
                ((StatementBuilder)localObject2).append("INSERT INTO ").append(str1).append(" VALUES(");
                for (int i3 = 0; i3 < localRow.getColumnCount(); i3++)
                {
                  ((StatementBuilder)localObject2).appendExceptFirst(", ");
                  ((StatementBuilder)localObject2).append(localRow.getValue(i3).getSQL());
                }
                ((StatementBuilder)localObject2).append(");");
                paramPrintWriter.println(((StatementBuilder)localObject2).toString());
              }
            }
          }
        }
        else if (i == 6)
        {
          j = localDataReader.readVarInt();
          setStorage(localDataReader.readVarInt());
          long l = localDataReader.readVarLong();
          paramPrintWriter.println("-- session " + j + " table " + this.storageId + " - " + l);
          if (this.transactionLog)
          {
            String str4;
            if (this.storageId == 0)
            {
              int i1 = (int)l;
              str4 = (String)this.tableMap.get(Integer.valueOf(i1));
              if (str4 != null) {
                paramPrintWriter.println("DROP TABLE IF EXISTS " + str4 + ";");
              }
            }
            else
            {
              String str2 = (String)this.tableMap.get(Integer.valueOf(this.storageId));
              if (str2 != null)
              {
                str4 = "DELETE FROM " + str2 + " WHERE _ROWID_ = " + l + ";";
                
                paramPrintWriter.println(str4);
              }
            }
          }
        }
        else if (i == 7)
        {
          j = localDataReader.readVarInt();
          setStorage(localDataReader.readVarInt());
          paramPrintWriter.println("-- session " + j + " table " + this.storageId + " truncate");
          if (this.transactionLog) {
            paramPrintWriter.println("TRUNCATE TABLE " + this.storageId);
          }
        }
        else if (i == 2)
        {
          j = localDataReader.readVarInt();
          paramPrintWriter.println("-- commit " + j);
        }
        else if (i == 4)
        {
          j = localDataReader.readVarInt();
          paramPrintWriter.println("-- rollback " + j);
        }
        else
        {
          Object localObject1;
          if (i == 3)
          {
            j = localDataReader.readVarInt();
            localObject1 = localDataReader.readString();
            paramPrintWriter.println("-- prepare commit " + j + " " + (String)localObject1);
          }
          else if (i != 0)
          {
            if (i == 8)
            {
              paramPrintWriter.println("-- checkpoint");
            }
            else if (i == 9)
            {
              j = localDataReader.readVarInt();
              localObject1 = new StringBuilder("-- free");
              for (int n = 0; n < j; n++) {
                ((StringBuilder)localObject1).append(' ').append(localDataReader.readVarInt());
              }
              paramPrintWriter.println(localObject1);
            }
            else
            {
              paramPrintWriter.println("-- ERROR: unknown operation " + i);
              break;
            }
          }
        }
      }
    }
  }
  
  private String setStorage(int paramInt)
  {
    this.storageId = paramInt;
    this.storageName = ("O_" + String.valueOf(paramInt).replace('-', 'M'));
    return this.storageName;
  }
  
  static class PageInputStream
    extends InputStream
  {
    private final PrintWriter writer;
    private final FileStore store;
    private final Data page;
    private final int pageSize;
    private long trunkPage;
    private long nextTrunkPage;
    private long dataPage;
    private final IntArray dataPages = new IntArray();
    private boolean endOfFile;
    private int remaining;
    private int logKey;
    
    public PageInputStream(PrintWriter paramPrintWriter, DataHandler paramDataHandler, FileStore paramFileStore, int paramInt1, long paramLong1, long paramLong2, int paramInt2)
    {
      this.writer = paramPrintWriter;
      this.store = paramFileStore;
      this.pageSize = paramInt2;
      this.logKey = (paramInt1 - 1);
      this.nextTrunkPage = paramLong1;
      this.dataPage = paramLong2;
      this.page = Data.create(paramDataHandler, paramInt2);
    }
    
    public int read()
    {
      byte[] arrayOfByte = { 0 };
      int i = read(arrayOfByte);
      return i < 0 ? -1 : arrayOfByte[0] & 0xFF;
    }
    
    public int read(byte[] paramArrayOfByte)
    {
      return read(paramArrayOfByte, 0, paramArrayOfByte.length);
    }
    
    public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    {
      if (paramInt2 == 0) {
        return 0;
      }
      int i = 0;
      while (paramInt2 > 0)
      {
        int j = readBlock(paramArrayOfByte, paramInt1, paramInt2);
        if (j < 0) {
          break;
        }
        i += j;
        paramInt1 += j;
        paramInt2 -= j;
      }
      return i == 0 ? -1 : i;
    }
    
    private int readBlock(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    {
      fillBuffer();
      if (this.endOfFile) {
        return -1;
      }
      int i = Math.min(this.remaining, paramInt2);
      this.page.read(paramArrayOfByte, paramInt1, i);
      this.remaining -= i;
      return i;
    }
    
    private void fillBuffer()
    {
      if ((this.remaining > 0) || (this.endOfFile)) {
        return;
      }
      int k;
      int m;
      int n;
      while (this.dataPages.size() == 0)
      {
        if (this.nextTrunkPage == 0L)
        {
          this.endOfFile = true;
          return;
        }
        this.trunkPage = this.nextTrunkPage;
        this.store.seek(this.trunkPage * this.pageSize);
        this.store.readFully(this.page.getBytes(), 0, this.pageSize);
        this.page.reset();
        if (!PageStore.checksumTest(this.page.getBytes(), (int)this.trunkPage, this.pageSize))
        {
          this.writer.println("-- ERROR: checksum mismatch page: " + this.trunkPage);
          this.endOfFile = true;
          return;
        }
        int i = this.page.readByte();
        this.page.readShortInt();
        if (i != 7)
        {
          this.writer.println("-- log eof " + this.trunkPage + " type: " + i + " expected type: " + 7);
          
          this.endOfFile = true;
          return;
        }
        this.page.readInt();
        int j = this.page.readInt();
        this.logKey += 1;
        if (j != this.logKey) {
          this.writer.println("-- log eof " + this.trunkPage + " type: " + i + " expected key: " + this.logKey + " got: " + j);
        }
        this.nextTrunkPage = this.page.readInt();
        this.writer.println("-- log " + j + ":" + this.trunkPage + " next: " + this.nextTrunkPage);
        
        k = this.page.readShortInt();
        for (m = 0; m < k; m++)
        {
          n = this.page.readInt();
          if (this.dataPage != 0L)
          {
            if (n == this.dataPage) {
              this.dataPage = 0L;
            }
          }
          else {
            this.dataPages.add(n);
          }
        }
      }
      if (this.dataPages.size() > 0)
      {
        this.page.reset();
        long l = this.dataPages.get(0);
        this.dataPages.remove(0);
        this.store.seek(l * this.pageSize);
        this.store.readFully(this.page.getBytes(), 0, this.pageSize);
        this.page.reset();
        k = this.page.readByte();
        if ((k != 0) && (!PageStore.checksumTest(this.page.getBytes(), (int)l, this.pageSize)))
        {
          this.writer.println("-- ERROR: checksum mismatch page: " + l);
          this.endOfFile = true;
          return;
        }
        this.page.readShortInt();
        m = this.page.readInt();
        n = this.page.readInt();
        this.writer.println("-- log " + n + ":" + this.trunkPage + "/" + l);
        if (k != 8)
        {
          this.writer.println("-- log eof " + l + " type: " + k + " parent: " + m + " expected type: " + 8);
          
          this.endOfFile = true;
          return;
        }
        if (n != this.logKey)
        {
          this.writer.println("-- log eof " + l + " type: " + k + " parent: " + m + " expected key: " + this.logKey + " got: " + n);
          
          this.endOfFile = true;
          return;
        }
        this.remaining = (this.pageSize - this.page.length());
      }
    }
  }
  
  private void dumpPageBtreeNode(PrintWriter paramPrintWriter, Data paramData, long paramLong, boolean paramBoolean)
  {
    int i = paramData.readInt();
    int j = paramData.readShortInt();
    int[] arrayOfInt1 = new int[j + 1];
    int[] arrayOfInt2 = new int[j];
    arrayOfInt1[j] = paramData.readInt();
    checkParent(paramPrintWriter, paramLong, arrayOfInt1, j);
    int k = Integer.MAX_VALUE;
    int n;
    for (int m = 0; m < j; m++)
    {
      arrayOfInt1[m] = paramData.readInt();
      checkParent(paramPrintWriter, paramLong, arrayOfInt1, m);
      n = paramData.readShortInt();
      k = Math.min(n, k);
      arrayOfInt2[m] = n;
    }
    k -= paramData.length();
    if (!this.trace) {
      return;
    }
    paramPrintWriter.println("--   empty: " + k);
    for (m = 0; m < j; m++)
    {
      n = arrayOfInt2[m];
      paramData.setPos(n);
      long l = paramData.readVarLong();
      Object localObject;
      if (paramBoolean) {
        localObject = ValueLong.get(l);
      } else {
        try
        {
          localObject = paramData.readValue();
        }
        catch (Throwable localThrowable)
        {
          writeDataError(paramPrintWriter, "exception " + localThrowable, paramData.getBytes());
          continue;
        }
      }
      paramPrintWriter.println("-- [" + m + "] child: " + arrayOfInt1[m] + " key: " + l + " data: " + localObject);
    }
    paramPrintWriter.println("-- [" + j + "] child: " + arrayOfInt1[j] + " rowCount: " + i);
  }
  
  private int dumpPageFreeList(PrintWriter paramPrintWriter, Data paramData, long paramLong1, long paramLong2)
  {
    int i = PageFreeList.getPagesAddressed(this.pageSize);
    BitField localBitField = new BitField();
    for (int j = 0; j < i; j += 8)
    {
      int k = paramData.readByte() & 0xFF;
      for (int m = 0; m < 8; m++) {
        if ((k & 1 << m) != 0) {
          localBitField.set(j + m);
        }
      }
    }
    j = 0;
    long l1 = 0L;
    for (long l2 = paramLong1; (l1 < i) && (l2 < paramLong2); l2 += 1L)
    {
      if ((l1 == 0L) || (l2 % 100L == 0L))
      {
        if (l1 > 0L) {
          paramPrintWriter.println();
        }
        paramPrintWriter.print("-- " + l2 + " ");
      }
      else if (l2 % 20L == 0L)
      {
        paramPrintWriter.print(" - ");
      }
      else if (l2 % 10L == 0L)
      {
        paramPrintWriter.print(' ');
      }
      paramPrintWriter.print(localBitField.get((int)l1) ? '1' : '0');
      if (!localBitField.get((int)l1)) {
        j++;
      }
      l1 += 1L;
    }
    paramPrintWriter.println();
    return j;
  }
  
  private void dumpPageBtreeLeaf(PrintWriter paramPrintWriter, Data paramData, int paramInt, boolean paramBoolean)
  {
    int[] arrayOfInt = new int[paramInt];
    int i = Integer.MAX_VALUE;
    int k;
    for (int j = 0; j < paramInt; j++)
    {
      k = paramData.readShortInt();
      i = Math.min(k, i);
      arrayOfInt[j] = k;
    }
    i -= paramData.length();
    paramPrintWriter.println("--   empty: " + i);
    for (j = 0; j < paramInt; j++)
    {
      k = arrayOfInt[j];
      paramData.setPos(k);
      long l = paramData.readVarLong();
      Object localObject;
      if (paramBoolean) {
        localObject = ValueLong.get(l);
      } else {
        try
        {
          localObject = paramData.readValue();
        }
        catch (Throwable localThrowable)
        {
          writeDataError(paramPrintWriter, "exception " + localThrowable, paramData.getBytes());
          continue;
        }
      }
      paramPrintWriter.println("-- [" + j + "] key: " + l + " data: " + localObject);
    }
  }
  
  private void checkParent(PrintWriter paramPrintWriter, long paramLong, int[] paramArrayOfInt, int paramInt)
  {
    int i = paramArrayOfInt[paramInt];
    if ((i < 0) || (i >= this.parents.length)) {
      paramPrintWriter.println("-- ERROR [" + paramLong + "] child[" + paramInt + "]: " + i + " >= page count: " + this.parents.length);
    } else if (this.parents[i] != paramLong) {
      paramPrintWriter.println("-- ERROR [" + paramLong + "] child[" + paramInt + "]: " + i + " parent: " + this.parents[i]);
    }
  }
  
  private void dumpPageDataNode(PrintWriter paramPrintWriter, Data paramData, long paramLong, int paramInt)
  {
    int[] arrayOfInt = new int[paramInt + 1];
    long[] arrayOfLong = new long[paramInt];
    arrayOfInt[paramInt] = paramData.readInt();
    checkParent(paramPrintWriter, paramLong, arrayOfInt, paramInt);
    for (int i = 0; i < paramInt; i++)
    {
      arrayOfInt[i] = paramData.readInt();
      checkParent(paramPrintWriter, paramLong, arrayOfInt, i);
      arrayOfLong[i] = paramData.readVarLong();
    }
    if (!this.trace) {
      return;
    }
    for (i = 0; i < paramInt; i++) {
      paramPrintWriter.println("-- [" + i + "] child: " + arrayOfInt[i] + " key: " + arrayOfLong[i]);
    }
    paramPrintWriter.println("-- [" + paramInt + "] child: " + arrayOfInt[paramInt]);
  }
  
  private void dumpPageDataLeaf(PrintWriter paramPrintWriter, Data paramData, boolean paramBoolean, long paramLong, int paramInt1, int paramInt2)
  {
    long[] arrayOfLong = new long[paramInt2];
    int[] arrayOfInt = new int[paramInt2];
    long l1 = 0L;
    if (!paramBoolean)
    {
      l1 = paramData.readInt();
      paramPrintWriter.println("--   next: " + l1);
    }
    int i = this.pageSize;
    for (int j = 0; j < paramInt2; j++)
    {
      arrayOfLong[j] = paramData.readVarLong();
      int m = paramData.readShortInt();
      i = Math.min(m, i);
      arrayOfInt[j] = m;
    }
    this.stat.pageDataRows += this.pageSize - i;
    i -= paramData.length();
    this.stat.pageDataHead += paramData.length();
    this.stat.pageDataEmpty += i;
    if (this.trace) {
      paramPrintWriter.println("--   empty: " + i);
    }
    long l2;
    int n;
    if (!paramBoolean)
    {
      Data localData = Data.create(this, this.pageSize);
      paramData.setPos(this.pageSize);
      l2 = paramLong;
      for (;;)
      {
        checkParent(paramPrintWriter, l2, new int[] { (int)l1 }, 0);
        l2 = l1;
        seek(l1);
        this.store.readFully(localData.getBytes(), 0, this.pageSize);
        localData.reset();
        n = localData.readByte();
        localData.readShortInt();
        localData.readInt();
        int i1;
        if (n == 19)
        {
          i1 = localData.readShortInt();
          paramPrintWriter.println("-- chain: " + l1 + " type: " + n + " size: " + i1);
          
          paramData.checkCapacity(i1);
          paramData.write(localData.getBytes(), localData.length(), i1);
          break;
        }
        if (n == 3)
        {
          l1 = localData.readInt();
          if (l1 == 0L)
          {
            writeDataError(paramPrintWriter, "next:0", localData.getBytes());
            break;
          }
          i1 = this.pageSize - localData.length();
          paramPrintWriter.println("-- chain: " + l1 + " type: " + n + " size: " + i1 + " next: " + l1);
          
          paramData.checkCapacity(i1);
          paramData.write(localData.getBytes(), localData.length(), i1);
        }
        else
        {
          writeDataError(paramPrintWriter, "type: " + n, localData.getBytes());
          break;
        }
      }
    }
    for (int k = 0; k < paramInt2; k++)
    {
      l2 = arrayOfLong[k];
      n = arrayOfInt[k];
      if (this.trace) {
        paramPrintWriter.println("-- [" + k + "] storage: " + this.storageId + " key: " + l2 + " off: " + n);
      }
      paramData.setPos(n);
      Value[] arrayOfValue = createRecord(paramPrintWriter, paramData, paramInt1);
      if (arrayOfValue != null)
      {
        createTemporaryTable(paramPrintWriter);
        writeRow(paramPrintWriter, paramData, arrayOfValue);
        if ((this.remove) && (this.storageId == 0))
        {
          String str1 = arrayOfValue[3].getString();
          if (str1.startsWith("CREATE USER "))
          {
            int i2 = Utils.indexOf(paramData.getBytes(), "SALT ".getBytes(), n);
            if (i2 >= 0)
            {
              String str2 = str1.substring("CREATE USER ".length(), str1.indexOf("SALT ") - 1);
              if (str2.startsWith("IF NOT EXISTS ")) {
                str2 = str2.substring("IF NOT EXISTS ".length());
              }
              if (str2.startsWith("\"")) {
                str2 = str2.substring(1, str2.length() - 1);
              }
              byte[] arrayOfByte1 = SHA256.getKeyPasswordHash(str2, "".toCharArray());
              
              byte[] arrayOfByte2 = MathUtils.secureRandomBytes(8);
              byte[] arrayOfByte3 = SHA256.getHashWithSalt(arrayOfByte1, arrayOfByte2);
              
              StringBuilder localStringBuilder = new StringBuilder();
              localStringBuilder.append("SALT '").append(StringUtils.convertBytesToHex(arrayOfByte2)).append("' HASH '").append(StringUtils.convertBytesToHex(arrayOfByte3)).append('\'');
              
              byte[] arrayOfByte4 = localStringBuilder.toString().getBytes();
              System.arraycopy(arrayOfByte4, 0, paramData.getBytes(), i2, arrayOfByte4.length);
              
              seek(paramLong);
              this.store.write(paramData.getBytes(), 0, this.pageSize);
              if (this.trace) {
                this.out.println("User: " + str2);
              }
              this.remove = false;
            }
          }
        }
      }
    }
  }
  
  private void seek(long paramLong)
  {
    this.store.seek(paramLong * this.pageSize);
  }
  
  private Value[] createRecord(PrintWriter paramPrintWriter, Data paramData, int paramInt)
  {
    this.recordLength = paramInt;
    if (paramInt <= 0)
    {
      writeDataError(paramPrintWriter, "columnCount<0", paramData.getBytes());
      return null;
    }
    Value[] arrayOfValue;
    try
    {
      arrayOfValue = new Value[paramInt];
    }
    catch (OutOfMemoryError localOutOfMemoryError)
    {
      writeDataError(paramPrintWriter, "out of memory", paramData.getBytes());
      return null;
    }
    return arrayOfValue;
  }
  
  private void writeRow(PrintWriter paramPrintWriter, Data paramData, Value[] paramArrayOfValue)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("INSERT INTO " + this.storageName + " VALUES(");
    Object localObject;
    for (this.valueId = 0; this.valueId < this.recordLength; this.valueId += 1) {
      try
      {
        Value localValue = paramData.readValue();
        paramArrayOfValue[this.valueId] = localValue;
        if (this.valueId > 0) {
          localStringBuilder.append(", ");
        }
        localObject = this.storageName + "." + this.valueId;
        localStringBuilder.append(getSQL((String)localObject, localValue));
      }
      catch (Exception localException)
      {
        writeDataError(paramPrintWriter, "exception " + localException, paramData.getBytes());
      }
      catch (OutOfMemoryError localOutOfMemoryError)
      {
        writeDataError(paramPrintWriter, "out of memory", paramData.getBytes());
      }
    }
    localStringBuilder.append(");");
    paramPrintWriter.println(localStringBuilder.toString());
    if (this.storageId == 0) {
      try
      {
        SimpleRow localSimpleRow = new SimpleRow(paramArrayOfValue);
        localObject = new MetaRecord(localSimpleRow);
        this.schema.add(localObject);
        if (((MetaRecord)localObject).getObjectType() == 0)
        {
          String str1 = paramArrayOfValue[3].getString();
          String str2 = extractTableOrViewName(str1);
          this.tableMap.put(Integer.valueOf(((MetaRecord)localObject).getId()), str2);
        }
      }
      catch (Throwable localThrowable)
      {
        writeError(paramPrintWriter, localThrowable);
      }
    }
  }
  
  private void resetSchema()
  {
    this.schema = New.arrayList();
    this.objectIdSet = New.hashSet();
    this.tableMap = New.hashMap();
    this.columnTypeMap = New.hashMap();
  }
  
  private void writeSchema(PrintWriter paramPrintWriter)
  {
    paramPrintWriter.println("---- Schema ----");
    Collections.sort(this.schema);
    for (Iterator localIterator = this.schema.iterator(); localIterator.hasNext();)
    {
      localObject1 = (MetaRecord)localIterator.next();
      if (!isSchemaObjectTypeDelayed((MetaRecord)localObject1))
      {
        localObject2 = ((MetaRecord)localObject1).getSQL();
        paramPrintWriter.println((String)localObject2 + ";");
      }
    }
    Object localObject2;
    int i = 0;
    for (Object localObject1 = this.tableMap.entrySet().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Map.Entry)((Iterator)localObject1).next();
      localObject3 = (Integer)((Map.Entry)localObject2).getKey();
      str = (String)((Map.Entry)localObject2).getValue();
      if ((this.objectIdSet.contains(localObject3)) && 
        (str.startsWith("INFORMATION_SCHEMA.LOB")))
      {
        setStorage(((Integer)localObject3).intValue());
        paramPrintWriter.println("DELETE FROM " + str + ";");
        paramPrintWriter.println("INSERT INTO " + str + " SELECT * FROM " + this.storageName + ";");
        if (str.startsWith("INFORMATION_SCHEMA.LOBS"))
        {
          paramPrintWriter.println("UPDATE " + str + " SET TABLE = " + -2 + ";");
          
          i = 1;
        }
      }
    }
    Object localObject3;
    String str;
    for (localObject1 = this.tableMap.entrySet().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Map.Entry)((Iterator)localObject1).next();
      localObject3 = (Integer)((Map.Entry)localObject2).getKey();
      str = (String)((Map.Entry)localObject2).getValue();
      if (this.objectIdSet.contains(localObject3))
      {
        setStorage(((Integer)localObject3).intValue());
        if (!str.startsWith("INFORMATION_SCHEMA.LOB")) {
          paramPrintWriter.println("INSERT INTO " + str + " SELECT * FROM " + this.storageName + ";");
        }
      }
    }
    for (localObject1 = this.objectIdSet.iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Integer)((Iterator)localObject1).next();
      setStorage(((Integer)localObject2).intValue());
      paramPrintWriter.println("DROP TABLE " + this.storageName + ";");
    }
    paramPrintWriter.println("DROP ALIAS READ_BLOB;");
    paramPrintWriter.println("DROP ALIAS READ_CLOB;");
    paramPrintWriter.println("DROP ALIAS READ_BLOB_DB;");
    paramPrintWriter.println("DROP ALIAS READ_CLOB_DB;");
    if (i != 0) {
      paramPrintWriter.println("DELETE FROM INFORMATION_SCHEMA.LOBS WHERE TABLE = -2;");
    }
    for (localObject1 = this.schema.iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (MetaRecord)((Iterator)localObject1).next();
      if (isSchemaObjectTypeDelayed((MetaRecord)localObject2))
      {
        localObject3 = ((MetaRecord)localObject2).getSQL();
        paramPrintWriter.println((String)localObject3 + ";");
      }
    }
  }
  
  private static boolean isSchemaObjectTypeDelayed(MetaRecord paramMetaRecord)
  {
    switch (paramMetaRecord.getObjectType())
    {
    case 1: 
    case 4: 
    case 5: 
      return true;
    }
    return false;
  }
  
  private void createTemporaryTable(PrintWriter paramPrintWriter)
  {
    if (!this.objectIdSet.contains(Integer.valueOf(this.storageId)))
    {
      this.objectIdSet.add(Integer.valueOf(this.storageId));
      StatementBuilder localStatementBuilder = new StatementBuilder("CREATE TABLE ");
      localStatementBuilder.append(this.storageName).append('(');
      for (int i = 0; i < this.recordLength; i++)
      {
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append('C').append(i).append(' ');
        String str = (String)this.columnTypeMap.get(this.storageName + "." + i);
        if (str == null) {
          localStatementBuilder.append("VARCHAR");
        } else {
          localStatementBuilder.append(str);
        }
      }
      paramPrintWriter.println(localStatementBuilder.append(");").toString());
      paramPrintWriter.flush();
    }
  }
  
  private static String extractTableOrViewName(String paramString)
  {
    int i = paramString.indexOf(" TABLE ");
    int j = paramString.indexOf(" VIEW ");
    if ((i > 0) && (j > 0)) {
      if (i < j) {
        j = -1;
      } else {
        i = -1;
      }
    }
    if (j > 0) {
      paramString = paramString.substring(j + " VIEW ".length());
    } else if (i > 0) {
      paramString = paramString.substring(i + " TABLE ".length());
    } else {
      return "UNKNOWN";
    }
    if (paramString.startsWith("IF NOT EXISTS ")) {
      paramString = paramString.substring("IF NOT EXISTS ".length());
    }
    int k = 0;
    for (int m = 0; m < paramString.length(); m++)
    {
      int n = paramString.charAt(m);
      if (n == 34)
      {
        k = k == 0 ? 1 : 0;
      }
      else if ((k == 0) && ((n <= 32) || (n == 40)))
      {
        paramString = paramString.substring(0, m);
        return paramString;
      }
    }
    return "UNKNOWN";
  }
  
  private static void closeSilently(FileStore paramFileStore)
  {
    if (paramFileStore != null) {
      paramFileStore.closeSilently();
    }
  }
  
  private void writeError(PrintWriter paramPrintWriter, Throwable paramThrowable)
  {
    if (paramPrintWriter != null) {
      paramPrintWriter.println("// error: " + paramThrowable);
    }
    traceError("Error", paramThrowable);
  }
  
  public String getDatabasePath()
  {
    return this.databaseName;
  }
  
  public FileStore openFile(String paramString1, String paramString2, boolean paramBoolean)
  {
    return FileStore.open(this, paramString1, "rw");
  }
  
  public void checkPowerOff() {}
  
  public void checkWritingAllowed() {}
  
  public int getMaxLengthInplaceLob()
  {
    throw DbException.throwInternalError();
  }
  
  public String getLobCompressionAlgorithm(int paramInt)
  {
    return null;
  }
  
  public Object getLobSyncObject()
  {
    return this;
  }
  
  public SmallLRUCache<String, String[]> getLobFileListCache()
  {
    return null;
  }
  
  public TempFileDeleter getTempFileDeleter()
  {
    return TempFileDeleter.getInstance();
  }
  
  public LobStorageBackend getLobStorage()
  {
    return null;
  }
  
  public int readLob(long paramLong1, byte[] paramArrayOfByte1, long paramLong2, byte[] paramArrayOfByte2, int paramInt1, int paramInt2)
  {
    throw DbException.throwInternalError();
  }
  
  public JavaObjectSerializer getJavaObjectSerializer()
  {
    return null;
  }
}
