package org.h2.engine;

import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.store.Data;
import org.h2.store.FileStore;
import org.h2.table.Table;
import org.h2.value.Value;

public class UndoLogRecord
{
  public static final short INSERT = 0;
  public static final short DELETE = 1;
  private static final int IN_MEMORY = 0;
  private static final int STORED = 1;
  private static final int IN_MEMORY_INVALID = 2;
  private Table table;
  private Row row;
  private short operation;
  private short state;
  private int filePos;
  
  UndoLogRecord(Table paramTable, short paramShort, Row paramRow)
  {
    this.table = paramTable;
    this.row = paramRow;
    this.operation = paramShort;
    this.state = 0;
  }
  
  boolean isStored()
  {
    return this.state == 1;
  }
  
  boolean canStore()
  {
    if (this.table.getUniqueIndex() != null) {
      return true;
    }
    return false;
  }
  
  void undo(Session paramSession)
  {
    Database localDatabase = paramSession.getDatabase();
    switch (this.operation)
    {
    case 0: 
      if (this.state == 2) {
        this.state = 0;
      }
      if ((localDatabase.getLockMode() == 0) && 
        (this.row.isDeleted())) {
        return;
      }
      try
      {
        this.row.setDeleted(false);
        this.table.removeRow(paramSession, this.row);
        this.table.fireAfterRow(paramSession, this.row, null, true);
      }
      catch (DbException localDbException1)
      {
        if ((paramSession.getDatabase().getLockMode() != 0) || (localDbException1.getErrorCode() != 90112)) {
          throw localDbException1;
        }
      }
    case 1: 
      try
      {
        this.table.addRow(paramSession, this.row);
        this.table.fireAfterRow(paramSession, null, this.row, true);
        
        this.row.commit();
      }
      catch (DbException localDbException2)
      {
        if ((paramSession.getDatabase().getLockMode() != 0) || (localDbException2.getSQLException().getErrorCode() != 23505)) {
          throw localDbException2;
        }
      }
    default: 
      DbException.throwInternalError("op=" + this.operation);
    }
  }
  
  void append(Data paramData, UndoLog paramUndoLog)
  {
    int i = paramData.length();
    paramData.writeInt(0);
    paramData.writeInt(this.operation);
    paramData.writeByte((byte)(this.row.isDeleted() ? 1 : 0));
    paramData.writeInt(paramUndoLog.getTableId(this.table));
    paramData.writeLong(this.row.getKey());
    paramData.writeInt(this.row.getSessionId());
    int j = this.row.getColumnCount();
    paramData.writeInt(j);
    for (int k = 0; k < j; k++)
    {
      Value localValue = this.row.getValue(k);
      paramData.checkCapacity(paramData.getValueLen(localValue));
      paramData.writeValue(localValue);
    }
    paramData.fillAligned();
    paramData.setInt(i, (paramData.length() - i) / 16);
  }
  
  void save(Data paramData, FileStore paramFileStore, UndoLog paramUndoLog)
  {
    paramData.reset();
    append(paramData, paramUndoLog);
    this.filePos = ((int)(paramFileStore.getFilePointer() / 16L));
    paramFileStore.write(paramData.getBytes(), 0, paramData.length());
    this.row = null;
    this.state = 1;
  }
  
  static UndoLogRecord loadFromBuffer(Data paramData, UndoLog paramUndoLog)
  {
    UndoLogRecord localUndoLogRecord = new UndoLogRecord(null, (short)0, null);
    int i = paramData.length();
    int j = paramData.readInt() * 16;
    localUndoLogRecord.load(paramData, paramUndoLog);
    paramData.setPos(i + j);
    return localUndoLogRecord;
  }
  
  void load(Data paramData, FileStore paramFileStore, UndoLog paramUndoLog)
  {
    int i = 16;
    paramUndoLog.seek(this.filePos);
    paramData.reset();
    paramFileStore.readFully(paramData.getBytes(), 0, i);
    int j = paramData.readInt() * 16;
    paramData.checkCapacity(j);
    if (j - i > 0) {
      paramFileStore.readFully(paramData.getBytes(), i, j - i);
    }
    int k = this.operation;
    load(paramData, paramUndoLog);
    if ((SysProperties.CHECK) && 
      (this.operation != k)) {
      DbException.throwInternalError("operation=" + this.operation + " op=" + k);
    }
  }
  
  private void load(Data paramData, UndoLog paramUndoLog)
  {
    this.operation = ((short)paramData.readInt());
    boolean bool = paramData.readByte() == 1;
    this.table = paramUndoLog.getTable(paramData.readInt());
    long l = paramData.readLong();
    int i = paramData.readInt();
    int j = paramData.readInt();
    Value[] arrayOfValue = new Value[j];
    for (int k = 0; k < j; k++) {
      arrayOfValue[k] = paramData.readValue();
    }
    this.row = new Row(arrayOfValue, -1);
    this.row.setKey(l);
    this.row.setDeleted(bool);
    this.row.setSessionId(i);
    this.state = 2;
  }
  
  public Table getTable()
  {
    return this.table;
  }
  
  public long getFilePos()
  {
    return this.filePos;
  }
  
  void commit()
  {
    this.table.commit(this.operation, this.row);
  }
  
  public Row getRow()
  {
    return this.row;
  }
  
  void invalidatePos()
  {
    if (this.state == 0) {
      this.state = 2;
    }
  }
}
