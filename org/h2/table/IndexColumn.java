package org.h2.table;

public class IndexColumn
{
  public String columnName;
  public Column column;
  public int sortType = 0;
  
  public String getSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder(this.column.getSQL());
    if ((this.sortType & 0x1) != 0) {
      localStringBuilder.append(" DESC");
    }
    if ((this.sortType & 0x2) != 0) {
      localStringBuilder.append(" NULLS FIRST");
    } else if ((this.sortType & 0x4) != 0) {
      localStringBuilder.append(" NULLS LAST");
    }
    return localStringBuilder.toString();
  }
  
  public static IndexColumn[] wrap(Column[] paramArrayOfColumn)
  {
    IndexColumn[] arrayOfIndexColumn = new IndexColumn[paramArrayOfColumn.length];
    for (int i = 0; i < arrayOfIndexColumn.length; i++)
    {
      arrayOfIndexColumn[i] = new IndexColumn();
      arrayOfIndexColumn[i].column = paramArrayOfColumn[i];
    }
    return arrayOfIndexColumn;
  }
  
  public static void mapColumns(IndexColumn[] paramArrayOfIndexColumn, Table paramTable)
  {
    for (IndexColumn localIndexColumn : paramArrayOfIndexColumn) {
      localIndexColumn.column = paramTable.getColumn(localIndexColumn.columnName);
    }
  }
}
