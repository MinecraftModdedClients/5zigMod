package org.h2.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import org.h2.engine.Database;
import org.h2.engine.SysProperties;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.tools.CompressTool;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.value.Value;
import org.h2.value.ValueLobDb;

public class LobStorageBackend
  implements LobStorageInterface
{
  public static final String LOB_DATA_TABLE = "LOB_DATA";
  private static final String LOB_SCHEMA = "INFORMATION_SCHEMA";
  private static final String LOBS = "INFORMATION_SCHEMA.LOBS";
  private static final String LOB_MAP = "INFORMATION_SCHEMA.LOB_MAP";
  private static final String LOB_DATA = "INFORMATION_SCHEMA.LOB_DATA";
  private static final int BLOCK_LENGTH = 20000;
  private static final int HASH_CACHE_SIZE = 4096;
  JdbcConnection conn;
  final Database database;
  private final HashMap<String, PreparedStatement> prepared = New.hashMap();
  private long nextBlock;
  private final CompressTool compress = CompressTool.getInstance();
  private long[] hashBlocks;
  private boolean init;
  
  public LobStorageBackend(Database paramDatabase)
  {
    this.database = paramDatabase;
  }
  
  public void init()
  {
    if (this.init) {
      return;
    }
    synchronized (this.database)
    {
      if (this.init) {
        return;
      }
      this.init = true;
      this.conn = this.database.getLobConnectionForRegularUse();
      JdbcConnection localJdbcConnection = this.database.getLobConnectionForInit();
      try
      {
        Statement localStatement = localJdbcConnection.createStatement();
        
        int i = 1;
        PreparedStatement localPreparedStatement = localJdbcConnection.prepareStatement("SELECT ZERO() FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND COLUMN_NAME=?");
        
        localPreparedStatement.setString(1, "INFORMATION_SCHEMA");
        localPreparedStatement.setString(2, "LOB_MAP");
        localPreparedStatement.setString(3, "POS");
        
        ResultSet localResultSet = localPreparedStatement.executeQuery();
        if (localResultSet.next())
        {
          localPreparedStatement = localJdbcConnection.prepareStatement("SELECT ZERO() FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?");
          
          localPreparedStatement.setString(1, "INFORMATION_SCHEMA");
          localPreparedStatement.setString(2, "LOB_DATA");
          localResultSet = localPreparedStatement.executeQuery();
          if (localResultSet.next()) {
            i = 0;
          }
        }
        if (i != 0)
        {
          localStatement.execute("CREATE CACHED TABLE IF NOT EXISTS INFORMATION_SCHEMA.LOBS(ID BIGINT PRIMARY KEY, BYTE_COUNT BIGINT, TABLE INT) HIDDEN");
          
          localStatement.execute("CREATE INDEX IF NOT EXISTS INFORMATION_SCHEMA.INDEX_LOB_TABLE ON INFORMATION_SCHEMA.LOBS(TABLE)");
          
          localStatement.execute("CREATE CACHED TABLE IF NOT EXISTS INFORMATION_SCHEMA.LOB_MAP(LOB BIGINT, SEQ INT, POS BIGINT, HASH INT, BLOCK BIGINT, PRIMARY KEY(LOB, SEQ)) HIDDEN");
          
          localStatement.execute("ALTER TABLE INFORMATION_SCHEMA.LOB_MAP RENAME TO INFORMATION_SCHEMA.LOB_MAP HIDDEN");
          
          localStatement.execute("ALTER TABLE INFORMATION_SCHEMA.LOB_MAP ADD IF NOT EXISTS POS BIGINT BEFORE HASH");
          
          localStatement.execute("ALTER TABLE INFORMATION_SCHEMA.LOB_MAP DROP COLUMN IF EXISTS \"OFFSET\"");
          
          localStatement.execute("CREATE INDEX IF NOT EXISTS INFORMATION_SCHEMA.INDEX_LOB_MAP_DATA_LOB ON INFORMATION_SCHEMA.LOB_MAP(BLOCK, LOB)");
          
          localStatement.execute("CREATE CACHED TABLE IF NOT EXISTS INFORMATION_SCHEMA.LOB_DATA(BLOCK BIGINT PRIMARY KEY, COMPRESSED INT, DATA BINARY) HIDDEN");
        }
        localResultSet = localStatement.executeQuery("SELECT MAX(BLOCK) FROM INFORMATION_SCHEMA.LOB_DATA");
        localResultSet.next();
        this.nextBlock = (localResultSet.getLong(1) + 1L);
        localStatement.close();
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convert(localSQLException);
      }
    }
  }
  
  private long getNextLobId()
    throws SQLException
  {
    String str = "SELECT MAX(LOB) FROM INFORMATION_SCHEMA.LOB_MAP";
    PreparedStatement localPreparedStatement = prepare(str);
    ResultSet localResultSet = localPreparedStatement.executeQuery();
    localResultSet.next();
    long l = localResultSet.getLong(1) + 1L;
    reuse(str, localPreparedStatement);
    str = "SELECT MAX(ID) FROM INFORMATION_SCHEMA.LOBS";
    localPreparedStatement = prepare(str);
    localResultSet = localPreparedStatement.executeQuery();
    localResultSet.next();
    l = Math.max(l, localResultSet.getLong(1) + 1L);
    reuse(str, localPreparedStatement);
    return l;
  }
  
  public void removeAllForTable(int paramInt)
  {
    init();
    try
    {
      String str = "SELECT ID FROM INFORMATION_SCHEMA.LOBS WHERE TABLE = ?";
      PreparedStatement localPreparedStatement = prepare(str);
      localPreparedStatement.setInt(1, paramInt);
      ResultSet localResultSet = localPreparedStatement.executeQuery();
      while (localResultSet.next()) {
        removeLob(localResultSet.getLong(1));
      }
      reuse(str, localPreparedStatement);
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
    if (paramInt == -1)
    {
      removeAllForTable(-2);
      removeAllForTable(-3);
    }
  }
  
  byte[] readBlock(long paramLong)
    throws SQLException
  {
    assertNotHolds(this.conn.getSession());
    synchronized (this.database)
    {
      synchronized (this.conn.getSession())
      {
        String str = "SELECT COMPRESSED, DATA FROM INFORMATION_SCHEMA.LOB_DATA WHERE BLOCK = ?";
        
        PreparedStatement localPreparedStatement = prepare(str);
        localPreparedStatement.setLong(1, paramLong);
        ResultSet localResultSet = localPreparedStatement.executeQuery();
        if (!localResultSet.next()) {
          throw DbException.get(90028, "Missing lob entry, block: " + paramLong).getSQLException();
        }
        int i = localResultSet.getInt(1);
        byte[] arrayOfByte = localResultSet.getBytes(2);
        if (i != 0) {
          arrayOfByte = this.compress.expand(arrayOfByte);
        }
        reuse(str, localPreparedStatement);
        return arrayOfByte;
      }
    }
  }
  
  PreparedStatement prepare(String paramString)
    throws SQLException
  {
    if ((SysProperties.CHECK2) && 
      (!Thread.holdsLock(this.database))) {
      throw DbException.throwInternalError();
    }
    PreparedStatement localPreparedStatement = (PreparedStatement)this.prepared.remove(paramString);
    if (localPreparedStatement == null) {
      localPreparedStatement = this.conn.prepareStatement(paramString);
    }
    return localPreparedStatement;
  }
  
  void reuse(String paramString, PreparedStatement paramPreparedStatement)
  {
    if ((SysProperties.CHECK2) && 
      (!Thread.holdsLock(this.database))) {
      throw DbException.throwInternalError();
    }
    this.prepared.put(paramString, paramPreparedStatement);
  }
  
  public void removeLob(ValueLobDb paramValueLobDb)
  {
    removeLob(paramValueLobDb.getLobId());
  }
  
  private void removeLob(long paramLong)
  {
    try
    {
      assertNotHolds(this.conn.getSession());
      synchronized (this.database)
      {
        synchronized (this.conn.getSession())
        {
          String str = "SELECT BLOCK, HASH FROM INFORMATION_SCHEMA.LOB_MAP D WHERE D.LOB = ? AND NOT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.LOB_MAP O WHERE O.BLOCK = D.BLOCK AND O.LOB <> ?)";
          
          PreparedStatement localPreparedStatement = prepare(str);
          localPreparedStatement.setLong(1, paramLong);
          localPreparedStatement.setLong(2, paramLong);
          ResultSet localResultSet = localPreparedStatement.executeQuery();
          ArrayList localArrayList = New.arrayList();
          while (localResultSet.next())
          {
            localArrayList.add(Long.valueOf(localResultSet.getLong(1)));
            int i = localResultSet.getInt(2);
            setHashCacheBlock(i, -1L);
          }
          reuse(str, localPreparedStatement);
          
          str = "DELETE FROM INFORMATION_SCHEMA.LOB_MAP WHERE LOB = ?";
          localPreparedStatement = prepare(str);
          localPreparedStatement.setLong(1, paramLong);
          localPreparedStatement.execute();
          reuse(str, localPreparedStatement);
          
          str = "DELETE FROM INFORMATION_SCHEMA.LOB_DATA WHERE BLOCK = ?";
          localPreparedStatement = prepare(str);
          for (Iterator localIterator = localArrayList.iterator(); localIterator.hasNext();)
          {
            long l = ((Long)localIterator.next()).longValue();
            localPreparedStatement.setLong(1, l);
            localPreparedStatement.execute();
          }
          reuse(str, localPreparedStatement);
          
          str = "DELETE FROM INFORMATION_SCHEMA.LOBS WHERE ID = ?";
          localPreparedStatement = prepare(str);
          localPreparedStatement.setLong(1, paramLong);
          localPreparedStatement.execute();
          reuse(str, localPreparedStatement);
        }
      }
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
  
  /* Error */
  public InputStream getInputStream(ValueLobDb paramValueLobDb, byte[] paramArrayOfByte, long paramLong)
    throws IOException
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 43	org/h2/store/LobStorageBackend:init	()V
    //   4: aload_0
    //   5: getfield 9	org/h2/store/LobStorageBackend:conn	Lorg/h2/jdbc/JdbcConnection;
    //   8: invokevirtual 48	org/h2/jdbc/JdbcConnection:getSession	()Lorg/h2/engine/SessionInterface;
    //   11: invokestatic 49	org/h2/store/LobStorageBackend:assertNotHolds	(Ljava/lang/Object;)V
    //   14: aload_0
    //   15: getfield 6	org/h2/store/LobStorageBackend:database	Lorg/h2/engine/Database;
    //   18: dup
    //   19: astore 5
    //   21: monitorenter
    //   22: aload_0
    //   23: getfield 9	org/h2/store/LobStorageBackend:conn	Lorg/h2/jdbc/JdbcConnection;
    //   26: invokevirtual 48	org/h2/jdbc/JdbcConnection:getSession	()Lorg/h2/engine/SessionInterface;
    //   29: dup
    //   30: astore 6
    //   32: monitorenter
    //   33: aload_1
    //   34: invokevirtual 70	org/h2/value/ValueLobDb:getLobId	()J
    //   37: lstore 7
    //   39: new 87	org/h2/store/LobStorageBackend$LobInputStream
    //   42: dup
    //   43: aload_0
    //   44: lload 7
    //   46: lload_3
    //   47: invokespecial 88	org/h2/store/LobStorageBackend$LobInputStream:<init>	(Lorg/h2/store/LobStorageBackend;JJ)V
    //   50: aload 6
    //   52: monitorexit
    //   53: aload 5
    //   55: monitorexit
    //   56: areturn
    //   57: astore 9
    //   59: aload 6
    //   61: monitorexit
    //   62: aload 9
    //   64: athrow
    //   65: astore 10
    //   67: aload 5
    //   69: monitorexit
    //   70: aload 10
    //   72: athrow
    //   73: astore 5
    //   75: aload 5
    //   77: invokestatic 89	org/h2/message/DbException:convertToIOException	(Ljava/lang/Throwable;)Ljava/io/IOException;
    //   80: athrow
    // Line number table:
    //   Java source line #335	-> byte code offset #0
    //   Java source line #336	-> byte code offset #4
    //   Java source line #338	-> byte code offset #14
    //   Java source line #339	-> byte code offset #22
    //   Java source line #340	-> byte code offset #33
    //   Java source line #341	-> byte code offset #39
    //   Java source line #342	-> byte code offset #57
    //   Java source line #343	-> byte code offset #65
    //   Java source line #344	-> byte code offset #73
    //   Java source line #345	-> byte code offset #75
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	81	0	this	LobStorageBackend
    //   0	81	1	paramValueLobDb	ValueLobDb
    //   0	81	2	paramArrayOfByte	byte[]
    //   0	81	3	paramLong	long
    //   73	3	5	localSQLException	SQLException
    //   37	8	7	l	long
    //   57	6	9	localObject1	Object
    //   65	6	10	localObject2	Object
    // Exception table:
    //   from	to	target	type
    //   33	53	57	finally
    //   57	62	57	finally
    //   22	56	65	finally
    //   57	70	65	finally
    //   0	56	73	java/sql/SQLException
    //   57	73	73	java/sql/SQLException
  }
  
  /* Error */
  private ValueLobDb addLob(InputStream paramInputStream, long paramLong, int paramInt, CountingReaderInputStream paramCountingReaderInputStream)
  {
    // Byte code:
    //   0: sipush 20000
    //   3: newarray <illegal type>
    //   5: astore 6
    //   7: lload_2
    //   8: lconst_0
    //   9: lcmp
    //   10: ifge +7 -> 17
    //   13: ldc2_w 90
    //   16: lstore_2
    //   17: lconst_0
    //   18: lstore 7
    //   20: ldc2_w 75
    //   23: lstore 9
    //   25: aload_0
    //   26: getfield 6	org/h2/store/LobStorageBackend:database	Lorg/h2/engine/Database;
    //   29: invokevirtual 92	org/h2/engine/Database:getMaxLengthInplaceLob	()I
    //   32: istore 11
    //   34: aload_0
    //   35: getfield 6	org/h2/store/LobStorageBackend:database	Lorg/h2/engine/Database;
    //   38: iload 4
    //   40: invokevirtual 93	org/h2/engine/Database:getLobCompressionAlgorithm	(I)Ljava/lang/String;
    //   43: astore 12
    //   45: aconst_null
    //   46: astore 13
    //   48: iconst_0
    //   49: istore 14
    //   51: lload_2
    //   52: lconst_0
    //   53: lcmp
    //   54: ifle +194 -> 248
    //   57: ldc2_w 94
    //   60: lload_2
    //   61: invokestatic 96	java/lang/Math:min	(JJ)J
    //   64: l2i
    //   65: istore 15
    //   67: aload_1
    //   68: aload 6
    //   70: iload 15
    //   72: invokestatic 97	org/h2/util/IOUtils:readFully	(Ljava/io/InputStream;[BI)I
    //   75: istore 15
    //   77: iload 15
    //   79: ifgt +6 -> 85
    //   82: goto +166 -> 248
    //   85: lload_2
    //   86: iload 15
    //   88: i2l
    //   89: lsub
    //   90: lstore_2
    //   91: iload 15
    //   93: aload 6
    //   95: arraylength
    //   96: if_icmpeq +23 -> 119
    //   99: iload 15
    //   101: newarray <illegal type>
    //   103: astore 16
    //   105: aload 6
    //   107: iconst_0
    //   108: aload 16
    //   110: iconst_0
    //   111: iload 15
    //   113: invokestatic 98	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   116: goto +7 -> 123
    //   119: aload 6
    //   121: astore 16
    //   123: iload 14
    //   125: ifne +27 -> 152
    //   128: aload 16
    //   130: arraylength
    //   131: sipush 20000
    //   134: if_icmpge +18 -> 152
    //   137: aload 16
    //   139: arraylength
    //   140: iload 11
    //   142: if_icmpgt +10 -> 152
    //   145: aload 16
    //   147: astore 13
    //   149: goto +99 -> 248
    //   152: aload_0
    //   153: getfield 9	org/h2/store/LobStorageBackend:conn	Lorg/h2/jdbc/JdbcConnection;
    //   156: invokevirtual 48	org/h2/jdbc/JdbcConnection:getSession	()Lorg/h2/engine/SessionInterface;
    //   159: invokestatic 49	org/h2/store/LobStorageBackend:assertNotHolds	(Ljava/lang/Object;)V
    //   162: aload_0
    //   163: getfield 6	org/h2/store/LobStorageBackend:database	Lorg/h2/engine/Database;
    //   166: dup
    //   167: astore 17
    //   169: monitorenter
    //   170: aload_0
    //   171: getfield 9	org/h2/store/LobStorageBackend:conn	Lorg/h2/jdbc/JdbcConnection;
    //   174: invokevirtual 48	org/h2/jdbc/JdbcConnection:getSession	()Lorg/h2/engine/SessionInterface;
    //   177: dup
    //   178: astore 18
    //   180: monitorenter
    //   181: iload 14
    //   183: ifne +9 -> 192
    //   186: aload_0
    //   187: invokespecial 99	org/h2/store/LobStorageBackend:getNextLobId	()J
    //   190: lstore 9
    //   192: aload_0
    //   193: lload 9
    //   195: iload 14
    //   197: lload 7
    //   199: aload 16
    //   201: aload 12
    //   203: invokevirtual 100	org/h2/store/LobStorageBackend:storeBlock	(JIJ[BLjava/lang/String;)V
    //   206: aload 18
    //   208: monitorexit
    //   209: goto +11 -> 220
    //   212: astore 19
    //   214: aload 18
    //   216: monitorexit
    //   217: aload 19
    //   219: athrow
    //   220: aload 17
    //   222: monitorexit
    //   223: goto +11 -> 234
    //   226: astore 20
    //   228: aload 17
    //   230: monitorexit
    //   231: aload 20
    //   233: athrow
    //   234: lload 7
    //   236: iload 15
    //   238: i2l
    //   239: ladd
    //   240: lstore 7
    //   242: iinc 14 1
    //   245: goto -194 -> 51
    //   248: lload 9
    //   250: ldc2_w 75
    //   253: lcmp
    //   254: ifne +13 -> 267
    //   257: aload 13
    //   259: ifnonnull +8 -> 267
    //   262: iconst_0
    //   263: newarray <illegal type>
    //   265: astore 13
    //   267: aload 13
    //   269: ifnull +36 -> 305
    //   272: aload 5
    //   274: ifnonnull +10 -> 284
    //   277: aload 13
    //   279: arraylength
    //   280: i2l
    //   281: goto +8 -> 289
    //   284: aload 5
    //   286: invokevirtual 101	org/h2/store/CountingReaderInputStream:getLength	()J
    //   289: lstore 14
    //   291: iload 4
    //   293: aload 13
    //   295: lload 14
    //   297: invokestatic 102	org/h2/value/ValueLobDb:createSmallLob	(I[BJ)Lorg/h2/value/ValueLobDb;
    //   300: astore 16
    //   302: aload 16
    //   304: areturn
    //   305: aload 5
    //   307: ifnonnull +8 -> 315
    //   310: lload 7
    //   312: goto +8 -> 320
    //   315: aload 5
    //   317: invokevirtual 101	org/h2/store/CountingReaderInputStream:getLength	()J
    //   320: lstore 14
    //   322: aload_0
    //   323: iload 4
    //   325: lload 9
    //   327: bipush -2
    //   329: lload 7
    //   331: lload 14
    //   333: invokespecial 103	org/h2/store/LobStorageBackend:registerLob	(IJIJJ)Lorg/h2/value/ValueLobDb;
    //   336: areturn
    //   337: astore 13
    //   339: lload 9
    //   341: ldc2_w 75
    //   344: lcmp
    //   345: ifeq +9 -> 354
    //   348: aload_0
    //   349: lload 9
    //   351: invokespecial 46	org/h2/store/LobStorageBackend:removeLob	(J)V
    //   354: aload 13
    //   356: aconst_null
    //   357: invokestatic 105	org/h2/message/DbException:convertIOException	(Ljava/io/IOException;Ljava/lang/String;)Lorg/h2/message/DbException;
    //   360: athrow
    //   361: astore 6
    //   363: aload 6
    //   365: invokestatic 37	org/h2/message/DbException:convert	(Ljava/lang/Throwable;)Lorg/h2/message/DbException;
    //   368: athrow
    // Line number table:
    //   Java source line #352	-> byte code offset #0
    //   Java source line #353	-> byte code offset #7
    //   Java source line #354	-> byte code offset #13
    //   Java source line #356	-> byte code offset #17
    //   Java source line #357	-> byte code offset #20
    //   Java source line #358	-> byte code offset #25
    //   Java source line #359	-> byte code offset #34
    //   Java source line #361	-> byte code offset #45
    //   Java source line #362	-> byte code offset #48
    //   Java source line #363	-> byte code offset #57
    //   Java source line #364	-> byte code offset #67
    //   Java source line #365	-> byte code offset #77
    //   Java source line #366	-> byte code offset #82
    //   Java source line #368	-> byte code offset #85
    //   Java source line #371	-> byte code offset #91
    //   Java source line #372	-> byte code offset #99
    //   Java source line #373	-> byte code offset #105
    //   Java source line #375	-> byte code offset #119
    //   Java source line #377	-> byte code offset #123
    //   Java source line #379	-> byte code offset #145
    //   Java source line #380	-> byte code offset #149
    //   Java source line #382	-> byte code offset #152
    //   Java source line #384	-> byte code offset #162
    //   Java source line #385	-> byte code offset #170
    //   Java source line #386	-> byte code offset #181
    //   Java source line #387	-> byte code offset #186
    //   Java source line #389	-> byte code offset #192
    //   Java source line #390	-> byte code offset #206
    //   Java source line #391	-> byte code offset #220
    //   Java source line #392	-> byte code offset #234
    //   Java source line #362	-> byte code offset #242
    //   Java source line #394	-> byte code offset #248
    //   Java source line #396	-> byte code offset #262
    //   Java source line #398	-> byte code offset #267
    //   Java source line #401	-> byte code offset #272
    //   Java source line #403	-> byte code offset #291
    //   Java source line #404	-> byte code offset #302
    //   Java source line #408	-> byte code offset #305
    //   Java source line #410	-> byte code offset #322
    //   Java source line #412	-> byte code offset #337
    //   Java source line #413	-> byte code offset #339
    //   Java source line #414	-> byte code offset #348
    //   Java source line #416	-> byte code offset #354
    //   Java source line #418	-> byte code offset #361
    //   Java source line #419	-> byte code offset #363
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	369	0	this	LobStorageBackend
    //   0	369	1	paramInputStream	InputStream
    //   0	369	2	paramLong	long
    //   0	369	4	paramInt	int
    //   0	369	5	paramCountingReaderInputStream	CountingReaderInputStream
    //   5	115	6	arrayOfByte	byte[]
    //   361	3	6	localSQLException	SQLException
    //   18	312	7	l1	long
    //   23	327	9	l2	long
    //   32	111	11	i	int
    //   43	159	12	str	String
    //   46	248	13	localObject1	Object
    //   337	18	13	localIOException	IOException
    //   49	194	14	j	int
    //   289	43	14	l3	long
    //   65	172	15	k	int
    //   103	200	16	localObject2	Object
    //   212	6	19	localObject3	Object
    //   226	6	20	localObject4	Object
    // Exception table:
    //   from	to	target	type
    //   181	209	212	finally
    //   212	217	212	finally
    //   170	223	226	finally
    //   226	231	226	finally
    //   45	304	337	java/io/IOException
    //   305	336	337	java/io/IOException
    //   0	304	361	java/sql/SQLException
    //   305	336	361	java/sql/SQLException
    //   337	361	361	java/sql/SQLException
  }
  
  private ValueLobDb registerLob(int paramInt1, long paramLong1, int paramInt2, long paramLong2, long paramLong3)
    throws SQLException
  {
    assertNotHolds(this.conn.getSession());
    synchronized (this.database)
    {
      synchronized (this.conn.getSession())
      {
        String str = "INSERT INTO INFORMATION_SCHEMA.LOBS(ID, BYTE_COUNT, TABLE) VALUES(?, ?, ?)";
        
        PreparedStatement localPreparedStatement = prepare(str);
        localPreparedStatement.setLong(1, paramLong1);
        localPreparedStatement.setLong(2, paramLong2);
        localPreparedStatement.setInt(3, paramInt2);
        localPreparedStatement.execute();
        reuse(str, localPreparedStatement);
        ValueLobDb localValueLobDb = ValueLobDb.create(paramInt1, this.database, paramInt2, paramLong1, null, paramLong3);
        
        return localValueLobDb;
      }
    }
  }
  
  public boolean isReadOnly()
  {
    return this.database.isReadOnly();
  }
  
  public ValueLobDb copyLob(ValueLobDb paramValueLobDb, int paramInt, long paramLong)
  {
    int i = paramValueLobDb.getType();
    long l1 = paramValueLobDb.getLobId();
    assertNotHolds(this.conn.getSession());
    synchronized (this.database)
    {
      synchronized (this.conn.getSession())
      {
        try
        {
          init();
          long l2 = getNextLobId();
          String str = "INSERT INTO INFORMATION_SCHEMA.LOB_MAP(LOB, SEQ, POS, HASH, BLOCK) SELECT ?, SEQ, POS, HASH, BLOCK FROM INFORMATION_SCHEMA.LOB_MAP WHERE LOB = ?";
          
          PreparedStatement localPreparedStatement = prepare(str);
          localPreparedStatement.setLong(1, l2);
          localPreparedStatement.setLong(2, l1);
          localPreparedStatement.executeUpdate();
          reuse(str, localPreparedStatement);
          
          str = "INSERT INTO INFORMATION_SCHEMA.LOBS(ID, BYTE_COUNT, TABLE) SELECT ?, BYTE_COUNT, ? FROM INFORMATION_SCHEMA.LOBS WHERE ID = ?";
          
          localPreparedStatement = prepare(str);
          localPreparedStatement.setLong(1, l2);
          localPreparedStatement.setLong(2, paramInt);
          localPreparedStatement.setLong(3, l1);
          localPreparedStatement.executeUpdate();
          reuse(str, localPreparedStatement);
          
          ValueLobDb localValueLobDb = ValueLobDb.create(i, this.database, paramInt, l2, null, paramLong);
          return localValueLobDb;
        }
        catch (SQLException localSQLException)
        {
          throw DbException.convert(localSQLException);
        }
      }
    }
  }
  
  private long getHashCacheBlock(int paramInt)
  {
    initHashCache();
    int i = paramInt & 0xFFF;
    long l = this.hashBlocks[i];
    if (l == paramInt) {
      return this.hashBlocks[(i + 4096)];
    }
    return -1L;
  }
  
  private void setHashCacheBlock(int paramInt, long paramLong)
  {
    initHashCache();
    int i = paramInt & 0xFFF;
    this.hashBlocks[i] = paramInt;
    this.hashBlocks[(i + 4096)] = paramLong;
  }
  
  private void initHashCache()
  {
    if (this.hashBlocks == null) {
      this.hashBlocks = new long[' '];
    }
  }
  
  void storeBlock(long paramLong1, int paramInt, long paramLong2, byte[] paramArrayOfByte, String paramString)
    throws SQLException
  {
    int i = 0;
    if (paramString != null) {
      paramArrayOfByte = this.compress.compress(paramArrayOfByte, paramString);
    }
    int j = Arrays.hashCode(paramArrayOfByte);
    assertHoldsLock(this.conn.getSession());
    assertHoldsLock(this.database);
    long l = getHashCacheBlock(j);
    if (l != -1L)
    {
      str = "SELECT COMPRESSED, DATA FROM INFORMATION_SCHEMA.LOB_DATA WHERE BLOCK = ?";
      
      localPreparedStatement = prepare(str);
      localPreparedStatement.setLong(1, l);
      ResultSet localResultSet = localPreparedStatement.executeQuery();
      if (localResultSet.next())
      {
        int k = localResultSet.getInt(1) != 0 ? 1 : 0;
        byte[] arrayOfByte = localResultSet.getBytes(2);
        if (k == (paramString != null ? 1 : 0)) {
          if (Arrays.equals(paramArrayOfByte, arrayOfByte)) {
            i = 1;
          }
        }
      }
      reuse(str, localPreparedStatement);
    }
    if (i == 0)
    {
      l = this.nextBlock++;
      setHashCacheBlock(j, l);
      str = "INSERT INTO INFORMATION_SCHEMA.LOB_DATA(BLOCK, COMPRESSED, DATA) VALUES(?, ?, ?)";
      
      localPreparedStatement = prepare(str);
      localPreparedStatement.setLong(1, l);
      localPreparedStatement.setInt(2, paramString == null ? 0 : 1);
      localPreparedStatement.setBytes(3, paramArrayOfByte);
      localPreparedStatement.execute();
      reuse(str, localPreparedStatement);
    }
    String str = "INSERT INTO INFORMATION_SCHEMA.LOB_MAP(LOB, SEQ, POS, HASH, BLOCK) VALUES(?, ?, ?, ?, ?)";
    
    PreparedStatement localPreparedStatement = prepare(str);
    localPreparedStatement.setLong(1, paramLong1);
    localPreparedStatement.setInt(2, paramInt);
    localPreparedStatement.setLong(3, paramLong2);
    localPreparedStatement.setLong(4, j);
    localPreparedStatement.setLong(5, l);
    localPreparedStatement.execute();
    reuse(str, localPreparedStatement);
  }
  
  public Value createBlob(InputStream paramInputStream, long paramLong)
  {
    init();
    return addLob(paramInputStream, paramLong, 15, null);
  }
  
  public Value createClob(Reader paramReader, long paramLong)
  {
    init();
    long l = paramLong == -1L ? Long.MAX_VALUE : paramLong;
    CountingReaderInputStream localCountingReaderInputStream = new CountingReaderInputStream(paramReader, l);
    ValueLobDb localValueLobDb = addLob(localCountingReaderInputStream, Long.MAX_VALUE, 16, localCountingReaderInputStream);
    return localValueLobDb;
  }
  
  public void setTable(ValueLobDb paramValueLobDb, int paramInt)
  {
    long l = paramValueLobDb.getLobId();
    assertNotHolds(this.conn.getSession());
    synchronized (this.database)
    {
      synchronized (this.conn.getSession())
      {
        try
        {
          init();
          String str = "UPDATE INFORMATION_SCHEMA.LOBS SET TABLE = ? WHERE ID = ?";
          PreparedStatement localPreparedStatement = prepare(str);
          localPreparedStatement.setInt(1, paramInt);
          localPreparedStatement.setLong(2, l);
          localPreparedStatement.executeUpdate();
          reuse(str, localPreparedStatement);
        }
        catch (SQLException localSQLException)
        {
          throw DbException.convert(localSQLException);
        }
      }
    }
  }
  
  private static void assertNotHolds(Object paramObject)
  {
    if (Thread.holdsLock(paramObject)) {
      throw DbException.throwInternalError();
    }
  }
  
  static void assertHoldsLock(Object paramObject)
  {
    if (!Thread.holdsLock(paramObject)) {
      throw DbException.throwInternalError();
    }
  }
  
  public class LobInputStream
    extends InputStream
  {
    private final long[] lobMapBlocks;
    private int lobMapIndex;
    private long remainingBytes;
    private byte[] buffer;
    private int bufferPos;
    
    public LobInputStream(long paramLong1, long paramLong2)
      throws SQLException
    {
      LobStorageBackend.assertHoldsLock(LobStorageBackend.this.conn.getSession());
      LobStorageBackend.assertHoldsLock(LobStorageBackend.this.database);
      if (paramLong2 == -1L)
      {
        str = "SELECT BYTE_COUNT FROM INFORMATION_SCHEMA.LOBS WHERE ID = ?";
        localPreparedStatement = LobStorageBackend.this.prepare(str);
        localPreparedStatement.setLong(1, paramLong1);
        localResultSet = localPreparedStatement.executeQuery();
        if (!localResultSet.next()) {
          throw DbException.get(90028, "Missing lob entry: " + paramLong1).getSQLException();
        }
        paramLong2 = localResultSet.getLong(1);
        LobStorageBackend.this.reuse(str, localPreparedStatement);
      }
      this.remainingBytes = paramLong2;
      
      String str = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.LOB_MAP WHERE LOB = ?";
      PreparedStatement localPreparedStatement = LobStorageBackend.this.prepare(str);
      localPreparedStatement.setLong(1, paramLong1);
      ResultSet localResultSet = localPreparedStatement.executeQuery();
      localResultSet.next();
      int i = localResultSet.getInt(1);
      if (i == 0) {
        throw DbException.get(90028, "Missing lob entry: " + paramLong1).getSQLException();
      }
      LobStorageBackend.this.reuse(str, localPreparedStatement);
      
      this.lobMapBlocks = new long[i];
      
      str = "SELECT BLOCK FROM INFORMATION_SCHEMA.LOB_MAP WHERE LOB = ? ORDER BY SEQ";
      localPreparedStatement = LobStorageBackend.this.prepare(str);
      localPreparedStatement.setLong(1, paramLong1);
      localResultSet = localPreparedStatement.executeQuery();
      int j = 0;
      while (localResultSet.next())
      {
        this.lobMapBlocks[j] = localResultSet.getLong(1);
        j++;
      }
      LobStorageBackend.this.reuse(str, localPreparedStatement);
    }
    
    public int read()
      throws IOException
    {
      fillBuffer();
      if (this.remainingBytes <= 0L) {
        return -1;
      }
      this.remainingBytes -= 1L;
      return this.buffer[(this.bufferPos++)] & 0xFF;
    }
    
    public long skip(long paramLong)
      throws IOException
    {
      if (paramLong <= 0L) {
        return 0L;
      }
      long l = paramLong;
      l -= skipSmall(l);
      if (l > 20000L)
      {
        while (l > 20000L)
        {
          l -= 20000L;
          this.remainingBytes -= 20000L;
          this.lobMapIndex += 1;
        }
        this.bufferPos = 0;
        this.buffer = null;
      }
      fillBuffer();
      l -= skipSmall(l);
      l -= super.skip(l);
      return paramLong - l;
    }
    
    private int skipSmall(long paramLong)
    {
      if ((this.buffer != null) && (this.bufferPos < this.buffer.length))
      {
        int i = MathUtils.convertLongToInt(Math.min(paramLong, this.buffer.length - this.bufferPos));
        this.bufferPos += i;
        this.remainingBytes -= i;
        return i;
      }
      return 0;
    }
    
    public int available()
      throws IOException
    {
      return MathUtils.convertLongToInt(this.remainingBytes);
    }
    
    public int read(byte[] paramArrayOfByte)
      throws IOException
    {
      return readFully(paramArrayOfByte, 0, paramArrayOfByte.length);
    }
    
    public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
      throws IOException
    {
      return readFully(paramArrayOfByte, paramInt1, paramInt2);
    }
    
    private int readFully(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
      throws IOException
    {
      if (paramInt2 == 0) {
        return 0;
      }
      int i = 0;
      while (paramInt2 > 0)
      {
        fillBuffer();
        if (this.remainingBytes <= 0L) {
          break;
        }
        int j = (int)Math.min(paramInt2, this.remainingBytes);
        j = Math.min(j, this.buffer.length - this.bufferPos);
        System.arraycopy(this.buffer, this.bufferPos, paramArrayOfByte, paramInt1, j);
        this.bufferPos += j;
        i += j;
        this.remainingBytes -= j;
        paramInt1 += j;
        paramInt2 -= j;
      }
      return i == 0 ? -1 : i;
    }
    
    private void fillBuffer()
      throws IOException
    {
      if ((this.buffer != null) && (this.bufferPos < this.buffer.length)) {
        return;
      }
      if (this.remainingBytes <= 0L) {
        return;
      }
      if (this.lobMapIndex >= this.lobMapBlocks.length) {
        System.out.println("halt!");
      }
      try
      {
        this.buffer = LobStorageBackend.this.readBlock(this.lobMapBlocks[this.lobMapIndex]);
        this.lobMapIndex += 1;
        this.bufferPos = 0;
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convertToIOException(localSQLException);
      }
    }
  }
}
