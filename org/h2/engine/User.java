package org.h2.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.security.SHA256;
import org.h2.table.MetaTable;
import org.h2.table.RangeTable;
import org.h2.table.Table;
import org.h2.table.TableView;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class User
  extends RightOwner
{
  private final boolean systemUser;
  private byte[] salt;
  private byte[] passwordHash;
  private boolean admin;
  
  public User(Database paramDatabase, int paramInt, String paramString, boolean paramBoolean)
  {
    super(paramDatabase, paramInt, paramString, "user");
    this.systemUser = paramBoolean;
  }
  
  public void setAdmin(boolean paramBoolean)
  {
    this.admin = paramBoolean;
  }
  
  public boolean isAdmin()
  {
    return this.admin;
  }
  
  public void setSaltAndHash(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    this.salt = paramArrayOfByte1;
    this.passwordHash = paramArrayOfByte2;
  }
  
  public void setUserPasswordHash(byte[] paramArrayOfByte)
  {
    if (paramArrayOfByte != null) {
      if (paramArrayOfByte.length == 0)
      {
        this.salt = (this.passwordHash = paramArrayOfByte);
      }
      else
      {
        this.salt = new byte[8];
        MathUtils.randomBytes(this.salt);
        this.passwordHash = SHA256.getHashWithSalt(paramArrayOfByte, this.salt);
      }
    }
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public String getCreateSQL()
  {
    return getCreateSQL(true);
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public void checkRight(Table paramTable, int paramInt)
  {
    if (!hasRight(paramTable, paramInt)) {
      throw DbException.get(90096, paramTable.getSQL());
    }
  }
  
  public boolean hasRight(Table paramTable, int paramInt)
  {
    if ((paramInt != 1) && (!this.systemUser) && (paramTable != null)) {
      paramTable.checkWritingAllowed();
    }
    if (this.admin) {
      return true;
    }
    Role localRole = this.database.getPublicRole();
    if (localRole.isRightGrantedRecursive(paramTable, paramInt)) {
      return true;
    }
    if (((paramTable instanceof MetaTable)) || ((paramTable instanceof RangeTable))) {
      return true;
    }
    if (paramTable != null)
    {
      if (hasRight(null, 16)) {
        return true;
      }
      String str = paramTable.getTableType();
      if ("VIEW".equals(str))
      {
        TableView localTableView = (TableView)paramTable;
        if (localTableView.getOwner() == this) {
          return true;
        }
      }
      else if (str == null)
      {
        return true;
      }
      if ((paramTable.isTemporary()) && (!paramTable.isGlobalTemporary())) {
        return true;
      }
    }
    if (isRightGrantedRecursive(paramTable, paramInt)) {
      return true;
    }
    return false;
  }
  
  public String getCreateSQL(boolean paramBoolean)
  {
    StringBuilder localStringBuilder = new StringBuilder("CREATE USER IF NOT EXISTS ");
    localStringBuilder.append(getSQL());
    if (this.comment != null) {
      localStringBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    if (paramBoolean) {
      localStringBuilder.append(" SALT '").append(StringUtils.convertBytesToHex(this.salt)).append("' HASH '").append(StringUtils.convertBytesToHex(this.passwordHash)).append('\'');
    } else {
      localStringBuilder.append(" PASSWORD ''");
    }
    if (this.admin) {
      localStringBuilder.append(" ADMIN");
    }
    return localStringBuilder.toString();
  }
  
  boolean validateUserPasswordHash(byte[] paramArrayOfByte)
  {
    if ((paramArrayOfByte.length == 0) && (this.passwordHash.length == 0)) {
      return true;
    }
    if (paramArrayOfByte.length == 0) {
      paramArrayOfByte = SHA256.getKeyPasswordHash(getName(), new char[0]);
    }
    byte[] arrayOfByte = SHA256.getHashWithSalt(paramArrayOfByte, this.salt);
    return Utils.compareSecure(arrayOfByte, this.passwordHash);
  }
  
  public void checkAdmin()
  {
    if (!this.admin) {
      throw DbException.get(90040);
    }
  }
  
  public void checkSchemaAdmin()
  {
    if (!hasRight(null, 16)) {
      throw DbException.get(90040);
    }
  }
  
  public int getType()
  {
    return 2;
  }
  
  public ArrayList<DbObject> getChildren()
  {
    ArrayList localArrayList = New.arrayList();
    for (Iterator localIterator = this.database.getAllRights().iterator(); localIterator.hasNext();)
    {
      localObject = (Right)localIterator.next();
      if (((Right)localObject).getGrantee() == this) {
        localArrayList.add(localObject);
      }
    }
    Object localObject;
    for (localIterator = this.database.getAllSchemas().iterator(); localIterator.hasNext();)
    {
      localObject = (Schema)localIterator.next();
      if (((Schema)localObject).getOwner() == this) {
        localArrayList.add(localObject);
      }
    }
    return localArrayList;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    for (Right localRight : this.database.getAllRights()) {
      if (localRight.getGrantee() == this) {
        this.database.removeDatabaseObject(paramSession, localRight);
      }
    }
    this.database.removeMeta(paramSession, getId());
    this.salt = null;
    Arrays.fill(this.passwordHash, (byte)0);
    this.passwordHash = null;
    invalidate();
  }
  
  public void checkRename() {}
  
  public void checkOwnsNoSchemas()
  {
    for (Schema localSchema : this.database.getAllSchemas()) {
      if (this == localSchema.getOwner()) {
        throw DbException.get(90107, new String[] { getName(), localSchema.getName() });
      }
    }
  }
}
