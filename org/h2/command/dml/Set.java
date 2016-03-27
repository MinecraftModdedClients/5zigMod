package org.h2.command.dml;

import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.Mode;
import org.h2.engine.Session;
import org.h2.engine.Setting;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.expression.ValueExpression;
import org.h2.message.DbException;
import org.h2.message.TraceSystem;
import org.h2.result.ResultInterface;
import org.h2.schema.Schema;
import org.h2.table.Table;
import org.h2.tools.CompressTool;
import org.h2.util.StringUtils;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueInt;

public class Set
  extends Prepared
{
  private final int type;
  private Expression expression;
  private String stringValue;
  private String[] stringValueList;
  
  public Set(Session paramSession, int paramInt)
  {
    super(paramSession);
    this.type = paramInt;
  }
  
  public void setString(String paramString)
  {
    this.stringValue = paramString;
  }
  
  public boolean isTransactional()
  {
    switch (this.type)
    {
    case 5: 
    case 9: 
    case 10: 
    case 13: 
    case 20: 
    case 26: 
    case 28: 
    case 35: 
    case 36: 
    case 40: 
      return true;
    }
    return false;
  }
  
  public int update()
  {
    Database localDatabase = this.session.getDatabase();
    String str1 = SetTypes.getTypeName(this.type);
    Object localObject1;
    int j;
    int k;
    int m;
    int n;
    switch (this.type)
    {
    case 24: 
      this.session.getUser().checkAdmin();
      int i = getIntValue();
      if ((i < 0) || (i > 2)) {
        throw DbException.getInvalidValueException("ALLOW_LITERALS", Integer.valueOf(getIntValue()));
      }
      localDatabase.setAllowLiterals(i);
      addOrUpdateSetting(str1, null, i);
      break;
    case 8: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("CACHE_SIZE", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      localDatabase.setCacheSize(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 13: 
      if (!"TRUE".equals(this.stringValue))
      {
        String str2 = StringUtils.quoteStringSQL(this.stringValue);
        if ((!str2.equals(localDatabase.getCluster())) && (!str2.equals("''"))) {
          this.session.getUser().checkAdmin();
        }
        localDatabase.setCluster(str2);
        
        localObject1 = localDatabase.getSystemSession();
        synchronized (localObject1)
        {
          synchronized (localDatabase)
          {
            addOrUpdateSetting((Session)localObject1, str1, str2, 0);
            ((Session)localObject1).commit(true);
          }
        }
      }
      break;
    case 12: 
      this.session.getUser().checkAdmin();
      boolean bool = localDatabase.getCompareMode().isBinaryUnsigned();
      
      ??? = new StringBuilder(this.stringValue);
      if (this.stringValue.equals("OFF"))
      {
        localObject1 = CompareMode.getInstance(null, 0, bool);
      }
      else
      {
        int i1 = getIntValue();
        ((StringBuilder)???).append(" STRENGTH ");
        if (i1 == 3) {
          ((StringBuilder)???).append("IDENTICAL");
        } else if (i1 == 0) {
          ((StringBuilder)???).append("PRIMARY");
        } else if (i1 == 1) {
          ((StringBuilder)???).append("SECONDARY");
        } else if (i1 == 2) {
          ((StringBuilder)???).append("TERTIARY");
        }
        localObject1 = CompareMode.getInstance(this.stringValue, i1, bool);
      }
      CompareMode localCompareMode = localDatabase.getCompareMode();
      if (!localCompareMode.equals(localObject1))
      {
        Table localTable3 = localDatabase.getFirstUserTable();
        if (localTable3 != null) {
          throw DbException.get(90089, localTable3.getSQL());
        }
        addOrUpdateSetting(str1, ((StringBuilder)???).toString(), 0);
        localDatabase.setCompareMode((CompareMode)localObject1);
      }
      break;
    case 38: 
      this.session.getUser().checkAdmin();
      Table localTable1 = localDatabase.getFirstUserTable();
      if (localTable1 != null) {
        throw DbException.get(90089, localTable1.getSQL());
      }
      localObject1 = localDatabase.getCompareMode();
      if (this.stringValue.equals("SIGNED")) {
        ??? = CompareMode.getInstance(((CompareMode)localObject1).getName(), ((CompareMode)localObject1).getStrength(), false);
      } else if (this.stringValue.equals("UNSIGNED")) {
        ??? = CompareMode.getInstance(((CompareMode)localObject1).getName(), ((CompareMode)localObject1).getStrength(), true);
      } else {
        throw DbException.getInvalidValueException("BINARY_COLLATION", this.stringValue);
      }
      addOrUpdateSetting(str1, this.stringValue, 0);
      localDatabase.setCompareMode((CompareMode)???);
      break;
    case 23: 
      this.session.getUser().checkAdmin();
      j = CompressTool.getCompressAlgorithm(this.stringValue);
      localDatabase.setLobCompressionAlgorithm(j == 0 ? null : this.stringValue);
      
      addOrUpdateSetting(str1, this.stringValue, 0);
      break;
    case 34: 
      this.session.getUser().checkAdmin();
      if (localDatabase.isStarting())
      {
        j = getIntValue();
        addOrUpdateSetting(str1, null, j);
      }
      break;
    case 15: 
      this.session.getUser().checkAdmin();
      localDatabase.setEventListenerClass(this.stringValue);
      break;
    case 18: 
      j = getIntValue();
      if (j != -1) {
        if (j < 0) {
          throw DbException.getInvalidValueException("DB_CLOSE_DELAY", Integer.valueOf(j));
        }
      }
      this.session.getUser().checkAdmin();
      localDatabase.setCloseDelay(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 6: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("DEFAULT_LOCK_TIMEOUT", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 7: 
      this.session.getUser().checkAdmin();
      localDatabase.setDefaultTableType(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 33: 
      this.session.getUser().checkAdmin();
      j = getIntValue();
      switch (j)
      {
      case 0: 
        localDatabase.setExclusiveSession(null, false);
        break;
      case 1: 
        localDatabase.setExclusiveSession(this.session, false);
        break;
      case 2: 
        localDatabase.setExclusiveSession(this.session, true);
        break;
      default: 
        throw DbException.getInvalidValueException("EXCLUSIVE", Integer.valueOf(j));
      }
      break;
    case 39: 
      this.session.getUser().checkAdmin();
      Table localTable2 = localDatabase.getFirstUserTable();
      if (localTable2 != null) {
        throw DbException.get(90141, localTable2.getSQL());
      }
      localDatabase.setJavaObjectSerializerName(this.stringValue);
      addOrUpdateSetting(str1, this.stringValue, 0);
      break;
    case 1: 
      this.session.getUser().checkAdmin();
      localDatabase.setIgnoreCase(getIntValue() == 1);
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 17: 
      this.session.getUser().checkAdmin();
      localDatabase.setLockMode(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 5: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("LOCK_TIMEOUT", Integer.valueOf(getIntValue()));
      }
      this.session.setLockTimeout(getIntValue());
      break;
    case 19: 
      k = getIntValue();
      if ((localDatabase.isPersistent()) && (k != localDatabase.getLogMode()))
      {
        this.session.getUser().checkAdmin();
        localDatabase.setLogMode(k);
      }
      break;
    case 22: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("MAX_LENGTH_INPLACE_LOB", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      localDatabase.setMaxLengthInplaceLob(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 2: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("MAX_LOG_SIZE", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      localDatabase.setMaxLogSize(getIntValue() * 1024L * 1024L);
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 16: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("MAX_MEMORY_ROWS", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      localDatabase.setMaxMemoryRows(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 21: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("MAX_MEMORY_UNDO", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      localDatabase.setMaxMemoryUndo(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 32: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("MAX_OPERATION_MEMORY", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      k = getIntValue();
      localDatabase.setMaxOperationMemory(k);
      break;
    case 3: 
      Mode localMode = Mode.getInstance(this.stringValue);
      if (localMode == null) {
        throw DbException.get(90088, this.stringValue);
      }
      if (localDatabase.getMode() != localMode)
      {
        this.session.getUser().checkAdmin();
        localDatabase.setMode(localMode);
      }
      break;
    case 25: 
      this.session.getUser().checkAdmin();
      localDatabase.setMultiThreaded(getIntValue() == 1);
      break;
    case 31: 
      if (localDatabase.isMultiVersion() != (getIntValue() == 1)) {
        throw DbException.get(90133, "MVCC");
      }
      break;
    case 27: 
      this.session.getUser().checkAdmin();
      localDatabase.setOptimizeReuseResults(getIntValue() != 0);
      break;
    case 36: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("QUERY_TIMEOUT", Integer.valueOf(getIntValue()));
      }
      m = getIntValue();
      this.session.setQueryTimeout(m);
      break;
    case 37: 
      m = getIntValue();
      this.session.setRedoLogBinary(m == 1);
      break;
    case 30: 
      this.session.getUser().checkAdmin();
      m = getIntValue();
      if ((m < 0) || (m > 1)) {
        throw DbException.getInvalidValueException("REFERENTIAL_INTEGRITY", Integer.valueOf(getIntValue()));
      }
      localDatabase.setReferentialIntegrity(m == 1);
      break;
    case 41: 
      this.session.getUser().checkAdmin();
      m = getIntValue();
      if ((m < 0) || (m > 1)) {
        throw DbException.getInvalidValueException("QUERY_STATISTICS", Integer.valueOf(getIntValue()));
      }
      localDatabase.setQueryStatistics(m == 1);
      break;
    case 26: 
      Schema localSchema = localDatabase.getSchema(this.stringValue);
      this.session.setCurrentSchema(localSchema);
      break;
    case 28: 
      this.session.setSchemaSearchPath(this.stringValueList);
      break;
    case 10: 
      this.session.getUser().checkAdmin();
      if (getCurrentObjectId() == 0) {
        localDatabase.getTraceSystem().setLevelFile(getIntValue());
      }
      break;
    case 9: 
      this.session.getUser().checkAdmin();
      if (getCurrentObjectId() == 0) {
        localDatabase.getTraceSystem().setLevelSystemOut(getIntValue());
      }
      break;
    case 11: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("TRACE_MAX_FILE_SIZE", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      n = getIntValue() * 1024 * 1024;
      localDatabase.getTraceSystem().setMaxFileSize(n);
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 20: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("THROTTLE", Integer.valueOf(getIntValue()));
      }
      this.session.setThrottle(getIntValue());
      break;
    case 29: 
      n = getIntValue();
      if ((n < 0) || (n > 1)) {
        throw DbException.getInvalidValueException("UNDO_LOG", Integer.valueOf(getIntValue()));
      }
      this.session.setUndoLogEnabled(n == 1);
      break;
    case 35: 
      Expression localExpression = this.expression.optimize(this.session);
      this.session.setVariable(this.stringValue, localExpression.getValue(this.session));
      break;
    case 14: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("WRITE_DELAY", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      localDatabase.setWriteDelay(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 40: 
      if (getIntValue() < 0) {
        throw DbException.getInvalidValueException("RETENTION_TIME", Integer.valueOf(getIntValue()));
      }
      this.session.getUser().checkAdmin();
      localDatabase.setRetentionTime(getIntValue());
      addOrUpdateSetting(str1, null, getIntValue());
      break;
    case 4: 
    default: 
      DbException.throwInternalError("type=" + this.type);
    }
    localDatabase.getNextModificationDataId();
    
    localDatabase.getNextModificationMetaId();
    return 0;
  }
  
  private int getIntValue()
  {
    this.expression = this.expression.optimize(this.session);
    return this.expression.getValue(this.session).getInt();
  }
  
  public void setInt(int paramInt)
  {
    this.expression = ValueExpression.get(ValueInt.get(paramInt));
  }
  
  public void setExpression(Expression paramExpression)
  {
    this.expression = paramExpression;
  }
  
  private void addOrUpdateSetting(String paramString1, String paramString2, int paramInt)
  {
    addOrUpdateSetting(this.session, paramString1, paramString2, paramInt);
  }
  
  private void addOrUpdateSetting(Session paramSession, String paramString1, String paramString2, int paramInt)
  {
    Database localDatabase = paramSession.getDatabase();
    if (localDatabase.isReadOnly()) {
      return;
    }
    Setting localSetting = localDatabase.findSetting(paramString1);
    int i = 0;
    if (localSetting == null)
    {
      i = 1;
      int j = getObjectId();
      localSetting = new Setting(localDatabase, j, paramString1);
    }
    if (paramString2 != null)
    {
      if ((i == 0) && (localSetting.getStringValue().equals(paramString2))) {
        return;
      }
      localSetting.setStringValue(paramString2);
    }
    else
    {
      if ((i == 0) && (localSetting.getIntValue() == paramInt)) {
        return;
      }
      localSetting.setIntValue(paramInt);
    }
    if (i != 0) {
      localDatabase.addDatabaseObject(paramSession, localSetting);
    } else {
      localDatabase.updateMeta(paramSession, localSetting);
    }
  }
  
  public boolean needRecompile()
  {
    return false;
  }
  
  public ResultInterface queryMeta()
  {
    return null;
  }
  
  public void setStringArray(String[] paramArrayOfString)
  {
    this.stringValueList = paramArrayOfString;
  }
  
  public int getType()
  {
    return 67;
  }
}
