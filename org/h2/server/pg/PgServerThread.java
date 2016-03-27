package org.h2.server.pg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import org.h2.engine.SysProperties;
import org.h2.jdbc.JdbcPreparedStatement;
import org.h2.jdbc.JdbcStatement;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.ScriptReader;
import org.h2.util.StringUtils;
import org.h2.util.Utils;
import org.h2.value.CaseInsensitiveMap;

public class PgServerThread
  implements Runnable
{
  private final PgServer server;
  private Socket socket;
  private Connection conn;
  private boolean stop;
  private DataInputStream dataInRaw;
  private DataInputStream dataIn;
  private OutputStream out;
  private int messageType;
  private ByteArrayOutputStream outBuffer;
  private DataOutputStream dataOut;
  private Thread thread;
  private boolean initDone;
  private String userName;
  private String databaseName;
  private int processId;
  private int secret;
  private JdbcStatement activeRequest;
  private String clientEncoding = SysProperties.PG_DEFAULT_CLIENT_ENCODING;
  private String dateStyle = "ISO";
  private final HashMap<String, Prepared> prepared = new CaseInsensitiveMap();
  private final HashMap<String, Portal> portals = new CaseInsensitiveMap();
  
  PgServerThread(Socket paramSocket, PgServer paramPgServer)
  {
    this.server = paramPgServer;
    this.socket = paramSocket;
    this.secret = ((int)MathUtils.secureRandomLong());
  }
  
  public void run()
  {
    try
    {
      this.server.trace("Connect");
      InputStream localInputStream = this.socket.getInputStream();
      this.out = this.socket.getOutputStream();
      this.dataInRaw = new DataInputStream(localInputStream);
      while (!this.stop)
      {
        process();
        this.out.flush();
      }
    }
    catch (EOFException localEOFException) {}catch (Exception localException)
    {
      this.server.traceError(localException);
    }
    finally
    {
      this.server.trace("Disconnect");
      close();
    }
  }
  
  private String readString()
    throws IOException
  {
    ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
    for (;;)
    {
      int i = this.dataIn.read();
      if (i <= 0) {
        break;
      }
      localByteArrayOutputStream.write(i);
    }
    return new String(localByteArrayOutputStream.toByteArray(), getEncoding());
  }
  
  private int readInt()
    throws IOException
  {
    return this.dataIn.readInt();
  }
  
  private short readShort()
    throws IOException
  {
    return this.dataIn.readShort();
  }
  
  private byte readByte()
    throws IOException
  {
    return this.dataIn.readByte();
  }
  
  private void readFully(byte[] paramArrayOfByte)
    throws IOException
  {
    this.dataIn.readFully(paramArrayOfByte);
  }
  
  /* Error */
  private void process()
    throws IOException
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 43	org/h2/server/pg/PgServerThread:initDone	Z
    //   4: ifeq +21 -> 25
    //   7: aload_0
    //   8: getfield 21	org/h2/server/pg/PgServerThread:dataInRaw	Ljava/io/DataInputStream;
    //   11: invokevirtual 33	java/io/DataInputStream:read	()I
    //   14: istore_1
    //   15: iload_1
    //   16: ifge +11 -> 27
    //   19: aload_0
    //   20: iconst_1
    //   21: putfield 22	org/h2/server/pg/PgServerThread:stop	Z
    //   24: return
    //   25: iconst_0
    //   26: istore_1
    //   27: aload_0
    //   28: getfield 21	org/h2/server/pg/PgServerThread:dataInRaw	Ljava/io/DataInputStream;
    //   31: invokevirtual 39	java/io/DataInputStream:readInt	()I
    //   34: istore_2
    //   35: iinc 2 -4
    //   38: iload_2
    //   39: invokestatic 44	org/h2/mvstore/DataUtils:newBytes	(I)[B
    //   42: astore_3
    //   43: aload_0
    //   44: getfield 21	org/h2/server/pg/PgServerThread:dataInRaw	Ljava/io/DataInputStream;
    //   47: aload_3
    //   48: iconst_0
    //   49: iload_2
    //   50: invokevirtual 45	java/io/DataInputStream:readFully	([BII)V
    //   53: aload_0
    //   54: new 19	java/io/DataInputStream
    //   57: dup
    //   58: new 46	java/io/ByteArrayInputStream
    //   61: dup
    //   62: aload_3
    //   63: iconst_0
    //   64: iload_2
    //   65: invokespecial 47	java/io/ByteArrayInputStream:<init>	([BII)V
    //   68: invokespecial 20	java/io/DataInputStream:<init>	(Ljava/io/InputStream;)V
    //   71: putfield 32	org/h2/server/pg/PgServerThread:dataIn	Ljava/io/DataInputStream;
    //   74: iload_1
    //   75: lookupswitch	default:+1884->1959, 0:+89->164, 66:+795->870, 67:+1022->1097, 68:+1144->1219, 69:+1356->1431, 80:+644->719, 81:+1600->1675, 83:+1584->1659, 88:+1868->1943, 112:+461->536
    //   164: aload_0
    //   165: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   168: ldc 48
    //   170: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   173: aload_0
    //   174: invokespecial 49	org/h2/server/pg/PgServerThread:readInt	()I
    //   177: istore 4
    //   179: iload 4
    //   181: ldc 50
    //   183: if_icmpne +102 -> 285
    //   186: aload_0
    //   187: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   190: ldc 51
    //   192: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   195: aload_0
    //   196: invokespecial 49	org/h2/server/pg/PgServerThread:readInt	()I
    //   199: istore 5
    //   201: aload_0
    //   202: invokespecial 49	org/h2/server/pg/PgServerThread:readInt	()I
    //   205: istore 6
    //   207: aload_0
    //   208: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   211: iload 5
    //   213: invokevirtual 52	org/h2/server/pg/PgServer:getThread	(I)Lorg/h2/server/pg/PgServerThread;
    //   216: astore 7
    //   218: aload 7
    //   220: ifnull +21 -> 241
    //   223: iload 6
    //   225: aload 7
    //   227: getfield 13	org/h2/server/pg/PgServerThread:secret	I
    //   230: if_icmpne +11 -> 241
    //   233: aload 7
    //   235: invokespecial 53	org/h2/server/pg/PgServerThread:cancelRequest	()V
    //   238: goto +40 -> 278
    //   241: aload_0
    //   242: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   245: new 54	java/lang/StringBuilder
    //   248: dup
    //   249: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   252: ldc 56
    //   254: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   257: iload 5
    //   259: invokevirtual 58	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   262: ldc 59
    //   264: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   267: iload 6
    //   269: invokevirtual 58	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   272: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   275: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   278: aload_0
    //   279: invokevirtual 26	org/h2/server/pg/PgServerThread:close	()V
    //   282: goto +1718 -> 2000
    //   285: iload 4
    //   287: ldc 61
    //   289: if_icmpne +24 -> 313
    //   292: aload_0
    //   293: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   296: ldc 62
    //   298: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   301: aload_0
    //   302: getfield 18	org/h2/server/pg/PgServerThread:out	Ljava/io/OutputStream;
    //   305: bipush 78
    //   307: invokevirtual 63	java/io/OutputStream:write	(I)V
    //   310: goto +1690 -> 2000
    //   313: aload_0
    //   314: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   317: ldc 64
    //   319: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   322: aload_0
    //   323: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   326: new 54	java/lang/StringBuilder
    //   329: dup
    //   330: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   333: ldc 65
    //   335: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   338: iload 4
    //   340: invokevirtual 58	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   343: ldc 66
    //   345: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   348: iload 4
    //   350: bipush 16
    //   352: ishr
    //   353: invokevirtual 58	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   356: ldc 67
    //   358: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   361: iload 4
    //   363: sipush 255
    //   366: iand
    //   367: invokevirtual 58	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   370: ldc 68
    //   372: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   375: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   378: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   381: aload_0
    //   382: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   385: astore 5
    //   387: aload 5
    //   389: invokevirtual 70	java/lang/String:length	()I
    //   392: ifne +6 -> 398
    //   395: goto +129 -> 524
    //   398: aload_0
    //   399: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   402: astore 6
    //   404: ldc 71
    //   406: aload 5
    //   408: invokevirtual 72	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   411: ifeq +12 -> 423
    //   414: aload_0
    //   415: aload 6
    //   417: putfield 73	org/h2/server/pg/PgServerThread:userName	Ljava/lang/String;
    //   420: goto +64 -> 484
    //   423: ldc 74
    //   425: aload 5
    //   427: invokevirtual 72	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   430: ifeq +19 -> 449
    //   433: aload_0
    //   434: aload_0
    //   435: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   438: aload 6
    //   440: invokevirtual 75	org/h2/server/pg/PgServer:checkKeyAndGetDatabaseName	(Ljava/lang/String;)Ljava/lang/String;
    //   443: putfield 76	org/h2/server/pg/PgServerThread:databaseName	Ljava/lang/String;
    //   446: goto +38 -> 484
    //   449: ldc 77
    //   451: aload 5
    //   453: invokevirtual 72	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   456: ifeq +12 -> 468
    //   459: aload_0
    //   460: aload 6
    //   462: putfield 3	org/h2/server/pg/PgServerThread:clientEncoding	Ljava/lang/String;
    //   465: goto +19 -> 484
    //   468: ldc 78
    //   470: aload 5
    //   472: invokevirtual 72	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   475: ifeq +9 -> 484
    //   478: aload_0
    //   479: aload 6
    //   481: putfield 5	org/h2/server/pg/PgServerThread:dateStyle	Ljava/lang/String;
    //   484: aload_0
    //   485: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   488: new 54	java/lang/StringBuilder
    //   491: dup
    //   492: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   495: ldc 79
    //   497: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   500: aload 5
    //   502: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   505: ldc 80
    //   507: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   510: aload 6
    //   512: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   515: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   518: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   521: goto -140 -> 381
    //   524: aload_0
    //   525: invokespecial 81	org/h2/server/pg/PgServerThread:sendAuthenticationCleartextPassword	()V
    //   528: aload_0
    //   529: iconst_1
    //   530: putfield 43	org/h2/server/pg/PgServerThread:initDone	Z
    //   533: goto +1467 -> 2000
    //   536: aload_0
    //   537: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   540: ldc 82
    //   542: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   545: aload_0
    //   546: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   549: astore 5
    //   551: new 83	java/util/Properties
    //   554: dup
    //   555: invokespecial 84	java/util/Properties:<init>	()V
    //   558: astore 6
    //   560: aload 6
    //   562: ldc 85
    //   564: ldc 86
    //   566: invokevirtual 87	java/util/Properties:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   569: pop
    //   570: aload 6
    //   572: ldc 88
    //   574: aload_0
    //   575: getfield 73	org/h2/server/pg/PgServerThread:userName	Ljava/lang/String;
    //   578: invokevirtual 87	java/util/Properties:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   581: pop
    //   582: aload 6
    //   584: ldc 89
    //   586: aload 5
    //   588: invokevirtual 87	java/util/Properties:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   591: pop
    //   592: new 54	java/lang/StringBuilder
    //   595: dup
    //   596: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   599: ldc 90
    //   601: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   604: aload_0
    //   605: getfield 76	org/h2/server/pg/PgServerThread:databaseName	Ljava/lang/String;
    //   608: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   611: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   614: astore 7
    //   616: new 91	org/h2/engine/ConnectionInfo
    //   619: dup
    //   620: aload 7
    //   622: aload 6
    //   624: invokespecial 92	org/h2/engine/ConnectionInfo:<init>	(Ljava/lang/String;Ljava/util/Properties;)V
    //   627: astore 8
    //   629: aload_0
    //   630: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   633: invokevirtual 93	org/h2/server/pg/PgServer:getBaseDir	()Ljava/lang/String;
    //   636: astore 9
    //   638: aload 9
    //   640: ifnonnull +8 -> 648
    //   643: invokestatic 94	org/h2/engine/SysProperties:getBaseDir	()Ljava/lang/String;
    //   646: astore 9
    //   648: aload 9
    //   650: ifnull +10 -> 660
    //   653: aload 8
    //   655: aload 9
    //   657: invokevirtual 95	org/h2/engine/ConnectionInfo:setBaseDir	(Ljava/lang/String;)V
    //   660: aload_0
    //   661: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   664: invokevirtual 96	org/h2/server/pg/PgServer:getIfExists	()Z
    //   667: ifeq +12 -> 679
    //   670: aload 8
    //   672: ldc 97
    //   674: ldc 98
    //   676: invokevirtual 99	org/h2/engine/ConnectionInfo:setProperty	(Ljava/lang/String;Ljava/lang/String;)V
    //   679: aload_0
    //   680: new 100	org/h2/jdbc/JdbcConnection
    //   683: dup
    //   684: aload 8
    //   686: iconst_0
    //   687: invokespecial 101	org/h2/jdbc/JdbcConnection:<init>	(Lorg/h2/engine/ConnectionInfo;Z)V
    //   690: putfield 102	org/h2/server/pg/PgServerThread:conn	Ljava/sql/Connection;
    //   693: aload_0
    //   694: invokespecial 103	org/h2/server/pg/PgServerThread:initDb	()V
    //   697: aload_0
    //   698: invokespecial 104	org/h2/server/pg/PgServerThread:sendAuthenticationOk	()V
    //   701: goto +1299 -> 2000
    //   704: astore 6
    //   706: aload 6
    //   708: invokevirtual 105	java/lang/Exception:printStackTrace	()V
    //   711: aload_0
    //   712: iconst_1
    //   713: putfield 22	org/h2/server/pg/PgServerThread:stop	Z
    //   716: goto +1284 -> 2000
    //   719: aload_0
    //   720: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   723: ldc 106
    //   725: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   728: new 107	org/h2/server/pg/PgServerThread$Prepared
    //   731: dup
    //   732: invokespecial 108	org/h2/server/pg/PgServerThread$Prepared:<init>	()V
    //   735: astore 5
    //   737: aload 5
    //   739: aload_0
    //   740: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   743: putfield 109	org/h2/server/pg/PgServerThread$Prepared:name	Ljava/lang/String;
    //   746: aload 5
    //   748: aload_0
    //   749: aload_0
    //   750: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   753: invokespecial 110	org/h2/server/pg/PgServerThread:getSQL	(Ljava/lang/String;)Ljava/lang/String;
    //   756: putfield 111	org/h2/server/pg/PgServerThread$Prepared:sql	Ljava/lang/String;
    //   759: aload_0
    //   760: invokespecial 112	org/h2/server/pg/PgServerThread:readShort	()S
    //   763: istore 6
    //   765: aload 5
    //   767: iload 6
    //   769: newarray <illegal type>
    //   771: putfield 113	org/h2/server/pg/PgServerThread$Prepared:paramType	[I
    //   774: iconst_0
    //   775: istore 7
    //   777: iload 7
    //   779: iload 6
    //   781: if_icmpge +34 -> 815
    //   784: aload_0
    //   785: invokespecial 49	org/h2/server/pg/PgServerThread:readInt	()I
    //   788: istore 8
    //   790: aload_0
    //   791: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   794: iload 8
    //   796: invokevirtual 114	org/h2/server/pg/PgServer:checkType	(I)V
    //   799: aload 5
    //   801: getfield 113	org/h2/server/pg/PgServerThread$Prepared:paramType	[I
    //   804: iload 7
    //   806: iload 8
    //   808: iastore
    //   809: iinc 7 1
    //   812: goto -35 -> 777
    //   815: aload 5
    //   817: aload_0
    //   818: getfield 102	org/h2/server/pg/PgServerThread:conn	Ljava/sql/Connection;
    //   821: aload 5
    //   823: getfield 111	org/h2/server/pg/PgServerThread$Prepared:sql	Ljava/lang/String;
    //   826: invokeinterface 115 2 0
    //   831: checkcast 116	org/h2/jdbc/JdbcPreparedStatement
    //   834: putfield 117	org/h2/server/pg/PgServerThread$Prepared:prep	Lorg/h2/jdbc/JdbcPreparedStatement;
    //   837: aload_0
    //   838: getfield 8	org/h2/server/pg/PgServerThread:prepared	Ljava/util/HashMap;
    //   841: aload 5
    //   843: getfield 109	org/h2/server/pg/PgServerThread$Prepared:name	Ljava/lang/String;
    //   846: aload 5
    //   848: invokevirtual 118	java/util/HashMap:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   851: pop
    //   852: aload_0
    //   853: invokespecial 119	org/h2/server/pg/PgServerThread:sendParseComplete	()V
    //   856: goto +1144 -> 2000
    //   859: astore 7
    //   861: aload_0
    //   862: aload 7
    //   864: invokespecial 120	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/Exception;)V
    //   867: goto +1133 -> 2000
    //   870: aload_0
    //   871: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   874: ldc 121
    //   876: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   879: new 122	org/h2/server/pg/PgServerThread$Portal
    //   882: dup
    //   883: invokespecial 123	org/h2/server/pg/PgServerThread$Portal:<init>	()V
    //   886: astore 5
    //   888: aload 5
    //   890: aload_0
    //   891: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   894: putfield 124	org/h2/server/pg/PgServerThread$Portal:name	Ljava/lang/String;
    //   897: aload_0
    //   898: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   901: astore 6
    //   903: aload_0
    //   904: getfield 8	org/h2/server/pg/PgServerThread:prepared	Ljava/util/HashMap;
    //   907: aload 6
    //   909: invokevirtual 125	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   912: checkcast 107	org/h2/server/pg/PgServerThread$Prepared
    //   915: astore 7
    //   917: aload 7
    //   919: ifnonnull +12 -> 931
    //   922: aload_0
    //   923: ldc 126
    //   925: invokespecial 127	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/String;)V
    //   928: goto +1072 -> 2000
    //   931: aload 5
    //   933: aload 7
    //   935: putfield 128	org/h2/server/pg/PgServerThread$Portal:prep	Lorg/h2/server/pg/PgServerThread$Prepared;
    //   938: aload_0
    //   939: getfield 9	org/h2/server/pg/PgServerThread:portals	Ljava/util/HashMap;
    //   942: aload 5
    //   944: getfield 124	org/h2/server/pg/PgServerThread$Portal:name	Ljava/lang/String;
    //   947: aload 5
    //   949: invokevirtual 118	java/util/HashMap:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   952: pop
    //   953: aload_0
    //   954: invokespecial 112	org/h2/server/pg/PgServerThread:readShort	()S
    //   957: istore 8
    //   959: iload 8
    //   961: newarray <illegal type>
    //   963: astore 9
    //   965: iconst_0
    //   966: istore 10
    //   968: iload 10
    //   970: iload 8
    //   972: if_icmpge +18 -> 990
    //   975: aload 9
    //   977: iload 10
    //   979: aload_0
    //   980: invokespecial 112	org/h2/server/pg/PgServerThread:readShort	()S
    //   983: iastore
    //   984: iinc 10 1
    //   987: goto -19 -> 968
    //   990: aload_0
    //   991: invokespecial 112	org/h2/server/pg/PgServerThread:readShort	()S
    //   994: istore 10
    //   996: iconst_0
    //   997: istore 11
    //   999: iload 11
    //   1001: iload 10
    //   1003: if_icmpge +30 -> 1033
    //   1006: aload_0
    //   1007: aload 7
    //   1009: getfield 117	org/h2/server/pg/PgServerThread$Prepared:prep	Lorg/h2/jdbc/JdbcPreparedStatement;
    //   1012: aload 7
    //   1014: getfield 113	org/h2/server/pg/PgServerThread$Prepared:paramType	[I
    //   1017: iload 11
    //   1019: iaload
    //   1020: iload 11
    //   1022: aload 9
    //   1024: invokespecial 129	org/h2/server/pg/PgServerThread:setParameter	(Ljava/sql/PreparedStatement;II[I)V
    //   1027: iinc 11 1
    //   1030: goto -31 -> 999
    //   1033: goto +14 -> 1047
    //   1036: astore 11
    //   1038: aload_0
    //   1039: aload 11
    //   1041: invokespecial 120	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/Exception;)V
    //   1044: goto +956 -> 2000
    //   1047: aload_0
    //   1048: invokespecial 112	org/h2/server/pg/PgServerThread:readShort	()S
    //   1051: istore 11
    //   1053: aload 5
    //   1055: iload 11
    //   1057: newarray <illegal type>
    //   1059: putfield 130	org/h2/server/pg/PgServerThread$Portal:resultColumnFormat	[I
    //   1062: iconst_0
    //   1063: istore 12
    //   1065: iload 12
    //   1067: iload 11
    //   1069: if_icmpge +21 -> 1090
    //   1072: aload 5
    //   1074: getfield 130	org/h2/server/pg/PgServerThread$Portal:resultColumnFormat	[I
    //   1077: iload 12
    //   1079: aload_0
    //   1080: invokespecial 112	org/h2/server/pg/PgServerThread:readShort	()S
    //   1083: iastore
    //   1084: iinc 12 1
    //   1087: goto -22 -> 1065
    //   1090: aload_0
    //   1091: invokespecial 131	org/h2/server/pg/PgServerThread:sendBindComplete	()V
    //   1094: goto +906 -> 2000
    //   1097: aload_0
    //   1098: invokespecial 132	org/h2/server/pg/PgServerThread:readByte	()B
    //   1101: i2c
    //   1102: istore 5
    //   1104: aload_0
    //   1105: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   1108: astore 6
    //   1110: aload_0
    //   1111: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1114: ldc -123
    //   1116: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1119: iload 5
    //   1121: bipush 83
    //   1123: if_icmpne +33 -> 1156
    //   1126: aload_0
    //   1127: getfield 8	org/h2/server/pg/PgServerThread:prepared	Ljava/util/HashMap;
    //   1130: aload 6
    //   1132: invokevirtual 134	java/util/HashMap:remove	(Ljava/lang/Object;)Ljava/lang/Object;
    //   1135: checkcast 107	org/h2/server/pg/PgServerThread$Prepared
    //   1138: astore 7
    //   1140: aload 7
    //   1142: ifnull +11 -> 1153
    //   1145: aload 7
    //   1147: getfield 117	org/h2/server/pg/PgServerThread$Prepared:prep	Lorg/h2/jdbc/JdbcPreparedStatement;
    //   1150: invokestatic 135	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   1153: goto +59 -> 1212
    //   1156: iload 5
    //   1158: bipush 80
    //   1160: if_icmpne +16 -> 1176
    //   1163: aload_0
    //   1164: getfield 9	org/h2/server/pg/PgServerThread:portals	Ljava/util/HashMap;
    //   1167: aload 6
    //   1169: invokevirtual 134	java/util/HashMap:remove	(Ljava/lang/Object;)Ljava/lang/Object;
    //   1172: pop
    //   1173: goto +39 -> 1212
    //   1176: aload_0
    //   1177: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1180: new 54	java/lang/StringBuilder
    //   1183: dup
    //   1184: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   1187: ldc -120
    //   1189: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1192: iload 5
    //   1194: invokevirtual 137	java/lang/StringBuilder:append	(C)Ljava/lang/StringBuilder;
    //   1197: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   1200: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1203: aload_0
    //   1204: ldc -118
    //   1206: invokespecial 127	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/String;)V
    //   1209: goto +791 -> 2000
    //   1212: aload_0
    //   1213: invokespecial 139	org/h2/server/pg/PgServerThread:sendCloseComplete	()V
    //   1216: goto +784 -> 2000
    //   1219: aload_0
    //   1220: invokespecial 132	org/h2/server/pg/PgServerThread:readByte	()B
    //   1223: i2c
    //   1224: istore 5
    //   1226: aload_0
    //   1227: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   1230: astore 6
    //   1232: aload_0
    //   1233: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1236: ldc -116
    //   1238: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1241: iload 5
    //   1243: bipush 83
    //   1245: if_icmpne +58 -> 1303
    //   1248: aload_0
    //   1249: getfield 8	org/h2/server/pg/PgServerThread:prepared	Ljava/util/HashMap;
    //   1252: aload 6
    //   1254: invokevirtual 125	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   1257: checkcast 107	org/h2/server/pg/PgServerThread$Prepared
    //   1260: astore 7
    //   1262: aload 7
    //   1264: ifnonnull +30 -> 1294
    //   1267: aload_0
    //   1268: new 54	java/lang/StringBuilder
    //   1271: dup
    //   1272: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   1275: ldc -115
    //   1277: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1280: aload 6
    //   1282: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1285: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   1288: invokespecial 127	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/String;)V
    //   1291: goto +9 -> 1300
    //   1294: aload_0
    //   1295: aload 7
    //   1297: invokespecial 142	org/h2/server/pg/PgServerThread:sendParameterDescription	(Lorg/h2/server/pg/PgServerThread$Prepared;)V
    //   1300: goto +700 -> 2000
    //   1303: iload 5
    //   1305: bipush 80
    //   1307: if_icmpne +88 -> 1395
    //   1310: aload_0
    //   1311: getfield 9	org/h2/server/pg/PgServerThread:portals	Ljava/util/HashMap;
    //   1314: aload 6
    //   1316: invokevirtual 125	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   1319: checkcast 122	org/h2/server/pg/PgServerThread$Portal
    //   1322: astore 7
    //   1324: aload 7
    //   1326: ifnonnull +30 -> 1356
    //   1329: aload_0
    //   1330: new 54	java/lang/StringBuilder
    //   1333: dup
    //   1334: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   1337: ldc -113
    //   1339: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1342: aload 6
    //   1344: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1347: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   1350: invokespecial 127	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/String;)V
    //   1353: goto +39 -> 1392
    //   1356: aload 7
    //   1358: getfield 128	org/h2/server/pg/PgServerThread$Portal:prep	Lorg/h2/server/pg/PgServerThread$Prepared;
    //   1361: getfield 117	org/h2/server/pg/PgServerThread$Prepared:prep	Lorg/h2/jdbc/JdbcPreparedStatement;
    //   1364: astore 8
    //   1366: aload 8
    //   1368: invokeinterface 144 1 0
    //   1373: astore 9
    //   1375: aload_0
    //   1376: aload 9
    //   1378: invokespecial 145	org/h2/server/pg/PgServerThread:sendRowDescription	(Ljava/sql/ResultSetMetaData;)V
    //   1381: goto +11 -> 1392
    //   1384: astore 9
    //   1386: aload_0
    //   1387: aload 9
    //   1389: invokespecial 120	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/Exception;)V
    //   1392: goto +608 -> 2000
    //   1395: aload_0
    //   1396: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1399: new 54	java/lang/StringBuilder
    //   1402: dup
    //   1403: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   1406: ldc -120
    //   1408: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1411: iload 5
    //   1413: invokevirtual 137	java/lang/StringBuilder:append	(C)Ljava/lang/StringBuilder;
    //   1416: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   1419: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1422: aload_0
    //   1423: ldc -118
    //   1425: invokespecial 127	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/String;)V
    //   1428: goto +572 -> 2000
    //   1431: aload_0
    //   1432: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   1435: astore 5
    //   1437: aload_0
    //   1438: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1441: ldc -110
    //   1443: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1446: aload_0
    //   1447: getfield 9	org/h2/server/pg/PgServerThread:portals	Ljava/util/HashMap;
    //   1450: aload 5
    //   1452: invokevirtual 125	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   1455: checkcast 122	org/h2/server/pg/PgServerThread$Portal
    //   1458: astore 6
    //   1460: aload 6
    //   1462: ifnonnull +30 -> 1492
    //   1465: aload_0
    //   1466: new 54	java/lang/StringBuilder
    //   1469: dup
    //   1470: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   1473: ldc -113
    //   1475: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1478: aload 5
    //   1480: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1483: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   1486: invokespecial 127	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/String;)V
    //   1489: goto +511 -> 2000
    //   1492: aload_0
    //   1493: invokespecial 112	org/h2/server/pg/PgServerThread:readShort	()S
    //   1496: istore 7
    //   1498: aload 6
    //   1500: getfield 128	org/h2/server/pg/PgServerThread$Portal:prep	Lorg/h2/server/pg/PgServerThread$Prepared;
    //   1503: astore 8
    //   1505: aload 8
    //   1507: getfield 117	org/h2/server/pg/PgServerThread$Prepared:prep	Lorg/h2/jdbc/JdbcPreparedStatement;
    //   1510: astore 9
    //   1512: aload_0
    //   1513: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1516: aload 8
    //   1518: getfield 111	org/h2/server/pg/PgServerThread$Prepared:sql	Ljava/lang/String;
    //   1521: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1524: aload 9
    //   1526: iload 7
    //   1528: invokevirtual 147	org/h2/jdbc/JdbcPreparedStatement:setMaxRows	(I)V
    //   1531: aload_0
    //   1532: aload 9
    //   1534: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1537: aload 9
    //   1539: invokevirtual 149	org/h2/jdbc/JdbcPreparedStatement:execute	()Z
    //   1542: istore 10
    //   1544: iload 10
    //   1546: ifeq +50 -> 1596
    //   1549: aload 9
    //   1551: invokevirtual 150	org/h2/jdbc/JdbcPreparedStatement:getResultSet	()Ljava/sql/ResultSet;
    //   1554: astore 11
    //   1556: aload 11
    //   1558: invokeinterface 151 1 0
    //   1563: ifeq +12 -> 1575
    //   1566: aload_0
    //   1567: aload 11
    //   1569: invokespecial 152	org/h2/server/pg/PgServerThread:sendDataRow	(Ljava/sql/ResultSet;)V
    //   1572: goto -16 -> 1556
    //   1575: aload_0
    //   1576: aload 9
    //   1578: iconst_0
    //   1579: invokespecial 153	org/h2/server/pg/PgServerThread:sendCommandComplete	(Lorg/h2/jdbc/JdbcStatement;I)V
    //   1582: goto +25 -> 1607
    //   1585: astore 11
    //   1587: aload_0
    //   1588: aload 11
    //   1590: invokespecial 120	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/Exception;)V
    //   1593: goto +14 -> 1607
    //   1596: aload_0
    //   1597: aload 9
    //   1599: aload 9
    //   1601: invokevirtual 154	org/h2/jdbc/JdbcPreparedStatement:getUpdateCount	()I
    //   1604: invokespecial 153	org/h2/server/pg/PgServerThread:sendCommandComplete	(Lorg/h2/jdbc/JdbcStatement;I)V
    //   1607: aload_0
    //   1608: aconst_null
    //   1609: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1612: goto +44 -> 1656
    //   1615: astore 10
    //   1617: aload 9
    //   1619: invokevirtual 155	org/h2/jdbc/JdbcPreparedStatement:wasCancelled	()Z
    //   1622: ifeq +10 -> 1632
    //   1625: aload_0
    //   1626: invokespecial 156	org/h2/server/pg/PgServerThread:sendCancelQueryResponse	()V
    //   1629: goto +9 -> 1638
    //   1632: aload_0
    //   1633: aload 10
    //   1635: invokespecial 120	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/Exception;)V
    //   1638: aload_0
    //   1639: aconst_null
    //   1640: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1643: goto +13 -> 1656
    //   1646: astore 13
    //   1648: aload_0
    //   1649: aconst_null
    //   1650: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1653: aload 13
    //   1655: athrow
    //   1656: goto +344 -> 2000
    //   1659: aload_0
    //   1660: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1663: ldc -99
    //   1665: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1668: aload_0
    //   1669: invokespecial 158	org/h2/server/pg/PgServerThread:sendReadyForQuery	()V
    //   1672: goto +328 -> 2000
    //   1675: aload_0
    //   1676: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1679: ldc -97
    //   1681: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1684: aload_0
    //   1685: invokespecial 69	org/h2/server/pg/PgServerThread:readString	()Ljava/lang/String;
    //   1688: astore 5
    //   1690: new 160	org/h2/util/ScriptReader
    //   1693: dup
    //   1694: new 161	java/io/StringReader
    //   1697: dup
    //   1698: aload 5
    //   1700: invokespecial 162	java/io/StringReader:<init>	(Ljava/lang/String;)V
    //   1703: invokespecial 163	org/h2/util/ScriptReader:<init>	(Ljava/io/Reader;)V
    //   1706: astore 6
    //   1708: aconst_null
    //   1709: astore 7
    //   1711: aload 6
    //   1713: invokevirtual 164	org/h2/util/ScriptReader:readStatement	()Ljava/lang/String;
    //   1716: astore 8
    //   1718: aload 8
    //   1720: ifnonnull +16 -> 1736
    //   1723: aload 7
    //   1725: invokestatic 135	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   1728: aload_0
    //   1729: aconst_null
    //   1730: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1733: goto +203 -> 1936
    //   1736: aload_0
    //   1737: aload 8
    //   1739: invokespecial 110	org/h2/server/pg/PgServerThread:getSQL	(Ljava/lang/String;)Ljava/lang/String;
    //   1742: astore 8
    //   1744: aload_0
    //   1745: getfield 102	org/h2/server/pg/PgServerThread:conn	Ljava/sql/Connection;
    //   1748: invokeinterface 165 1 0
    //   1753: checkcast 166	org/h2/jdbc/JdbcStatement
    //   1756: astore 7
    //   1758: aload_0
    //   1759: aload 7
    //   1761: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1764: aload 7
    //   1766: aload 8
    //   1768: invokevirtual 167	org/h2/jdbc/JdbcStatement:execute	(Ljava/lang/String;)Z
    //   1771: istore 9
    //   1773: iload 9
    //   1775: ifeq +78 -> 1853
    //   1778: aload 7
    //   1780: invokevirtual 168	org/h2/jdbc/JdbcStatement:getResultSet	()Ljava/sql/ResultSet;
    //   1783: astore 10
    //   1785: aload 10
    //   1787: invokeinterface 169 1 0
    //   1792: astore 11
    //   1794: aload_0
    //   1795: aload 11
    //   1797: invokespecial 145	org/h2/server/pg/PgServerThread:sendRowDescription	(Ljava/sql/ResultSetMetaData;)V
    //   1800: aload 10
    //   1802: invokeinterface 151 1 0
    //   1807: ifeq +12 -> 1819
    //   1810: aload_0
    //   1811: aload 10
    //   1813: invokespecial 152	org/h2/server/pg/PgServerThread:sendDataRow	(Ljava/sql/ResultSet;)V
    //   1816: goto -16 -> 1800
    //   1819: aload_0
    //   1820: aload 7
    //   1822: iconst_0
    //   1823: invokespecial 153	org/h2/server/pg/PgServerThread:sendCommandComplete	(Lorg/h2/jdbc/JdbcStatement;I)V
    //   1826: goto +24 -> 1850
    //   1829: astore 12
    //   1831: aload_0
    //   1832: aload 12
    //   1834: invokespecial 120	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/Exception;)V
    //   1837: aload 7
    //   1839: invokestatic 135	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   1842: aload_0
    //   1843: aconst_null
    //   1844: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1847: goto +89 -> 1936
    //   1850: goto +14 -> 1864
    //   1853: aload_0
    //   1854: aload 7
    //   1856: aload 7
    //   1858: invokevirtual 170	org/h2/jdbc/JdbcStatement:getUpdateCount	()I
    //   1861: invokespecial 153	org/h2/server/pg/PgServerThread:sendCommandComplete	(Lorg/h2/jdbc/JdbcStatement;I)V
    //   1864: aload 7
    //   1866: invokestatic 135	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   1869: aload_0
    //   1870: aconst_null
    //   1871: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1874: goto +59 -> 1933
    //   1877: astore 8
    //   1879: aload 7
    //   1881: ifnull +18 -> 1899
    //   1884: aload 7
    //   1886: invokevirtual 172	org/h2/jdbc/JdbcStatement:wasCancelled	()Z
    //   1889: ifeq +10 -> 1899
    //   1892: aload_0
    //   1893: invokespecial 156	org/h2/server/pg/PgServerThread:sendCancelQueryResponse	()V
    //   1896: goto +9 -> 1905
    //   1899: aload_0
    //   1900: aload 8
    //   1902: invokespecial 120	org/h2/server/pg/PgServerThread:sendErrorResponse	(Ljava/lang/Exception;)V
    //   1905: aload 7
    //   1907: invokestatic 135	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   1910: aload_0
    //   1911: aconst_null
    //   1912: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1915: goto +21 -> 1936
    //   1918: astore 14
    //   1920: aload 7
    //   1922: invokestatic 135	org/h2/util/JdbcUtils:closeSilently	(Ljava/sql/Statement;)V
    //   1925: aload_0
    //   1926: aconst_null
    //   1927: invokespecial 148	org/h2/server/pg/PgServerThread:setActiveRequest	(Lorg/h2/jdbc/JdbcStatement;)V
    //   1930: aload 14
    //   1932: athrow
    //   1933: goto -225 -> 1708
    //   1936: aload_0
    //   1937: invokespecial 158	org/h2/server/pg/PgServerThread:sendReadyForQuery	()V
    //   1940: goto +60 -> 2000
    //   1943: aload_0
    //   1944: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1947: ldc -83
    //   1949: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   1952: aload_0
    //   1953: invokevirtual 26	org/h2/server/pg/PgServerThread:close	()V
    //   1956: goto +44 -> 2000
    //   1959: aload_0
    //   1960: getfield 10	org/h2/server/pg/PgServerThread:server	Lorg/h2/server/pg/PgServer;
    //   1963: new 54	java/lang/StringBuilder
    //   1966: dup
    //   1967: invokespecial 55	java/lang/StringBuilder:<init>	()V
    //   1970: ldc -82
    //   1972: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1975: iload_1
    //   1976: invokevirtual 58	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   1979: ldc 66
    //   1981: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1984: iload_1
    //   1985: i2c
    //   1986: invokevirtual 137	java/lang/StringBuilder:append	(C)Ljava/lang/StringBuilder;
    //   1989: ldc 68
    //   1991: invokevirtual 57	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1994: invokevirtual 60	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   1997: invokevirtual 15	org/h2/server/pg/PgServer:trace	(Ljava/lang/String;)V
    //   2000: return
    // Line number table:
    //   Java source line #133	-> byte code offset #0
    //   Java source line #134	-> byte code offset #7
    //   Java source line #135	-> byte code offset #15
    //   Java source line #136	-> byte code offset #19
    //   Java source line #137	-> byte code offset #24
    //   Java source line #140	-> byte code offset #25
    //   Java source line #142	-> byte code offset #27
    //   Java source line #143	-> byte code offset #35
    //   Java source line #144	-> byte code offset #38
    //   Java source line #145	-> byte code offset #43
    //   Java source line #146	-> byte code offset #53
    //   Java source line #147	-> byte code offset #74
    //   Java source line #149	-> byte code offset #164
    //   Java source line #150	-> byte code offset #173
    //   Java source line #151	-> byte code offset #179
    //   Java source line #152	-> byte code offset #186
    //   Java source line #153	-> byte code offset #195
    //   Java source line #154	-> byte code offset #201
    //   Java source line #155	-> byte code offset #207
    //   Java source line #156	-> byte code offset #218
    //   Java source line #157	-> byte code offset #233
    //   Java source line #162	-> byte code offset #241
    //   Java source line #164	-> byte code offset #278
    //   Java source line #165	-> byte code offset #282
    //   Java source line #166	-> byte code offset #292
    //   Java source line #167	-> byte code offset #301
    //   Java source line #169	-> byte code offset #313
    //   Java source line #170	-> byte code offset #322
    //   Java source line #173	-> byte code offset #381
    //   Java source line #174	-> byte code offset #387
    //   Java source line #175	-> byte code offset #395
    //   Java source line #177	-> byte code offset #398
    //   Java source line #178	-> byte code offset #404
    //   Java source line #179	-> byte code offset #414
    //   Java source line #180	-> byte code offset #423
    //   Java source line #181	-> byte code offset #433
    //   Java source line #182	-> byte code offset #449
    //   Java source line #184	-> byte code offset #459
    //   Java source line #185	-> byte code offset #468
    //   Java source line #186	-> byte code offset #478
    //   Java source line #190	-> byte code offset #484
    //   Java source line #191	-> byte code offset #521
    //   Java source line #192	-> byte code offset #524
    //   Java source line #193	-> byte code offset #528
    //   Java source line #195	-> byte code offset #533
    //   Java source line #197	-> byte code offset #536
    //   Java source line #198	-> byte code offset #545
    //   Java source line #200	-> byte code offset #551
    //   Java source line #201	-> byte code offset #560
    //   Java source line #202	-> byte code offset #570
    //   Java source line #203	-> byte code offset #582
    //   Java source line #204	-> byte code offset #592
    //   Java source line #205	-> byte code offset #616
    //   Java source line #206	-> byte code offset #629
    //   Java source line #207	-> byte code offset #638
    //   Java source line #208	-> byte code offset #643
    //   Java source line #210	-> byte code offset #648
    //   Java source line #211	-> byte code offset #653
    //   Java source line #213	-> byte code offset #660
    //   Java source line #214	-> byte code offset #670
    //   Java source line #216	-> byte code offset #679
    //   Java source line #220	-> byte code offset #693
    //   Java source line #221	-> byte code offset #697
    //   Java source line #225	-> byte code offset #701
    //   Java source line #222	-> byte code offset #704
    //   Java source line #223	-> byte code offset #706
    //   Java source line #224	-> byte code offset #711
    //   Java source line #226	-> byte code offset #716
    //   Java source line #229	-> byte code offset #719
    //   Java source line #230	-> byte code offset #728
    //   Java source line #231	-> byte code offset #737
    //   Java source line #232	-> byte code offset #746
    //   Java source line #233	-> byte code offset #759
    //   Java source line #234	-> byte code offset #765
    //   Java source line #235	-> byte code offset #774
    //   Java source line #236	-> byte code offset #784
    //   Java source line #237	-> byte code offset #790
    //   Java source line #238	-> byte code offset #799
    //   Java source line #235	-> byte code offset #809
    //   Java source line #241	-> byte code offset #815
    //   Java source line #242	-> byte code offset #837
    //   Java source line #243	-> byte code offset #852
    //   Java source line #246	-> byte code offset #856
    //   Java source line #244	-> byte code offset #859
    //   Java source line #245	-> byte code offset #861
    //   Java source line #247	-> byte code offset #867
    //   Java source line #250	-> byte code offset #870
    //   Java source line #251	-> byte code offset #879
    //   Java source line #252	-> byte code offset #888
    //   Java source line #253	-> byte code offset #897
    //   Java source line #254	-> byte code offset #903
    //   Java source line #255	-> byte code offset #917
    //   Java source line #256	-> byte code offset #922
    //   Java source line #257	-> byte code offset #928
    //   Java source line #259	-> byte code offset #931
    //   Java source line #260	-> byte code offset #938
    //   Java source line #261	-> byte code offset #953
    //   Java source line #262	-> byte code offset #959
    //   Java source line #263	-> byte code offset #965
    //   Java source line #264	-> byte code offset #975
    //   Java source line #263	-> byte code offset #984
    //   Java source line #266	-> byte code offset #990
    //   Java source line #268	-> byte code offset #996
    //   Java source line #269	-> byte code offset #1006
    //   Java source line #268	-> byte code offset #1027
    //   Java source line #274	-> byte code offset #1033
    //   Java source line #271	-> byte code offset #1036
    //   Java source line #272	-> byte code offset #1038
    //   Java source line #273	-> byte code offset #1044
    //   Java source line #275	-> byte code offset #1047
    //   Java source line #276	-> byte code offset #1053
    //   Java source line #277	-> byte code offset #1062
    //   Java source line #278	-> byte code offset #1072
    //   Java source line #277	-> byte code offset #1084
    //   Java source line #280	-> byte code offset #1090
    //   Java source line #281	-> byte code offset #1094
    //   Java source line #284	-> byte code offset #1097
    //   Java source line #285	-> byte code offset #1104
    //   Java source line #286	-> byte code offset #1110
    //   Java source line #287	-> byte code offset #1119
    //   Java source line #288	-> byte code offset #1126
    //   Java source line #289	-> byte code offset #1140
    //   Java source line #290	-> byte code offset #1145
    //   Java source line #292	-> byte code offset #1153
    //   Java source line #293	-> byte code offset #1163
    //   Java source line #295	-> byte code offset #1176
    //   Java source line #296	-> byte code offset #1203
    //   Java source line #297	-> byte code offset #1209
    //   Java source line #299	-> byte code offset #1212
    //   Java source line #300	-> byte code offset #1216
    //   Java source line #303	-> byte code offset #1219
    //   Java source line #304	-> byte code offset #1226
    //   Java source line #305	-> byte code offset #1232
    //   Java source line #306	-> byte code offset #1241
    //   Java source line #307	-> byte code offset #1248
    //   Java source line #308	-> byte code offset #1262
    //   Java source line #309	-> byte code offset #1267
    //   Java source line #311	-> byte code offset #1294
    //   Java source line #313	-> byte code offset #1300
    //   Java source line #314	-> byte code offset #1310
    //   Java source line #315	-> byte code offset #1324
    //   Java source line #316	-> byte code offset #1329
    //   Java source line #318	-> byte code offset #1356
    //   Java source line #320	-> byte code offset #1366
    //   Java source line #321	-> byte code offset #1375
    //   Java source line #324	-> byte code offset #1381
    //   Java source line #322	-> byte code offset #1384
    //   Java source line #323	-> byte code offset #1386
    //   Java source line #326	-> byte code offset #1392
    //   Java source line #327	-> byte code offset #1395
    //   Java source line #328	-> byte code offset #1422
    //   Java source line #330	-> byte code offset #1428
    //   Java source line #333	-> byte code offset #1431
    //   Java source line #334	-> byte code offset #1437
    //   Java source line #335	-> byte code offset #1446
    //   Java source line #336	-> byte code offset #1460
    //   Java source line #337	-> byte code offset #1465
    //   Java source line #338	-> byte code offset #1489
    //   Java source line #340	-> byte code offset #1492
    //   Java source line #341	-> byte code offset #1498
    //   Java source line #342	-> byte code offset #1505
    //   Java source line #343	-> byte code offset #1512
    //   Java source line #345	-> byte code offset #1524
    //   Java source line #346	-> byte code offset #1531
    //   Java source line #347	-> byte code offset #1537
    //   Java source line #348	-> byte code offset #1544
    //   Java source line #350	-> byte code offset #1549
    //   Java source line #352	-> byte code offset #1556
    //   Java source line #353	-> byte code offset #1566
    //   Java source line #355	-> byte code offset #1575
    //   Java source line #358	-> byte code offset #1582
    //   Java source line #356	-> byte code offset #1585
    //   Java source line #357	-> byte code offset #1587
    //   Java source line #358	-> byte code offset #1593
    //   Java source line #360	-> byte code offset #1596
    //   Java source line #369	-> byte code offset #1607
    //   Java source line #370	-> byte code offset #1612
    //   Java source line #362	-> byte code offset #1615
    //   Java source line #363	-> byte code offset #1617
    //   Java source line #364	-> byte code offset #1625
    //   Java source line #366	-> byte code offset #1632
    //   Java source line #369	-> byte code offset #1638
    //   Java source line #370	-> byte code offset #1643
    //   Java source line #369	-> byte code offset #1646
    //   Java source line #371	-> byte code offset #1656
    //   Java source line #374	-> byte code offset #1659
    //   Java source line #375	-> byte code offset #1668
    //   Java source line #376	-> byte code offset #1672
    //   Java source line #379	-> byte code offset #1675
    //   Java source line #380	-> byte code offset #1684
    //   Java source line #381	-> byte code offset #1690
    //   Java source line #383	-> byte code offset #1708
    //   Java source line #385	-> byte code offset #1711
    //   Java source line #386	-> byte code offset #1718
    //   Java source line #417	-> byte code offset #1723
    //   Java source line #418	-> byte code offset #1728
    //   Java source line #389	-> byte code offset #1736
    //   Java source line #390	-> byte code offset #1744
    //   Java source line #391	-> byte code offset #1758
    //   Java source line #392	-> byte code offset #1764
    //   Java source line #393	-> byte code offset #1773
    //   Java source line #394	-> byte code offset #1778
    //   Java source line #395	-> byte code offset #1785
    //   Java source line #397	-> byte code offset #1794
    //   Java source line #398	-> byte code offset #1800
    //   Java source line #399	-> byte code offset #1810
    //   Java source line #401	-> byte code offset #1819
    //   Java source line #405	-> byte code offset #1826
    //   Java source line #402	-> byte code offset #1829
    //   Java source line #403	-> byte code offset #1831
    //   Java source line #417	-> byte code offset #1837
    //   Java source line #418	-> byte code offset #1842
    //   Java source line #406	-> byte code offset #1850
    //   Java source line #407	-> byte code offset #1853
    //   Java source line #417	-> byte code offset #1864
    //   Java source line #418	-> byte code offset #1869
    //   Java source line #419	-> byte code offset #1874
    //   Java source line #409	-> byte code offset #1877
    //   Java source line #410	-> byte code offset #1879
    //   Java source line #411	-> byte code offset #1892
    //   Java source line #413	-> byte code offset #1899
    //   Java source line #417	-> byte code offset #1905
    //   Java source line #418	-> byte code offset #1910
    //   Java source line #417	-> byte code offset #1918
    //   Java source line #418	-> byte code offset #1925
    //   Java source line #420	-> byte code offset #1933
    //   Java source line #421	-> byte code offset #1936
    //   Java source line #422	-> byte code offset #1940
    //   Java source line #425	-> byte code offset #1943
    //   Java source line #426	-> byte code offset #1952
    //   Java source line #427	-> byte code offset #1956
    //   Java source line #430	-> byte code offset #1959
    //   Java source line #433	-> byte code offset #2000
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	2001	0	this	PgServerThread
    //   14	1971	1	i	int
    //   34	31	2	j	int
    //   42	21	3	arrayOfByte	byte[]
    //   177	190	4	k	int
    //   199	59	5	m	int
    //   385	688	5	localObject1	Object
    //   1102	310	5	c	char
    //   1435	264	5	str	String
    //   205	63	6	n	int
    //   402	221	6	localObject2	Object
    //   704	3	6	localException1	Exception
    //   763	19	6	i1	int
    //   901	811	6	localObject3	Object
    //   216	405	7	localObject4	Object
    //   775	35	7	i2	int
    //   859	4	7	localException2	Exception
    //   915	442	7	localObject5	Object
    //   1496	31	7	i3	int
    //   1709	212	7	localObject6	Object
    //   627	58	8	localConnectionInfo	org.h2.engine.ConnectionInfo
    //   788	185	8	i4	int
    //   1364	403	8	localObject7	Object
    //   1877	24	8	localSQLException	SQLException
    //   636	741	9	localObject8	Object
    //   1384	4	9	localException3	Exception
    //   1510	108	9	localJdbcPreparedStatement	JdbcPreparedStatement
    //   1771	3	9	bool1	boolean
    //   966	38	10	i5	int
    //   1542	3	10	bool2	boolean
    //   1615	19	10	localException4	Exception
    //   1783	29	10	localResultSet1	ResultSet
    //   997	31	11	i6	int
    //   1036	4	11	localException5	Exception
    //   1051	19	11	i7	int
    //   1554	14	11	localResultSet2	ResultSet
    //   1585	4	11	localException6	Exception
    //   1792	4	11	localResultSetMetaData	ResultSetMetaData
    //   1063	22	12	i8	int
    //   1829	4	12	localException7	Exception
    //   1646	8	13	localObject9	Object
    //   1918	13	14	localObject10	Object
    // Exception table:
    //   from	to	target	type
    //   551	701	704	java/lang/Exception
    //   815	856	859	java/lang/Exception
    //   996	1033	1036	java/lang/Exception
    //   1366	1381	1384	java/lang/Exception
    //   1549	1582	1585	java/lang/Exception
    //   1524	1607	1615	java/lang/Exception
    //   1524	1607	1646	finally
    //   1615	1638	1646	finally
    //   1646	1648	1646	finally
    //   1794	1826	1829	java/lang/Exception
    //   1711	1723	1877	java/sql/SQLException
    //   1736	1837	1877	java/sql/SQLException
    //   1850	1864	1877	java/sql/SQLException
    //   1711	1723	1918	finally
    //   1736	1837	1918	finally
    //   1850	1864	1918	finally
    //   1877	1905	1918	finally
    //   1918	1920	1918	finally
  }
  
  private String getSQL(String paramString)
  {
    String str = StringUtils.toLowerEnglish(paramString);
    if (str.startsWith("show max_identifier_length")) {
      paramString = "CALL 63";
    } else if (str.startsWith("set client_encoding to")) {
      paramString = "set DATESTYLE ISO";
    }
    if (this.server.getTrace()) {
      this.server.trace(paramString + ";");
    }
    return paramString;
  }
  
  private void sendCommandComplete(JdbcStatement paramJdbcStatement, int paramInt)
    throws IOException
  {
    startMessage(67);
    switch (paramJdbcStatement.getLastExecutedCommandType())
    {
    case 61: 
      writeStringPart("INSERT 0 ");
      writeString(Integer.toString(paramInt));
      break;
    case 68: 
      writeStringPart("UPDATE ");
      writeString(Integer.toString(paramInt));
      break;
    case 58: 
      writeStringPart("DELETE ");
      writeString(Integer.toString(paramInt));
      break;
    case 57: 
    case 66: 
      writeString("SELECT");
      break;
    case 83: 
      writeString("BEGIN");
      break;
    default: 
      this.server.trace("check CommandComplete tag for command " + paramJdbcStatement);
      writeStringPart("UPDATE ");
      writeString(Integer.toString(paramInt));
    }
    sendMessage();
  }
  
  private void sendDataRow(ResultSet paramResultSet)
    throws Exception
  {
    ResultSetMetaData localResultSetMetaData = paramResultSet.getMetaData();
    int i = localResultSetMetaData.getColumnCount();
    startMessage(68);
    writeShort(i);
    for (int j = 1; j <= i; j++) {
      writeDataColumn(paramResultSet, j, PgServer.convertType(localResultSetMetaData.getColumnType(j)));
    }
    sendMessage();
  }
  
  private void writeDataColumn(ResultSet paramResultSet, int paramInt1, int paramInt2)
    throws Exception
  {
    Object localObject;
    if (formatAsText(paramInt2)) {
      switch (paramInt2)
      {
      case 16: 
        writeInt(1);
        this.dataOut.writeByte(paramResultSet.getBoolean(paramInt1) ? 116 : 102);
        break;
      default: 
        localObject = paramResultSet.getString(paramInt1);
        if (localObject == null)
        {
          writeInt(-1);
        }
        else
        {
          byte[] arrayOfByte = ((String)localObject).getBytes(getEncoding());
          writeInt(arrayOfByte.length);
          write(arrayOfByte);
        }
        break;
      }
    } else {
      switch (paramInt2)
      {
      case 21: 
        writeInt(2);
        writeShort(paramResultSet.getShort(paramInt1));
        break;
      case 23: 
        writeInt(4);
        writeInt(paramResultSet.getInt(paramInt1));
        break;
      case 20: 
        writeInt(8);
        this.dataOut.writeLong(paramResultSet.getLong(paramInt1));
        break;
      case 700: 
        writeInt(4);
        this.dataOut.writeFloat(paramResultSet.getFloat(paramInt1));
        break;
      case 701: 
        writeInt(8);
        this.dataOut.writeDouble(paramResultSet.getDouble(paramInt1));
        break;
      case 17: 
        localObject = paramResultSet.getBytes(paramInt1);
        if (localObject == null)
        {
          writeInt(-1);
        }
        else
        {
          writeInt(localObject.length);
          write((byte[])localObject);
        }
        break;
      default: 
        throw new IllegalStateException("output binary format is undefined");
      }
    }
  }
  
  private String getEncoding()
  {
    if ("UNICODE".equals(this.clientEncoding)) {
      return "UTF-8";
    }
    return this.clientEncoding;
  }
  
  private void setParameter(PreparedStatement paramPreparedStatement, int paramInt1, int paramInt2, int[] paramArrayOfInt)
    throws SQLException, IOException
  {
    int i = (paramInt2 >= paramArrayOfInt.length) || (paramArrayOfInt[paramInt2] == 0) ? 1 : 0;
    int j = paramInt2 + 1;
    int k = readInt();
    if (k == -1)
    {
      paramPreparedStatement.setNull(j, 0);
    }
    else
    {
      byte[] arrayOfByte1;
      if (i != 0)
      {
        arrayOfByte1 = DataUtils.newBytes(k);
        readFully(arrayOfByte1);
        paramPreparedStatement.setString(j, new String(arrayOfByte1, getEncoding()));
      }
      else
      {
        switch (paramInt1)
        {
        case 21: 
          checkParamLength(4, k);
          paramPreparedStatement.setShort(j, readShort());
          break;
        case 23: 
          checkParamLength(4, k);
          paramPreparedStatement.setInt(j, readInt());
          break;
        case 20: 
          checkParamLength(8, k);
          paramPreparedStatement.setLong(j, this.dataIn.readLong());
          break;
        case 700: 
          checkParamLength(4, k);
          paramPreparedStatement.setFloat(j, this.dataIn.readFloat());
          break;
        case 701: 
          checkParamLength(8, k);
          paramPreparedStatement.setDouble(j, this.dataIn.readDouble());
          break;
        case 17: 
          arrayOfByte1 = DataUtils.newBytes(k);
          readFully(arrayOfByte1);
          paramPreparedStatement.setBytes(j, arrayOfByte1);
          break;
        default: 
          this.server.trace("Binary format for type: " + paramInt1 + " is unsupported");
          byte[] arrayOfByte2 = DataUtils.newBytes(k);
          readFully(arrayOfByte2);
          paramPreparedStatement.setString(j, new String(arrayOfByte2, getEncoding()));
        }
      }
    }
  }
  
  private static void checkParamLength(int paramInt1, int paramInt2)
  {
    if (paramInt1 != paramInt2) {
      throw DbException.getInvalidValueException("paramLen", Integer.valueOf(paramInt2));
    }
  }
  
  private void sendErrorResponse(Exception paramException)
    throws IOException
  {
    SQLException localSQLException = DbException.toSQLException(paramException);
    this.server.traceError(localSQLException);
    startMessage(69);
    write(83);
    writeString("ERROR");
    write(67);
    writeString(localSQLException.getSQLState());
    write(77);
    writeString(localSQLException.getMessage());
    write(68);
    writeString(localSQLException.toString());
    write(0);
    sendMessage();
  }
  
  private void sendCancelQueryResponse()
    throws IOException
  {
    this.server.trace("CancelSuccessResponse");
    startMessage(69);
    write(83);
    writeString("ERROR");
    write(67);
    writeString("57014");
    write(77);
    writeString("canceling statement due to user request");
    write(0);
    sendMessage();
  }
  
  private void sendParameterDescription(Prepared paramPrepared)
    throws IOException
  {
    try
    {
      JdbcPreparedStatement localJdbcPreparedStatement = paramPrepared.prep;
      ParameterMetaData localParameterMetaData = localJdbcPreparedStatement.getParameterMetaData();
      int i = localParameterMetaData.getParameterCount();
      startMessage(116);
      writeShort(i);
      for (int j = 0; j < i; j++)
      {
        int k;
        if ((paramPrepared.paramType != null) && (paramPrepared.paramType[j] != 0)) {
          k = paramPrepared.paramType[j];
        } else {
          k = 1043;
        }
        this.server.checkType(k);
        writeInt(k);
      }
      sendMessage();
    }
    catch (Exception localException)
    {
      sendErrorResponse(localException);
    }
  }
  
  private void sendNoData()
    throws IOException
  {
    startMessage(110);
    sendMessage();
  }
  
  private void sendRowDescription(ResultSetMetaData paramResultSetMetaData)
    throws Exception
  {
    if (paramResultSetMetaData == null)
    {
      sendNoData();
    }
    else
    {
      int i = paramResultSetMetaData.getColumnCount();
      int[] arrayOfInt1 = new int[i];
      int[] arrayOfInt2 = new int[i];
      String[] arrayOfString = new String[i];
      for (int j = 0; j < i; j++)
      {
        String str = paramResultSetMetaData.getColumnName(j + 1);
        arrayOfString[j] = str;
        int k = paramResultSetMetaData.getColumnType(j + 1);
        int m = PgServer.convertType(k);
        
        arrayOfInt2[j] = paramResultSetMetaData.getColumnDisplaySize(j + 1);
        if (k != 0) {
          this.server.checkType(m);
        }
        arrayOfInt1[j] = m;
      }
      startMessage(84);
      writeShort(i);
      for (j = 0; j < i; j++)
      {
        writeString(StringUtils.toLowerEnglish(arrayOfString[j]));
        
        writeInt(0);
        
        writeShort(0);
        
        writeInt(arrayOfInt1[j]);
        
        writeShort(getTypeSize(arrayOfInt1[j], arrayOfInt2[j]));
        
        writeInt(-1);
        
        writeShort(formatAsText(arrayOfInt1[j]) ? 0 : 1);
      }
      sendMessage();
    }
  }
  
  private static boolean formatAsText(int paramInt)
  {
    switch (paramInt)
    {
    case 17: 
      return false;
    }
    return true;
  }
  
  private static int getTypeSize(int paramInt1, int paramInt2)
  {
    switch (paramInt1)
    {
    case 16: 
      return 1;
    case 1043: 
      return Math.max(255, paramInt2 + 10);
    }
    return paramInt2 + 4;
  }
  
  private void sendErrorResponse(String paramString)
    throws IOException
  {
    this.server.trace("Exception: " + paramString);
    startMessage(69);
    write(83);
    writeString("ERROR");
    write(67);
    
    writeString("08P01");
    write(77);
    writeString(paramString);
    sendMessage();
  }
  
  private void sendParseComplete()
    throws IOException
  {
    startMessage(49);
    sendMessage();
  }
  
  private void sendBindComplete()
    throws IOException
  {
    startMessage(50);
    sendMessage();
  }
  
  private void sendCloseComplete()
    throws IOException
  {
    startMessage(51);
    sendMessage();
  }
  
  private void initDb()
    throws SQLException
  {
    Statement localStatement = null;
    ResultSet localResultSet = null;
    try
    {
      synchronized (this.server)
      {
        localResultSet = this.conn.getMetaData().getTables(null, "PG_CATALOG", "PG_VERSION", null);
        boolean bool = localResultSet.next();
        localStatement = this.conn.createStatement();
        if (!bool) {
          installPgCatalog(localStatement);
        }
        localResultSet = localStatement.executeQuery("select * from pg_catalog.pg_version");
        if ((!localResultSet.next()) || (localResultSet.getInt(1) < 2))
        {
          installPgCatalog(localStatement);
        }
        else
        {
          int i = localResultSet.getInt(2);
          if (i > 2) {
            throw DbException.throwInternalError("Incompatible PG_VERSION");
          }
        }
      }
      localStatement.execute("set search_path = PUBLIC, pg_catalog");
      ??? = this.server.getTypeSet();
      if (((HashSet)???).size() == 0)
      {
        localResultSet = localStatement.executeQuery("select oid from pg_catalog.pg_type");
        while (localResultSet.next()) {
          ((HashSet)???).add(Integer.valueOf(localResultSet.getInt(1)));
        }
      }
    }
    finally
    {
      JdbcUtils.closeSilently(localStatement);
      JdbcUtils.closeSilently(localResultSet);
    }
  }
  
  private static void installPgCatalog(Statement paramStatement)
    throws SQLException
  {
    InputStreamReader localInputStreamReader = null;
    try
    {
      localInputStreamReader = new InputStreamReader(new ByteArrayInputStream(Utils.getResource("/org/h2/server/pg/pg_catalog.sql")));
      
      ScriptReader localScriptReader = new ScriptReader(localInputStreamReader);
      for (;;)
      {
        String str = localScriptReader.readStatement();
        if (str == null) {
          break;
        }
        paramStatement.execute(str);
      }
      localScriptReader.close();
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, "Can not read pg_catalog resource");
    }
    finally
    {
      IOUtils.closeSilently(localInputStreamReader);
    }
  }
  
  void close()
  {
    try
    {
      this.stop = true;
      JdbcUtils.closeSilently(this.conn);
      if (this.socket != null) {
        this.socket.close();
      }
      this.server.trace("Close");
    }
    catch (Exception localException)
    {
      this.server.traceError(localException);
    }
    this.conn = null;
    this.socket = null;
    this.server.remove(this);
  }
  
  private void sendAuthenticationCleartextPassword()
    throws IOException
  {
    startMessage(82);
    writeInt(3);
    sendMessage();
  }
  
  private void sendAuthenticationOk()
    throws IOException
  {
    startMessage(82);
    writeInt(0);
    sendMessage();
    sendParameterStatus("client_encoding", this.clientEncoding);
    sendParameterStatus("DateStyle", this.dateStyle);
    sendParameterStatus("integer_datetimes", "off");
    sendParameterStatus("is_superuser", "off");
    sendParameterStatus("server_encoding", "SQL_ASCII");
    sendParameterStatus("server_version", "8.1.4");
    sendParameterStatus("session_authorization", this.userName);
    sendParameterStatus("standard_conforming_strings", "off");
    
    sendParameterStatus("TimeZone", "CET");
    sendBackendKeyData();
    sendReadyForQuery();
  }
  
  private void sendReadyForQuery()
    throws IOException
  {
    startMessage(90);
    int i;
    try
    {
      if (this.conn.getAutoCommit()) {
        i = 73;
      } else {
        i = 84;
      }
    }
    catch (SQLException localSQLException)
    {
      i = 69;
    }
    write((byte)i);
    sendMessage();
  }
  
  private void sendBackendKeyData()
    throws IOException
  {
    startMessage(75);
    writeInt(this.processId);
    writeInt(this.secret);
    sendMessage();
  }
  
  private void writeString(String paramString)
    throws IOException
  {
    writeStringPart(paramString);
    write(0);
  }
  
  private void writeStringPart(String paramString)
    throws IOException
  {
    write(paramString.getBytes(getEncoding()));
  }
  
  private void writeInt(int paramInt)
    throws IOException
  {
    this.dataOut.writeInt(paramInt);
  }
  
  private void writeShort(int paramInt)
    throws IOException
  {
    this.dataOut.writeShort(paramInt);
  }
  
  private void write(byte[] paramArrayOfByte)
    throws IOException
  {
    this.dataOut.write(paramArrayOfByte);
  }
  
  private void write(int paramInt)
    throws IOException
  {
    this.dataOut.write(paramInt);
  }
  
  private void startMessage(int paramInt)
  {
    this.messageType = paramInt;
    this.outBuffer = new ByteArrayOutputStream();
    this.dataOut = new DataOutputStream(this.outBuffer);
  }
  
  private void sendMessage()
    throws IOException
  {
    this.dataOut.flush();
    byte[] arrayOfByte = this.outBuffer.toByteArray();
    int i = arrayOfByte.length;
    this.dataOut = new DataOutputStream(this.out);
    this.dataOut.write(this.messageType);
    this.dataOut.writeInt(i + 4);
    this.dataOut.write(arrayOfByte);
    this.dataOut.flush();
  }
  
  private void sendParameterStatus(String paramString1, String paramString2)
    throws IOException
  {
    startMessage(83);
    writeString(paramString1);
    writeString(paramString2);
    sendMessage();
  }
  
  void setThread(Thread paramThread)
  {
    this.thread = paramThread;
  }
  
  Thread getThread()
  {
    return this.thread;
  }
  
  void setProcessId(int paramInt)
  {
    this.processId = paramInt;
  }
  
  int getProcessId()
  {
    return this.processId;
  }
  
  private synchronized void setActiveRequest(JdbcStatement paramJdbcStatement)
  {
    this.activeRequest = paramJdbcStatement;
  }
  
  private synchronized void cancelRequest()
  {
    if (this.activeRequest != null) {
      try
      {
        this.activeRequest.cancel();
        this.activeRequest = null;
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convert(localSQLException);
      }
    }
  }
  
  static class Portal
  {
    String name;
    int[] resultColumnFormat;
    PgServerThread.Prepared prep;
  }
  
  static class Prepared
  {
    String name;
    String sql;
    JdbcPreparedStatement prep;
    int[] paramType;
  }
}
