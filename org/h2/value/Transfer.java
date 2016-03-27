package org.h2.value;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import org.h2.engine.Constants;
import org.h2.engine.SessionInterface;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.security.SHA256;
import org.h2.store.Data;
import org.h2.store.DataHandler;
import org.h2.store.DataReader;
import org.h2.store.LobStorageInterface;
import org.h2.tools.SimpleResultSet;
import org.h2.util.DateTimeUtils;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.NetUtils;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class Transfer
{
  private static final int BUFFER_SIZE = 65536;
  private static final int LOB_MAGIC = 4660;
  private static final int LOB_MAC_SALT_LENGTH = 16;
  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;
  private SessionInterface session;
  private boolean ssl;
  private int version;
  private byte[] lobMacSalt;
  
  public Transfer(SessionInterface paramSessionInterface)
  {
    this.session = paramSessionInterface;
  }
  
  public void setSocket(Socket paramSocket)
  {
    this.socket = paramSocket;
  }
  
  public synchronized void init()
    throws IOException
  {
    if (this.socket != null)
    {
      this.in = new DataInputStream(new BufferedInputStream(this.socket.getInputStream(), 65536));
      
      this.out = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream(), 65536));
    }
  }
  
  public void flush()
    throws IOException
  {
    this.out.flush();
  }
  
  public Transfer writeBoolean(boolean paramBoolean)
    throws IOException
  {
    this.out.writeByte((byte)(paramBoolean ? 1 : 0));
    return this;
  }
  
  public boolean readBoolean()
    throws IOException
  {
    return this.in.readByte() == 1;
  }
  
  private Transfer writeByte(byte paramByte)
    throws IOException
  {
    this.out.writeByte(paramByte);
    return this;
  }
  
  private byte readByte()
    throws IOException
  {
    return this.in.readByte();
  }
  
  public Transfer writeInt(int paramInt)
    throws IOException
  {
    this.out.writeInt(paramInt);
    return this;
  }
  
  public int readInt()
    throws IOException
  {
    return this.in.readInt();
  }
  
  public Transfer writeLong(long paramLong)
    throws IOException
  {
    this.out.writeLong(paramLong);
    return this;
  }
  
  public long readLong()
    throws IOException
  {
    return this.in.readLong();
  }
  
  private Transfer writeDouble(double paramDouble)
    throws IOException
  {
    this.out.writeDouble(paramDouble);
    return this;
  }
  
  private Transfer writeFloat(float paramFloat)
    throws IOException
  {
    this.out.writeFloat(paramFloat);
    return this;
  }
  
  private double readDouble()
    throws IOException
  {
    return this.in.readDouble();
  }
  
  private float readFloat()
    throws IOException
  {
    return this.in.readFloat();
  }
  
  public Transfer writeString(String paramString)
    throws IOException
  {
    if (paramString == null)
    {
      this.out.writeInt(-1);
    }
    else
    {
      int i = paramString.length();
      this.out.writeInt(i);
      for (int j = 0; j < i; j++) {
        this.out.writeChar(paramString.charAt(j));
      }
    }
    return this;
  }
  
  public String readString()
    throws IOException
  {
    int i = this.in.readInt();
    if (i == -1) {
      return null;
    }
    StringBuilder localStringBuilder = new StringBuilder(i);
    for (int j = 0; j < i; j++) {
      localStringBuilder.append(this.in.readChar());
    }
    String str = localStringBuilder.toString();
    str = StringUtils.cache(str);
    return str;
  }
  
  public Transfer writeBytes(byte[] paramArrayOfByte)
    throws IOException
  {
    if (paramArrayOfByte == null)
    {
      writeInt(-1);
    }
    else
    {
      writeInt(paramArrayOfByte.length);
      this.out.write(paramArrayOfByte);
    }
    return this;
  }
  
  public Transfer writeBytes(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    this.out.write(paramArrayOfByte, paramInt1, paramInt2);
    return this;
  }
  
  public byte[] readBytes()
    throws IOException
  {
    int i = readInt();
    if (i == -1) {
      return null;
    }
    byte[] arrayOfByte = DataUtils.newBytes(i);
    this.in.readFully(arrayOfByte);
    return arrayOfByte;
  }
  
  public void readBytes(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    this.in.readFully(paramArrayOfByte, paramInt1, paramInt2);
  }
  
  public synchronized void close()
  {
    if (this.socket != null) {
      try
      {
        if (this.out != null) {
          this.out.flush();
        }
        if (this.socket != null) {
          this.socket.close();
        }
      }
      catch (IOException localIOException)
      {
        DbException.traceThrowable(localIOException);
      }
      finally
      {
        this.socket = null;
      }
    }
  }
  
  public void writeValue(Value paramValue)
    throws IOException
  {
    int i = paramValue.getType();
    writeInt(i);
    Object localObject1;
    Object localObject2;
    Object localObject3;
    int j;
    switch (i)
    {
    case 0: 
      break;
    case 12: 
    case 19: 
      writeBytes(paramValue.getBytesNoCopy());
      break;
    case 20: 
      localObject1 = (ValueUuid)paramValue;
      writeLong(((ValueUuid)localObject1).getHigh());
      writeLong(((ValueUuid)localObject1).getLow());
      break;
    case 1: 
      writeBoolean(paramValue.getBoolean().booleanValue());
      break;
    case 2: 
      writeByte(paramValue.getByte());
      break;
    case 9: 
      if (this.version >= 9) {
        writeLong(((ValueTime)paramValue).getNanos());
      } else if (this.version >= 7) {
        writeLong(DateTimeUtils.getTimeLocalWithoutDst(paramValue.getTime()));
      } else {
        writeLong(paramValue.getTime().getTime());
      }
      break;
    case 10: 
      if (this.version >= 9) {
        writeLong(((ValueDate)paramValue).getDateValue());
      } else if (this.version >= 7) {
        writeLong(DateTimeUtils.getTimeLocalWithoutDst(paramValue.getDate()));
      } else {
        writeLong(paramValue.getDate().getTime());
      }
      break;
    case 11: 
      if (this.version >= 9)
      {
        localObject1 = (ValueTimestamp)paramValue;
        writeLong(((ValueTimestamp)localObject1).getDateValue());
        writeLong(((ValueTimestamp)localObject1).getTimeNanos());
      }
      else if (this.version >= 7)
      {
        localObject1 = paramValue.getTimestamp();
        writeLong(DateTimeUtils.getTimeLocalWithoutDst((java.util.Date)localObject1));
        writeInt(((Timestamp)localObject1).getNanos() % 1000000);
      }
      else
      {
        localObject1 = paramValue.getTimestamp();
        writeLong(((Timestamp)localObject1).getTime());
        writeInt(((Timestamp)localObject1).getNanos() % 1000000);
      }
      break;
    case 6: 
      writeString(paramValue.getString());
      break;
    case 7: 
      writeDouble(paramValue.getDouble());
      break;
    case 8: 
      writeFloat(paramValue.getFloat());
      break;
    case 4: 
      writeInt(paramValue.getInt());
      break;
    case 5: 
      writeLong(paramValue.getLong());
      break;
    case 3: 
      writeInt(paramValue.getShort());
      break;
    case 13: 
    case 14: 
    case 21: 
      writeString(paramValue.getString());
      break;
    case 15: 
      if ((this.version >= 11) && 
        ((paramValue instanceof ValueLobDb)))
      {
        localObject1 = (ValueLobDb)paramValue;
        if (((ValueLobDb)localObject1).isStored())
        {
          writeLong(-1L);
          writeInt(((ValueLobDb)localObject1).getTableId());
          writeLong(((ValueLobDb)localObject1).getLobId());
          if (this.version >= 12) {
            writeBytes(calculateLobMac(((ValueLobDb)localObject1).getLobId()));
          }
          writeLong(((ValueLobDb)localObject1).getPrecision());
          break;
        }
      }
      long l1 = paramValue.getPrecision();
      if (l1 < 0L) {
        throw DbException.get(90067, "length=" + l1);
      }
      writeLong(l1);
      long l3 = IOUtils.copyAndCloseInput(paramValue.getInputStream(), this.out);
      if (l3 != l1) {
        throw DbException.get(90067, "length:" + l1 + " written:" + l3);
      }
      writeInt(4660);
      break;
    case 16: 
      if ((this.version >= 11) && 
        ((paramValue instanceof ValueLobDb)))
      {
        ValueLobDb localValueLobDb = (ValueLobDb)paramValue;
        if (localValueLobDb.isStored())
        {
          writeLong(-1L);
          writeInt(localValueLobDb.getTableId());
          writeLong(localValueLobDb.getLobId());
          if (this.version >= 12) {
            writeBytes(calculateLobMac(localValueLobDb.getLobId()));
          }
          writeLong(localValueLobDb.getPrecision());
          break;
        }
      }
      long l2 = paramValue.getPrecision();
      if (l2 < 0L) {
        throw DbException.get(90067, "length=" + l2);
      }
      writeLong(l2);
      Reader localReader = paramValue.getReader();
      Data.copyString(localReader, this.out);
      writeInt(4660);
      break;
    case 17: 
      localObject2 = (ValueArray)paramValue;
      localObject3 = ((ValueArray)localObject2).getList();
      j = localObject3.length;
      Class localClass = ((ValueArray)localObject2).getComponentType();
      if (localClass == Object.class)
      {
        writeInt(j);
      }
      else
      {
        writeInt(-(j + 1));
        writeString(localClass.getName());
      }
      for (Value localValue2 : localObject3) {
        writeValue(localValue2);
      }
      break;
    case 18: 
      try
      {
        localObject2 = ((ValueResultSet)paramValue).getResultSet();
        ((ResultSet)localObject2).beforeFirst();
        localObject3 = ((ResultSet)localObject2).getMetaData();
        j = ((ResultSetMetaData)localObject3).getColumnCount();
        writeInt(j);
        for (int k = 0; k < j; k++)
        {
          writeString(((ResultSetMetaData)localObject3).getColumnName(k + 1));
          writeInt(((ResultSetMetaData)localObject3).getColumnType(k + 1));
          writeInt(((ResultSetMetaData)localObject3).getPrecision(k + 1));
          writeInt(((ResultSetMetaData)localObject3).getScale(k + 1));
        }
        while (((ResultSet)localObject2).next())
        {
          writeBoolean(true);
          for (k = 0; k < j; k++)
          {
            int m = DataType.getValueTypeFromResultSet((ResultSetMetaData)localObject3, k + 1);
            Value localValue1 = DataType.readValue(this.session, (ResultSet)localObject2, k + 1, m);
            writeValue(localValue1);
          }
        }
        writeBoolean(false);
        ((ResultSet)localObject2).beforeFirst();
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convertToIOException(localSQLException);
      }
    case 22: 
      if (this.version >= 14) {
        writeBytes(paramValue.getBytesNoCopy());
      } else {
        writeString(paramValue.getString());
      }
      break;
    default: 
      throw DbException.get(90067, "type=" + i);
    }
  }
  
  public Value readValue()
    throws IOException
  {
    int i = readInt();
    long l1;
    byte[] arrayOfByte2;
    long l4;
    Object localObject;
    int i3;
    switch (i)
    {
    case 0: 
      return ValueNull.INSTANCE;
    case 12: 
      return ValueBytes.getNoCopy(readBytes());
    case 20: 
      return ValueUuid.get(readLong(), readLong());
    case 19: 
      return ValueJavaObject.getNoCopy(null, readBytes(), this.session.getDataHandler());
    case 1: 
      return ValueBoolean.get(readBoolean());
    case 2: 
      return ValueByte.get(readByte());
    case 10: 
      if (this.version >= 9) {
        return ValueDate.fromDateValue(readLong());
      }
      if (this.version >= 7) {
        return ValueDate.fromMillis(DateTimeUtils.getTimeUTCWithoutDst(readLong()));
      }
      return ValueDate.fromMillis(readLong());
    case 9: 
      if (this.version >= 9) {
        return ValueTime.fromNanos(readLong());
      }
      if (this.version >= 7) {
        return ValueTime.fromMillis(DateTimeUtils.getTimeUTCWithoutDst(readLong()));
      }
      return ValueTime.fromMillis(readLong());
    case 11: 
      if (this.version >= 9) {
        return ValueTimestamp.fromDateValueAndNanos(readLong(), readLong());
      }
      if (this.version >= 7) {
        return ValueTimestamp.fromMillisNanos(DateTimeUtils.getTimeUTCWithoutDst(readLong()), readInt() % 1000000);
      }
      return ValueTimestamp.fromMillisNanos(readLong(), readInt() % 1000000);
    case 6: 
      return ValueDecimal.get(new BigDecimal(readString()));
    case 7: 
      return ValueDouble.get(readDouble());
    case 8: 
      return ValueFloat.get(readFloat());
    case 4: 
      return ValueInt.get(readInt());
    case 5: 
      return ValueLong.get(readLong());
    case 3: 
      return ValueShort.get((short)readInt());
    case 13: 
      return ValueString.get(readString());
    case 14: 
      return ValueStringIgnoreCase.get(readString());
    case 21: 
      return ValueStringFixed.get(readString());
    case 15: 
      l1 = readLong();
      if (this.version >= 11)
      {
        if (l1 == -1L)
        {
          m = readInt();
          long l2 = readLong();
          if (this.version >= 12) {
            arrayOfByte2 = readBytes();
          } else {
            arrayOfByte2 = null;
          }
          l4 = readLong();
          return ValueLobDb.create(15, this.session.getDataHandler(), m, l2, arrayOfByte2, l4);
        }
        int m = (int)l1;
        byte[] arrayOfByte1 = new byte[m];
        IOUtils.readFully(this.in, arrayOfByte1, m);
        int i4 = readInt();
        if (i4 != 4660) {
          throw DbException.get(90067, "magic=" + i4);
        }
        return ValueLobDb.createSmallLob(15, arrayOfByte1, l1);
      }
      Value localValue = this.session.getDataHandler().getLobStorage().createBlob(this.in, l1);
      int i2 = readInt();
      if (i2 != 4660) {
        throw DbException.get(90067, "magic=" + i2);
      }
      return localValue;
    case 16: 
      l1 = readLong();
      if (this.version >= 11)
      {
        if (l1 == -1L)
        {
          int n = readInt();
          long l3 = readLong();
          if (this.version >= 12) {
            arrayOfByte2 = readBytes();
          } else {
            arrayOfByte2 = null;
          }
          l4 = readLong();
          return ValueLobDb.create(16, this.session.getDataHandler(), n, l3, arrayOfByte2, l4);
        }
        localObject = new DataReader(this.in);
        i3 = (int)l1;
        char[] arrayOfChar = new char[i3];
        IOUtils.readFully((Reader)localObject, arrayOfChar, i3);
        int i5 = readInt();
        if (i5 != 4660) {
          throw DbException.get(90067, "magic=" + i5);
        }
        byte[] arrayOfByte3 = new String(arrayOfChar).getBytes(Constants.UTF8);
        return ValueLobDb.createSmallLob(16, arrayOfByte3, l1);
      }
      localObject = this.session.getDataHandler().getLobStorage().createClob(new DataReader(this.in), l1);
      
      i3 = readInt();
      if (i3 != 4660) {
        throw DbException.get(90067, "magic=" + i3);
      }
      return (Value)localObject;
    case 17: 
      int j = readInt();
      Class localClass = Object.class;
      if (j < 0)
      {
        j = -(j + 1);
        localClass = JdbcUtils.loadUserClass(readString());
      }
      localObject = new Value[j];
      for (i3 = 0; i3 < j; i3++) {
        localObject[i3] = readValue();
      }
      return ValueArray.get(localClass, (Value[])localObject);
    case 18: 
      SimpleResultSet localSimpleResultSet = new SimpleResultSet();
      localSimpleResultSet.setAutoClose(false);
      int k = readInt();
      for (int i1 = 0; i1 < k; i1++) {
        localSimpleResultSet.addColumn(readString(), readInt(), readInt(), readInt());
      }
      while (readBoolean())
      {
        Object[] arrayOfObject = new Object[k];
        for (i3 = 0; i3 < k; i3++) {
          arrayOfObject[i3] = readValue().getObject();
        }
        localSimpleResultSet.addRow(arrayOfObject);
      }
      return ValueResultSet.get(localSimpleResultSet);
    case 22: 
      if (this.version >= 14) {
        return ValueGeometry.get(readBytes());
      }
      return ValueGeometry.get(readString());
    }
    throw DbException.get(90067, "type=" + i);
  }
  
  public Socket getSocket()
  {
    return this.socket;
  }
  
  public void setSession(SessionInterface paramSessionInterface)
  {
    this.session = paramSessionInterface;
  }
  
  public void setSSL(boolean paramBoolean)
  {
    this.ssl = paramBoolean;
  }
  
  public Transfer openNewConnection()
    throws IOException
  {
    InetAddress localInetAddress = this.socket.getInetAddress();
    int i = this.socket.getPort();
    Socket localSocket = NetUtils.createSocket(localInetAddress, i, this.ssl);
    Transfer localTransfer = new Transfer(null);
    localTransfer.setSocket(localSocket);
    localTransfer.setSSL(this.ssl);
    return localTransfer;
  }
  
  public void setVersion(int paramInt)
  {
    this.version = paramInt;
  }
  
  public synchronized boolean isClosed()
  {
    return (this.socket == null) || (this.socket.isClosed());
  }
  
  public void verifyLobMac(byte[] paramArrayOfByte, long paramLong)
  {
    byte[] arrayOfByte = calculateLobMac(paramLong);
    if (!Utils.compareSecure(paramArrayOfByte, arrayOfByte)) {
      throw DbException.get(90067, "Invalid lob hmac; possibly the connection was re-opened internally");
    }
  }
  
  private byte[] calculateLobMac(long paramLong)
  {
    if (this.lobMacSalt == null) {
      this.lobMacSalt = MathUtils.secureRandomBytes(16);
    }
    byte[] arrayOfByte1 = new byte[8];
    Utils.writeLong(arrayOfByte1, 0, paramLong);
    byte[] arrayOfByte2 = SHA256.getHashWithSalt(arrayOfByte1, this.lobMacSalt);
    return arrayOfByte2;
  }
}
