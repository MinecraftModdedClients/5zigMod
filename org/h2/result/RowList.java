package org.h2.result;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.store.Data;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.util.New;
import org.h2.value.Value;

public class RowList
{
  private final Session session;
  private final ArrayList<Row> list = New.arrayList();
  private int size;
  private int index;
  private int listIndex;
  private FileStore file;
  private Data rowBuff;
  private ArrayList<Value> lobs;
  private final int maxMemory;
  private int memory;
  private boolean written;
  private boolean readUncached;
  
  public RowList(Session paramSession)
  {
    this.session = paramSession;
    if (paramSession.getDatabase().isPersistent()) {
      this.maxMemory = paramSession.getDatabase().getMaxOperationMemory();
    } else {
      this.maxMemory = 0;
    }
  }
  
  private void writeRow(Data paramData, Row paramRow)
  {
    paramData.checkCapacity(33);
    paramData.writeByte((byte)1);
    paramData.writeInt(paramRow.getMemory());
    int i = paramRow.getColumnCount();
    paramData.writeInt(i);
    paramData.writeLong(paramRow.getKey());
    paramData.writeInt(paramRow.getVersion());
    paramData.writeInt(paramRow.isDeleted() ? 1 : 0);
    paramData.writeInt(paramRow.getSessionId());
    for (int j = 0; j < i; j++)
    {
      Value localValue = paramRow.getValue(j);
      paramData.checkCapacity(1);
      if (localValue == null)
      {
        paramData.writeByte((byte)0);
      }
      else
      {
        paramData.writeByte((byte)1);
        if ((localValue.getType() == 16) || (localValue.getType() == 15)) {
          if ((localValue.getSmall() == null) && (localValue.getTableId() == 0))
          {
            if (this.lobs == null) {
              this.lobs = New.arrayList();
            }
            localValue = localValue.copyToTemp();
            this.lobs.add(localValue);
          }
        }
        paramData.checkCapacity(paramData.getValueLen(localValue));
        paramData.writeValue(localValue);
      }
    }
  }
  
  private void writeAllRows()
  {
    if (this.file == null)
    {
      localObject = this.session.getDatabase();
      String str = ((Database)localObject).createTempFile();
      this.file = ((Database)localObject).openFile(str, "rw", false);
      this.file.setCheckedWriting(false);
      this.file.seek(48L);
      this.rowBuff = Data.create((DataHandler)localObject, 4096);
      this.file.seek(48L);
    }
    Object localObject = this.rowBuff;
    initBuffer((Data)localObject);
    int i = 0;
    for (int j = this.list.size(); i < j; i++)
    {
      if ((i > 0) && (((Data)localObject).length() > 4096))
      {
        flushBuffer((Data)localObject);
        initBuffer((Data)localObject);
      }
      Row localRow = (Row)this.list.get(i);
      writeRow((Data)localObject, localRow);
    }
    flushBuffer((Data)localObject);
    this.file.autoDelete();
    this.list.clear();
    this.memory = 0;
  }
  
  private static void initBuffer(Data paramData)
  {
    paramData.reset();
    paramData.writeInt(0);
  }
  
  private void flushBuffer(Data paramData)
  {
    paramData.checkCapacity(1);
    paramData.writeByte((byte)0);
    paramData.fillAligned();
    paramData.setInt(0, paramData.length() / 16);
    this.file.write(paramData.getBytes(), 0, paramData.length());
  }
  
  public void add(Row paramRow)
  {
    this.list.add(paramRow);
    this.memory += paramRow.getMemory() + 8;
    if ((this.maxMemory > 0) && (this.memory > this.maxMemory)) {
      writeAllRows();
    }
    this.size += 1;
  }
  
  public void reset()
  {
    this.index = 0;
    if (this.file != null)
    {
      this.listIndex = 0;
      if (!this.written)
      {
        writeAllRows();
        this.written = true;
      }
      this.list.clear();
      this.file.seek(48L);
    }
  }
  
  public boolean hasNext()
  {
    return this.index < this.size;
  }
  
  private Row readRow(Data paramData)
  {
    if (paramData.readByte() == 0) {
      return null;
    }
    int i = paramData.readInt();
    int j = paramData.readInt();
    long l = paramData.readLong();
    int k = paramData.readInt();
    if (this.readUncached) {
      l = 0L;
    }
    boolean bool = paramData.readInt() == 1;
    int m = paramData.readInt();
    Value[] arrayOfValue = new Value[j];
    for (int n = 0; n < j; n++)
    {
      Value localValue;
      if (paramData.readByte() == 0)
      {
        localValue = null;
      }
      else
      {
        localValue = paramData.readValue();
        if (localValue.isLinked()) {
          if (localValue.getTableId() == 0) {
            this.session.unlinkAtCommit(localValue);
          }
        }
      }
      arrayOfValue[n] = localValue;
    }
    Row localRow = new Row(arrayOfValue, i);
    localRow.setKey(l);
    localRow.setVersion(k);
    localRow.setDeleted(bool);
    localRow.setSessionId(m);
    return localRow;
  }
  
  public Row next()
  {
    Row localRow;
    if (this.file == null)
    {
      localRow = (Row)this.list.get(this.index++);
    }
    else
    {
      if (this.listIndex >= this.list.size())
      {
        this.list.clear();
        this.listIndex = 0;
        Data localData = this.rowBuff;
        localData.reset();
        int i = 16;
        this.file.readFully(localData.getBytes(), 0, i);
        int j = localData.readInt() * 16;
        localData.checkCapacity(j);
        if (j - i > 0) {
          this.file.readFully(localData.getBytes(), i, j - i);
        }
        for (;;)
        {
          localRow = readRow(localData);
          if (localRow == null) {
            break;
          }
          this.list.add(localRow);
        }
      }
      this.index += 1;
      localRow = (Row)this.list.get(this.listIndex++);
    }
    return localRow;
  }
  
  public int size()
  {
    return this.size;
  }
  
  public void invalidateCache()
  {
    this.readUncached = true;
  }
  
  public void close()
  {
    if (this.file != null)
    {
      this.file.autoDelete();
      this.file.closeAndDeleteSilently();
      this.file = null;
      this.rowBuff = null;
    }
  }
}
