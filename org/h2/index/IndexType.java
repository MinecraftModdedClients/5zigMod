package org.h2.index;

public class IndexType
{
  private boolean primaryKey;
  private boolean persistent;
  private boolean unique;
  private boolean hash;
  private boolean scan;
  private boolean spatial;
  private boolean belongsToConstraint;
  
  public static IndexType createPrimaryKey(boolean paramBoolean1, boolean paramBoolean2)
  {
    IndexType localIndexType = new IndexType();
    localIndexType.primaryKey = true;
    localIndexType.persistent = paramBoolean1;
    localIndexType.hash = paramBoolean2;
    localIndexType.unique = true;
    return localIndexType;
  }
  
  public static IndexType createUnique(boolean paramBoolean1, boolean paramBoolean2)
  {
    IndexType localIndexType = new IndexType();
    localIndexType.unique = true;
    localIndexType.persistent = paramBoolean1;
    localIndexType.hash = paramBoolean2;
    return localIndexType;
  }
  
  public static IndexType createNonUnique(boolean paramBoolean)
  {
    return createNonUnique(paramBoolean, false, false);
  }
  
  public static IndexType createNonUnique(boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    IndexType localIndexType = new IndexType();
    localIndexType.persistent = paramBoolean1;
    localIndexType.hash = paramBoolean2;
    localIndexType.spatial = paramBoolean3;
    return localIndexType;
  }
  
  public static IndexType createScan(boolean paramBoolean)
  {
    IndexType localIndexType = new IndexType();
    localIndexType.persistent = paramBoolean;
    localIndexType.scan = true;
    return localIndexType;
  }
  
  public void setBelongsToConstraint(boolean paramBoolean)
  {
    this.belongsToConstraint = paramBoolean;
  }
  
  public boolean getBelongsToConstraint()
  {
    return this.belongsToConstraint;
  }
  
  public boolean isHash()
  {
    return this.hash;
  }
  
  public boolean isSpatial()
  {
    return this.spatial;
  }
  
  public boolean isPersistent()
  {
    return this.persistent;
  }
  
  public boolean isPrimaryKey()
  {
    return this.primaryKey;
  }
  
  public boolean isUnique()
  {
    return this.unique;
  }
  
  public String getSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (this.primaryKey)
    {
      localStringBuilder.append("PRIMARY KEY");
      if (this.hash) {
        localStringBuilder.append(" HASH");
      }
    }
    else
    {
      if (this.unique) {
        localStringBuilder.append("UNIQUE ");
      }
      if (this.hash) {
        localStringBuilder.append("HASH ");
      }
      if (this.spatial) {
        localStringBuilder.append("SPATIAL ");
      }
      localStringBuilder.append("INDEX");
    }
    return localStringBuilder.toString();
  }
  
  public boolean isScan()
  {
    return this.scan;
  }
}
