package org.h2.engine;

import org.h2.message.DbException;
import org.h2.table.Table;
import org.h2.util.StringUtils;

public class Comment
  extends DbObjectBase
{
  private final int objectType;
  private final String objectName;
  private String commentText;
  
  public Comment(Database paramDatabase, int paramInt, DbObject paramDbObject)
  {
    initDbObjectBase(paramDatabase, paramInt, getKey(paramDbObject), "database");
    this.objectType = paramDbObject.getType();
    this.objectName = paramDbObject.getSQL();
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  private static String getTypeName(int paramInt)
  {
    switch (paramInt)
    {
    case 11: 
      return "CONSTANT";
    case 5: 
      return "CONSTRAINT";
    case 9: 
      return "ALIAS";
    case 1: 
      return "INDEX";
    case 7: 
      return "ROLE";
    case 10: 
      return "SCHEMA";
    case 3: 
      return "SEQUENCE";
    case 0: 
      return "TABLE";
    case 4: 
      return "TRIGGER";
    case 2: 
      return "USER";
    case 12: 
      return "DOMAIN";
    }
    return "type" + paramInt;
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder("COMMENT ON ");
    localStringBuilder.append(getTypeName(this.objectType)).append(' ').append(this.objectName).append(" IS ");
    if (this.commentText == null) {
      localStringBuilder.append("NULL");
    } else {
      localStringBuilder.append(StringUtils.quoteStringSQL(this.commentText));
    }
    return localStringBuilder.toString();
  }
  
  public int getType()
  {
    return 13;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.database.removeMeta(paramSession, getId());
  }
  
  public void checkRename()
  {
    DbException.throwInternalError();
  }
  
  static String getKey(DbObject paramDbObject)
  {
    return getTypeName(paramDbObject.getType()) + " " + paramDbObject.getSQL();
  }
  
  public void setCommentText(String paramString)
  {
    this.commentText = paramString;
  }
}
