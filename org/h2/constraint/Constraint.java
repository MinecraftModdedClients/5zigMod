package org.h2.constraint;

import java.util.HashSet;
import org.h2.engine.Session;
import org.h2.expression.ExpressionVisitor;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObjectBase;
import org.h2.table.Column;
import org.h2.table.Table;

public abstract class Constraint
  extends SchemaObjectBase
  implements Comparable<Constraint>
{
  public static final String CHECK = "CHECK";
  public static final String REFERENTIAL = "REFERENTIAL";
  public static final String UNIQUE = "UNIQUE";
  public static final String PRIMARY_KEY = "PRIMARY KEY";
  protected Table table;
  
  Constraint(Schema paramSchema, int paramInt, String paramString, Table paramTable)
  {
    initSchemaObjectBase(paramSchema, paramInt, paramString, "constraint");
    this.table = paramTable;
    setTemporary(paramTable.isTemporary());
  }
  
  public abstract String getConstraintType();
  
  public abstract void checkRow(Session paramSession, Table paramTable, Row paramRow1, Row paramRow2);
  
  public abstract boolean usesIndex(Index paramIndex);
  
  public abstract void setIndexOwner(Index paramIndex);
  
  public abstract HashSet<Column> getReferencedColumns(Table paramTable);
  
  public abstract String getCreateSQLWithoutIndexes();
  
  public abstract boolean isBefore();
  
  public abstract void checkExistingData(Session paramSession);
  
  public abstract void rebuild();
  
  public abstract Index getUniqueIndex();
  
  public void checkRename() {}
  
  public int getType()
  {
    return 5;
  }
  
  public Table getTable()
  {
    return this.table;
  }
  
  public Table getRefTable()
  {
    return this.table;
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  private int getConstraintTypeOrder()
  {
    String str = getConstraintType();
    if ("CHECK".equals(str)) {
      return 0;
    }
    if ("PRIMARY KEY".equals(str)) {
      return 1;
    }
    if ("UNIQUE".equals(str)) {
      return 2;
    }
    if ("REFERENTIAL".equals(str)) {
      return 3;
    }
    throw DbException.throwInternalError("type: " + str);
  }
  
  public int compareTo(Constraint paramConstraint)
  {
    if (this == paramConstraint) {
      return 0;
    }
    int i = getConstraintTypeOrder();
    int j = paramConstraint.getConstraintTypeOrder();
    return i - j;
  }
  
  public boolean isHidden()
  {
    return this.table.isHidden();
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return true;
  }
}
