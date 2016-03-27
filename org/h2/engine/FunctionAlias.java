package org.h2.engine;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import org.h2.Driver;
import org.h2.command.Parser;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObjectBase;
import org.h2.table.Table;
import org.h2.util.JdbcUtils;
import org.h2.util.New;
import org.h2.util.SourceCompiler;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueNull;

public class FunctionAlias
  extends SchemaObjectBase
{
  private String className;
  private String methodName;
  private String source;
  private JavaMethod[] javaMethods;
  private boolean deterministic;
  private boolean bufferResultSetToLocalTemp = true;
  
  private FunctionAlias(Schema paramSchema, int paramInt, String paramString)
  {
    initSchemaObjectBase(paramSchema, paramInt, paramString, "function");
  }
  
  public static FunctionAlias newInstance(Schema paramSchema, int paramInt, String paramString1, String paramString2, boolean paramBoolean1, boolean paramBoolean2)
  {
    FunctionAlias localFunctionAlias = new FunctionAlias(paramSchema, paramInt, paramString1);
    int i = paramString2.indexOf('(');
    int j = paramString2.lastIndexOf('.', i < 0 ? paramString2.length() : i);
    if (j < 0) {
      throw DbException.get(42000, paramString2);
    }
    localFunctionAlias.className = paramString2.substring(0, j);
    localFunctionAlias.methodName = paramString2.substring(j + 1);
    localFunctionAlias.bufferResultSetToLocalTemp = paramBoolean2;
    localFunctionAlias.init(paramBoolean1);
    return localFunctionAlias;
  }
  
  public static FunctionAlias newInstanceFromSource(Schema paramSchema, int paramInt, String paramString1, String paramString2, boolean paramBoolean1, boolean paramBoolean2)
  {
    FunctionAlias localFunctionAlias = new FunctionAlias(paramSchema, paramInt, paramString1);
    localFunctionAlias.source = paramString2;
    localFunctionAlias.bufferResultSetToLocalTemp = paramBoolean2;
    localFunctionAlias.init(paramBoolean1);
    return localFunctionAlias;
  }
  
  private void init(boolean paramBoolean)
  {
    try
    {
      load();
    }
    catch (DbException localDbException)
    {
      if (!paramBoolean) {
        throw localDbException;
      }
    }
  }
  
  private synchronized void load()
  {
    if (this.javaMethods != null) {
      return;
    }
    if (this.source != null) {
      loadFromSource();
    } else {
      loadClass();
    }
  }
  
  private void loadFromSource()
  {
    SourceCompiler localSourceCompiler = this.database.getCompiler();
    synchronized (localSourceCompiler)
    {
      String str = "org.h2.dynamic." + getName();
      localSourceCompiler.setSource(str, this.source);
      try
      {
        Method localMethod = localSourceCompiler.getMethod(str);
        JavaMethod localJavaMethod = new JavaMethod(localMethod, 0);
        this.javaMethods = new JavaMethod[] { localJavaMethod };
      }
      catch (DbException localDbException)
      {
        throw localDbException;
      }
      catch (Exception localException)
      {
        throw DbException.get(42000, localException, new String[] { this.source });
      }
    }
  }
  
  private void loadClass()
  {
    Class localClass = JdbcUtils.loadUserClass(this.className);
    Method[] arrayOfMethod = localClass.getMethods();
    ArrayList localArrayList = New.arrayList();
    int i = 0;
    for (int j = arrayOfMethod.length; i < j; i++)
    {
      Method localMethod = arrayOfMethod[i];
      if (Modifier.isStatic(localMethod.getModifiers())) {
        if ((localMethod.getName().equals(this.methodName)) || (getMethodSignature(localMethod).equals(this.methodName)))
        {
          JavaMethod localJavaMethod1 = new JavaMethod(localMethod, i);
          for (JavaMethod localJavaMethod2 : localArrayList) {
            if (localJavaMethod2.getParameterCount() == localJavaMethod1.getParameterCount()) {
              throw DbException.get(90073, new String[] { localJavaMethod2.toString(), localJavaMethod1.toString() });
            }
          }
          localArrayList.add(localJavaMethod1);
        }
      }
    }
    if (localArrayList.size() == 0) {
      throw DbException.get(90139, this.methodName + " (" + this.className + ")");
    }
    this.javaMethods = new JavaMethod[localArrayList.size()];
    localArrayList.toArray(this.javaMethods);
    
    Arrays.sort(this.javaMethods);
  }
  
  private static String getMethodSignature(Method paramMethod)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder(paramMethod.getName());
    localStatementBuilder.append('(');
    for (Class localClass : paramMethod.getParameterTypes())
    {
      localStatementBuilder.appendExceptFirst(",");
      if (localClass.isArray()) {
        localStatementBuilder.append(localClass.getComponentType().getName()).append("[]");
      } else {
        localStatementBuilder.append(localClass.getName());
      }
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public String getDropSQL()
  {
    return "DROP ALIAS IF EXISTS " + getSQL();
  }
  
  public String getSQL()
  {
    if ((this.database.getSettings().functionsInSchema) || (!getSchema().getName().equals("PUBLIC"))) {
      return super.getSQL();
    }
    return Parser.quoteIdentifier(getName());
  }
  
  public String getCreateSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder("CREATE FORCE ALIAS ");
    localStringBuilder.append(getSQL());
    if (this.deterministic) {
      localStringBuilder.append(" DETERMINISTIC");
    }
    if (!this.bufferResultSetToLocalTemp) {
      localStringBuilder.append(" NOBUFFER");
    }
    if (this.source != null) {
      localStringBuilder.append(" AS ").append(StringUtils.quoteStringSQL(this.source));
    } else {
      localStringBuilder.append(" FOR ").append(Parser.quoteIdentifier(this.className + "." + this.methodName));
    }
    return localStringBuilder.toString();
  }
  
  public int getType()
  {
    return 9;
  }
  
  public synchronized void removeChildrenAndResources(Session paramSession)
  {
    this.database.removeMeta(paramSession, getId());
    this.className = null;
    this.methodName = null;
    this.javaMethods = null;
    invalidate();
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("RENAME");
  }
  
  public JavaMethod findJavaMethod(Expression[] paramArrayOfExpression)
  {
    load();
    int i = paramArrayOfExpression.length;
    for (JavaMethod localJavaMethod : this.javaMethods)
    {
      int m = localJavaMethod.getParameterCount();
      if ((m == i) || ((localJavaMethod.isVarArgs()) && (m <= i + 1))) {
        return localJavaMethod;
      }
    }
    throw DbException.get(90087, getName() + " (" + this.className + ", parameter count: " + i + ")");
  }
  
  public String getJavaClassName()
  {
    return this.className;
  }
  
  public String getJavaMethodName()
  {
    return this.methodName;
  }
  
  public JavaMethod[] getJavaMethods()
  {
    load();
    return this.javaMethods;
  }
  
  public void setDeterministic(boolean paramBoolean)
  {
    this.deterministic = paramBoolean;
  }
  
  public boolean isDeterministic()
  {
    return this.deterministic;
  }
  
  public String getSource()
  {
    return this.source;
  }
  
  static boolean isVarArgs(Method paramMethod)
  {
    if ("1.5".compareTo(SysProperties.JAVA_SPECIFICATION_VERSION) > 0) {
      return false;
    }
    try
    {
      Method localMethod = paramMethod.getClass().getMethod("isVarArgs", new Class[0]);
      Boolean localBoolean = (Boolean)localMethod.invoke(paramMethod, new Object[0]);
      return localBoolean.booleanValue();
    }
    catch (Exception localException) {}
    return false;
  }
  
  public boolean isBufferResultSetToLocalTemp()
  {
    return this.bufferResultSetToLocalTemp;
  }
  
  public static class JavaMethod
    implements Comparable<JavaMethod>
  {
    private final int id;
    private final Method method;
    private final int dataType;
    private boolean hasConnectionParam;
    private boolean varArgs;
    private Class<?> varArgClass;
    private int paramCount;
    
    JavaMethod(Method paramMethod, int paramInt)
    {
      this.method = paramMethod;
      this.id = paramInt;
      Class[] arrayOfClass = paramMethod.getParameterTypes();
      this.paramCount = arrayOfClass.length;
      if (this.paramCount > 0)
      {
        localClass = arrayOfClass[0];
        if (Connection.class.isAssignableFrom(localClass))
        {
          this.hasConnectionParam = true;
          this.paramCount -= 1;
        }
      }
      if (this.paramCount > 0)
      {
        localClass = arrayOfClass[(arrayOfClass.length - 1)];
        if ((localClass.isArray()) && (FunctionAlias.isVarArgs(paramMethod)))
        {
          this.varArgs = true;
          this.varArgClass = localClass.getComponentType();
        }
      }
      Class localClass = paramMethod.getReturnType();
      this.dataType = DataType.getTypeFromClass(localClass);
    }
    
    public String toString()
    {
      return this.method.toString();
    }
    
    public boolean hasConnectionParam()
    {
      return this.hasConnectionParam;
    }
    
    public Value getValue(Session paramSession, Expression[] paramArrayOfExpression, boolean paramBoolean)
    {
      Class[] arrayOfClass = this.method.getParameterTypes();
      Object[] arrayOfObject1 = new Object[arrayOfClass.length];
      int i = 0;
      if ((this.hasConnectionParam) && (arrayOfObject1.length > 0)) {
        arrayOfObject1[(i++)] = paramSession.createConnection(paramBoolean);
      }
      Object localObject1 = null;
      if (this.varArgs)
      {
        j = paramArrayOfExpression.length - arrayOfObject1.length + 1 + (this.hasConnectionParam ? 1 : 0);
        
        localObject1 = Array.newInstance(this.varArgClass, j);
        arrayOfObject1[(arrayOfObject1.length - 1)] = localObject1;
      }
      int j = 0;
      Object localObject2;
      Object localObject3;
      Object localObject4;
      for (int k = paramArrayOfExpression.length; j < k; i++)
      {
        bool2 = (this.varArgs) && (i >= arrayOfClass.length - 1);
        if (bool2) {
          localObject2 = this.varArgClass;
        } else {
          localObject2 = arrayOfClass[i];
        }
        int m = DataType.getTypeFromClass((Class)localObject2);
        localObject3 = paramArrayOfExpression[j].getValue(paramSession);
        if (Value.class.isAssignableFrom((Class)localObject2))
        {
          localObject4 = localObject3;
        }
        else if ((((Value)localObject3).getType() == 17) && (((Class)localObject2).isArray()) && (((Class)localObject2).getComponentType() != Object.class))
        {
          Value[] arrayOfValue = ((ValueArray)localObject3).getList();
          Object[] arrayOfObject2 = (Object[])Array.newInstance(((Class)localObject2).getComponentType(), arrayOfValue.length);
          
          int i2 = DataType.getTypeFromClass(((Class)localObject2).getComponentType());
          for (int i3 = 0; i3 < arrayOfObject2.length; i3++) {
            arrayOfObject2[i3] = arrayOfValue[i3].convertTo(i2).getObject();
          }
          localObject4 = arrayOfObject2;
        }
        else
        {
          localObject3 = ((Value)localObject3).convertTo(m);
          localObject4 = ((Value)localObject3).getObject();
        }
        if (localObject4 == null)
        {
          if (((Class)localObject2).isPrimitive()) {
            if (paramBoolean) {
              localObject4 = DataType.getDefaultForPrimitiveType((Class)localObject2);
            } else {
              return ValueNull.INSTANCE;
            }
          }
        }
        else if ((!((Class)localObject2).isAssignableFrom(localObject4.getClass())) && (!((Class)localObject2).isPrimitive())) {
          localObject4 = DataType.convertTo(paramSession.createConnection(false), (Value)localObject3, (Class)localObject2);
        }
        if (bool2) {
          Array.set(localObject1, i - arrayOfObject1.length + 1, localObject4);
        } else {
          arrayOfObject1[i] = localObject4;
        }
        j++;
      }
      boolean bool1 = paramSession.getAutoCommit();
      Value localValue1 = paramSession.getLastScopeIdentity();
      boolean bool2 = paramSession.getDatabase().getSettings().defaultConnection;
      try
      {
        paramSession.setAutoCommit(false);
        try
        {
          if (bool2) {
            Driver.setDefaultConnection(paramSession.createConnection(paramBoolean));
          }
          localObject2 = this.method.invoke(null, arrayOfObject1);
          if (localObject2 == null) {
            return ValueNull.INSTANCE;
          }
        }
        catch (InvocationTargetException localInvocationTargetException)
        {
          localObject3 = new StatementBuilder(this.method.getName());
          ((StatementBuilder)localObject3).append('(');
          for (Object localObject5 : arrayOfObject1)
          {
            ((StatementBuilder)localObject3).appendExceptFirst(", ");
            ((StatementBuilder)localObject3).append(localObject5 == null ? "null" : localObject5.toString());
          }
          ((StatementBuilder)localObject3).append(')');
          throw DbException.convertInvocation(localInvocationTargetException, ((StatementBuilder)localObject3).toString());
        }
        catch (Exception localException)
        {
          throw DbException.convert(localException);
        }
        if (Value.class.isAssignableFrom(this.method.getReturnType())) {
          return (Value)localObject2;
        }
        Value localValue2 = DataType.convertToValue(paramSession, localObject2, this.dataType);
        return localValue2.convertTo(this.dataType);
      }
      finally
      {
        paramSession.setLastScopeIdentity(localValue1);
        paramSession.setAutoCommit(bool1);
        if (bool2) {
          Driver.setDefaultConnection(null);
        }
      }
    }
    
    public Class<?>[] getColumnClasses()
    {
      return this.method.getParameterTypes();
    }
    
    public int getDataType()
    {
      return this.dataType;
    }
    
    public int getParameterCount()
    {
      return this.paramCount;
    }
    
    public boolean isVarArgs()
    {
      return this.varArgs;
    }
    
    public int compareTo(JavaMethod paramJavaMethod)
    {
      if (this.varArgs != paramJavaMethod.varArgs) {
        return this.varArgs ? 1 : -1;
      }
      if (this.paramCount != paramJavaMethod.paramCount) {
        return this.paramCount - paramJavaMethod.paramCount;
      }
      if (this.hasConnectionParam != paramJavaMethod.hasConnectionParam) {
        return this.hasConnectionParam ? 1 : -1;
      }
      return this.id - paramJavaMethod.id;
    }
  }
}
