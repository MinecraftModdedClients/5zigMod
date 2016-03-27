package org.h2.command.ddl;

import java.util.ArrayList;
import java.util.HashSet;
import org.h2.constraint.Constraint;
import org.h2.constraint.ConstraintCheck;
import org.h2.constraint.ConstraintReferential;
import org.h2.constraint.ConstraintUnique;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;

public class AlterTableAddConstraint
  extends SchemaCommand
{
  private int type;
  private String constraintName;
  private String tableName;
  private IndexColumn[] indexColumns;
  private int deleteAction;
  private int updateAction;
  private Schema refSchema;
  private String refTableName;
  private IndexColumn[] refIndexColumns;
  private Expression checkExpression;
  private Index index;
  private Index refIndex;
  private String comment;
  private boolean checkExisting;
  private boolean primaryKeyHash;
  private final boolean ifNotExists;
  private ArrayList<Index> createdIndexes = New.arrayList();
  
  public AlterTableAddConstraint(Session paramSession, Schema paramSchema, boolean paramBoolean)
  {
    super(paramSession, paramSchema);
    this.ifNotExists = paramBoolean;
  }
  
  private String generateConstraintName(Table paramTable)
  {
    if (this.constraintName == null) {
      this.constraintName = getSchema().getUniqueConstraintName(this.session, paramTable);
    }
    return this.constraintName;
  }
  
  public int update()
  {
    try
    {
      return tryUpdate();
    }
    catch (DbException localDbException)
    {
      for (Index localIndex : this.createdIndexes) {
        this.session.getDatabase().removeSchemaObject(this.session, localIndex);
      }
      throw localDbException;
    }
    finally
    {
      getSchema().freeUniqueName(this.constraintName);
    }
  }
  
  private int tryUpdate()
  {
    if (!this.transactional) {
      this.session.commit(true);
    }
    Database localDatabase = this.session.getDatabase();
    Table localTable1 = getSchema().getTableOrView(this.session, this.tableName);
    if (getSchema().findConstraint(this.session, this.constraintName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(90045, this.constraintName);
    }
    this.session.getUser().checkRight(localTable1, 15);
    localDatabase.lockMeta(this.session);
    localTable1.lock(this.session, true, true);
    Object localObject3;
    int k;
    Object localObject4;
    Object localObject1;
    switch (this.type)
    {
    case 6: 
      IndexColumn.mapColumns(this.indexColumns, localTable1);
      this.index = localTable1.findPrimaryKey();
      ArrayList localArrayList = localTable1.getConstraints();
      for (int j = 0; (localArrayList != null) && (j < localArrayList.size()); j++)
      {
        Constraint localConstraint = (Constraint)localArrayList.get(j);
        if ("PRIMARY KEY".equals(localConstraint.getConstraintType())) {
          throw DbException.get(90017);
        }
      }
      Object localObject2;
      if (this.index != null)
      {
        localObject2 = this.index.getIndexColumns();
        if (localObject2.length != this.indexColumns.length) {
          throw DbException.get(90017);
        }
        for (int m = 0; m < localObject2.length; m++) {
          if (localObject2[m].column != this.indexColumns[m].column) {
            throw DbException.get(90017);
          }
        }
      }
      if (this.index == null)
      {
        localObject2 = IndexType.createPrimaryKey(localTable1.isPersistIndexes(), this.primaryKeyHash);
        
        localObject3 = localTable1.getSchema().getUniqueIndexName(this.session, localTable1, "PRIMARY_KEY_");
        
        int n = getObjectId();
        try
        {
          this.index = localTable1.addIndex(this.session, (String)localObject3, n, this.indexColumns, (IndexType)localObject2, true, null);
        }
        finally
        {
          getSchema().freeUniqueName((String)localObject3);
        }
      }
      this.index.getIndexType().setBelongsToConstraint(true);
      k = getObjectId();
      localObject3 = generateConstraintName(localTable1);
      localObject4 = new ConstraintUnique(getSchema(), k, (String)localObject3, localTable1, true);
      
      ((ConstraintUnique)localObject4).setColumns(this.indexColumns);
      ((ConstraintUnique)localObject4).setIndex(this.index, true);
      localObject1 = localObject4;
      break;
    case 4: 
      IndexColumn.mapColumns(this.indexColumns, localTable1);
      boolean bool1 = false;
      if ((this.index != null) && (canUseUniqueIndex(this.index, localTable1, this.indexColumns)))
      {
        bool1 = true;
        this.index.getIndexType().setBelongsToConstraint(true);
      }
      else
      {
        this.index = getUniqueIndex(localTable1, this.indexColumns);
        if (this.index == null)
        {
          this.index = createIndex(localTable1, this.indexColumns, true);
          bool1 = true;
        }
      }
      k = getObjectId();
      localObject3 = generateConstraintName(localTable1);
      localObject4 = new ConstraintUnique(getSchema(), k, (String)localObject3, localTable1, false);
      
      ((ConstraintUnique)localObject4).setColumns(this.indexColumns);
      ((ConstraintUnique)localObject4).setIndex(this.index, bool1);
      localObject1 = localObject4;
      break;
    case 3: 
      int i = getObjectId();
      String str1 = generateConstraintName(localTable1);
      localObject3 = new ConstraintCheck(getSchema(), i, str1, localTable1);
      localObject4 = new TableFilter(this.session, localTable1, null, false, null);
      this.checkExpression.mapColumns((ColumnResolver)localObject4, 0);
      this.checkExpression = this.checkExpression.optimize(this.session);
      ((ConstraintCheck)localObject3).setExpression(this.checkExpression);
      ((ConstraintCheck)localObject3).setTableFilter((TableFilter)localObject4);
      localObject1 = localObject3;
      if (this.checkExisting) {
        ((ConstraintCheck)localObject3).checkExistingData(this.session);
      }
      break;
    case 5: 
      Table localTable2 = this.refSchema.getTableOrView(this.session, this.refTableName);
      this.session.getUser().checkRight(localTable2, 15);
      if (!localTable2.canReference()) {
        throw DbException.getUnsupportedException("Reference " + localTable2.getSQL());
      }
      boolean bool2 = false;
      IndexColumn.mapColumns(this.indexColumns, localTable1);
      if ((this.index != null) && (canUseIndex(this.index, localTable1, this.indexColumns, false)))
      {
        bool2 = true;
        this.index.getIndexType().setBelongsToConstraint(true);
      }
      else
      {
        if (localDatabase.isStarting()) {
          this.index = getIndex(localTable1, this.indexColumns, true);
        } else {
          this.index = getIndex(localTable1, this.indexColumns, false);
        }
        if (this.index == null)
        {
          this.index = createIndex(localTable1, this.indexColumns, false);
          bool2 = true;
        }
      }
      if (this.refIndexColumns == null)
      {
        localObject3 = localTable2.getPrimaryKey();
        this.refIndexColumns = ((Index)localObject3).getIndexColumns();
      }
      else
      {
        IndexColumn.mapColumns(this.refIndexColumns, localTable2);
      }
      if (this.refIndexColumns.length != this.indexColumns.length) {
        throw DbException.get(21002);
      }
      boolean bool3 = false;
      if ((this.refIndex != null) && (this.refIndex.getTable() == localTable2) && (canUseIndex(this.refIndex, localTable2, this.refIndexColumns, false)))
      {
        bool3 = true;
        this.refIndex.getIndexType().setBelongsToConstraint(true);
      }
      else
      {
        this.refIndex = null;
      }
      if (this.refIndex == null)
      {
        this.refIndex = getIndex(localTable2, this.refIndexColumns, false);
        if (this.refIndex == null)
        {
          this.refIndex = createIndex(localTable2, this.refIndexColumns, true);
          bool3 = true;
        }
      }
      int i1 = getObjectId();
      String str2 = generateConstraintName(localTable1);
      ConstraintReferential localConstraintReferential = new ConstraintReferential(getSchema(), i1, str2, localTable1);
      
      localConstraintReferential.setColumns(this.indexColumns);
      localConstraintReferential.setIndex(this.index, bool2);
      localConstraintReferential.setRefTable(localTable2);
      localConstraintReferential.setRefColumns(this.refIndexColumns);
      localConstraintReferential.setRefIndex(this.refIndex, bool3);
      if (this.checkExisting) {
        localConstraintReferential.checkExistingData(this.session);
      }
      localObject1 = localConstraintReferential;
      localTable2.addConstraint((Constraint)localObject1);
      localConstraintReferential.setDeleteAction(this.deleteAction);
      localConstraintReferential.setUpdateAction(this.updateAction);
      break;
    default: 
      throw DbException.throwInternalError("type=" + this.type);
    }
    ((Constraint)localObject1).setComment(this.comment);
    if ((localTable1.isTemporary()) && (!localTable1.isGlobalTemporary())) {
      this.session.addLocalTempTableConstraint((Constraint)localObject1);
    } else {
      localDatabase.addSchemaObject(this.session, (SchemaObject)localObject1);
    }
    localTable1.addConstraint((Constraint)localObject1);
    return 0;
  }
  
  private Index createIndex(Table paramTable, IndexColumn[] paramArrayOfIndexColumn, boolean paramBoolean)
  {
    int i = getObjectId();
    IndexType localIndexType;
    if (paramBoolean) {
      localIndexType = IndexType.createUnique(paramTable.isPersistIndexes(), false);
    } else {
      localIndexType = IndexType.createNonUnique(paramTable.isPersistIndexes());
    }
    localIndexType.setBelongsToConstraint(true);
    String str1 = this.constraintName == null ? "CONSTRAINT" : this.constraintName;
    String str2 = paramTable.getSchema().getUniqueIndexName(this.session, paramTable, str1 + "_INDEX_");
    try
    {
      Index localIndex1 = paramTable.addIndex(this.session, str2, i, paramArrayOfIndexColumn, localIndexType, true, null);
      
      this.createdIndexes.add(localIndex1);
      return localIndex1;
    }
    finally
    {
      getSchema().freeUniqueName(str2);
    }
  }
  
  public void setDeleteAction(int paramInt)
  {
    this.deleteAction = paramInt;
  }
  
  public void setUpdateAction(int paramInt)
  {
    this.updateAction = paramInt;
  }
  
  private static Index getUniqueIndex(Table paramTable, IndexColumn[] paramArrayOfIndexColumn)
  {
    for (Index localIndex : paramTable.getIndexes()) {
      if (canUseUniqueIndex(localIndex, paramTable, paramArrayOfIndexColumn)) {
        return localIndex;
      }
    }
    return null;
  }
  
  private static Index getIndex(Table paramTable, IndexColumn[] paramArrayOfIndexColumn, boolean paramBoolean)
  {
    for (Index localIndex : paramTable.getIndexes()) {
      if (canUseIndex(localIndex, paramTable, paramArrayOfIndexColumn, paramBoolean)) {
        return localIndex;
      }
    }
    return null;
  }
  
  private static boolean canUseUniqueIndex(Index paramIndex, Table paramTable, IndexColumn[] paramArrayOfIndexColumn)
  {
    if ((paramIndex.getTable() != paramTable) || (!paramIndex.getIndexType().isUnique())) {
      return false;
    }
    Column[] arrayOfColumn = paramIndex.getColumns();
    if (arrayOfColumn.length > paramArrayOfIndexColumn.length) {
      return false;
    }
    HashSet localHashSet = New.hashSet();
    Object localObject2;
    for (localObject2 : paramArrayOfIndexColumn) {
      localHashSet.add(((IndexColumn)localObject2).column);
    }
    for (localObject2 : arrayOfColumn) {
      if (!localHashSet.contains(localObject2)) {
        return false;
      }
    }
    return true;
  }
  
  private static boolean canUseIndex(Index paramIndex, Table paramTable, IndexColumn[] paramArrayOfIndexColumn, boolean paramBoolean)
  {
    if ((paramIndex.getTable() != paramTable) || (paramIndex.getCreateSQL() == null)) {
      return false;
    }
    Column[] arrayOfColumn = paramIndex.getColumns();
    IndexColumn localIndexColumn;
    int k;
    if (paramBoolean)
    {
      if (arrayOfColumn.length < paramArrayOfIndexColumn.length) {
        return false;
      }
      for (localIndexColumn : paramArrayOfIndexColumn)
      {
        k = paramIndex.getColumnIndex(localIndexColumn.column);
        if ((k < 0) || (k >= paramArrayOfIndexColumn.length)) {
          return false;
        }
      }
    }
    else
    {
      if (arrayOfColumn.length != paramArrayOfIndexColumn.length) {
        return false;
      }
      for (localIndexColumn : paramArrayOfIndexColumn)
      {
        k = paramIndex.getColumnIndex(localIndexColumn.column);
        if (k < 0) {
          return false;
        }
      }
    }
    return true;
  }
  
  public void setConstraintName(String paramString)
  {
    this.constraintName = paramString;
  }
  
  public void setType(int paramInt)
  {
    this.type = paramInt;
  }
  
  public int getType()
  {
    return this.type;
  }
  
  public void setCheckExpression(Expression paramExpression)
  {
    this.checkExpression = paramExpression;
  }
  
  public void setTableName(String paramString)
  {
    this.tableName = paramString;
  }
  
  public void setIndexColumns(IndexColumn[] paramArrayOfIndexColumn)
  {
    this.indexColumns = paramArrayOfIndexColumn;
  }
  
  public IndexColumn[] getIndexColumns()
  {
    return this.indexColumns;
  }
  
  public void setRefTableName(Schema paramSchema, String paramString)
  {
    this.refSchema = paramSchema;
    this.refTableName = paramString;
  }
  
  public void setRefIndexColumns(IndexColumn[] paramArrayOfIndexColumn)
  {
    this.refIndexColumns = paramArrayOfIndexColumn;
  }
  
  public void setIndex(Index paramIndex)
  {
    this.index = paramIndex;
  }
  
  public void setRefIndex(Index paramIndex)
  {
    this.refIndex = paramIndex;
  }
  
  public void setComment(String paramString)
  {
    this.comment = paramString;
  }
  
  public void setCheckExisting(boolean paramBoolean)
  {
    this.checkExisting = paramBoolean;
  }
  
  public void setPrimaryKeyHash(boolean paramBoolean)
  {
    this.primaryKeyHash = paramBoolean;
  }
}
