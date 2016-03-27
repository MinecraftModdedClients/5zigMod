package org.h2.command.ddl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.h2.command.Parser;
import org.h2.command.Prepared;
import org.h2.constraint.Constraint;
import org.h2.constraint.ConstraintReferential;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.schema.Sequence;
import org.h2.schema.TriggerObject;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.table.TableView;
import org.h2.util.New;
import org.h2.value.Value;

public class AlterTableAlterColumn
  extends SchemaCommand
{
  private Table table;
  private Column oldColumn;
  private Column newColumn;
  private int type;
  private Expression defaultExpression;
  private Expression newSelectivity;
  private String addBefore;
  private String addAfter;
  private boolean ifNotExists;
  private ArrayList<Column> columnsToAdd;
  
  public AlterTableAlterColumn(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setTable(Table paramTable)
  {
    this.table = paramTable;
  }
  
  public void setOldColumn(Column paramColumn)
  {
    this.oldColumn = paramColumn;
  }
  
  public void setAddBefore(String paramString)
  {
    this.addBefore = paramString;
  }
  
  public void setAddAfter(String paramString)
  {
    this.addAfter = paramString;
  }
  
  public int update()
  {
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    this.session.getUser().checkRight(this.table, 15);
    this.table.checkSupportAlter();
    this.table.lock(this.session, true, true);
    Sequence localSequence = this.oldColumn == null ? null : this.oldColumn.getSequence();
    if (this.newColumn != null) {
      checkDefaultReferencesTable(this.newColumn.getDefaultExpression());
    }
    Iterator localIterator;
    if (this.columnsToAdd != null) {
      for (localIterator = this.columnsToAdd.iterator(); localIterator.hasNext();)
      {
        localColumn = (Column)localIterator.next();
        checkDefaultReferencesTable(localColumn.getDefaultExpression());
      }
    }
    Column localColumn;
    switch (this.type)
    {
    case 8: 
      if (this.oldColumn.isNullable())
      {
        checkNoNullValues();
        this.oldColumn.setNullable(false);
        localDatabase.updateMeta(this.session, this.table);
      }
      break;
    case 9: 
      if (!this.oldColumn.isNullable())
      {
        checkNullable();
        this.oldColumn.setNullable(true);
        localDatabase.updateMeta(this.session, this.table);
      }
      break;
    case 10: 
      checkDefaultReferencesTable(this.defaultExpression);
      this.oldColumn.setSequence(null);
      this.oldColumn.setDefaultExpression(this.session, this.defaultExpression);
      removeSequence(localSequence);
      localDatabase.updateMeta(this.session, this.table);
      break;
    case 11: 
      if (this.oldColumn.isWideningConversion(this.newColumn))
      {
        convertAutoIncrementColumn(this.newColumn);
        this.oldColumn.copy(this.newColumn);
        localDatabase.updateMeta(this.session, this.table);
      }
      else
      {
        this.oldColumn.setSequence(null);
        this.oldColumn.setDefaultExpression(this.session, null);
        this.oldColumn.setConvertNullToDefault(false);
        if ((this.oldColumn.isNullable()) && (!this.newColumn.isNullable())) {
          checkNoNullValues();
        } else if ((!this.oldColumn.isNullable()) && (this.newColumn.isNullable())) {
          checkNullable();
        }
        convertAutoIncrementColumn(this.newColumn);
        copyData();
      }
      break;
    case 7: 
      if ((!this.ifNotExists) || (this.columnsToAdd.size() != 1) || (!this.table.doesColumnExist(((Column)this.columnsToAdd.get(0)).getName())))
      {
        for (localIterator = this.columnsToAdd.iterator(); localIterator.hasNext();)
        {
          localColumn = (Column)localIterator.next();
          if (localColumn.isAutoIncrement())
          {
            int j = getObjectId();
            localColumn.convertAutoIncrementToSequence(this.session, getSchema(), j, this.table.isTemporary());
          }
        }
        copyData();
      }
      break;
    case 12: 
      if (this.table.getColumns().length == 1) {
        throw DbException.get(90084, this.oldColumn.getSQL());
      }
      this.table.dropSingleColumnConstraintsAndIndexes(this.session, this.oldColumn);
      copyData();
      break;
    case 13: 
      int i = this.newSelectivity.optimize(this.session).getValue(this.session).getInt();
      this.oldColumn.setSelectivity(i);
      localDatabase.updateMeta(this.session, this.table);
      break;
    default: 
      DbException.throwInternalError("type=" + this.type);
    }
    return 0;
  }
  
  private void checkDefaultReferencesTable(Expression paramExpression)
  {
    if (paramExpression == null) {
      return;
    }
    HashSet localHashSet = New.hashSet();
    ExpressionVisitor localExpressionVisitor = ExpressionVisitor.getDependenciesVisitor(localHashSet);
    
    paramExpression.isEverything(localExpressionVisitor);
    if (localHashSet.contains(this.table)) {
      throw DbException.get(90083, paramExpression.getSQL());
    }
  }
  
  private void convertAutoIncrementColumn(Column paramColumn)
  {
    if (paramColumn.isAutoIncrement()) {
      if (paramColumn.isPrimaryKey())
      {
        paramColumn.setOriginalSQL("IDENTITY");
      }
      else
      {
        int i = getObjectId();
        paramColumn.convertAutoIncrementToSequence(this.session, getSchema(), i, this.table.isTemporary());
      }
    }
  }
  
  private void removeSequence(Sequence paramSequence)
  {
    if (paramSequence != null)
    {
      this.table.removeSequence(paramSequence);
      paramSequence.setBelongsToTable(false);
      Database localDatabase = this.session.getDatabase();
      localDatabase.removeSchemaObject(this.session, paramSequence);
    }
  }
  
  private void copyData()
  {
    if (this.table.isTemporary()) {
      throw DbException.getUnsupportedException("TEMP TABLE");
    }
    Database localDatabase = this.session.getDatabase();
    String str1 = this.table.getName();
    String str2 = localDatabase.getTempTableName(str1, this.session);
    Column[] arrayOfColumn = this.table.getColumns();
    ArrayList localArrayList1 = New.arrayList();
    Table localTable = cloneTableStructure(arrayOfColumn, localDatabase, str2, localArrayList1);
    try
    {
      checkViews(this.table, localTable);
    }
    catch (DbException localDbException)
    {
      execute("DROP TABLE " + localTable.getName(), true);
      throw DbException.get(90109, localDbException, new String[] { getSQL(), localDbException.getMessage() });
    }
    String str3 = this.table.getName();
    ArrayList localArrayList2 = this.table.getViews();
    if (localArrayList2 != null)
    {
      localArrayList2 = New.arrayList(localArrayList2);
      for (localIterator = localArrayList2.iterator(); localIterator.hasNext();)
      {
        localObject = (TableView)localIterator.next();
        this.table.removeView((TableView)localObject);
      }
    }
    Object localObject;
    execute("DROP TABLE " + this.table.getSQL() + " IGNORE", true);
    localDatabase.renameSchemaObject(this.session, localTable, str3);
    for (Iterator localIterator = localTable.getChildren().iterator(); localIterator.hasNext();)
    {
      localObject = (DbObject)localIterator.next();
      if (!(localObject instanceof Sequence))
      {
        str4 = ((DbObject)localObject).getName();
        if ((str4 != null) && (((DbObject)localObject).getCreateSQL() != null)) {
          if (str4.startsWith(str2 + "_"))
          {
            str4 = str4.substring(str2.length() + 1);
            SchemaObject localSchemaObject = (SchemaObject)localObject;
            if ((localSchemaObject instanceof Constraint))
            {
              if (localSchemaObject.getSchema().findConstraint(this.session, str4) != null) {
                str4 = localSchemaObject.getSchema().getUniqueConstraintName(this.session, localTable);
              }
            }
            else if (((localSchemaObject instanceof Index)) && 
              (localSchemaObject.getSchema().findIndex(this.session, str4) != null)) {
              str4 = localSchemaObject.getSchema().getUniqueIndexName(this.session, localTable, str4);
            }
            localDatabase.renameSchemaObject(this.session, localSchemaObject, str4);
          }
        }
      }
    }
    String str4;
    if (localArrayList2 != null) {
      for (localIterator = localArrayList2.iterator(); localIterator.hasNext();)
      {
        localObject = (TableView)localIterator.next();
        str4 = ((TableView)localObject).getCreateSQL(true, true);
        execute(str4, true);
      }
    }
  }
  
  private Table cloneTableStructure(Column[] paramArrayOfColumn, Database paramDatabase, String paramString, ArrayList<Column> paramArrayList)
  {
    for (localObject3 : paramArrayOfColumn) {
      paramArrayList.add(((Column)localObject3).getClone());
    }
    if (this.type == 12)
    {
      i = this.oldColumn.getColumnId();
      paramArrayList.remove(i);
    }
    else if (this.type == 7)
    {
      if (this.addBefore != null) {
        i = this.table.getColumn(this.addBefore).getColumnId();
      } else if (this.addAfter != null) {
        i = this.table.getColumn(this.addAfter).getColumnId() + 1;
      } else {
        i = paramArrayOfColumn.length;
      }
      for (localObject1 = this.columnsToAdd.iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (Column)((Iterator)localObject1).next();
        paramArrayList.add(i++, localObject2);
      }
    }
    else if (this.type == 11)
    {
      i = this.oldColumn.getColumnId();
      paramArrayList.remove(i);
      paramArrayList.add(i, this.newColumn);
    }
    int i = paramDatabase.allocateObjectId();
    Object localObject1 = new CreateTableData();
    ((CreateTableData)localObject1).tableName = paramString;
    ((CreateTableData)localObject1).id = i;
    ((CreateTableData)localObject1).columns = paramArrayList;
    ((CreateTableData)localObject1).temporary = this.table.isTemporary();
    ((CreateTableData)localObject1).persistData = this.table.isPersistData();
    ((CreateTableData)localObject1).persistIndexes = this.table.isPersistIndexes();
    ((CreateTableData)localObject1).isHidden = this.table.isHidden();
    ((CreateTableData)localObject1).create = true;
    ((CreateTableData)localObject1).session = this.session;
    Object localObject2 = getSchema().createTable((CreateTableData)localObject1);
    ((Table)localObject2).setComment(this.table.getComment());
    Object localObject3 = new StringBuilder();
    ((StringBuilder)localObject3).append(((Table)localObject2).getCreateSQL());
    StringBuilder localStringBuilder = new StringBuilder();
    for (Object localObject4 = paramArrayList.iterator(); ((Iterator)localObject4).hasNext();)
    {
      localObject5 = (Column)((Iterator)localObject4).next();
      if (localStringBuilder.length() > 0) {
        localStringBuilder.append(", ");
      }
      if ((this.type == 7) && (this.columnsToAdd.contains(localObject5)))
      {
        localObject6 = ((Column)localObject5).getDefaultExpression();
        localStringBuilder.append(localObject6 == null ? "NULL" : ((Expression)localObject6).getSQL());
      }
      else
      {
        localStringBuilder.append(((Column)localObject5).getSQL());
      }
    }
    ((StringBuilder)localObject3).append(" AS SELECT ");
    if (localStringBuilder.length() == 0) {
      ((StringBuilder)localObject3).append('*');
    } else {
      ((StringBuilder)localObject3).append(localStringBuilder);
    }
    ((StringBuilder)localObject3).append(" FROM ").append(this.table.getSQL());
    localObject4 = ((StringBuilder)localObject3).toString();
    Object localObject5 = ((Table)localObject2).getName();
    Object localObject6 = ((Table)localObject2).getSchema();
    ((Table)localObject2).removeChildrenAndResources(this.session);
    
    execute((String)localObject4, true);
    localObject2 = ((Schema)localObject6).getTableOrView(this.session, (String)localObject5);
    ArrayList localArrayList = New.arrayList();
    for (Iterator localIterator = this.table.getChildren().iterator(); localIterator.hasNext();)
    {
      localObject7 = (DbObject)localIterator.next();
      if (!(localObject7 instanceof Sequence)) {
        if ((localObject7 instanceof Index))
        {
          localObject8 = (Index)localObject7;
          if (((Index)localObject8).getIndexType().getBelongsToConstraint()) {}
        }
        else
        {
          localObject8 = ((DbObject)localObject7).getCreateSQL();
          if ((localObject8 != null) && 
          
            (!(localObject7 instanceof TableView)))
          {
            if (((DbObject)localObject7).getType() == 0) {
              DbException.throwInternalError();
            }
            String str1 = Parser.quoteIdentifier(paramString + "_" + ((DbObject)localObject7).getName());
            String str2 = null;
            if ((localObject7 instanceof ConstraintReferential))
            {
              ConstraintReferential localConstraintReferential = (ConstraintReferential)localObject7;
              if (localConstraintReferential.getTable() != this.table) {
                str2 = localConstraintReferential.getCreateSQLForCopy(localConstraintReferential.getTable(), (Table)localObject2, str1, false);
              }
            }
            if (str2 == null) {
              str2 = ((DbObject)localObject7).getCreateSQLForCopy((Table)localObject2, str1);
            }
            if (str2 != null) {
              if ((localObject7 instanceof TriggerObject)) {
                localArrayList.add(str2);
              } else {
                execute(str2, true);
              }
            }
          }
        }
      }
    }
    Object localObject7;
    Object localObject8;
    this.table.setModified();
    for (localIterator = paramArrayList.iterator(); localIterator.hasNext();)
    {
      localObject7 = (Column)localIterator.next();
      localObject8 = ((Column)localObject7).getSequence();
      if (localObject8 != null)
      {
        this.table.removeSequence((Sequence)localObject8);
        ((Column)localObject7).setSequence(null);
      }
    }
    for (localIterator = localArrayList.iterator(); localIterator.hasNext();)
    {
      localObject7 = (String)localIterator.next();
      execute((String)localObject7, true);
    }
    return (Table)localObject2;
  }
  
  private void checkViews(SchemaObject paramSchemaObject1, SchemaObject paramSchemaObject2)
  {
    String str1 = paramSchemaObject1.getName();
    String str2 = paramSchemaObject2.getName();
    Database localDatabase = paramSchemaObject1.getDatabase();
    
    String str3 = localDatabase.getTempTableName(str1, this.session);
    localDatabase.renameSchemaObject(this.session, paramSchemaObject1, str3);
    try
    {
      localDatabase.renameSchemaObject(this.session, paramSchemaObject2, str1);
      checkViewsAreValid(paramSchemaObject1);
    }
    finally
    {
      try
      {
        localDatabase.renameSchemaObject(this.session, paramSchemaObject2, str2);
      }
      finally
      {
        localDatabase.renameSchemaObject(this.session, paramSchemaObject1, str1);
      }
    }
  }
  
  private void checkViewsAreValid(DbObject paramDbObject)
  {
    for (DbObject localDbObject : paramDbObject.getChildren()) {
      if ((localDbObject instanceof TableView))
      {
        String str = ((TableView)localDbObject).getQuery();
        
        this.session.prepare(str);
        checkViewsAreValid(localDbObject);
      }
    }
  }
  
  private void execute(String paramString, boolean paramBoolean)
  {
    Prepared localPrepared = this.session.prepare(paramString);
    localPrepared.update();
    if (paramBoolean) {
      this.session.commit(true);
    }
  }
  
  private void checkNullable()
  {
    for (Index localIndex : this.table.getIndexes()) {
      if (localIndex.getColumnIndex(this.oldColumn) >= 0)
      {
        IndexType localIndexType = localIndex.getIndexType();
        if ((localIndexType.isPrimaryKey()) || (localIndexType.isHash())) {
          throw DbException.get(90075, localIndex.getSQL());
        }
      }
    }
  }
  
  private void checkNoNullValues()
  {
    String str = "SELECT COUNT(*) FROM " + this.table.getSQL() + " WHERE " + this.oldColumn.getSQL() + " IS NULL";
    
    Prepared localPrepared = this.session.prepare(str);
    ResultInterface localResultInterface = localPrepared.query(0);
    localResultInterface.next();
    if (localResultInterface.currentRow()[0].getInt() > 0) {
      throw DbException.get(90081, this.oldColumn.getSQL());
    }
  }
  
  public void setType(int paramInt)
  {
    this.type = paramInt;
  }
  
  public void setSelectivity(Expression paramExpression)
  {
    this.newSelectivity = paramExpression;
  }
  
  public void setDefaultExpression(Expression paramExpression)
  {
    this.defaultExpression = paramExpression;
  }
  
  public void setNewColumn(Column paramColumn)
  {
    this.newColumn = paramColumn;
  }
  
  public int getType()
  {
    return this.type;
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public void setNewColumns(ArrayList<Column> paramArrayList)
  {
    this.columnsToAdd = paramArrayList;
  }
}
