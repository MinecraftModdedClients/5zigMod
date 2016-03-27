package org.h2.engine;

import java.util.ArrayList;
import java.util.HashMap;
import org.h2.message.DbException;
import org.h2.store.Data;
import org.h2.store.FileStore;
import org.h2.table.Table;
import org.h2.util.New;

public class UndoLog
{
  private final Database database;
  private final ArrayList<Long> storedEntriesPos = New.arrayList();
  private final ArrayList<UndoLogRecord> records = New.arrayList();
  private FileStore file;
  private Data rowBuff;
  private int memoryUndo;
  private int storedEntries;
  private HashMap<Integer, Table> tables;
  private final boolean largeTransactions;
  
  UndoLog(Session paramSession)
  {
    this.database = paramSession.getDatabase();
    this.largeTransactions = this.database.getSettings().largeTransactions;
  }
  
  int size()
  {
    if (this.largeTransactions) {
      return this.storedEntries + this.records.size();
    }
    if ((SysProperties.CHECK) && (this.memoryUndo > this.records.size())) {
      DbException.throwInternalError();
    }
    return this.records.size();
  }
  
  void clear()
  {
    this.records.clear();
    this.storedEntries = 0;
    this.storedEntriesPos.clear();
    this.memoryUndo = 0;
    if (this.file != null)
    {
      this.file.closeAndDeleteSilently();
      this.file = null;
      this.rowBuff = null;
    }
  }
  
  public UndoLogRecord getLast()
  {
    long l1 = this.records.size() - 1;
    long l4;
    if (this.largeTransactions)
    {
      if ((l1 < 0) && (this.storedEntries > 0))
      {
        int i = this.storedEntriesPos.size() - 1;
        long l2 = ((Long)this.storedEntriesPos.get(i)).longValue();
        this.storedEntriesPos.remove(i);
        l4 = this.file.length();
        int j = (int)(l4 - l2);
        Data localData = Data.create(this.database, j);
        this.file.seek(l2);
        this.file.readFully(localData.getBytes(), 0, j);
        while (localData.length() < j)
        {
          UndoLogRecord localUndoLogRecord3 = UndoLogRecord.loadFromBuffer(localData, this);
          this.records.add(localUndoLogRecord3);
          this.memoryUndo += 1;
        }
        this.storedEntries -= this.records.size();
        this.file.setLength(l2);
        this.file.seek(l2);
      }
      l1 = this.records.size() - 1;
    }
    UndoLogRecord localUndoLogRecord1 = (UndoLogRecord)this.records.get(l1);
    if (localUndoLogRecord1.isStored())
    {
      long l3 = Math.max(0, l1 - this.database.getMaxMemoryUndo() / 2);
      Object localObject = null;
      UndoLogRecord localUndoLogRecord2;
      for (l4 = l3; l4 <= l1; l4++)
      {
        localUndoLogRecord2 = (UndoLogRecord)this.records.get(l4);
        if (localUndoLogRecord2.isStored())
        {
          localUndoLogRecord2.load(this.rowBuff, this.file, this);
          this.memoryUndo += 1;
          if (localObject == null) {
            localObject = localUndoLogRecord2;
          }
        }
      }
      for (long l5 = 0; l5 < l1; l5++)
      {
        localUndoLogRecord2 = (UndoLogRecord)this.records.get(l5);
        localUndoLogRecord2.invalidatePos();
      }
      seek(((UndoLogRecord)localObject).getFilePos());
    }
    return localUndoLogRecord1;
  }
  
  void seek(long paramLong)
  {
    this.file.seek(paramLong * 16L);
  }
  
  void removeLast(boolean paramBoolean)
  {
    int i = this.records.size() - 1;
    UndoLogRecord localUndoLogRecord = (UndoLogRecord)this.records.remove(i);
    if (!localUndoLogRecord.isStored()) {
      this.memoryUndo -= 1;
    }
    if ((paramBoolean) && (i > 1024) && ((i & 0x3FF) == 0)) {
      this.records.trimToSize();
    }
  }
  
  void add(UndoLogRecord paramUndoLogRecord)
  {
    this.records.add(paramUndoLogRecord);
    Object localObject;
    if (this.largeTransactions)
    {
      this.memoryUndo += 1;
      if ((this.memoryUndo > this.database.getMaxMemoryUndo()) && (this.database.isPersistent()) && (!this.database.isMultiVersion()))
      {
        if (this.file == null)
        {
          localObject = this.database.createTempFile();
          this.file = this.database.openFile((String)localObject, "rw", false);
          this.file.setCheckedWriting(false);
          this.file.setLength(48L);
        }
        localObject = Data.create(this.database, 4096);
        for (int i = 0; i < this.records.size(); i++)
        {
          UndoLogRecord localUndoLogRecord1 = (UndoLogRecord)this.records.get(i);
          ((Data)localObject).checkCapacity(4096);
          localUndoLogRecord1.append((Data)localObject, this);
          if ((i == this.records.size() - 1) || (((Data)localObject).length() > 1048576))
          {
            this.storedEntriesPos.add(Long.valueOf(this.file.getFilePointer()));
            this.file.write(((Data)localObject).getBytes(), 0, ((Data)localObject).length());
            ((Data)localObject).reset();
          }
        }
        this.storedEntries += this.records.size();
        this.memoryUndo = 0;
        this.records.clear();
        this.file.autoDelete();
      }
    }
    else
    {
      if (!paramUndoLogRecord.isStored()) {
        this.memoryUndo += 1;
      }
      if ((this.memoryUndo > this.database.getMaxMemoryUndo()) && (this.database.isPersistent()) && (!this.database.isMultiVersion()))
      {
        if (this.file == null)
        {
          localObject = this.database.createTempFile();
          this.file = this.database.openFile((String)localObject, "rw", false);
          this.file.setCheckedWriting(false);
          this.file.seek(48L);
          this.rowBuff = Data.create(this.database, 4096);
          Data localData = this.rowBuff;
          for (int j = 0; j < this.records.size(); j++)
          {
            UndoLogRecord localUndoLogRecord2 = (UndoLogRecord)this.records.get(j);
            saveIfPossible(localUndoLogRecord2, localData);
          }
        }
        else
        {
          saveIfPossible(paramUndoLogRecord, this.rowBuff);
        }
        this.file.autoDelete();
      }
    }
  }
  
  private void saveIfPossible(UndoLogRecord paramUndoLogRecord, Data paramData)
  {
    if ((!paramUndoLogRecord.isStored()) && (paramUndoLogRecord.canStore()))
    {
      paramUndoLogRecord.save(paramData, this.file, this);
      this.memoryUndo -= 1;
    }
  }
  
  int getTableId(Table paramTable)
  {
    int i = paramTable.getId();
    if (this.tables == null) {
      this.tables = New.hashMap();
    }
    this.tables.put(Integer.valueOf(i), paramTable);
    return i;
  }
  
  Table getTable(int paramInt)
  {
    return (Table)this.tables.get(Integer.valueOf(paramInt));
  }
}
