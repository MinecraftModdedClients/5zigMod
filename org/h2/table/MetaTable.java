package org.h2.table;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.h2.command.Command;
import org.h2.constraint.Constraint;
import org.h2.constraint.ConstraintCheck;
import org.h2.constraint.ConstraintReferential;
import org.h2.constraint.ConstraintUnique;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.DbSettings;
import org.h2.engine.FunctionAlias;
import org.h2.engine.FunctionAlias.JavaMethod;
import org.h2.engine.Mode;
import org.h2.engine.QueryStatisticsData;
import org.h2.engine.QueryStatisticsData.QueryEntry;
import org.h2.engine.Right;
import org.h2.engine.Role;
import org.h2.engine.Session;
import org.h2.engine.Setting;
import org.h2.engine.User;
import org.h2.engine.UserAggregate;
import org.h2.engine.UserDataType;
import org.h2.expression.Expression;
import org.h2.expression.ValueExpression;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.MetaIndex;
import org.h2.index.MultiVersionIndex;
import org.h2.message.DbException;
import org.h2.mvstore.FileStore;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.MVTableEngine.Store;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.schema.Constant;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.schema.Sequence;
import org.h2.schema.TriggerObject;
import org.h2.store.InDoubtTransaction;
import org.h2.store.PageStore;
import org.h2.tools.Csv;
import org.h2.util.Cache;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.Utils;
import org.h2.value.CompareMode;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;
import org.h2.value.ValueStringIgnoreCase;

