package org.h2.result;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class UpdatableRow
{
  private final JdbcConnection conn;
  private final ResultInterface result;
  private final int columnCount;
  private String schemaName;
  private String tableName;
  private ArrayList<String> key;
  private boolean isUpdatable;
  
  public UpdatableRow(JdbcConnection paramJdbcConnection, ResultInterface paramResultInterface)
    throws SQLException
  {
    this.conn = paramJdbcConnection;
    this.result = paramResultInterface;
    this.columnCount = paramResultInterface.getVisibleColumnCount();
    for (int i = 0; i < this.columnCount; i++)
    {
      localObject = paramResultInterface.getTableName(i);
      str1 = paramResultInterface.getSchemaName(i);
      if ((localObject == null) || (str1 == null)) {
        return;
      }
      if (this.tableName == null) {
        this.tableName = ((String)localObject);
      } else if (!this.tableName.equals(localObject)) {
        return;
      }
      if (this.schemaName == null) {
        this.schemaName = str1;
      } else if (!this.schemaName.equals(str1)) {
        return;
      }
    }
    DatabaseMetaData localDatabaseMetaData = paramJdbcConnection.getMetaData();
    Object localObject = localDatabaseMetaData.getTables(null, StringUtils.escapeMetaDataPattern(this.schemaName), StringUtils.escapeMetaDataPattern(this.tableName), new String[] { "TABLE" });
    if (!((ResultSet)localObject).next()) {
      return;
    }
    if (((ResultSet)localObject).getString("SQL") == null) {
      return;
    }
    String str1 = ((ResultSet)localObject).getString("TABLE_NAME");
    
    int j = (!str1.equals(this.tableName)) && (str1.equalsIgnoreCase(this.tableName)) ? 1 : 0;
    this.key = New.arrayList();
    localObject = localDatabaseMetaData.getPrimaryKeys(null, StringUtils.escapeMetaDataPattern(this.schemaName), this.tableName);
    while (((ResultSet)localObject).next())
    {
      String str2 = ((ResultSet)localObject).getString("COLUMN_NAME");
      this.key.add(j != 0 ? StringUtils.toUpperEnglish(str2) : str2);
    }
    if (isIndexUsable(this.key))
    {
      this.isUpdatable = true;
      return;
    }
    this.key.clear();
    localObject = localDatabaseMetaData.getIndexInfo(null, StringUtils.escapeMetaDataPattern(this.schemaName), this.tableName, true, true);
    while (((ResultSet)localObject).next())
    {
      int k = ((ResultSet)localObject).getShort("ORDINAL_POSITION");
      if (k == 1)
      {
        if (isIndexUsable(this.key))
        {
          this.isUpdatable = true;
          return;
        }
        this.key.clear();
      }
      String str3 = ((ResultSet)localObject).getString("COLUMN_NAME");
      this.key.add(j != 0 ? StringUtils.toUpperEnglish(str3) : str3);
    }
    if (isIndexUsable(this.key))
    {
      this.isUpdatable = true;
      return;
    }
    this.key = null;
  }
  
  private boolean isIndexUsable(ArrayList<String> paramArrayList)
  {
    if (paramArrayList.size() == 0) {
      return false;
    }
    for (String str : paramArrayList) {
      if (findColumnIndex(str) < 0) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isUpdatable()
  {
    return this.isUpdatable;
  }
  
  private int findColumnIndex(String paramString)
  {
    for (int i = 0; i < this.columnCount; i++)
    {
      String str = this.result.getColumnName(i);
      if (str.equals(paramString)) {
        return i;
      }
    }
    return -1;
  }
  
  private int getColumnIndex(String paramString)
  {
    int i = findColumnIndex(paramString);
    if (i < 0) {
      throw DbException.get(42122, paramString);
    }
    return i;
  }
  
  private void appendColumnList(StatementBuilder paramStatementBuilder, boolean paramBoolean)
  {
    paramStatementBuilder.resetCount();
    for (int i = 0; i < this.columnCount; i++)
    {
      paramStatementBuilder.appendExceptFirst(",");
      String str = this.result.getColumnName(i);
      paramStatementBuilder.append(StringUtils.quoteIdentifier(str));
      if (paramBoolean) {
        paramStatementBuilder.append("=? ");
      }
    }
  }
  
  private void appendKeyCondition(StatementBuilder paramStatementBuilder)
  {
    paramStatementBuilder.append(" WHERE ");
    paramStatementBuilder.resetCount();
    for (String str : this.key)
    {
      paramStatementBuilder.appendExceptFirst(" AND ");
      paramStatementBuilder.append(StringUtils.quoteIdentifier(str)).append("=?");
    }
  }
  
  private void setKey(PreparedStatement paramPreparedStatement, int paramInt, Value[] paramArrayOfValue)
    throws SQLException
  {
    int i = 0;
    for (int j = this.key.size(); i < j; i++)
    {
      String str = (String)this.key.get(i);
      int k = getColumnIndex(str);
      Value localValue = paramArrayOfValue[k];
      if ((localValue == null) || (localValue == ValueNull.INSTANCE)) {
        throw DbException.get(2000);
      }
      localValue.set(paramPreparedStatement, paramInt + i);
    }
  }
  
  private void appendTableName(StatementBuilder paramStatementBuilder)
  {
    if ((this.schemaName != null) && (this.schemaName.length() > 0)) {
      paramStatementBuilder.append(StringUtils.quoteIdentifier(this.schemaName)).append('.');
    }
    paramStatementBuilder.append(StringUtils.quoteIdentifier(this.tableName));
  }
  
  public Value[] readRow(Value[] paramArrayOfValue)
    throws SQLException
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("SELECT ");
    appendColumnList(localStatementBuilder, false);
    localStatementBuilder.append(" FROM ");
    appendTableName(localStatementBuilder);
    appendKeyCondition(localStatementBuilder);
    PreparedStatement localPreparedStatement = this.conn.prepareStatement(localStatementBuilder.toString());
    setKey(localPreparedStatement, 1, paramArrayOfValue);
    ResultSet localResultSet = localPreparedStatement.executeQuery();
    if (!localResultSet.next()) {
      throw DbException.get(2000);
    }
    Value[] arrayOfValue = new Value[this.columnCount];
    for (int i = 0; i < this.columnCount; i++)
    {
      int j = this.result.getColumnType(i);
      arrayOfValue[i] = DataType.readValue(this.conn.getSession(), localResultSet, i + 1, j);
    }
    return arrayOfValue;
  }
  
  public void deleteRow(Value[] paramArrayOfValue)
    throws SQLException
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("DELETE FROM ");
    appendTableName(localStatementBuilder);
    appendKeyCondition(localStatementBuilder);
    PreparedStatement localPreparedStatement = this.conn.prepareStatement(localStatementBuilder.toString());
    setKey(localPreparedStatement, 1, paramArrayOfValue);
    int i = localPreparedStatement.executeUpdate();
    if (i != 1) {
      throw DbException.get(2000);
    }
  }
  
  public void updateRow(Value[] paramArrayOfValue1, Value[] paramArrayOfValue2)
    throws SQLException
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("UPDATE ");
    appendTableName(localStatementBuilder);
    localStatementBuilder.append(" SET ");
    appendColumnList(localStatementBuilder, true);
    
    appendKeyCondition(localStatementBuilder);
    PreparedStatement localPreparedStatement = this.conn.prepareStatement(localStatementBuilder.toString());
    int i = 1;
    for (int j = 0; j < this.columnCount; j++)
    {
      Value localValue = paramArrayOfValue2[j];
      if (localValue == null) {
        localValue = paramArrayOfValue1[j];
      }
      localValue.set(localPreparedStatement, i++);
    }
    setKey(localPreparedStatement, i, paramArrayOfValue1);
    j = localPreparedStatement.executeUpdate();
    if (j != 1) {
      throw DbException.get(2000);
    }
  }
  
  public void insertRow(Value[] paramArrayOfValue)
    throws SQLException
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("INSERT INTO ");
    appendTableName(localStatementBuilder);
    localStatementBuilder.append('(');
    appendColumnList(localStatementBuilder, false);
    localStatementBuilder.append(")VALUES(");
    localStatementBuilder.resetCount();
    for (int i = 0; i < this.columnCount; i++)
    {
      localStatementBuilder.appendExceptFirst(",");
      Value localValue1 = paramArrayOfValue[i];
      if (localValue1 == null) {
        localStatementBuilder.append("DEFAULT");
      } else {
        localStatementBuilder.append('?');
      }
    }
    localStatementBuilder.append(')');
    PreparedStatement localPreparedStatement = this.conn.prepareStatement(localStatementBuilder.toString());
    int j = 0;
    for (int k = 0; j < this.columnCount; j++)
    {
      Value localValue2 = paramArrayOfValue[j];
      if (localValue2 != null) {
        localValue2.set(localPreparedStatement, k++ + 1);
      }
    }
    j = localPreparedStatement.executeUpdate();
    if (j != 1) {
      throw DbException.get(2000);
    }
  }
}
