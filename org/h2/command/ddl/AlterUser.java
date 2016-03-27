package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.security.SHA256;
import org.h2.util.StringUtils;
import org.h2.value.Value;

public class AlterUser
  extends DefineCommand
{
  private int type;
  private User user;
  private String newName;
  private Expression password;
  private Expression salt;
  private Expression hash;
  private boolean admin;
  
  public AlterUser(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setType(int paramInt)
  {
    this.type = paramInt;
  }
  
  public void setNewName(String paramString)
  {
    this.newName = paramString;
  }
  
  public void setUser(User paramUser)
  {
    this.user = paramUser;
  }
  
  public void setAdmin(boolean paramBoolean)
  {
    this.admin = paramBoolean;
  }
  
  public void setSalt(Expression paramExpression)
  {
    this.salt = paramExpression;
  }
  
  public void setHash(Expression paramExpression)
  {
    this.hash = paramExpression;
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
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    switch (this.type)
    {
    case 19: 
      if (this.user != this.session.getUser()) {
        this.session.getUser().checkAdmin();
      }
      if ((this.hash != null) && (this.salt != null))
      {
        this.user.setSaltAndHash(getByteArray(this.salt), getByteArray(this.hash));
      }
      else
      {
        String str = this.newName == null ? this.user.getName() : this.newName;
        char[] arrayOfChar = getCharArray(this.password);
        byte[] arrayOfByte = SHA256.getKeyPasswordHash(str, arrayOfChar);
        this.user.setUserPasswordHash(arrayOfByte);
      }
      break;
    case 18: 
      this.session.getUser().checkAdmin();
      if ((localDatabase.findUser(this.newName) != null) || (this.newName.equals(this.user.getName()))) {
        throw DbException.get(90033, this.newName);
      }
      localDatabase.renameDatabaseObject(this.session, this.user, this.newName);
      break;
    case 17: 
      this.session.getUser().checkAdmin();
      if (!this.admin) {
        this.user.checkOwnsNoSchemas();
      }
      this.user.setAdmin(this.admin);
      break;
    default: 
      DbException.throwInternalError("type=" + this.type);
    }
    localDatabase.updateMeta(this.session, this.user);
    return 0;
  }
  
  public int getType()
  {
    return this.type;
  }
}
