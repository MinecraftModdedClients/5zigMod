package org.h2.table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.LinkedIndex;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.result.RowList;
import org.h2.schema.Schema;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.DataType;
import org.h2.value.Value;

public class TableLink
  extends Table
{
  private static final int MAX_RETRY = 2;
  private static final long ROW_COUNT_APPROXIMATION = 100000L;
  private final String originalSchema;
  private String driver;
  private String url;
  private String user;
  private String password;
  private String originalTable;
  private String qualifiedTableName;
  private TableLinkConnection conn;
  private HashMap<String, PreparedStatement> preparedMap = New.hashMap();
  private final ArrayList<Index> indexes = New.arrayList();
  private final boolean emitUpdates;
  private LinkedIndex linkedIndex;
  private DbException connectException;
  private boolean storesLowerCase;
  private boolean storesMixedCase;
  private boolean storesMixedCaseQuoted;
  private boolean supportsMixedCaseIdentifiers;
  private boolean globalTemporary;
  private boolean readOnly;
  
  public TableLink(Schema paramSchema, int paramInt, String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, String paramString6, String paramString7, boolean paramBoolean1, boolean paramBoolean2)
  {
    super(paramSchema, paramInt, paramString1, false, true);
    this.driver = paramString2;
    this.url = paramString3;
    this.user = paramString4;
    this.password = paramString5;
    this.originalSchema = paramString6;
    this.originalTable = paramString7;
    this.emitUpdates = paramBoolean1;
    try
    {
      connect();
    }
    catch (DbException localDbException)
    {
      if (!paramBoolean2) {
        throw localDbException;
      }
      Column[] arrayOfColumn = new Column[0];
      setColumns(arrayOfColumn);
      this.linkedIndex = new LinkedIndex(this, paramInt, IndexColumn.wrap(arrayOfColumn), IndexType.createNonUnique(false));
      
      this.indexes.add(this.linkedIndex);
    }
  }
  
  private void connect()
  {
    this.connectException = null;
    int i = 0;
    for (;;)
    {
      try
      {
        this.conn = this.database.getLinkConnection(this.driver, this.url, this.user, this.password);
        synchronized (this.conn)
        {
          try
          {
            readMetaData();
            return;
          }
          catch (Exception localException)
          {
            this.conn.close(true);
            this.conn = null;
            throw DbException.convert(localException);
          }
        }
        i++;
      }
      catch (DbException localDbException)
      {
        if (i >= 2)
        {
          this.connectException = localDbException;
          throw localDbException;
        }
      }
    }
  }
  
  private void readMetaData()
    throws SQLException
  {
    DatabaseMetaData localDatabaseMetaData = this.conn.getConnection().getMetaData();
    this.storesLowerCase = localDatabaseMetaData.storesLowerCaseIdentifiers();
    this.storesMixedCase = localDatabaseMetaData.storesMixedCaseIdentifiers();
    this.storesMixedCaseQuoted = localDatabaseMetaData.storesMixedCaseQuotedIdentifiers();
    this.supportsMixedCaseIdentifiers = localDatabaseMetaData.supportsMixedCaseIdentifiers();
    ResultSet localResultSet = localDatabaseMetaData.getTables(null, this.originalSchema, this.originalTable, null);
    if ((localResultSet.next()) && (localResultSet.next())) {
      throw DbException.get(90080, this.originalTable);
    }
    localResultSet.close();
    localResultSet = localDatabaseMetaData.getColumns(null, this.originalSchema, this.originalTable, null);
    int i = 0;
    ArrayList localArrayList1 = New.arrayList();
    HashMap localHashMap = New.hashMap();
    Object localObject1 = null;Object localObject2 = null;
    Object localObject4;
    String str1;
    int k;
    long l;
    int n;
    int i1;
    int i2;
    Object localObject8;
    while (localResultSet.next())
    {
      localObject3 = localResultSet.getString("TABLE_CAT");
      if (localObject1 == null) {
        localObject1 = localObject3;
      }
      localObject4 = localResultSet.getString("TABLE_SCHEM");
      if (localObject2 == null) {
        localObject2 = localObject4;
      }
      if ((!StringUtils.equals((String)localObject1, (String)localObject3)) || (!StringUtils.equals((String)localObject2, (String)localObject4)))
      {
        localHashMap.clear();
        localArrayList1.clear();
        break;
      }
      str1 = localResultSet.getString("COLUMN_NAME");
      str1 = convertColumnName(str1);
      k = localResultSet.getInt("DATA_TYPE");
      l = localResultSet.getInt("COLUMN_SIZE");
      l = convertPrecision(k, l);
      n = localResultSet.getInt("DECIMAL_DIGITS");
      n = convertScale(k, n);
      i1 = MathUtils.convertLongToInt(l);
      i2 = DataType.convertSQLTypeToValueType(k);
      localObject8 = new Column(str1, i2, l, n, i1);
      ((Column)localObject8).setTable(this, i++);
      localArrayList1.add(localObject8);
      localHashMap.put(str1, localObject8);
    }
    localResultSet.close();
    if ((this.originalTable.indexOf('.') < 0) && (!StringUtils.isNullOrEmpty((String)localObject2))) {
      this.qualifiedTableName = ((String)localObject2 + "." + this.originalTable);
    } else {
      this.qualifiedTableName = this.originalTable;
    }
    Object localObject3 = null;
    try
    {
      localObject3 = this.conn.getConnection().createStatement();
      localResultSet = ((Statement)localObject3).executeQuery("SELECT * FROM " + this.qualifiedTableName + " T WHERE 1=0");
      if (localArrayList1.size() == 0)
      {
        localObject4 = localResultSet.getMetaData();
        for (i = 0; i < ((ResultSetMetaData)localObject4).getColumnCount();)
        {
          str1 = ((ResultSetMetaData)localObject4).getColumnName(i + 1);
          str1 = convertColumnName(str1);
          k = ((ResultSetMetaData)localObject4).getColumnType(i + 1);
          l = ((ResultSetMetaData)localObject4).getPrecision(i + 1);
          l = convertPrecision(k, l);
          n = ((ResultSetMetaData)localObject4).getScale(i + 1);
          n = convertScale(k, n);
          i1 = ((ResultSetMetaData)localObject4).getColumnDisplaySize(i + 1);
          i2 = DataType.getValueTypeFromResultSet((ResultSetMetaData)localObject4, i + 1);
          localObject8 = new Column(str1, i2, l, n, i1);
          ((Column)localObject8).setTable(this, i++);
          localArrayList1.add(localObject8);
          localHashMap.put(str1, localObject8);
        }
      }
      localResultSet.close();
    }
    catch (Exception localException1)
    {
      throw DbException.get(42102, localException1, new String[] { this.originalTable + "(" + localException1.toString() + ")" });
    }
    finally
    {
      JdbcUtils.closeSilently((Statement)localObject3);
    }
    Column[] arrayOfColumn = new Column[localArrayList1.size()];
    localArrayList1.toArray(arrayOfColumn);
    setColumns(arrayOfColumn);
    int j = getId();
    this.linkedIndex = new LinkedIndex(this, j, IndexColumn.wrap(arrayOfColumn), IndexType.createNonUnique(false));
    
    this.indexes.add(this.linkedIndex);
    try
    {
      localResultSet = localDatabaseMetaData.getPrimaryKeys(null, this.originalSchema, this.originalTable);
    }
    catch (Exception localException2)
    {
      localResultSet = null;
    }
    String str2 = "";
    Object localObject7;
    if ((localResultSet != null) && (localResultSet.next()))
    {
      localArrayList2 = New.arrayList();
      do
      {
        int m = localResultSet.getInt("KEY_SEQ");
        if (str2 == null) {
          str2 = localResultSet.getString("PK_NAME");
        }
        while (localArrayList2.size() < m) {
          localArrayList2.add(null);
        }
        localObject6 = localResultSet.getString("COLUMN_NAME");
        localObject6 = convertColumnName((String)localObject6);
        localObject7 = (Column)localHashMap.get(localObject6);
        if (m == 0) {
          localArrayList2.add(localObject7);
        } else {
          localArrayList2.set(m - 1, localObject7);
        }
      } while (localResultSet.next());
      addIndex(localArrayList2, IndexType.createPrimaryKey(false, false));
      localResultSet.close();
    }
    try
    {
      localResultSet = localDatabaseMetaData.getIndexInfo(null, this.originalSchema, this.originalTable, false, true);
    }
    catch (Exception localException3)
    {
      localResultSet = null;
    }
    Object localObject5 = null;
    ArrayList localArrayList2 = New.arrayList();
    Object localObject6 = null;
    if (localResultSet != null)
    {
      while (localResultSet.next()) {
        if (localResultSet.getShort("TYPE") != 0)
        {
          localObject7 = localResultSet.getString("INDEX_NAME");
          if (!str2.equals(localObject7))
          {
            if ((localObject5 != null) && (!((String)localObject5).equals(localObject7)))
            {
              addIndex(localArrayList2, (IndexType)localObject6);
              localObject5 = null;
            }
            if (localObject5 == null)
            {
              localObject5 = localObject7;
              localArrayList2.clear();
            }
            i2 = !localResultSet.getBoolean("NON_UNIQUE") ? 1 : 0;
            localObject6 = i2 != 0 ? IndexType.createUnique(false, false) : IndexType.createNonUnique(false);
            
            localObject8 = localResultSet.getString("COLUMN_NAME");
            localObject8 = convertColumnName((String)localObject8);
            Column localColumn = (Column)localHashMap.get(localObject8);
            localArrayList2.add(localColumn);
          }
        }
      }
      localResultSet.close();
    }
    if (localObject5 != null) {
      addIndex(localArrayList2, (IndexType)localObject6);
    }
  }
  
  private static long convertPrecision(int paramInt, long paramLong)
  {
    switch (paramInt)
    {
    case 2: 
    case 3: 
      if (paramLong == 0L) {
        paramLong = 65535L;
      }
      break;
    case 91: 
      paramLong = Math.max(8L, paramLong);
      break;
    case 93: 
      paramLong = Math.max(23L, paramLong);
      break;
    case 92: 
      paramLong = Math.max(6L, paramLong);
    }
    return paramLong;
  }
  
  private static int convertScale(int paramInt1, int paramInt2)
  {
    switch (paramInt1)
    {
    case 2: 
    case 3: 
      if (paramInt2 < 0) {
        paramInt2 = 32767;
      }
      break;
    }
    return paramInt2;
  }
  
  private String convertColumnName(String paramString)
  {
    if (((this.storesMixedCase) || (this.storesLowerCase)) && (paramString.equals(StringUtils.toLowerEnglish(paramString)))) {
      paramString = StringUtils.toUpperEnglish(paramString);
    } else if ((this.storesMixedCase) && (!this.supportsMixedCaseIdentifiers)) {
      paramString = StringUtils.toUpperEnglish(paramString);
    } else if ((this.storesMixedCase) && (this.storesMixedCaseQuoted)) {
      paramString = StringUtils.toUpperEnglish(paramString);
    }
    return paramString;
  }
  
  private void addIndex(ArrayList<Column> paramArrayList, IndexType paramIndexType)
  {
    Column[] arrayOfColumn = new Column[paramArrayList.size()];
    paramArrayList.toArray(arrayOfColumn);
    LinkedIndex localLinkedIndex = new LinkedIndex(this, 0, IndexColumn.wrap(arrayOfColumn), paramIndexType);
    this.indexes.add(localLinkedIndex);
  }
  
  public String getDropSQL()
  {
    return "DROP TABLE IF EXISTS " + getSQL();
  }
  
  public String getCreateSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder("CREATE FORCE ");
    if (isTemporary())
    {
      if (this.globalTemporary) {
        localStringBuilder.append("GLOBAL ");
      } else {
        localStringBuilder.append("LOCAL ");
      }
      localStringBuilder.append("TEMPORARY ");
    }
    localStringBuilder.append("LINKED TABLE ").append(getSQL());
    if (this.comment != null) {
      localStringBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    localStringBuilder.append('(').append(StringUtils.quoteStringSQL(this.driver)).append(", ").append(StringUtils.quoteStringSQL(this.url)).append(", ").append(StringUtils.quoteStringSQL(this.user)).append(", ").append(StringUtils.quoteStringSQL(this.password)).append(", ").append(StringUtils.quoteStringSQL(this.originalTable)).append(')');
    if (this.emitUpdates) {
      localStringBuilder.append(" EMIT UPDATES");
    }
    if (this.readOnly) {
      localStringBuilder.append(" READONLY");
    }
    localStringBuilder.append(" /*--hide--*/");
    return localStringBuilder.toString();
  }
  
  public Index addIndex(Session paramSession, String paramString1, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean, String paramString2)
  {
    throw DbException.getUnsupportedException("LINK");
  }
  
  public boolean lock(Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    return false;
  }
  
  public boolean isLockedExclusively()
  {
    return false;
  }
  
  public Index getScanIndex(Session paramSession)
  {
    return this.linkedIndex;
  }
  
  private void checkReadOnly()
  {
    if (this.readOnly) {
      throw DbException.get(90097);
    }
  }
  
  public void removeRow(Session paramSession, Row paramRow)
  {
    checkReadOnly();
    getScanIndex(paramSession).remove(paramSession, paramRow);
  }
  
  public void addRow(Session paramSession, Row paramRow)
  {
    checkReadOnly();
    getScanIndex(paramSession).add(paramSession, paramRow);
  }
  
  public void close(Session paramSession)
  {
    if (this.conn != null) {
      try
      {
        this.conn.close(false);
      }
      finally
      {
        this.conn = null;
      }
    }
  }
  
  public synchronized long getRowCount(Session paramSession)
  {
    String str = "SELECT COUNT(*) FROM " + this.qualifiedTableName;
    try
    {
      PreparedStatement localPreparedStatement = execute(str, null, false);
      ResultSet localResultSet = localPreparedStatement.getResultSet();
      localResultSet.next();
      long l = localResultSet.getLong(1);
      localResultSet.close();
      reusePreparedStatement(localPreparedStatement, str);
      return l;
    }
    catch (Exception localException)
    {
      throw wrapException(str, localException);
    }
  }
  
  public static DbException wrapException(String paramString, Exception paramException)
  {
    SQLException localSQLException = DbException.toSQLException(paramException);
    return DbException.get(90111, localSQLException, new String[] { paramString, localSQLException.toString() });
  }
  
  public String getQualifiedTable()
  {
    return this.qualifiedTableName;
  }
  
  public PreparedStatement execute(String paramString, ArrayList<Value> paramArrayList, boolean paramBoolean)
  {
    if (this.conn == null) {
      throw this.connectException;
    }
    int i = 0;
    for (;;)
    {
      try
      {
        synchronized (this.conn)
        {
          PreparedStatement localPreparedStatement = (PreparedStatement)this.preparedMap.remove(paramString);
          if (localPreparedStatement == null) {
            localPreparedStatement = this.conn.getConnection().prepareStatement(paramString);
          }
          int k;
          Object localObject1;
          if (this.trace.isDebugEnabled())
          {
            StatementBuilder localStatementBuilder = new StatementBuilder();
            localStatementBuilder.append(getName()).append(":\n").append(paramString);
            if ((paramArrayList != null) && (paramArrayList.size() > 0))
            {
              localStatementBuilder.append(" {");
              k = 1;
              localObject1 = paramArrayList.iterator();
              if (((Iterator)localObject1).hasNext())
              {
                Value localValue = (Value)((Iterator)localObject1).next();
                localStatementBuilder.appendExceptFirst(", ");
                localStatementBuilder.append(k++).append(": ").append(localValue.getSQL()); continue;
              }
              localStatementBuilder.append('}');
            }
            localStatementBuilder.append(';');
            this.trace.debug(localStatementBuilder.toString());
          }
          if (paramArrayList != null)
          {
            int j = 0;k = paramArrayList.size();
            if (j < k)
            {
              localObject1 = (Value)paramArrayList.get(j);
              ((Value)localObject1).set(localPreparedStatement, j + 1);j++; continue;
            }
          }
          localPreparedStatement.execute();
          if (paramBoolean)
          {
            reusePreparedStatement(localPreparedStatement, paramString);
            return null;
          }
          return localPreparedStatement;
        }
        i++;
      }
      catch (SQLException localSQLException)
      {
        if (i >= 2) {
          throw DbException.convert(localSQLException);
        }
        this.conn.close(true);
        connect();
      }
    }
  }
  
  public void unlock(Session paramSession) {}
  
  public void checkRename() {}
  
  public void checkSupportAlter()
  {
    throw DbException.getUnsupportedException("LINK");
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("LINK");
  }
  
  public boolean canGetRowCount()
  {
    return true;
  }
  
  public boolean canDrop()
  {
    return true;
  }
  
  public String getTableType()
  {
    return "TABLE LINK";
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    super.removeChildrenAndResources(paramSession);
    close(paramSession);
    this.database.removeMeta(paramSession, getId());
    this.driver = null;
    this.url = (this.user = this.password = this.originalTable = null);
    this.preparedMap = null;
    invalidate();
  }
  
  public boolean isOracle()
  {
    return this.url.startsWith("jdbc:oracle:");
  }
  
  public ArrayList<Index> getIndexes()
  {
    return this.indexes;
  }
  
  public long getMaxDataModificationId()
  {
    return Long.MAX_VALUE;
  }
  
  public Index getUniqueIndex()
  {
    for (Index localIndex : this.indexes) {
      if (localIndex.getIndexType().isUnique()) {
        return localIndex;
      }
    }
    return null;
  }
  
  public void updateRows(Prepared paramPrepared, Session paramSession, RowList paramRowList)
  {
    checkReadOnly();
    int i;
    if (this.emitUpdates)
    {
      for (paramRowList.reset(); paramRowList.hasNext();)
      {
        paramPrepared.checkCanceled();
        Row localRow1 = paramRowList.next();
        Row localRow2 = paramRowList.next();
        this.linkedIndex.update(localRow1, localRow2);
        paramSession.log(this, (short)1, localRow1);
        paramSession.log(this, (short)0, localRow2);
      }
      i = 0;
    }
    else
    {
      i = 1;
    }
    if (i != 0) {
      super.updateRows(paramPrepared, paramSession, paramRowList);
    }
  }
  
  public void setGlobalTemporary(boolean paramBoolean)
  {
    this.globalTemporary = paramBoolean;
  }
  
  public void setReadOnly(boolean paramBoolean)
  {
    this.readOnly = paramBoolean;
  }
  
  public long getRowCountApproximation()
  {
    return 100000L;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public void reusePreparedStatement(PreparedStatement paramPreparedStatement, String paramString)
  {
    synchronized (this.conn)
    {
      this.preparedMap.put(paramString, paramPreparedStatement);
    }
  }
  
  public boolean isDeterministic()
  {
    return false;
  }
  
  public void checkWritingAllowed() {}
  
  public void validateConvertUpdateSequence(Session paramSession, Row paramRow)
  {
    for (int i = 0; i < this.columns.length; i++)
    {
      Value localValue1 = paramRow.getValue(i);
      if (localValue1 != null)
      {
        Column localColumn = this.columns[i];
        Value localValue2 = localColumn.validateConvertUpdateSequence(paramSession, localValue1);
        if (localValue2 != localValue1) {
          paramRow.setValue(i, localValue2);
        }
      }
    }
  }
  
  public Value getDefaultValue(Session paramSession, Column paramColumn)
  {
    return null;
  }
}