public class MetaTable
  extends Table
{
  public static final long ROW_COUNT_APPROXIMATION = 1000L;
  private static final String CHARACTER_SET_NAME = "Unicode";
  private static final int TABLES = 0;
  private static final int COLUMNS = 1;
  private static final int INDEXES = 2;
  private static final int TABLE_TYPES = 3;
  private static final int TYPE_INFO = 4;
  private static final int CATALOGS = 5;
  private static final int SETTINGS = 6;
  private static final int HELP = 7;
  private static final int SEQUENCES = 8;
  private static final int USERS = 9;
  private static final int ROLES = 10;
  private static final int RIGHTS = 11;
  private static final int FUNCTION_ALIASES = 12;
  private static final int SCHEMATA = 13;
  private static final int TABLE_PRIVILEGES = 14;
  private static final int COLUMN_PRIVILEGES = 15;
  private static final int COLLATIONS = 16;
  private static final int VIEWS = 17;
  private static final int IN_DOUBT = 18;
  private static final int CROSS_REFERENCES = 19;
  private static final int CONSTRAINTS = 20;
  private static final int FUNCTION_COLUMNS = 21;
  private static final int CONSTANTS = 22;
  private static final int DOMAINS = 23;
  private static final int TRIGGERS = 24;
  private static final int SESSIONS = 25;
  private static final int LOCKS = 26;
  private static final int SESSION_STATE = 27;
  private static final int QUERY_STATISTICS = 28;
  private static final int META_TABLE_TYPE_COUNT = 29;
  private final int type;
  private final int indexColumn;
  private final MetaIndex metaIndex;
  
  public MetaTable(Schema paramSchema, int paramInt1, int paramInt2)
  {
    super(paramSchema, paramInt1, null, true, true);
    this.type = paramInt2;
    
    String str = null;
    Column[] arrayOfColumn;
    switch (paramInt2)
    {
    case 0: 
      setObjectName("TABLES");
      arrayOfColumn = createColumns(new String[] { "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "TABLE_TYPE", "STORAGE_TYPE", "SQL", "REMARKS", "LAST_MODIFICATION BIGINT", "ID INT", "TYPE_NAME", "TABLE_CLASS", "ROW_COUNT_ESTIMATE BIGINT" });
      
      str = "TABLE_NAME";
      break;
    case 1: 
      setObjectName("COLUMNS");
      arrayOfColumn = createColumns(new String[] { "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "COLUMN_NAME", "ORDINAL_POSITION INT", "COLUMN_DEFAULT", "IS_NULLABLE", "DATA_TYPE INT", "CHARACTER_MAXIMUM_LENGTH INT", "CHARACTER_OCTET_LENGTH INT", "NUMERIC_PRECISION INT", "NUMERIC_PRECISION_RADIX INT", "NUMERIC_SCALE INT", "CHARACTER_SET_NAME", "COLLATION_NAME", "TYPE_NAME", "NULLABLE INT", "IS_COMPUTED BIT", "SELECTIVITY INT", "CHECK_CONSTRAINT", "SEQUENCE_NAME", "REMARKS", "SOURCE_DATA_TYPE SMALLINT" });
      
      str = "TABLE_NAME";
      break;
    case 2: 
      setObjectName("INDEXES");
      arrayOfColumn = createColumns(new String[] { "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "NON_UNIQUE BIT", "INDEX_NAME", "ORDINAL_POSITION SMALLINT", "COLUMN_NAME", "CARDINALITY INT", "PRIMARY_KEY BIT", "INDEX_TYPE_NAME", "IS_GENERATED BIT", "INDEX_TYPE SMALLINT", "ASC_OR_DESC", "PAGES INT", "FILTER_CONDITION", "REMARKS", "SQL", "ID INT", "SORT_TYPE INT", "CONSTRAINT_NAME", "INDEX_CLASS" });
      
      str = "TABLE_NAME";
      break;
    case 3: 
      setObjectName("TABLE_TYPES");
      arrayOfColumn = createColumns(new String[] { "TYPE" });
      break;
    case 4: 
      setObjectName("TYPE_INFO");
      arrayOfColumn = createColumns(new String[] { "TYPE_NAME", "DATA_TYPE INT", "PRECISION INT", "PREFIX", "SUFFIX", "PARAMS", "AUTO_INCREMENT BIT", "MINIMUM_SCALE SMALLINT", "MAXIMUM_SCALE SMALLINT", "RADIX INT", "POS INT", "CASE_SENSITIVE BIT", "NULLABLE SMALLINT", "SEARCHABLE SMALLINT" });
      
      break;
    case 5: 
      setObjectName("CATALOGS");
      arrayOfColumn = createColumns(new String[] { "CATALOG_NAME" });
      break;
    case 6: 
      setObjectName("SETTINGS");
      arrayOfColumn = createColumns(new String[] { "NAME", "VALUE" });
      break;
    case 7: 
      setObjectName("HELP");
      arrayOfColumn = createColumns(new String[] { "ID INT", "SECTION", "TOPIC", "SYNTAX", "TEXT" });
      
      break;
    case 8: 
      setObjectName("SEQUENCES");
      arrayOfColumn = createColumns(new String[] { "SEQUENCE_CATALOG", "SEQUENCE_SCHEMA", "SEQUENCE_NAME", "CURRENT_VALUE BIGINT", "INCREMENT BIGINT", "IS_GENERATED BIT", "REMARKS", "CACHE BIGINT", "MIN_VALUE BIGINT", "MAX_VALUE BIGINT", "IS_CYCLE BIT", "ID INT" });
      
      break;
    case 9: 
      setObjectName("USERS");
      arrayOfColumn = createColumns(new String[] { "NAME", "ADMIN", "REMARKS", "ID INT" });
      
      break;
    case 10: 
      setObjectName("ROLES");
      arrayOfColumn = createColumns(new String[] { "NAME", "REMARKS", "ID INT" });
      
      break;
    case 11: 
      setObjectName("RIGHTS");
      arrayOfColumn = createColumns(new String[] { "GRANTEE", "GRANTEETYPE", "GRANTEDROLE", "RIGHTS", "TABLE_SCHEMA", "TABLE_NAME", "ID INT" });
      
      str = "TABLE_NAME";
      break;
    case 12: 
      setObjectName("FUNCTION_ALIASES");
      arrayOfColumn = createColumns(new String[] { "ALIAS_CATALOG", "ALIAS_SCHEMA", "ALIAS_NAME", "JAVA_CLASS", "JAVA_METHOD", "DATA_TYPE INT", "TYPE_NAME", "COLUMN_COUNT INT", "RETURNS_RESULT SMALLINT", "REMARKS", "ID INT", "SOURCE" });
      
      break;
    case 21: 
      setObjectName("FUNCTION_COLUMNS");
      arrayOfColumn = createColumns(new String[] { "ALIAS_CATALOG", "ALIAS_SCHEMA", "ALIAS_NAME", "JAVA_CLASS", "JAVA_METHOD", "COLUMN_COUNT INT", "POS INT", "COLUMN_NAME", "DATA_TYPE INT", "TYPE_NAME", "PRECISION INT", "SCALE SMALLINT", "RADIX SMALLINT", "NULLABLE SMALLINT", "COLUMN_TYPE SMALLINT", "REMARKS", "COLUMN_DEFAULT" });
      
      break;
    case 13: 
      setObjectName("SCHEMATA");
      arrayOfColumn = createColumns(new String[] { "CATALOG_NAME", "SCHEMA_NAME", "SCHEMA_OWNER", "DEFAULT_CHARACTER_SET_NAME", "DEFAULT_COLLATION_NAME", "IS_DEFAULT BIT", "REMARKS", "ID INT" });
      
      break;
    case 14: 
      setObjectName("TABLE_PRIVILEGES");
      arrayOfColumn = createColumns(new String[] { "GRANTOR", "GRANTEE", "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "PRIVILEGE_TYPE", "IS_GRANTABLE" });
      
      str = "TABLE_NAME";
      break;
    case 15: 
      setObjectName("COLUMN_PRIVILEGES");
      arrayOfColumn = createColumns(new String[] { "GRANTOR", "GRANTEE", "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "COLUMN_NAME", "PRIVILEGE_TYPE", "IS_GRANTABLE" });
      
      str = "TABLE_NAME";
      break;
    case 16: 
      setObjectName("COLLATIONS");
      arrayOfColumn = createColumns(new String[] { "NAME", "KEY" });
      
      break;
    case 17: 
      setObjectName("VIEWS");
      arrayOfColumn = createColumns(new String[] { "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "VIEW_DEFINITION", "CHECK_OPTION", "IS_UPDATABLE", "STATUS", "REMARKS", "ID INT" });
      
      str = "TABLE_NAME";
      break;
    case 18: 
      setObjectName("IN_DOUBT");
      arrayOfColumn = createColumns(new String[] { "TRANSACTION", "STATE" });
      
      break;
    case 19: 
      setObjectName("CROSS_REFERENCES");
      arrayOfColumn = createColumns(new String[] { "PKTABLE_CATALOG", "PKTABLE_SCHEMA", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CATALOG", "FKTABLE_SCHEMA", "FKTABLE_NAME", "FKCOLUMN_NAME", "ORDINAL_POSITION SMALLINT", "UPDATE_RULE SMALLINT", "DELETE_RULE SMALLINT", "FK_NAME", "PK_NAME", "DEFERRABILITY SMALLINT" });
      
      str = "PKTABLE_NAME";
      break;
    case 20: 
      setObjectName("CONSTRAINTS");
      arrayOfColumn = createColumns(new String[] { "CONSTRAINT_CATALOG", "CONSTRAINT_SCHEMA", "CONSTRAINT_NAME", "CONSTRAINT_TYPE", "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "UNIQUE_INDEX_NAME", "CHECK_EXPRESSION", "COLUMN_LIST", "REMARKS", "SQL", "ID INT" });
      
      str = "TABLE_NAME";
      break;
    case 22: 
      setObjectName("CONSTANTS");
      arrayOfColumn = createColumns(new String[] { "CONSTANT_CATALOG", "CONSTANT_SCHEMA", "CONSTANT_NAME", "DATA_TYPE INT", "REMARKS", "SQL", "ID INT" });
      
      break;
    case 23: 
      setObjectName("DOMAINS");
      arrayOfColumn = createColumns(new String[] { "DOMAIN_CATALOG", "DOMAIN_SCHEMA", "DOMAIN_NAME", "COLUMN_DEFAULT", "IS_NULLABLE", "DATA_TYPE INT", "PRECISION INT", "SCALE INT", "TYPE_NAME", "SELECTIVITY INT", "CHECK_CONSTRAINT", "REMARKS", "SQL", "ID INT" });
      
      break;
    case 24: 
      setObjectName("TRIGGERS");
      arrayOfColumn = createColumns(new String[] { "TRIGGER_CATALOG", "TRIGGER_SCHEMA", "TRIGGER_NAME", "TRIGGER_TYPE", "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "BEFORE BIT", "JAVA_CLASS", "QUEUE_SIZE INT", "NO_WAIT BIT", "REMARKS", "SQL", "ID INT" });
      
      break;
    case 25: 
      setObjectName("SESSIONS");
      arrayOfColumn = createColumns(new String[] { "ID INT", "USER_NAME", "SESSION_START", "STATEMENT", "STATEMENT_START", "CONTAINS_UNCOMMITTED" });
      
      break;
    case 26: 
      setObjectName("LOCKS");
      arrayOfColumn = createColumns(new String[] { "TABLE_SCHEMA", "TABLE_NAME", "SESSION_ID INT", "LOCK_TYPE" });
      
      break;
    case 27: 
      setObjectName("SESSION_STATE");
      arrayOfColumn = createColumns(new String[] { "KEY", "SQL" });
      
      break;
    case 28: 
      setObjectName("QUERY_STATISTICS");
      arrayOfColumn = createColumns(new String[] { "SQL_STATEMENT", "EXECUTION_COUNT INT", "MIN_EXECUTION_TIME LONG", "MAX_EXECUTION_TIME LONG", "CUMULATIVE_EXECUTION_TIME LONG", "AVERAGE_EXECUTION_TIME DOUBLE", "STD_DEV_EXECUTION_TIME DOUBLE", "MIN_ROW_COUNT INT", "MAX_ROW_COUNT INT", "CUMULATIVE_ROW_COUNT LONG", "AVERAGE_ROW_COUNT DOUBLE", "STD_DEV_ROW_COUNT DOUBLE" });
      
      break;
    default: 
      throw DbException.throwInternalError("type=" + paramInt2);
    }
    setColumns(arrayOfColumn);
    if (str == null)
    {
      this.indexColumn = -1;
      this.metaIndex = null;
    }
    else
    {
      this.indexColumn = getColumn(str).getColumnId();
      IndexColumn[] arrayOfIndexColumn = IndexColumn.wrap(new Column[] { arrayOfColumn[this.indexColumn] });
      
      this.metaIndex = new MetaIndex(this, arrayOfIndexColumn, false);
    }
  }
  
  private Column[] createColumns(String... paramVarArgs)
  {
    Column[] arrayOfColumn = new Column[paramVarArgs.length];
    for (int i = 0; i < paramVarArgs.length; i++)
    {
      String str1 = paramVarArgs[i];
      int j = str1.indexOf(' ');
      int k;
      String str2;
      if (j < 0)
      {
        k = this.database.getMode().lowerCaseIdentifiers ? 14 : 13;
        
        str2 = str1;
      }
      else
      {
        k = DataType.getTypeByName(str1.substring(j + 1)).type;
        str2 = str1.substring(0, j);
      }
      arrayOfColumn[i] = new Column(str2, k);
    }
    return arrayOfColumn;
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public Index addIndex(Session paramSession, String paramString1, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean, String paramString2)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public boolean lock(Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    return false;
  }
  
  public boolean isLockedExclusively()
  {
    return false;
  }
  
  private String identifier(String paramString)
  {
    if (this.database.getMode().lowerCaseIdentifiers) {
      paramString = paramString == null ? null : StringUtils.toLowerEnglish(paramString);
    }
    return paramString;
  }
  
  private ArrayList<Table> getAllTables(Session paramSession)
  {
    ArrayList localArrayList1 = this.database.getAllTablesAndViews(true);
    ArrayList localArrayList2 = paramSession.getLocalTempTables();
    localArrayList1.addAll(localArrayList2);
    return localArrayList1;
  }
  
  private boolean checkIndex(Session paramSession, String paramString, Value paramValue1, Value paramValue2)
  {
    if ((paramString == null) || ((paramValue1 == null) && (paramValue2 == null))) {
      return true;
    }
    Database localDatabase = paramSession.getDatabase();
    Object localObject;
    if (this.database.getMode().lowerCaseIdentifiers) {
      localObject = ValueStringIgnoreCase.get(paramString);
    } else {
      localObject = ValueString.get(paramString);
    }
    if ((paramValue1 != null) && (localDatabase.compare((Value)localObject, paramValue1) < 0)) {
      return false;
    }
    if ((paramValue2 != null) && (localDatabase.compare((Value)localObject, paramValue2) > 0)) {
      return false;
    }
    return true;
  }
  
  private static String replaceNullWithEmpty(String paramString)
  {
    return paramString == null ? "" : paramString;
  }
  
  private boolean hideTable(Table paramTable, Session paramSession)
  {
    return (paramTable.isHidden()) && (paramSession != this.database.getSystemSession());
  }
  
  public ArrayList<Row> generateRows(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    Value localValue1 = null;Value localValue2 = null;
    if (this.indexColumn >= 0)
    {
      if (paramSearchRow1 != null) {
        localValue1 = paramSearchRow1.getValue(this.indexColumn);
      }
      if (paramSearchRow2 != null) {
        localValue2 = paramSearchRow2.getValue(this.indexColumn);
      }
    }
    ArrayList localArrayList = New.arrayList();
    String str1 = identifier(this.database.getShortName());
    boolean bool = paramSession.getUser().isAdmin();
    Object localObject1;
    Object localObject3;
    String str2;
    Object localObject11;
    Object localObject13;
    int i7;
    Object localObject17;
    Object localObject19;
    label1083:
    Object localObject26;
    Object localObject7;
    Object localObject12;
    Object localObject4;
    Object localObject20;
    Object localObject8;
    int i22;
    int i23;
    Object localObject5;
    Object localObject9;
    IndexColumn[] arrayOfIndexColumn1;
    Object localObject21;
    String str4;
    Session localSession;
    Object localObject6;
    switch (this.type)
    {
    case 0: 
      for (localObject1 = getAllTables(paramSession).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject3 = (Table)((Iterator)localObject1).next();
        str2 = identifier(((Table)localObject3).getName());
        if ((checkIndex(paramSession, str2, localValue1, localValue2)) && 
        
          (!hideTable((Table)localObject3, paramSession)))
        {
          if (((Table)localObject3).isTemporary())
          {
            if (((Table)localObject3).isGlobalTemporary()) {
              localObject11 = "GLOBAL TEMPORARY";
            } else {
              localObject11 = "LOCAL TEMPORARY";
            }
          }
          else {
            localObject11 = ((Table)localObject3).isPersistIndexes() ? "CACHED" : "MEMORY";
          }
          localObject13 = ((Table)localObject3).getCreateSQL();
          if ((!bool) && 
            (localObject13 != null) && (((String)localObject13).contains("--hide--"))) {
            localObject13 = "-";
          }
          add(localArrayList, new String[] { str1, identifier(((Table)localObject3).getSchema().getName()), str2, ((Table)localObject3).getTableType(), localObject11, localObject13, replaceNullWithEmpty(((Table)localObject3).getComment()), "" + ((Table)localObject3).getMaxDataModificationId(), "" + ((Table)localObject3).getId(), null, localObject3.getClass().getName(), "" + ((Table)localObject3).getRowCountApproximation() });
        }
      }
      break;
    case 1: 
      for (localObject1 = getAllTables(paramSession).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject3 = (Table)((Iterator)localObject1).next();
        str2 = identifier(((Table)localObject3).getName());
        if ((checkIndex(paramSession, str2, localValue1, localValue2)) && 
        
          (!hideTable((Table)localObject3, paramSession)))
        {
          localObject11 = ((Table)localObject3).getColumns();
          localObject13 = this.database.getCompareMode().getName();
          for (i7 = 0; i7 < localObject11.length; i7++)
          {
            localObject17 = localObject11[i7];
            localObject19 = ((Column)localObject17).getSequence();
            add(localArrayList, new String[] { str1, identifier(((Table)localObject3).getSchema().getName()), str2, identifier(((Column)localObject17).getName()), String.valueOf(i7 + 1), ((Column)localObject17).getDefaultSQL(), ((Column)localObject17).isNullable() ? "YES" : "NO", "" + DataType.convertTypeToSQLType(((Column)localObject17).getType()), "" + ((Column)localObject17).getPrecisionAsInt(), "" + ((Column)localObject17).getPrecisionAsInt(), "" + ((Column)localObject17).getPrecisionAsInt(), "10", "" + ((Column)localObject17).getScale(), "Unicode", localObject13, identifier(DataType.getDataType(((Column)localObject17).getType()).name), "" + (((Column)localObject17).isNullable() ? 1 : 0), "" + (((Column)localObject17).getComputed() ? "TRUE" : "FALSE"), "" + ((Column)localObject17).getSelectivity(), ((Column)localObject17).getCheckConstraintSQL(paramSession, ((Column)localObject17).getName()), localObject19 == null ? null : ((Sequence)localObject19).getName(), replaceNullWithEmpty(((Column)localObject17).getComment()), null });
          }
        }
      }
      break;
    case 2: 
      for (localObject1 = getAllTables(paramSession).iterator(); ((Iterator)localObject1).hasNext(); goto 1163)
      {
        localObject3 = (Table)((Iterator)localObject1).next();
        str2 = identifier(((Table)localObject3).getName());
        if ((!checkIndex(paramSession, str2, localValue1, localValue2)) || 
        
          (hideTable((Table)localObject3, paramSession))) {
          break label1083;
        }
        localObject11 = ((Table)localObject3).getIndexes();
        localObject13 = ((Table)localObject3).getConstraints();
        i7 = 0;
        if ((localObject11 != null) && (i7 < ((ArrayList)localObject11).size()))
        {
          localObject17 = (Index)((ArrayList)localObject11).get(i7);
          if (((Index)localObject17).getCreateSQL() != null)
          {
            localObject19 = null;
            Object localObject24;
            for (int i15 = 0; (localObject13 != null) && (i15 < ((ArrayList)localObject13).size()); i15++)
            {
              localObject24 = (Constraint)((ArrayList)localObject13).get(i15);
              if (((Constraint)localObject24).usesIndex((Index)localObject17)) {
                if (((Index)localObject17).getIndexType().isPrimaryKey())
                {
                  if (((Constraint)localObject24).getConstraintType().equals("PRIMARY KEY")) {
                    localObject19 = ((Constraint)localObject24).getName();
                  }
                }
                else {
                  localObject19 = ((Constraint)localObject24).getName();
                }
              }
            }
            IndexColumn[] arrayOfIndexColumn2 = ((Index)localObject17).getIndexColumns();
            if ((localObject17 instanceof MultiVersionIndex)) {
              localObject24 = ((MultiVersionIndex)localObject17).getBaseIndex().getClass().getName();
            } else {
              localObject24 = localObject17.getClass().getName();
            }
            for (int i20 = 0; i20 < arrayOfIndexColumn2.length; i20++)
            {
              IndexColumn localIndexColumn = arrayOfIndexColumn2[i20];
              localObject26 = localIndexColumn.column;
              add(localArrayList, new String[] { str1, identifier(((Table)localObject3).getSchema().getName()), str2, ((Index)localObject17).getIndexType().isUnique() ? "FALSE" : "TRUE", identifier(((Index)localObject17).getName()), "" + (i20 + 1), identifier(((Column)localObject26).getName()), "0", ((Index)localObject17).getIndexType().isPrimaryKey() ? "TRUE" : "FALSE", ((Index)localObject17).getIndexType().getSQL(), ((Index)localObject17).getIndexType().getBelongsToConstraint() ? "TRUE" : "FALSE", "3", (localIndexColumn.sortType & 0x1) != 0 ? "D" : "A", "0", "", replaceNullWithEmpty(((Index)localObject17).getComment()), ((Index)localObject17).getCreateSQL(), "" + ((Index)localObject17).getId(), "" + localIndexColumn.sortType, localObject19, localObject24 });
            }
          }
          i7++;
        }
      }
      break;
    case 3: 
      add(localArrayList, new String[] { "TABLE" });
      add(localArrayList, new String[] { "TABLE LINK" });
      add(localArrayList, new String[] { "SYSTEM TABLE" });
      add(localArrayList, new String[] { "VIEW" });
      break;
    case 5: 
      add(localArrayList, new String[] { str1 });
      break;
    case 6: 
      for (localObject1 = this.database.getAllSettings().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject3 = (Setting)((Iterator)localObject1).next();
        str2 = ((Setting)localObject3).getStringValue();
        if (str2 == null) {
          str2 = "" + ((Setting)localObject3).getIntValue();
        }
        add(localArrayList, new String[] { identifier(((Setting)localObject3).getName()), str2 });
      }
      add(localArrayList, new String[] { "info.BUILD_ID", "183" });
      add(localArrayList, new String[] { "info.VERSION_MAJOR", "1" });
      add(localArrayList, new String[] { "info.VERSION_MINOR", "4" });
      add(localArrayList, new String[] { "info.VERSION", "" + Constants.getFullVersion() });
      if (bool)
      {
        localObject1 = new String[] { "java.runtime.version", "java.vm.name", "java.vendor", "os.name", "os.arch", "os.version", "sun.os.patch.level", "file.separator", "path.separator", "line.separator", "user.country", "user.language", "user.variant", "file.encoding" };
        for (localObject13 : localObject1) {
          add(localArrayList, new String[] { "property." + (String)localObject13, Utils.getProperty((String)localObject13, "") });
        }
      }
      add(localArrayList, new String[] { "EXCLUSIVE", this.database.getExclusiveSession() == null ? "FALSE" : "TRUE" });
      
      add(localArrayList, new String[] { "MODE", this.database.getMode().getName() });
      add(localArrayList, new String[] { "MULTI_THREADED", this.database.isMultiThreaded() ? "1" : "0" });
      add(localArrayList, new String[] { "MVCC", this.database.isMultiVersion() ? "TRUE" : "FALSE" });
      add(localArrayList, new String[] { "QUERY_TIMEOUT", "" + paramSession.getQueryTimeout() });
      add(localArrayList, new String[] { "RETENTION_TIME", "" + this.database.getRetentionTime() });
      add(localArrayList, new String[] { "LOG", "" + this.database.getLogMode() });
      
      localObject1 = New.arrayList();
      localObject3 = this.database.getSettings().getSettings();
      for (localObject7 = ((HashMap)localObject3).keySet().iterator(); ((Iterator)localObject7).hasNext();)
      {
        localObject12 = (String)((Iterator)localObject7).next();
        ((ArrayList)localObject1).add(localObject12);
      }
      Collections.sort((List)localObject1);
      for (localObject7 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject7).hasNext();)
      {
        localObject12 = (String)((Iterator)localObject7).next();
        add(localArrayList, new String[] { localObject12, (String)((HashMap)localObject3).get(localObject12) });
      }
      if (this.database.isPersistent())
      {
        localObject7 = this.database.getPageStore();
        if (localObject7 != null)
        {
          add(localArrayList, new String[] { "info.FILE_WRITE_TOTAL", "" + ((PageStore)localObject7).getWriteCountTotal() });
          
          add(localArrayList, new String[] { "info.FILE_WRITE", "" + ((PageStore)localObject7).getWriteCount() });
          
          add(localArrayList, new String[] { "info.FILE_READ", "" + ((PageStore)localObject7).getReadCount() });
          
          add(localArrayList, new String[] { "info.PAGE_COUNT", "" + ((PageStore)localObject7).getPageCount() });
          
          add(localArrayList, new String[] { "info.PAGE_SIZE", "" + ((PageStore)localObject7).getPageSize() });
          
          add(localArrayList, new String[] { "info.CACHE_MAX_SIZE", "" + ((PageStore)localObject7).getCache().getMaxMemory() });
          
          add(localArrayList, new String[] { "info.CACHE_SIZE", "" + ((PageStore)localObject7).getCache().getMemory() });
        }
        localObject12 = this.database.getMvStore();
        if (localObject12 != null)
        {
          localObject13 = ((MVTableEngine.Store)localObject12).getStore().getFileStore();
          add(localArrayList, new String[] { "info.FILE_WRITE", "" + ((FileStore)localObject13).getWriteCount() });
          
          add(localArrayList, new String[] { "info.FILE_READ", "" + ((FileStore)localObject13).getReadCount() });
          long l2;
          try
          {
            l2 = ((FileStore)localObject13).getFile().size();
          }
          catch (IOException localIOException)
          {
            throw DbException.convertIOException(localIOException, "Can not get size");
          }
          int i13 = 4096;
          long l4 = l2 / i13;
          add(localArrayList, new String[] { "info.PAGE_COUNT", "" + l4 });
          
          add(localArrayList, new String[] { "info.PAGE_SIZE", "" + i13 });
          
          add(localArrayList, new String[] { "info.CACHE_MAX_SIZE", "" + ((MVTableEngine.Store)localObject12).getStore().getCacheSize() });
          
          add(localArrayList, new String[] { "info.CACHE_SIZE", "" + ((MVTableEngine.Store)localObject12).getStore().getCacheSizeUsed() });
        }
      }
      break;
    case 4: 
      for (localObject1 = DataType.getTypes().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject3 = (DataType)((Iterator)localObject1).next();
        if ((!((DataType)localObject3).hidden) && (((DataType)localObject3).sqlType != 0)) {
          add(localArrayList, new String[] { ((DataType)localObject3).name, String.valueOf(((DataType)localObject3).sqlType), String.valueOf(MathUtils.convertLongToInt(((DataType)localObject3).maxPrecision)), ((DataType)localObject3).prefix, ((DataType)localObject3).suffix, ((DataType)localObject3).params, String.valueOf(((DataType)localObject3).autoIncrement), String.valueOf(((DataType)localObject3).minScale), String.valueOf(((DataType)localObject3).maxScale), ((DataType)localObject3).decimal ? "10" : null, String.valueOf(((DataType)localObject3).sqlTypePos), String.valueOf(((DataType)localObject3).caseSensitive), "1", "3" });
        }
      }
      break;
    case 7: 
      localObject1 = "/org/h2/res/help.csv";
      try
      {
        localObject3 = Utils.getResource((String)localObject1);
        localObject7 = new InputStreamReader(new ByteArrayInputStream((byte[])localObject3));
        
        localObject12 = new Csv();
        ((Csv)localObject12).setLineCommentCharacter('#');
        localObject13 = ((Csv)localObject12).read((Reader)localObject7, null);
        for (int i8 = 0; ((ResultSet)localObject13).next(); i8++) {
          add(localArrayList, new String[] { String.valueOf(i8), ((ResultSet)localObject13).getString(1).trim(), ((ResultSet)localObject13).getString(2).trim(), ((ResultSet)localObject13).getString(3).trim(), ((ResultSet)localObject13).getString(4).trim() });
        }
      }
      catch (Exception localException)
      {
        throw DbException.convert(localException);
      }
    case 8: 
      for (localObject1 = this.database.getAllSchemaObjects(3).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject4 = (SchemaObject)((Iterator)localObject1).next();
        
        localObject7 = (Sequence)localObject4;
        add(localArrayList, new String[] { str1, identifier(((Sequence)localObject7).getSchema().getName()), identifier(((Sequence)localObject7).getName()), String.valueOf(((Sequence)localObject7).getCurrentValue()), String.valueOf(((Sequence)localObject7).getIncrement()), ((Sequence)localObject7).getBelongsToTable() ? "TRUE" : "FALSE", replaceNullWithEmpty(((Sequence)localObject7).getComment()), String.valueOf(((Sequence)localObject7).getCacheSize()), String.valueOf(((Sequence)localObject7).getMinValue()), String.valueOf(((Sequence)localObject7).getMaxValue()), ((Sequence)localObject7).getCycle() ? "TRUE" : "FALSE", "" + ((Sequence)localObject7).getId() });
      }
      break;
    case 9: 
      for (localObject1 = this.database.getAllUsers().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject4 = (User)((Iterator)localObject1).next();
        if ((bool) || (paramSession.getUser() == localObject4)) {
          add(localArrayList, new String[] { identifier(((User)localObject4).getName()), String.valueOf(((User)localObject4).isAdmin()), replaceNullWithEmpty(((User)localObject4).getComment()), "" + ((User)localObject4).getId() });
        }
      }
      break;
    case 10: 
      for (localObject1 = this.database.getAllRoles().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject4 = (Role)((Iterator)localObject1).next();
        if ((bool) || (paramSession.getUser().isRoleGranted((Role)localObject4))) {
          add(localArrayList, new String[] { identifier(((Role)localObject4).getName()), replaceNullWithEmpty(((Role)localObject4).getComment()), "" + ((Role)localObject4).getId() });
        }
      }
      break;
    case 11: 
      if (bool) {
        for (localObject1 = this.database.getAllRights().iterator(); ((Iterator)localObject1).hasNext();)
        {
          localObject4 = (Right)((Iterator)localObject1).next();
          localObject7 = ((Right)localObject4).getGrantedRole();
          localObject12 = ((Right)localObject4).getGrantee();
          localObject13 = ((DbObject)localObject12).getType() == 2 ? "USER" : "ROLE";
          if (localObject7 == null)
          {
            Table localTable = ((Right)localObject4).getGrantedTable();
            localObject17 = identifier(localTable.getName());
            if (checkIndex(paramSession, (String)localObject17, localValue1, localValue2)) {
              add(localArrayList, new String[] { identifier(((DbObject)localObject12).getName()), localObject13, "", ((Right)localObject4).getRights(), identifier(localTable.getSchema().getName()), identifier(localTable.getName()), "" + ((Right)localObject4).getId() });
            }
          }
          else
          {
            add(localArrayList, new String[] { identifier(((DbObject)localObject12).getName()), localObject13, identifier(((Role)localObject7).getName()), "", "", "", "" + ((Right)localObject4).getId() });
          }
        }
      }
      break;
    case 12: 
      for (localObject1 = this.database.getAllSchemaObjects(9).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject4 = (SchemaObject)((Iterator)localObject1).next();
        localObject7 = (FunctionAlias)localObject4;
        try
        {
          localObject12 = ((FunctionAlias)localObject7).getJavaMethods();
        }
        catch (DbException localDbException1)
        {
          localObject12 = new FunctionAlias.JavaMethod[0];
        }
        for (localObject20 : localObject12)
        {
          int i16 = ((FunctionAlias.JavaMethod)localObject20).getDataType() == 0 ? 1 : 2;
          
          add(localArrayList, new String[] { str1, ((FunctionAlias)localObject7).getSchema().getName(), identifier(((FunctionAlias)localObject7).getName()), ((FunctionAlias)localObject7).getJavaClassName(), ((FunctionAlias)localObject7).getJavaMethodName(), "" + DataType.convertTypeToSQLType(((FunctionAlias.JavaMethod)localObject20).getDataType()), DataType.getDataType(((FunctionAlias.JavaMethod)localObject20).getDataType()).name, "" + ((FunctionAlias.JavaMethod)localObject20).getParameterCount(), "" + i16, replaceNullWithEmpty(((FunctionAlias)localObject7).getComment()), "" + ((FunctionAlias)localObject7).getId(), ((FunctionAlias)localObject7).getSource() });
        }
      }
      for (localObject1 = this.database.getAllAggregates().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject4 = (UserAggregate)((Iterator)localObject1).next();
        int m = 2;
        add(localArrayList, new String[] { str1, "PUBLIC", identifier(((UserAggregate)localObject4).getName()), ((UserAggregate)localObject4).getJavaClassName(), "", "" + DataType.convertTypeToSQLType(0), DataType.getDataType(0).name, "1", "" + m, replaceNullWithEmpty(((UserAggregate)localObject4).getComment()), "" + ((UserAggregate)localObject4).getId(), "" });
      }
      break;
    case 21: 
      for (localObject1 = this.database.getAllSchemaObjects(9).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject4 = (SchemaObject)((Iterator)localObject1).next();
        localObject8 = (FunctionAlias)localObject4;
        try
        {
          localObject12 = ((FunctionAlias)localObject8).getJavaMethods();
        }
        catch (DbException localDbException2)
        {
          localObject12 = new FunctionAlias.JavaMethod[0];
        }
        for (localObject20 : localObject12)
        {
          if (((FunctionAlias.JavaMethod)localObject20).getDataType() != 0)
          {
            localObject23 = DataType.getDataType(((FunctionAlias.JavaMethod)localObject20).getDataType());
            add(localArrayList, new String[] { str1, ((FunctionAlias)localObject8).getSchema().getName(), identifier(((FunctionAlias)localObject8).getName()), ((FunctionAlias)localObject8).getJavaClassName(), ((FunctionAlias)localObject8).getJavaMethodName(), "" + ((FunctionAlias.JavaMethod)localObject20).getParameterCount(), "0", "P0", "" + DataType.convertTypeToSQLType(((FunctionAlias.JavaMethod)localObject20).getDataType()), ((DataType)localObject23).name, "" + MathUtils.convertLongToInt(((DataType)localObject23).defaultPrecision), "" + ((DataType)localObject23).defaultScale, "10", "2", "5", "", null });
          }
          Object localObject23 = ((FunctionAlias.JavaMethod)localObject20).getColumnClasses();
          for (int i18 = 0; i18 < localObject23.length; i18++) {
            if ((!((FunctionAlias.JavaMethod)localObject20).hasConnectionParam()) || (i18 != 0))
            {
              Class localClass = localObject23[i18];
              i22 = DataType.getTypeFromClass(localClass);
              localObject26 = DataType.getDataType(i22);
              i23 = localClass.isPrimitive() ? 0 : 1;
              
              add(localArrayList, new String[] { str1, ((FunctionAlias)localObject8).getSchema().getName(), identifier(((FunctionAlias)localObject8).getName()), ((FunctionAlias)localObject8).getJavaClassName(), ((FunctionAlias)localObject8).getJavaMethodName(), "" + ((FunctionAlias.JavaMethod)localObject20).getParameterCount(), "" + (i18 + (((FunctionAlias.JavaMethod)localObject20).hasConnectionParam() ? 0 : 1)), "P" + (i18 + 1), "" + DataType.convertTypeToSQLType(((DataType)localObject26).type), ((DataType)localObject26).name, "" + MathUtils.convertLongToInt(((DataType)localObject26).defaultPrecision), "" + ((DataType)localObject26).defaultScale, "10", "" + i23, "1", "", null });
            }
          }
        }
      }
      break;
    case 13: 
      localObject1 = this.database.getCompareMode().getName();
      for (localObject4 = this.database.getAllSchemas().iterator(); ((Iterator)localObject4).hasNext();)
      {
        localObject8 = (Schema)((Iterator)localObject4).next();
        add(localArrayList, new String[] { str1, identifier(((Schema)localObject8).getName()), identifier(((Schema)localObject8).getOwner().getName()), "Unicode", localObject1, "PUBLIC".equals(((Schema)localObject8).getName()) ? "TRUE" : "FALSE", replaceNullWithEmpty(((Schema)localObject8).getComment()), "" + ((Schema)localObject8).getId() });
      }
      break;
    case 14: 
      for (localObject1 = this.database.getAllRights().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject4 = (Right)((Iterator)localObject1).next();
        localObject8 = ((Right)localObject4).getGrantedTable();
        if ((localObject8 != null) && (!hideTable((Table)localObject8, paramSession)))
        {
          localObject12 = identifier(((Table)localObject8).getName());
          if (checkIndex(paramSession, (String)localObject12, localValue1, localValue2)) {
            addPrivileges(localArrayList, ((Right)localObject4).getGrantee(), str1, (Table)localObject8, null, ((Right)localObject4).getRightMask());
          }
        }
      }
      break;
    case 15: 
      for (localObject1 = this.database.getAllRights().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject4 = (Right)((Iterator)localObject1).next();
        localObject8 = ((Right)localObject4).getGrantedTable();
        if ((localObject8 != null) && (!hideTable((Table)localObject8, paramSession)))
        {
          localObject12 = identifier(((Table)localObject8).getName());
          if (checkIndex(paramSession, (String)localObject12, localValue1, localValue2))
          {
            ??? = ((Right)localObject4).getGrantee();
            ??? = ((Right)localObject4).getRightMask();
            for (Object localObject25 : ((Table)localObject8).getColumns()) {
              addPrivileges(localArrayList, (DbObject)???, str1, (Table)localObject8, ((Column)localObject25).getName(), ???);
            }
          }
        }
      }
      break;
    case 16: 
      for (localObject12 : Collator.getAvailableLocales()) {
        add(localArrayList, new String[] { CompareMode.getName((Locale)localObject12), ((Locale)localObject12).toString() });
      }
      break;
    case 17: 
      for (localObject1 = getAllTables(paramSession).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject5 = (Table)((Iterator)localObject1).next();
        if (((Table)localObject5).getTableType().equals("VIEW"))
        {
          localObject9 = identifier(((Table)localObject5).getName());
          if (checkIndex(paramSession, (String)localObject9, localValue1, localValue2))
          {
            localObject12 = (TableView)localObject5;
            add(localArrayList, new String[] { str1, identifier(((Table)localObject5).getSchema().getName()), localObject9, ((Table)localObject5).getCreateSQL(), "NONE", "NO", ((TableView)localObject12).isInvalid() ? "INVALID" : "VALID", replaceNullWithEmpty(((TableView)localObject12).getComment()), "" + ((TableView)localObject12).getId() });
          }
        }
      }
      break;
    case 18: 
      localObject1 = this.database.getInDoubtTransactions();
      if ((localObject1 != null) && (bool)) {
        for (localObject5 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject5).hasNext();)
        {
          localObject9 = (InDoubtTransaction)((Iterator)localObject5).next();
          add(localArrayList, new String[] { ((InDoubtTransaction)localObject9).getTransactionName(), ((InDoubtTransaction)localObject9).getState() });
        }
      }
      break;
    case 19: 
      for (localObject1 = this.database.getAllSchemaObjects(5).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject5 = (SchemaObject)((Iterator)localObject1).next();
        
        localObject9 = (Constraint)localObject5;
        if (((Constraint)localObject9).getConstraintType().equals("REFERENTIAL"))
        {
          localObject12 = (ConstraintReferential)localObject9;
          ??? = ((ConstraintReferential)localObject12).getColumns();
          arrayOfIndexColumn1 = ((ConstraintReferential)localObject12).getRefColumns();
          ??? = ((ConstraintReferential)localObject12).getTable();
          localObject21 = ((ConstraintReferential)localObject12).getRefTable();
          str4 = identifier(((Table)localObject21).getName());
          if (checkIndex(paramSession, str4, localValue1, localValue2))
          {
            int i19 = getRefAction(((ConstraintReferential)localObject12).getUpdateAction());
            int i21 = getRefAction(((ConstraintReferential)localObject12).getDeleteAction());
            for (i22 = 0; i22 < ???.length; i22++) {
              add(localArrayList, new String[] { str1, identifier(((Table)localObject21).getSchema().getName()), identifier(((Table)localObject21).getName()), identifier(arrayOfIndexColumn1[i22].column.getName()), str1, identifier(((Table)???).getSchema().getName()), identifier(((Table)???).getName()), identifier(???[i22].column.getName()), String.valueOf(i22 + 1), String.valueOf(i19), String.valueOf(i21), identifier(((ConstraintReferential)localObject12).getName()), identifier(((ConstraintReferential)localObject12).getUniqueIndex().getName()), "7" });
            }
          }
        }
      }
      break;
    case 20: 
      for (localObject1 = this.database.getAllSchemaObjects(5).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject5 = (SchemaObject)((Iterator)localObject1).next();
        
        localObject9 = (Constraint)localObject5;
        localObject12 = ((Constraint)localObject9).getConstraintType();
        ??? = null;
        arrayOfIndexColumn1 = null;
        ??? = ((Constraint)localObject9).getTable();
        if (!hideTable((Table)???, paramSession))
        {
          localObject21 = ((Constraint)localObject9).getUniqueIndex();
          str4 = null;
          if (localObject21 != null) {
            str4 = ((Index)localObject21).getName();
          }
          String str5 = identifier(((Table)???).getName());
          if (checkIndex(paramSession, str5, localValue1, localValue2))
          {
            if (((String)localObject12).equals("CHECK")) {
              ??? = ((ConstraintCheck)localObject9).getExpression().getSQL();
            } else if ((((String)localObject12).equals("UNIQUE")) || (((String)localObject12).equals("PRIMARY KEY"))) {
              arrayOfIndexColumn1 = ((ConstraintUnique)localObject9).getColumns();
            } else if (((String)localObject12).equals("REFERENTIAL")) {
              arrayOfIndexColumn1 = ((ConstraintReferential)localObject9).getColumns();
            }
            String str6 = null;
            if (arrayOfIndexColumn1 != null)
            {
              StatementBuilder localStatementBuilder = new StatementBuilder();
              for (Object localObject27 : arrayOfIndexColumn1)
              {
                localStatementBuilder.appendExceptFirst(",");
                localStatementBuilder.append(((IndexColumn)localObject27).column.getName());
              }
              str6 = localStatementBuilder.toString();
            }
            add(localArrayList, new String[] { str1, identifier(((Constraint)localObject9).getSchema().getName()), identifier(((Constraint)localObject9).getName()), localObject12, str1, identifier(((Table)???).getSchema().getName()), str5, str4, ???, str6, replaceNullWithEmpty(((Constraint)localObject9).getComment()), ((Constraint)localObject9).getCreateSQL(), "" + ((Constraint)localObject9).getId() });
          }
        }
      }
      break;
    case 22: 
      for (localObject1 = this.database.getAllSchemaObjects(11).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject5 = (SchemaObject)((Iterator)localObject1).next();
        
        localObject9 = (Constant)localObject5;
        localObject12 = ((Constant)localObject9).getValue();
        add(localArrayList, new String[] { str1, identifier(((Constant)localObject9).getSchema().getName()), identifier(((Constant)localObject9).getName()), "" + DataType.convertTypeToSQLType(((ValueExpression)localObject12).getType()), replaceNullWithEmpty(((Constant)localObject9).getComment()), ((ValueExpression)localObject12).getSQL(), "" + ((Constant)localObject9).getId() });
      }
      break;
    case 23: 
      for (localObject1 = this.database.getAllUserDataTypes().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject5 = (UserDataType)((Iterator)localObject1).next();
        localObject9 = ((UserDataType)localObject5).getColumn();
        add(localArrayList, new String[] { str1, "PUBLIC", identifier(((UserDataType)localObject5).getName()), ((Column)localObject9).getDefaultSQL(), ((Column)localObject9).isNullable() ? "YES" : "NO", "" + ((Column)localObject9).getDataType().sqlType, "" + ((Column)localObject9).getPrecisionAsInt(), "" + ((Column)localObject9).getScale(), ((Column)localObject9).getDataType().name, "" + ((Column)localObject9).getSelectivity(), "" + ((Column)localObject9).getCheckConstraintSQL(paramSession, "VALUE"), replaceNullWithEmpty(((UserDataType)localObject5).getComment()), "" + ((UserDataType)localObject5).getCreateSQL(), "" + ((UserDataType)localObject5).getId() });
      }
      break;
    case 24: 
      for (localObject1 = this.database.getAllSchemaObjects(4).iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject5 = (SchemaObject)((Iterator)localObject1).next();
        
        localObject9 = (TriggerObject)localObject5;
        localObject12 = ((TriggerObject)localObject9).getTable();
        add(localArrayList, new String[] { str1, identifier(((TriggerObject)localObject9).getSchema().getName()), identifier(((TriggerObject)localObject9).getName()), ((TriggerObject)localObject9).getTypeNameList(), str1, identifier(((Table)localObject12).getSchema().getName()), identifier(((Table)localObject12).getName()), "" + ((TriggerObject)localObject9).isBefore(), ((TriggerObject)localObject9).getTriggerClassName(), "" + ((TriggerObject)localObject9).getQueueSize(), "" + ((TriggerObject)localObject9).isNoWait(), replaceNullWithEmpty(((TriggerObject)localObject9).getComment()), ((TriggerObject)localObject9).getCreateSQL(), "" + ((TriggerObject)localObject9).getId() });
      }
      break;
    case 25: 
      long l1 = System.currentTimeMillis();
      for (arrayOfIndexColumn1 : this.database.getSessions(false)) {
        if ((bool) || (arrayOfIndexColumn1 == paramSession))
        {
          ??? = arrayOfIndexColumn1.getCurrentCommand();
          long l3 = arrayOfIndexColumn1.getCurrentCommandStart();
          if (l3 == 0L) {
            l3 = l1;
          }
          add(localArrayList, new String[] { "" + arrayOfIndexColumn1.getId(), arrayOfIndexColumn1.getUser().getName(), new Timestamp(arrayOfIndexColumn1.getSessionStart()).toString(), ??? == null ? null : ((Command)???).toString(), new Timestamp(l3).toString(), "" + arrayOfIndexColumn1.containsUncommitted() });
        }
      }
      break;
    case 26: 
      for (localSession : this.database.getSessions(false)) {
        if ((bool) || (localSession == paramSession)) {
          for (Object localObject22 : localSession.getLocks()) {
            add(localArrayList, new String[] { ((Table)localObject22).getSchema().getName(), ((Table)localObject22).getName(), "" + localSession.getId(), ((Table)localObject22).isLockedExclusivelyBy(localSession) ? "WRITE" : "READ" });
          }
        }
      }
      break;
    case 27: 
      for (localSession : paramSession.getVariableNames())
      {
        ??? = paramSession.getVariable(localSession);
        add(localArrayList, new String[] { "@" + localSession, "SET @" + localSession + " " + ((Value)???).getSQL() });
      }
      for (??? = paramSession.getLocalTempTables().iterator(); ((Iterator)???).hasNext();)
      {
        localObject6 = (Table)((Iterator)???).next();
        add(localArrayList, new String[] { "TABLE " + ((Table)localObject6).getName(), ((Table)localObject6).getCreateSQL() });
      }
      ??? = paramSession.getSchemaSearchPath();
      if ((??? != null) && (???.length > 0))
      {
        localObject6 = new StatementBuilder("SET SCHEMA_SEARCH_PATH ");
        for (String str3 : ???)
        {
          ((StatementBuilder)localObject6).appendExceptFirst(", ");
          ((StatementBuilder)localObject6).append(StringUtils.quoteIdentifier(str3));
        }
        add(localArrayList, new String[] { "SCHEMA_SEARCH_PATH", ((StatementBuilder)localObject6).toString() });
      }
      localObject6 = paramSession.getCurrentSchemaName();
      if (localObject6 != null) {
        add(localArrayList, new String[] { "SCHEMA", "SET SCHEMA " + StringUtils.quoteIdentifier((String)localObject6) });
      }
      break;
    case 28: 
      ??? = this.database.getQueryStatisticsData();
      if (??? != null) {
        for (localObject6 = ((QueryStatisticsData)???).getQueries().iterator(); ((Iterator)localObject6).hasNext();)
        {
          ??? = (QueryStatisticsData.QueryEntry)((Iterator)localObject6).next();
          add(localArrayList, new String[] { ((QueryStatisticsData.QueryEntry)???).sqlStatement, "" + ((QueryStatisticsData.QueryEntry)???).count, "" + ((QueryStatisticsData.QueryEntry)???).executionTimeMin, "" + ((QueryStatisticsData.QueryEntry)???).executionTimeMax, "" + ((QueryStatisticsData.QueryEntry)???).executionTimeCumulative, "" + ((QueryStatisticsData.QueryEntry)???).executionTimeMean, "" + ((QueryStatisticsData.QueryEntry)???).getExecutionTimeStandardDeviation(), "" + ((QueryStatisticsData.QueryEntry)???).rowCountMin, "" + ((QueryStatisticsData.QueryEntry)???).rowCountMax, "" + ((QueryStatisticsData.QueryEntry)???).rowCountCumulative, "" + ((QueryStatisticsData.QueryEntry)???).rowCountMean, "" + ((QueryStatisticsData.QueryEntry)???).getRowCountStandardDeviation() });
        }
      }
      break;
    default: 
      DbException.throwInternalError("type=" + this.type);
    }
    return localArrayList;
  }
  
  private static int getRefAction(int paramInt)
  {
    switch (paramInt)
    {
    case 1: 
      return 0;
    case 0: 
      return 1;
    case 2: 
      return 4;
    case 3: 
      return 2;
    }
    throw DbException.throwInternalError("action=" + paramInt);
  }
  
  public void removeRow(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public void addRow(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public void close(Session paramSession) {}
  
  public void unlock(Session paramSession) {}
  
  private void addPrivileges(ArrayList<Row> paramArrayList, DbObject paramDbObject, String paramString1, Table paramTable, String paramString2, int paramInt)
  {
    if ((paramInt & 0x1) != 0) {
      addPrivilege(paramArrayList, paramDbObject, paramString1, paramTable, paramString2, "SELECT");
    }
    if ((paramInt & 0x4) != 0) {
      addPrivilege(paramArrayList, paramDbObject, paramString1, paramTable, paramString2, "INSERT");
    }
    if ((paramInt & 0x8) != 0) {
      addPrivilege(paramArrayList, paramDbObject, paramString1, paramTable, paramString2, "UPDATE");
    }
    if ((paramInt & 0x2) != 0) {
      addPrivilege(paramArrayList, paramDbObject, paramString1, paramTable, paramString2, "DELETE");
    }
  }
  
  private void addPrivilege(ArrayList<Row> paramArrayList, DbObject paramDbObject, String paramString1, Table paramTable, String paramString2, String paramString3)
  {
    String str = "NO";
    if (paramDbObject.getType() == 2)
    {
      User localUser = (User)paramDbObject;
      if (localUser.isAdmin()) {
        str = "YES";
      }
    }
    if (paramString2 == null) {
      add(paramArrayList, new String[] { null, identifier(paramDbObject.getName()), paramString1, identifier(paramTable.getSchema().getName()), identifier(paramTable.getName()), paramString3, str });
    } else {
      add(paramArrayList, new String[] { null, identifier(paramDbObject.getName()), paramString1, identifier(paramTable.getSchema().getName()), identifier(paramTable.getName()), identifier(paramString2), paramString3, str });
    }
  }
  
  private void add(ArrayList<Row> paramArrayList, String... paramVarArgs)
  {
    Value[] arrayOfValue = new Value[paramVarArgs.length];
    for (int i = 0; i < paramVarArgs.length; i++)
    {
      String str = paramVarArgs[i];
      Value localValue = str == null ? ValueNull.INSTANCE : ValueString.get(str);
      Column localColumn = this.columns[i];
      localValue = localColumn.convert(localValue);
      arrayOfValue[i] = localValue;
    }
    Row localRow = new Row(arrayOfValue, 1);
    localRow.setKey(paramArrayList.size());
    paramArrayList.add(localRow);
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public void checkSupportAlter()
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("META");
  }
  
  public long getRowCount(Session paramSession)
  {
    throw DbException.throwInternalError();
  }
  
  public boolean canGetRowCount()
  {
    return false;
  }
  
  public boolean canDrop()
  {
    return false;
  }
  
  public String getTableType()
  {
    return "SYSTEM TABLE";
  }
  
  public Index getScanIndex(Session paramSession)
  {
    return new MetaIndex(this, IndexColumn.wrap(this.columns), true);
  }
  
  public ArrayList<Index> getIndexes()
  {
    ArrayList localArrayList = New.arrayList();
    if (this.metaIndex == null) {
      return localArrayList;
    }
    localArrayList.add(new MetaIndex(this, IndexColumn.wrap(this.columns), true));
    
    localArrayList.add(this.metaIndex);
    return localArrayList;
  }
  
  public long getMaxDataModificationId()
  {
    switch (this.type)
    {
    case 6: 
    case 18: 
    case 25: 
    case 26: 
    case 27: 
      return Long.MAX_VALUE;
    }
    return this.database.getModificationDataId();
  }
  
  public Index getUniqueIndex()
  {
    return null;
  }
  
  public static int getMetaTableTypeCount()
  {
    return 29;
  }
  
  public long getRowCountApproximation()
  {
    return 1000L;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public boolean isDeterministic()
  {
    return true;
  }
  
  public boolean canReference()
  {
    return false;
  }
}
