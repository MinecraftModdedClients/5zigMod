package org.h2.command.ddl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.h2.command.dml.Insert;
import org.h2.command.dml.Query;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.schema.Sequence;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.util.New;
import org.h2.value.DataType;

public class CreateTable
  extends SchemaCommand
{
  private final CreateTableData data = new CreateTableData();
  private final ArrayList<DefineCommand> constraintCommands = New.arrayList();
  private IndexColumn[] pkColumns;
  private boolean ifNotExists;
  private boolean onCommitDrop;
  private boolean onCommitTruncate;
  private Query asQuery;
  private String comment;
  private boolean sortedInsertMode;
  
  public CreateTable(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
    this.data.persistIndexes = true;
    this.data.persistData = true;
  }
  
  public void setQuery(Query paramQuery)
  {
    this.asQuery = paramQuery;
  }
  
  public void setTemporary(boolean paramBoolean)
  {
    this.data.temporary = paramBoolean;
  }
  
  public void setTableName(String paramString)
  {
    this.data.tableName = paramString;
  }
  
  public void addColumn(Column paramColumn)
  {
    this.data.columns.add(paramColumn);
  }
  
  public void addConstraintCommand(DefineCommand paramDefineCommand)
  {
    if ((paramDefineCommand instanceof CreateIndex))
    {
      this.constraintCommands.add(paramDefineCommand);
    }
    else
    {
      AlterTableAddConstraint localAlterTableAddConstraint = (AlterTableAddConstraint)paramDefineCommand;
      boolean bool;
      if (localAlterTableAddConstraint.getType() == 6) {
        bool = setPrimaryKeyColumns(localAlterTableAddConstraint.getIndexColumns());
      } else {
        bool = false;
      }
      if (!bool) {
        this.constraintCommands.add(paramDefineCommand);
      }
    }
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public int update()
  {
    if (!this.transactional) {
      this.session.commit(true);
    }
    Database localDatabase = this.session.getDatabase();
    if (!localDatabase.isPersistent()) {
      this.data.persistIndexes = false;
    }
    if (getSchema().findTableOrView(this.session, this.data.tableName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(42101, this.data.tableName);
    }
    if (this.asQuery != null)
    {
      this.asQuery.prepare();
      if (this.data.columns.size() == 0) {
        generateColumnsFromQuery();
      } else if (this.data.columns.size() != this.asQuery.getColumnCount()) {
        throw DbException.get(21002);
      }
    }
    Iterator localIterator1;
    if (this.pkColumns != null) {
      for (localIterator1 = this.data.columns.iterator(); localIterator1.hasNext();)
      {
        localObject1 = (Column)localIterator1.next();
        for (Object localObject4 : this.pkColumns) {
          if (((Column)localObject1).getName().equals(((IndexColumn)localObject4).columnName)) {
            ((Column)localObject1).setNullable(false);
          }
        }
      }
    }
    this.data.id = getObjectId();
    this.data.create = this.create;
    this.data.session = this.session;
    int i = (this.data.temporary) && (!this.data.globalTemporary) ? 1 : 0;
    if (i == 0) {
      localDatabase.lockMeta(this.session);
    }
    Object localObject1 = getSchema().createTable(this.data);
    ??? = New.arrayList();
    for (Iterator localIterator2 = this.data.columns.iterator(); localIterator2.hasNext();)
    {
      localObject3 = (Column)localIterator2.next();
      if (((Column)localObject3).isAutoIncrement())
      {
        int m = getObjectId();
        ((Column)localObject3).convertAutoIncrementToSequence(this.session, getSchema(), m, this.data.temporary);
      }
      localObject5 = ((Column)localObject3).getSequence();
      if (localObject5 != null) {
        ((ArrayList)???).add(localObject5);
      }
    }
    Object localObject3;
    Object localObject5;
    ((Table)localObject1).setComment(this.comment);
    if (i != 0)
    {
      if (this.onCommitDrop) {
        ((Table)localObject1).setOnCommitDrop(true);
      }
      if (this.onCommitTruncate) {
        ((Table)localObject1).setOnCommitTruncate(true);
      }
      this.session.addLocalTempTable((Table)localObject1);
    }
    else
    {
      localDatabase.lockMeta(this.session);
      localDatabase.addSchemaObject(this.session, (SchemaObject)localObject1);
    }
    try
    {
      for (localIterator2 = this.data.columns.iterator(); localIterator2.hasNext();)
      {
        localObject3 = (Column)localIterator2.next();
        ((Column)localObject3).prepareExpression(this.session);
      }
      for (localIterator2 = ((ArrayList)???).iterator(); localIterator2.hasNext();)
      {
        localObject3 = (Sequence)localIterator2.next();
        ((Table)localObject1).addSequence((Sequence)localObject3);
      }
      for (localIterator2 = this.constraintCommands.iterator(); localIterator2.hasNext();)
      {
        localObject3 = (DefineCommand)localIterator2.next();
        ((DefineCommand)localObject3).setTransactional(this.transactional);
        ((DefineCommand)localObject3).update();
      }
      if (this.asQuery != null)
      {
        boolean bool = this.session.isUndoLogEnabled();
        try
        {
          this.session.setUndoLogEnabled(false);
          this.session.startStatementWithinTransaction();
          localObject3 = null;
          localObject3 = new Insert(this.session);
          ((Insert)localObject3).setSortedInsertMode(this.sortedInsertMode);
          ((Insert)localObject3).setQuery(this.asQuery);
          ((Insert)localObject3).setTable((Table)localObject1);
          ((Insert)localObject3).setInsertFromSelect(true);
          ((Insert)localObject3).prepare();
          ((Insert)localObject3).update();
        }
        finally
        {
          this.session.setUndoLogEnabled(bool);
        }
      }
      HashSet localHashSet = New.hashSet();
      localHashSet.clear();
      ((Table)localObject1).addDependencies(localHashSet);
      for (localObject3 = localHashSet.iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject5 = (DbObject)((Iterator)localObject3).next();
        if (localObject5 != localObject1) {
          if ((((DbObject)localObject5).getType() == 0) && 
            ((localObject5 instanceof Table)))
          {
            Table localTable = (Table)localObject5;
            if (localTable.getId() > ((Table)localObject1).getId()) {
              throw DbException.get(50100, "Table depends on another table with a higher ID: " + localTable + ", this is currently not supported, " + "as it would prevent the database from " + "being re-opened");
            }
          }
        }
      }
    }
    catch (DbException localDbException)
    {
      localDatabase.checkPowerOff();
      localDatabase.removeSchemaObject(this.session, (SchemaObject)localObject1);
      if (!this.transactional) {
        this.session.commit(true);
      }
      throw localDbException;
    }
    return 0;
  }
  
  private void generateColumnsFromQuery()
  {
    int i = this.asQuery.getColumnCount();
    ArrayList localArrayList = this.asQuery.getExpressions();
    for (int j = 0; j < i; j++)
    {
      Expression localExpression = (Expression)localArrayList.get(j);
      int k = localExpression.getType();
      String str = localExpression.getAlias();
      long l = localExpression.getPrecision();
      int m = localExpression.getDisplaySize();
      DataType localDataType = DataType.getDataType(k);
      if ((l > 0L) && ((localDataType.defaultPrecision == 0L) || ((localDataType.defaultPrecision > l) && (localDataType.defaultPrecision < 127L)))) {
        l = localDataType.defaultPrecision;
      }
      int n = localExpression.getScale();
      if ((n > 0) && ((localDataType.defaultScale == 0) || ((localDataType.defaultScale > n) && (localDataType.defaultScale < l)))) {
        n = localDataType.defaultScale;
      }
      if (n > l) {
        l = n;
      }
      Column localColumn = new Column(str, k, l, n, m);
      addColumn(localColumn);
    }
  }
  
  private boolean setPrimaryKeyColumns(IndexColumn[] paramArrayOfIndexColumn)
  {
    if (this.pkColumns != null)
    {
      int i = paramArrayOfIndexColumn.length;
      if (i != this.pkColumns.length) {
        throw DbException.get(90017);
      }
      for (int j = 0; j < i; j++) {
        if (!paramArrayOfIndexColumn[j].columnName.equals(this.pkColumns[j].columnName)) {
          throw DbException.get(90017);
        }
      }
      return true;
    }
    this.pkColumns = paramArrayOfIndexColumn;
    return false;
  }
  
  public void setPersistIndexes(boolean paramBoolean)
  {
    this.data.persistIndexes = paramBoolean;
  }
  
  public void setGlobalTemporary(boolean paramBoolean)
  {
    this.data.globalTemporary = paramBoolean;
  }
  
  public void setOnCommitDrop()
  {
    this.onCommitDrop = true;
  }
  
  public void setOnCommitTruncate()
  {
    this.onCommitTruncate = true;
  }
  
  public void setComment(String paramString)
  {
    this.comment = paramString;
  }
  
  public void setPersistData(boolean paramBoolean)
  {
    this.data.persistData = paramBoolean;
    if (!paramBoolean) {
      this.data.persistIndexes = false;
    }
  }
  
  public void setSortedInsertMode(boolean paramBoolean)
  {
    this.sortedInsertMode = paramBoolean;
  }
  
  public void setTableEngine(String paramString)
  {
    this.data.tableEngine = paramString;
  }
  
  public void setTableEngineParams(ArrayList<String> paramArrayList)
  {
    this.data.tableEngineParams = paramArrayList;
  }
  
  public void setHidden(boolean paramBoolean)
  {
    this.data.isHidden = paramBoolean;
  }
  
  public int getType()
  {
    return 30;
  }
}
