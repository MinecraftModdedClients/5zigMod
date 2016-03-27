package org.h2.message;

import java.text.MessageFormat;
import java.util.ArrayList;
import org.h2.engine.SysProperties;
import org.h2.expression.ParameterInterface;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.Value;

public class Trace
{
  public static final String COMMAND = "command";
  public static final String CONSTRAINT = "constraint";
  public static final String DATABASE = "database";
  public static final String FUNCTION = "function";
  public static final String FILE_LOCK = "fileLock";
  public static final String INDEX = "index";
  public static final String JDBC = "jdbc";
  public static final String LOCK = "lock";
  public static final String SCHEMA = "schema";
  public static final String SEQUENCE = "sequence";
  public static final String SETTING = "setting";
  public static final String TABLE = "table";
  public static final String TRIGGER = "trigger";
  public static final String USER = "user";
  public static final String PAGE_STORE = "pageStore";
  private final TraceWriter traceWriter;
  private final String module;
  private final String lineSeparator;
  private int traceLevel = -1;
  
  Trace(TraceWriter paramTraceWriter, String paramString)
  {
    this.traceWriter = paramTraceWriter;
    this.module = paramString;
    this.lineSeparator = SysProperties.LINE_SEPARATOR;
  }
  
  public void setLevel(int paramInt)
  {
    this.traceLevel = paramInt;
  }
  
  private boolean isEnabled(int paramInt)
  {
    if (this.traceLevel == -1) {
      return this.traceWriter.isEnabled(paramInt);
    }
    return paramInt <= this.traceLevel;
  }
  
  public boolean isInfoEnabled()
  {
    return isEnabled(2);
  }
  
  public boolean isDebugEnabled()
  {
    return isEnabled(3);
  }
  
  public void error(Throwable paramThrowable, String paramString)
  {
    if (isEnabled(1)) {
      this.traceWriter.write(1, this.module, paramString, paramThrowable);
    }
  }
  
  public void error(Throwable paramThrowable, String paramString, Object... paramVarArgs)
  {
    if (isEnabled(1))
    {
      paramString = MessageFormat.format(paramString, paramVarArgs);
      this.traceWriter.write(1, this.module, paramString, paramThrowable);
    }
  }
  
  public void info(String paramString)
  {
    if (isEnabled(2)) {
      this.traceWriter.write(2, this.module, paramString, null);
    }
  }
  
  public void info(String paramString, Object... paramVarArgs)
  {
    if (isEnabled(2))
    {
      paramString = MessageFormat.format(paramString, paramVarArgs);
      this.traceWriter.write(2, this.module, paramString, null);
    }
  }
  
  void info(Throwable paramThrowable, String paramString)
  {
    if (isEnabled(2)) {
      this.traceWriter.write(2, this.module, paramString, paramThrowable);
    }
  }
  
  public static String formatParams(ArrayList<? extends ParameterInterface> paramArrayList)
  {
    if (paramArrayList.size() == 0) {
      return "";
    }
    StatementBuilder localStatementBuilder = new StatementBuilder();
    int i = 0;
    int j = 0;
    for (ParameterInterface localParameterInterface : paramArrayList) {
      if (localParameterInterface.isValueSet())
      {
        if (j == 0)
        {
          localStatementBuilder.append(" {");
          j = 1;
        }
        localStatementBuilder.appendExceptFirst(", ");
        Value localValue = localParameterInterface.getParamValue();
        localStatementBuilder.append(++i).append(": ").append(localValue.getTraceSQL());
      }
    }
    if (j != 0) {
      localStatementBuilder.append('}');
    }
    return localStatementBuilder.toString();
  }
  
  public void infoSQL(String paramString1, String paramString2, int paramInt, long paramLong)
  {
    if (!isEnabled(2)) {
      return;
    }
    StringBuilder localStringBuilder = new StringBuilder(paramString1.length() + paramString2.length() + 20);
    localStringBuilder.append(this.lineSeparator).append("/*SQL");
    int i = 0;
    if (paramString2.length() > 0)
    {
      i = 1;
      localStringBuilder.append(" l:").append(paramString1.length());
    }
    if (paramInt > 0)
    {
      i = 1;
      localStringBuilder.append(" #:").append(paramInt);
    }
    if (paramLong > 0L)
    {
      i = 1;
      localStringBuilder.append(" t:").append(paramLong);
    }
    if (i == 0) {
      localStringBuilder.append(' ');
    }
    localStringBuilder.append("*/").append(StringUtils.javaEncode(paramString1)).append(StringUtils.javaEncode(paramString2)).append(';');
    
    paramString1 = localStringBuilder.toString();
    this.traceWriter.write(2, this.module, paramString1, null);
  }
  
  public void debug(String paramString, Object... paramVarArgs)
  {
    if (isEnabled(3))
    {
      paramString = MessageFormat.format(paramString, paramVarArgs);
      this.traceWriter.write(3, this.module, paramString, null);
    }
  }
  
  public void debug(String paramString)
  {
    if (isEnabled(3)) {
      this.traceWriter.write(3, this.module, paramString, null);
    }
  }
  
  public void debug(Throwable paramThrowable, String paramString)
  {
    if (isEnabled(3)) {
      this.traceWriter.write(3, this.module, paramString, paramThrowable);
    }
  }
  
  public void infoCode(String paramString)
  {
    if (isEnabled(2)) {
      this.traceWriter.write(2, this.module, this.lineSeparator + "/**/" + paramString, null);
    }
  }
  
  void debugCode(String paramString)
  {
    if (isEnabled(3)) {
      this.traceWriter.write(3, this.module, this.lineSeparator + "/**/" + paramString, null);
    }
  }
}
