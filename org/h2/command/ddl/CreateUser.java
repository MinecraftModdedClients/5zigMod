package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.security.SHA256;
import org.h2.util.StringUtils;
import org.h2.value.Value;

public class CreateUser
  extends DefineCommand
{
  private String userName;
  private boolean admin;
  private Expression password;
  private Expression salt;
  private Expression hash;
  private boolean ifNotExists;
  private String comment;
  
  public CreateUser(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public void setUserName(String paramString)
  {
    this.userName = paramString;
  }
  
  public void setPassword(Expression paramExpression)
  {
    this.password = paramExpression;
  }
  
  private char[] getCharArray(Expression paramExpression)
  {
    return paramExpression.optimize(this.session).getValue(this.session).getString().toCharArray();
  }
  
  private byte[] getByteArray(Expression paramExpression)
  {
    return StringUtils.convertHexToBytes(paramExpression.optimize(this.session).getValue(this.session).getString());
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    if (localDatabase.findRole(this.userName) != null) {
      throw DbException.get(90069, this.userName);
    }
    if (localDatabase.findUser(this.userName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(90033, this.userName);
    }
    int i = getObjectId();
    User localUser = new User(localDatabase, i, this.userName, false);
    localUser.setAdmin(this.admin);
    localUser.setComment(this.comment);
    if ((this.hash != null) && (this.salt != null))
    {
      localUser.setSaltAndHash(getByteArray(this.salt), getByteArray(this.hash));
    }
    else if (this.password != null)
    {
      char[] arrayOfChar = getCharArray(this.password);
      byte[] arrayOfByte;
      if ((this.userName.length() == 0) && (arrayOfChar.length == 0)) {
        arrayOfByte = new byte[0];
      } else {
        arrayOfByte = SHA256.getKeyPasswordHash(this.userName, arrayOfChar);
      }
      localUser.setUserPasswordHash(arrayOfByte);
    }
    else
    {
      throw DbException.throwInternalError();
    }
    localDatabase.addDatabaseObject(this.session, localUser);
    return 0;
  }
  
  public void setSalt(Expression paramExpression)
  {
    this.salt = paramExpression;
  }
  
  public void setHash(Expression paramExpression)
  {
    this.hash = paramExpression;
  }
  
  public void setAdmin(boolean paramBoolean)
  {
    this.admin = paramBoolean;
  }
  
  public void setComment(String paramString)
  {
    this.comment = paramString;
  }
  
  public int getType()
  {
    return 32;
  }
}
