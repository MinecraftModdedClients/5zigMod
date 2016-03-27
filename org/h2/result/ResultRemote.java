package org.h2.result;

import java.io.IOException;
import java.util.ArrayList;
import org.h2.engine.SessionRemote;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.util.New;
import org.h2.value.Transfer;
import org.h2.value.Value;

public class ResultRemote
  implements ResultInterface
{
  private int fetchSize;
  private SessionRemote session;
  private Transfer transfer;
  private int id;
  private final ResultColumn[] columns;
  private Value[] currentRow;
  private final int rowCount;
  private int rowId;
  private int rowOffset;
  private ArrayList<Value[]> result;
  private final Trace trace;
  
  public ResultRemote(SessionRemote paramSessionRemote, Transfer paramTransfer, int paramInt1, int paramInt2, int paramInt3)
    throws IOException
  {
    this.session = paramSessionRemote;
    this.trace = paramSessionRemote.getTrace();
    this.transfer = paramTransfer;
    this.id = paramInt1;
    this.columns = new ResultColumn[paramInt2];
    this.rowCount = paramTransfer.readInt();
    for (int i = 0; i < paramInt2; i++) {
      this.columns[i] = new ResultColumn(paramTransfer);
    }
    this.rowId = -1;
    this.result = New.arrayList();
    this.fetchSize = paramInt3;
    fetchRows(false);
  }
  
  public String getAlias(int paramInt)
  {
    return this.columns[paramInt].alias;
  }
  
  public String getSchemaName(int paramInt)
  {
    return this.columns[paramInt].schemaName;
  }
  
  public String getTableName(int paramInt)
  {
    return this.columns[paramInt].tableName;
  }
  
  public String getColumnName(int paramInt)
  {
    return this.columns[paramInt].columnName;
  }
  
  public int getColumnType(int paramInt)
  {
    return this.columns[paramInt].columnType;
  }
  
  public long getColumnPrecision(int paramInt)
  {
    return this.columns[paramInt].precision;
  }
  
  public int getColumnScale(int paramInt)
  {
    return this.columns[paramInt].scale;
  }
  
  public int getDisplaySize(int paramInt)
  {
    return this.columns[paramInt].displaySize;
  }
  
  public boolean isAutoIncrement(int paramInt)
  {
    return this.columns[paramInt].autoIncrement;
  }
  
  public int getNullable(int paramInt)
  {
    return this.columns[paramInt].nullable;
  }
  
  public void reset()
  {
    this.rowId = -1;
    this.currentRow = null;
    if (this.session == null) {
      return;
    }
    synchronized (this.session)
    {
      this.session.checkClosed();
      try
      {
        this.session.traceOperation("RESULT_RESET", this.id);
        this.transfer.writeInt(6).writeInt(this.id).flush();
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, null);
      }
    }
  }
  
  public Value[] currentRow()
  {
    return this.currentRow;
  }
  
  public boolean next()
  {
    if (this.rowId < this.rowCount)
    {
      this.rowId += 1;
      remapIfOld();
      if (this.rowId < this.rowCount)
      {
        if (this.rowId - this.rowOffset >= this.result.size()) {
          fetchRows(true);
        }
        this.currentRow = ((Value[])this.result.get(this.rowId - this.rowOffset));
        return true;
      }
      this.currentRow = null;
    }
    return false;
  }
  
  public int getRowId()
  {
    return this.rowId;
  }
  
  public int getVisibleColumnCount()
  {
    return this.columns.length;
  }
  
  public int getRowCount()
  {
    return this.rowCount;
  }
  
  private void sendClose()
  {
    if (this.session == null) {
      return;
    }
    try
    {
      synchronized (this.session)
      {
        this.session.traceOperation("RESULT_CLOSE", this.id);
        this.transfer.writeInt(7).writeInt(this.id);
      }
    }
    catch (IOException localIOException)
    {
      this.trace.error(localIOException, "close");
    }
    finally
    {
      this.transfer = null;
      this.session = null;
    }
  }
  
  public void close()
  {
    this.result = null;
    sendClose();
  }
  
  private void remapIfOld()
  {
    if (this.session == null) {
      return;
    }
    try
    {
      if (this.id <= this.session.getCurrentId() - SysProperties.SERVER_CACHED_OBJECTS / 2)
      {
        int i = this.session.getNextId();
        this.session.traceOperation("CHANGE_ID", this.id);
        this.transfer.writeInt(9).writeInt(this.id).writeInt(i);
        this.id = i;
      }
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  private void fetchRows(boolean paramBoolean)
  {
    synchronized (this.session)
    {
      this.session.checkClosed();
      try
      {
        this.rowOffset += this.result.size();
        this.result.clear();
        int i = Math.min(this.fetchSize, this.rowCount - this.rowOffset);
        if (paramBoolean)
        {
          this.session.traceOperation("RESULT_FETCH_ROWS", this.id);
          this.transfer.writeInt(5).writeInt(this.id).writeInt(i);
          
          this.session.done(this.transfer);
        }
        for (int j = 0; j < i; j++)
        {
          boolean bool = this.transfer.readBoolean();
          if (!bool) {
            break;
          }
          int k = this.columns.length;
          Value[] arrayOfValue = new Value[k];
          for (int m = 0; m < k; m++)
          {
            Value localValue = this.transfer.readValue();
            arrayOfValue[m] = localValue;
          }
          this.result.add(arrayOfValue);
        }
        if (this.rowOffset + this.result.size() >= this.rowCount) {
          sendClose();
        }
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, null);
      }
    }
  }
  
  public String toString()
  {
    return "columns: " + this.columns.length + " rows: " + this.rowCount + " pos: " + this.rowId;
  }
  
  public int getFetchSize()
  {
    return this.fetchSize;
  }
  
  public void setFetchSize(int paramInt)
  {
    this.fetchSize = paramInt;
  }
  
  public boolean needToClose()
  {
    return true;
  }
}
