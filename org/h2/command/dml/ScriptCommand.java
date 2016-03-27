package org.h2.command.dml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.h2.command.Parser;
import org.h2.constraint.Constraint;
import org.h2.engine.Comment;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Right;
import org.h2.engine.Role;
import org.h2.engine.Session;
import org.h2.engine.Setting;
import org.h2.engine.SysProperties;
import org.h2.engine.User;
import org.h2.engine.UserAggregate;
import org.h2.engine.UserDataType;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.schema.Constant;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.schema.Sequence;
import org.h2.schema.TriggerObject;
import org.h2.table.Column;
import org.h2.table.PlanItem;
import org.h2.table.Table;
import org.h2.util.IOUtils;
import org.h2.util.MathUtils;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.Utils;
import org.h2.value.Value;
import org.h2.value.ValueString;

public class ScriptCommand
  extends ScriptBase
{
  private Charset charset = Constants.UTF8;
  private Set<String> schemaNames;
  private Collection<Table> tables;
  private boolean passwords;
  private boolean data;
  private boolean settings;
  private boolean drop;
  private boolean simple;
  private LocalResult result;
  private String lineSeparatorString;
  private byte[] lineSeparator;
  private byte[] buffer;
  private boolean tempLobTableCreated;
  private int nextLobId;
  private int lobBlockSize = 4096;
  
  public ScriptCommand(Session paramSession)
  {
    super(paramSession);
  }
  
  public boolean isQuery()
  {
    return true;
  }
  
  public void setSchemaNames(Set<String> paramSet)
  {
    this.schemaNames = paramSet;
  }
  
  public void setTables(Collection<Table> paramCollection)
  {
    this.tables = paramCollection;
  }
  
  public void setData(boolean paramBoolean)
  {
    this.data = paramBoolean;
  }
  
  public void setPasswords(boolean paramBoolean)
  {
    this.passwords = paramBoolean;
  }
  
  public void setSettings(boolean paramBoolean)
  {
    this.settings = paramBoolean;
  }
  
  public void setLobBlockSize(long paramLong)
  {
    this.lobBlockSize = MathUtils.convertLongToInt(paramLong);
  }
  
  public void setDrop(boolean paramBoolean)
  {
    this.drop = paramBoolean;
  }
  
  public ResultInterface queryMeta()
  {
    LocalResult localLocalResult = createResult();
    localLocalResult.done();
    return localLocalResult;
  }
  
  private LocalResult createResult()
  {
    Expression[] arrayOfExpression = { new ExpressionColumn(this.session.getDatabase(), new Column("SCRIPT", 13)) };
    
    return new LocalResult(this.session, arrayOfExpression, 1);
  }
  
  public ResultInterface query(int paramInt)
  {
    this.session.getUser().checkAdmin();
    reset();
    Database localDatabase = this.session.getDatabase();
    Object localObject1;
    if (this.schemaNames != null) {
      for (localObject1 = this.schemaNames.iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (String)((Iterator)localObject1).next();
        localObject3 = localDatabase.findSchema((String)localObject2);
        if (localObject3 == null) {
          throw DbException.get(90079, (String)localObject2);
        }
      }
    }
    try
    {
      this.result = createResult();
      deleteStore();
      openOutput();
      if (this.out != null) {
        this.buffer = new byte['က'];
      }
      if (this.settings) {
        for (localObject1 = localDatabase.getAllSettings().iterator(); ((Iterator)localObject1).hasNext();)
        {
          localObject2 = (Setting)((Iterator)localObject1).next();
          if (!((Setting)localObject2).getName().equals(SetTypes.getTypeName(34))) {
            add(((Setting)localObject2).getCreateSQL(), false);
          }
        }
      }
      if (this.out != null) {
        add("", true);
      }
      for (localObject1 = localDatabase.getAllUsers().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (User)((Iterator)localObject1).next();
        add(((User)localObject2).getCreateSQL(this.passwords), false);
      }
      for (localObject1 = localDatabase.getAllRoles().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (Role)((Iterator)localObject1).next();
        add(((Role)localObject2).getCreateSQL(true), false);
      }
      for (localObject1 = localDatabase.getAllSchemas().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (Schema)((Iterator)localObject1).next();
        if (!excludeSchema((Schema)localObject2)) {
          add(((Schema)localObject2).getCreateSQL(), false);
        }
      }
      for (localObject1 = localDatabase.getAllUserDataTypes().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (UserDataType)((Iterator)localObject1).next();
        if (this.drop) {
          add(((UserDataType)localObject2).getDropSQL(), false);
        }
        add(((UserDataType)localObject2).getCreateSQL(), false);
      }
      for (localObject1 = localDatabase.getAllSchemaObjects(11).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (SchemaObject)((Iterator)localObject1).next();
        if (!excludeSchema(((SchemaObject)localObject2).getSchema()))
        {
          localObject3 = (Constant)localObject2;
          add(((Constant)localObject3).getCreateSQL(), false);
        }
      }
      localObject1 = localDatabase.getAllTablesAndViews(false);
      
      Collections.sort((List)localObject1, new Comparator()
      {
        public int compare(Table paramAnonymousTable1, Table paramAnonymousTable2)
        {
          return paramAnonymousTable1.getId() - paramAnonymousTable2.getId();
        }
      });
      for (Object localObject2 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject2).hasNext();)
      {
        localObject3 = (Table)((Iterator)localObject2).next();
        if ((!excludeSchema(((Table)localObject3).getSchema())) && 
        
          (!excludeTable((Table)localObject3)) && 
          
          (!((Table)localObject3).isHidden()))
        {
          ((Table)localObject3).lock(this.session, false, false);
          localObject4 = ((Table)localObject3).getCreateSQL();
          if (localObject4 != null) {
            if (this.drop) {
              add(((Table)localObject3).getDropSQL(), false);
            }
          }
        }
      }
      for (localObject2 = localDatabase.getAllSchemaObjects(9).iterator(); ((Iterator)localObject2).hasNext();)
      {
        localObject3 = (SchemaObject)((Iterator)localObject2).next();
        if (!excludeSchema(((SchemaObject)localObject3).getSchema()))
        {
          if (this.drop) {
            add(((SchemaObject)localObject3).getDropSQL(), false);
          }
          add(((SchemaObject)localObject3).getCreateSQL(), false);
        }
      }
      for (localObject2 = localDatabase.getAllAggregates().iterator(); ((Iterator)localObject2).hasNext();)
      {
        localObject3 = (UserAggregate)((Iterator)localObject2).next();
        if (this.drop) {
          add(((UserAggregate)localObject3).getDropSQL(), false);
        }
        add(((UserAggregate)localObject3).getCreateSQL(), false);
      }
      for (localObject2 = localDatabase.getAllSchemaObjects(3).iterator(); ((Iterator)localObject2).hasNext();)
      {
        localObject3 = (SchemaObject)((Iterator)localObject2).next();
        if (!excludeSchema(((SchemaObject)localObject3).getSchema()))
        {
          localObject4 = (Sequence)localObject3;
          if (this.drop) {
            add(((Sequence)localObject4).getDropSQL(), false);
          }
          add(((Sequence)localObject4).getCreateSQL(), false);
        }
      }
      int i = 0;
      for (Object localObject3 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (Table)((Iterator)localObject3).next();
        if ((!excludeSchema(((Table)localObject4).getSchema())) && 
        
          (!excludeTable((Table)localObject4)) && 
          
          (!((Table)localObject4).isHidden()))
        {
          ((Table)localObject4).lock(this.session, false, false);
          localObject5 = ((Table)localObject4).getCreateSQL();
          if (localObject5 != null)
          {
            localObject6 = ((Table)localObject4).getTableType();
            add((String)localObject5, false);
            ArrayList localArrayList = ((Table)localObject4).getConstraints();
            if (localArrayList != null) {
              for (localObject7 = localArrayList.iterator(); ((Iterator)localObject7).hasNext();)
              {
                Constraint localConstraint = (Constraint)((Iterator)localObject7).next();
                if ("PRIMARY KEY".equals(localConstraint.getConstraintType())) {
                  add(localConstraint.getCreateSQLWithoutIndexes(), false);
                }
              }
            }
            if ("TABLE".equals(localObject6))
            {
              if (((Table)localObject4).canGetRowCount())
              {
                localObject7 = "-- " + ((Table)localObject4).getRowCountApproximation() + " +/- SELECT COUNT(*) FROM " + ((Table)localObject4).getSQL();
                
                add((String)localObject7, false);
              }
              if (this.data) {
                i = generateInsertValues(i, (Table)localObject4);
              }
            }
            Object localObject7 = ((Table)localObject4).getIndexes();
            for (int j = 0; (localObject7 != null) && (j < ((ArrayList)localObject7).size()); j++)
            {
              Index localIndex = (Index)((ArrayList)localObject7).get(j);
              if (!localIndex.getIndexType().getBelongsToConstraint()) {
                add(localIndex.getCreateSQL(), false);
              }
            }
          }
        }
      }
      Object localObject5;
      Object localObject6;
      if (this.tempLobTableCreated)
      {
        add("DROP TABLE IF EXISTS SYSTEM_LOB_STREAM", true);
        add("CALL SYSTEM_COMBINE_BLOB(-1)", true);
        add("DROP ALIAS IF EXISTS SYSTEM_COMBINE_CLOB", true);
        add("DROP ALIAS IF EXISTS SYSTEM_COMBINE_BLOB", true);
        this.tempLobTableCreated = false;
      }
      localObject3 = localDatabase.getAllSchemaObjects(5);
      
      Collections.sort((List)localObject3, new Comparator()
      {
        public int compare(SchemaObject paramAnonymousSchemaObject1, SchemaObject paramAnonymousSchemaObject2)
        {
          return ((Constraint)paramAnonymousSchemaObject1).compareTo((Constraint)paramAnonymousSchemaObject2);
        }
      });
      for (Object localObject4 = ((ArrayList)localObject3).iterator(); ((Iterator)localObject4).hasNext();)
      {
        localObject5 = (SchemaObject)((Iterator)localObject4).next();
        if (!excludeSchema(((SchemaObject)localObject5).getSchema()))
        {
          localObject6 = (Constraint)localObject5;
          if ((!excludeTable(((Constraint)localObject6).getTable())) && 
          
            (!((Constraint)localObject6).getTable().isHidden())) {
            if (!"PRIMARY KEY".equals(((Constraint)localObject6).getConstraintType())) {
              add(((Constraint)localObject6).getCreateSQLWithoutIndexes(), false);
            }
          }
        }
      }
      for (localObject4 = localDatabase.getAllSchemaObjects(4).iterator(); ((Iterator)localObject4).hasNext();)
      {
        localObject5 = (SchemaObject)((Iterator)localObject4).next();
        if (!excludeSchema(((SchemaObject)localObject5).getSchema()))
        {
          localObject6 = (TriggerObject)localObject5;
          if (!excludeTable(((TriggerObject)localObject6).getTable())) {
            add(((TriggerObject)localObject6).getCreateSQL(), false);
          }
        }
      }
      for (localObject4 = localDatabase.getAllRights().iterator(); ((Iterator)localObject4).hasNext();)
      {
        localObject5 = (Right)((Iterator)localObject4).next();
        localObject6 = ((Right)localObject5).getGrantedTable();
        if ((localObject6 == null) || (
          (!excludeSchema(((Table)localObject6).getSchema())) && 
          
          (!excludeTable((Table)localObject6)))) {
          add(((Right)localObject5).getCreateSQL(), false);
        }
      }
      for (localObject4 = localDatabase.getAllComments().iterator(); ((Iterator)localObject4).hasNext();)
      {
        localObject5 = (Comment)((Iterator)localObject4).next();
        add(((Comment)localObject5).getCreateSQL(), false);
      }
      if (this.out != null) {
        this.out.close();
      }
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, getFileName());
    }
    finally
    {
      closeIO();
    }
    this.result.done();
    LocalResult localLocalResult = this.result;
    reset();
    return localLocalResult;
  }
  
  private int generateInsertValues(int paramInt, Table paramTable)
    throws IOException
  {
    PlanItem localPlanItem = paramTable.getBestPlanItem(this.session, null, null, null);
    Index localIndex = localPlanItem.getIndex();
    Cursor localCursor = localIndex.find(this.session, null, null);
    Column[] arrayOfColumn = paramTable.getColumns();
    StatementBuilder localStatementBuilder = new StatementBuilder("INSERT INTO ");
    localStatementBuilder.append(paramTable.getSQL()).append('(');
    Value localValue;
    for (localValue : arrayOfColumn)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(Parser.quoteIdentifier(localValue.getName()));
    }
    localStatementBuilder.append(") VALUES");
    if (!this.simple) {
      localStatementBuilder.append('\n');
    }
    localStatementBuilder.append('(');
    ??? = localStatementBuilder.toString();
    localStatementBuilder = null;
    while (localCursor.next())
    {
      Row localRow = localCursor.get();
      if (localStatementBuilder == null) {
        localStatementBuilder = new StatementBuilder((String)???);
      } else {
        localStatementBuilder.append(",\n(");
      }
      for (??? = 0; ??? < localRow.getColumnCount(); ???++)
      {
        if (??? > 0) {
          localStatementBuilder.append(", ");
        }
        localValue = localRow.getValue(???);
        if (localValue.getPrecision() > this.lobBlockSize)
        {
          int k;
          if (localValue.getType() == 16)
          {
            k = writeLobStream(localValue);
            localStatementBuilder.append("SYSTEM_COMBINE_CLOB(" + k + ")");
          }
          else if (localValue.getType() == 15)
          {
            k = writeLobStream(localValue);
            localStatementBuilder.append("SYSTEM_COMBINE_BLOB(" + k + ")");
          }
          else
          {
            localStatementBuilder.append(localValue.getSQL());
          }
        }
        else
        {
          localStatementBuilder.append(localValue.getSQL());
        }
      }
      localStatementBuilder.append(')');
      paramInt++;
      if ((paramInt & 0x7F) == 0) {
        checkCanceled();
      }
      if ((this.simple) || (localStatementBuilder.length() > 4096))
      {
        add(localStatementBuilder.toString(), true);
        localStatementBuilder = null;
      }
    }
    if (localStatementBuilder != null) {
      add(localStatementBuilder.toString(), true);
    }
    return paramInt;
  }
  
  private int writeLobStream(Value paramValue)
    throws IOException
  {
    if (!this.tempLobTableCreated)
    {
      add("CREATE TABLE IF NOT EXISTS SYSTEM_LOB_STREAM(ID INT NOT NULL, PART INT NOT NULL, CDATA VARCHAR, BDATA BINARY)", true);
      
      add("CREATE PRIMARY KEY SYSTEM_LOB_STREAM_PRIMARY_KEY ON SYSTEM_LOB_STREAM(ID, PART)", true);
      
      add("CREATE ALIAS IF NOT EXISTS SYSTEM_COMBINE_CLOB FOR \"" + getClass().getName() + ".combineClob\"", true);
      
      add("CREATE ALIAS IF NOT EXISTS SYSTEM_COMBINE_BLOB FOR \"" + getClass().getName() + ".combineBlob\"", true);
      
      this.tempLobTableCreated = true;
    }
    int i = this.nextLobId++;
    Object localObject1;
    Object localObject2;
    int j;
    StringBuilder localStringBuilder;
    int k;
    String str;
    switch (paramValue.getType())
    {
    case 15: 
      localObject1 = new byte[this.lobBlockSize];
      localObject2 = paramValue.getInputStream();
      try
      {
        for (j = 0;; j++)
        {
          localStringBuilder = new StringBuilder(this.lobBlockSize * 2);
          localStringBuilder.append("INSERT INTO SYSTEM_LOB_STREAM VALUES(" + i + ", " + j + ", NULL, '");
          
          k = IOUtils.readFully((InputStream)localObject2, (byte[])localObject1, this.lobBlockSize);
          if (k <= 0) {
            break;
          }
          localStringBuilder.append(StringUtils.convertBytesToHex((byte[])localObject1, k)).append("')");
          str = localStringBuilder.toString();
          add(str, true);
        }
      }
      finally
      {
        IOUtils.closeSilently((InputStream)localObject2);
      }
      break;
    case 16: 
      localObject1 = new char[this.lobBlockSize];
      localObject2 = paramValue.getReader();
      try
      {
        for (j = 0;; j++)
        {
          localStringBuilder = new StringBuilder(this.lobBlockSize * 2);
          localStringBuilder.append("INSERT INTO SYSTEM_LOB_STREAM VALUES(" + i + ", " + j + ", ");
          k = IOUtils.readFully((Reader)localObject2, (char[])localObject1, this.lobBlockSize);
          if (k == 0) {
            break;
          }
          localStringBuilder.append(StringUtils.quoteStringSQL(new String((char[])localObject1, 0, k))).append(", NULL)");
          
          str = localStringBuilder.toString();
          add(str, true);
        }
      }
      finally
      {
        IOUtils.closeSilently((Reader)localObject2);
      }
      break;
    default: 
      DbException.throwInternalError("type:" + paramValue.getType());
    }
    return i;
  }
  
  public static InputStream combineBlob(Connection paramConnection, int paramInt)
    throws SQLException
  {
    if (paramInt < 0) {
      return null;
    }
    ResultSet localResultSet = getLobStream(paramConnection, "BDATA", paramInt);
    new InputStream()
    {
      private InputStream current;
      private boolean closed;
      
      /* Error */
      public int read()
        throws IOException
      {
        // Byte code:
        //   0: aload_0
        //   1: getfield 3	org/h2/command/dml/ScriptCommand$3:current	Ljava/io/InputStream;
        //   4: ifnonnull +59 -> 63
        //   7: aload_0
        //   8: getfield 4	org/h2/command/dml/ScriptCommand$3:closed	Z
        //   11: ifeq +5 -> 16
        //   14: iconst_m1
        //   15: ireturn
        //   16: aload_0
        //   17: getfield 1	org/h2/command/dml/ScriptCommand$3:val$rs	Ljava/sql/ResultSet;
        //   20: invokeinterface 5 1 0
        //   25: ifne +9 -> 34
        //   28: aload_0
        //   29: invokevirtual 6	org/h2/command/dml/ScriptCommand$3:close	()V
        //   32: iconst_m1
        //   33: ireturn
        //   34: aload_0
        //   35: aload_0
        //   36: getfield 1	org/h2/command/dml/ScriptCommand$3:val$rs	Ljava/sql/ResultSet;
        //   39: iconst_1
        //   40: invokeinterface 7 2 0
        //   45: putfield 3	org/h2/command/dml/ScriptCommand$3:current	Ljava/io/InputStream;
        //   48: aload_0
        //   49: new 8	java/io/BufferedInputStream
        //   52: dup
        //   53: aload_0
        //   54: getfield 3	org/h2/command/dml/ScriptCommand$3:current	Ljava/io/InputStream;
        //   57: invokespecial 9	java/io/BufferedInputStream:<init>	(Ljava/io/InputStream;)V
        //   60: putfield 3	org/h2/command/dml/ScriptCommand$3:current	Ljava/io/InputStream;
        //   63: aload_0
        //   64: getfield 3	org/h2/command/dml/ScriptCommand$3:current	Ljava/io/InputStream;
        //   67: invokevirtual 10	java/io/InputStream:read	()I
        //   70: istore_1
        //   71: iload_1
        //   72: iflt +5 -> 77
        //   75: iload_1
        //   76: ireturn
        //   77: aload_0
        //   78: aconst_null
        //   79: putfield 3	org/h2/command/dml/ScriptCommand$3:current	Ljava/io/InputStream;
        //   82: goto -82 -> 0
        //   85: astore_1
        //   86: aload_1
        //   87: invokestatic 12	org/h2/message/DbException:convertToIOException	(Ljava/lang/Throwable;)Ljava/io/IOException;
        //   90: athrow
        // Line number table:
        //   Java source line #531	-> byte code offset #0
        //   Java source line #532	-> byte code offset #7
        //   Java source line #533	-> byte code offset #14
        //   Java source line #535	-> byte code offset #16
        //   Java source line #536	-> byte code offset #28
        //   Java source line #537	-> byte code offset #32
        //   Java source line #539	-> byte code offset #34
        //   Java source line #540	-> byte code offset #48
        //   Java source line #542	-> byte code offset #63
        //   Java source line #543	-> byte code offset #71
        //   Java source line #544	-> byte code offset #75
        //   Java source line #546	-> byte code offset #77
        //   Java source line #549	-> byte code offset #82
        //   Java source line #547	-> byte code offset #85
        //   Java source line #548	-> byte code offset #86
        // Local variable table:
        //   start	length	slot	name	signature
        //   0	91	0	this	3
        //   70	6	1	i	int
        //   85	2	1	localSQLException	SQLException
        // Exception table:
        //   from	to	target	type
        //   0	15	85	java/sql/SQLException
        //   16	33	85	java/sql/SQLException
        //   34	76	85	java/sql/SQLException
        //   77	82	85	java/sql/SQLException
      }
      
      public void close()
        throws IOException
      {
        if (this.closed) {
          return;
        }
        this.closed = true;
        try
        {
          this.val$rs.close();
        }
        catch (SQLException localSQLException)
        {
          throw DbException.convertToIOException(localSQLException);
        }
      }
    };
  }
  
  public static Reader combineClob(Connection paramConnection, int paramInt)
    throws SQLException
  {
    if (paramInt < 0) {
      return null;
    }
    ResultSet localResultSet = getLobStream(paramConnection, "CDATA", paramInt);
    new Reader()
    {
      private Reader current;
      private boolean closed;
      
      /* Error */
      public int read()
        throws IOException
      {
        // Byte code:
        //   0: aload_0
        //   1: getfield 3	org/h2/command/dml/ScriptCommand$4:current	Ljava/io/Reader;
        //   4: ifnonnull +59 -> 63
        //   7: aload_0
        //   8: getfield 4	org/h2/command/dml/ScriptCommand$4:closed	Z
        //   11: ifeq +5 -> 16
        //   14: iconst_m1
        //   15: ireturn
        //   16: aload_0
        //   17: getfield 1	org/h2/command/dml/ScriptCommand$4:val$rs	Ljava/sql/ResultSet;
        //   20: invokeinterface 5 1 0
        //   25: ifne +9 -> 34
        //   28: aload_0
        //   29: invokevirtual 6	org/h2/command/dml/ScriptCommand$4:close	()V
        //   32: iconst_m1
        //   33: ireturn
        //   34: aload_0
        //   35: aload_0
        //   36: getfield 1	org/h2/command/dml/ScriptCommand$4:val$rs	Ljava/sql/ResultSet;
        //   39: iconst_1
        //   40: invokeinterface 7 2 0
        //   45: putfield 3	org/h2/command/dml/ScriptCommand$4:current	Ljava/io/Reader;
        //   48: aload_0
        //   49: new 8	java/io/BufferedReader
        //   52: dup
        //   53: aload_0
        //   54: getfield 3	org/h2/command/dml/ScriptCommand$4:current	Ljava/io/Reader;
        //   57: invokespecial 9	java/io/BufferedReader:<init>	(Ljava/io/Reader;)V
        //   60: putfield 3	org/h2/command/dml/ScriptCommand$4:current	Ljava/io/Reader;
        //   63: aload_0
        //   64: getfield 3	org/h2/command/dml/ScriptCommand$4:current	Ljava/io/Reader;
        //   67: invokevirtual 10	java/io/Reader:read	()I
        //   70: istore_1
        //   71: iload_1
        //   72: iflt +5 -> 77
        //   75: iload_1
        //   76: ireturn
        //   77: aload_0
        //   78: aconst_null
        //   79: putfield 3	org/h2/command/dml/ScriptCommand$4:current	Ljava/io/Reader;
        //   82: goto -82 -> 0
        //   85: astore_1
        //   86: aload_1
        //   87: invokestatic 12	org/h2/message/DbException:convertToIOException	(Ljava/lang/Throwable;)Ljava/io/IOException;
        //   90: athrow
        // Line number table:
        //   Java source line #587	-> byte code offset #0
        //   Java source line #588	-> byte code offset #7
        //   Java source line #589	-> byte code offset #14
        //   Java source line #591	-> byte code offset #16
        //   Java source line #592	-> byte code offset #28
        //   Java source line #593	-> byte code offset #32
        //   Java source line #595	-> byte code offset #34
        //   Java source line #596	-> byte code offset #48
        //   Java source line #598	-> byte code offset #63
        //   Java source line #599	-> byte code offset #71
        //   Java source line #600	-> byte code offset #75
        //   Java source line #602	-> byte code offset #77
        //   Java source line #605	-> byte code offset #82
        //   Java source line #603	-> byte code offset #85
        //   Java source line #604	-> byte code offset #86
        // Local variable table:
        //   start	length	slot	name	signature
        //   0	91	0	this	4
        //   70	6	1	i	int
        //   85	2	1	localSQLException	SQLException
        // Exception table:
        //   from	to	target	type
        //   0	15	85	java/sql/SQLException
        //   16	33	85	java/sql/SQLException
        //   34	76	85	java/sql/SQLException
        //   77	82	85	java/sql/SQLException
      }
      
      public void close()
        throws IOException
      {
        if (this.closed) {
          return;
        }
        this.closed = true;
        try
        {
          this.val$rs.close();
        }
        catch (SQLException localSQLException)
        {
          throw DbException.convertToIOException(localSQLException);
        }
      }
      
      public int read(char[] paramAnonymousArrayOfChar, int paramAnonymousInt1, int paramAnonymousInt2)
        throws IOException
      {
        if (paramAnonymousInt2 == 0) {
          return 0;
        }
        int i = read();
        if (i == -1) {
          return -1;
        }
        paramAnonymousArrayOfChar[paramAnonymousInt1] = ((char)i);
        for (int j = 1; j < paramAnonymousInt2; j++)
        {
          i = read();
          if (i == -1) {
            break;
          }
          paramAnonymousArrayOfChar[(paramAnonymousInt1 + j)] = ((char)i);
        }
        return j;
      }
    };
  }
  
  private static ResultSet getLobStream(Connection paramConnection, String paramString, int paramInt)
    throws SQLException
  {
    PreparedStatement localPreparedStatement = paramConnection.prepareStatement("SELECT " + paramString + " FROM SYSTEM_LOB_STREAM WHERE ID=? ORDER BY PART");
    
    localPreparedStatement.setInt(1, paramInt);
    return localPreparedStatement.executeQuery();
  }
  
  private void reset()
  {
    this.result = null;
    this.buffer = null;
    this.lineSeparatorString = SysProperties.LINE_SEPARATOR;
    this.lineSeparator = this.lineSeparatorString.getBytes(this.charset);
  }
  
  private boolean excludeSchema(Schema paramSchema)
  {
    if ((this.schemaNames != null) && (!this.schemaNames.contains(paramSchema.getName()))) {
      return true;
    }
    if (this.tables != null)
    {
      for (Table localTable : paramSchema.getAllTablesAndViews()) {
        if (this.tables.contains(localTable)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
  
  private boolean excludeTable(Table paramTable)
  {
    return (this.tables != null) && (!this.tables.contains(paramTable));
  }
  
  private void add(String paramString, boolean paramBoolean)
    throws IOException
  {
    if (paramString == null) {
      return;
    }
    if ((this.lineSeparator.length > 1) || (this.lineSeparator[0] != 10)) {
      paramString = StringUtils.replaceAll(paramString, "\n", this.lineSeparatorString);
    }
    paramString = paramString + ";";
    Object localObject;
    if (this.out != null)
    {
      localObject = paramString.getBytes(this.charset);
      int i = MathUtils.roundUpInt(localObject.length + this.lineSeparator.length, 16);
      
      this.buffer = Utils.copy((byte[])localObject, this.buffer);
      if (i > this.buffer.length) {
        this.buffer = new byte[i];
      }
      System.arraycopy(localObject, 0, this.buffer, 0, localObject.length);
      for (int j = localObject.length; j < i - this.lineSeparator.length; j++) {
        this.buffer[j] = 32;
      }
      j = 0;
      for (int k = i - this.lineSeparator.length; k < i; j++)
      {
        this.buffer[k] = this.lineSeparator[j];k++;
      }
      this.out.write(this.buffer, 0, i);
      if (!paramBoolean)
      {
        Value[] arrayOfValue = { ValueString.get(paramString) };
        this.result.addRow(arrayOfValue);
      }
    }
    else
    {
      localObject = new Value[] { ValueString.get(paramString) };
      this.result.addRow((Value[])localObject);
    }
  }
  
  public void setSimple(boolean paramBoolean)
  {
    this.simple = paramBoolean;
  }
  
  public void setCharset(Charset paramCharset)
  {
    this.charset = paramCharset;
  }
  
  public int getType()
  {
    return 65;
  }
}
