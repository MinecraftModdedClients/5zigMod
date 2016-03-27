package org.h2.command;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.h2.command.ddl.AlterIndexRename;
import org.h2.command.ddl.AlterSchemaRename;
import org.h2.command.ddl.AlterTableAddConstraint;
import org.h2.command.ddl.AlterTableAlterColumn;
import org.h2.command.ddl.AlterTableDropConstraint;
import org.h2.command.ddl.AlterTableRename;
import org.h2.command.ddl.AlterTableRenameColumn;
import org.h2.command.ddl.AlterUser;
import org.h2.command.ddl.AlterView;
import org.h2.command.ddl.Analyze;
import org.h2.command.ddl.CreateAggregate;
import org.h2.command.ddl.CreateConstant;
import org.h2.command.ddl.CreateFunctionAlias;
import org.h2.command.ddl.CreateIndex;
import org.h2.command.ddl.CreateLinkedTable;
import org.h2.command.ddl.CreateRole;
import org.h2.command.ddl.CreateSchema;
import org.h2.command.ddl.CreateSequence;
import org.h2.command.ddl.CreateTable;
import org.h2.command.ddl.CreateTableData;
import org.h2.command.ddl.CreateTrigger;
import org.h2.command.ddl.CreateUser;
import org.h2.command.ddl.CreateUserDataType;
import org.h2.command.ddl.CreateView;
import org.h2.command.ddl.DeallocateProcedure;
import org.h2.command.ddl.DefineCommand;
import org.h2.command.ddl.DropAggregate;
import org.h2.command.ddl.DropConstant;
import org.h2.command.ddl.DropDatabase;
import org.h2.command.ddl.DropFunctionAlias;
import org.h2.command.ddl.DropIndex;
import org.h2.command.ddl.DropRole;
import org.h2.command.ddl.DropSchema;
import org.h2.command.ddl.DropSequence;
import org.h2.command.ddl.DropTable;
import org.h2.command.ddl.DropTrigger;
import org.h2.command.ddl.DropUser;
import org.h2.command.ddl.DropUserDataType;
import org.h2.command.ddl.DropView;
import org.h2.command.ddl.GrantRevoke;
import org.h2.command.ddl.PrepareProcedure;
import org.h2.command.ddl.SetComment;
import org.h2.command.ddl.TruncateTable;
import org.h2.command.dml.AlterSequence;
import org.h2.command.dml.AlterTableSet;
import org.h2.command.dml.BackupCommand;
import org.h2.command.dml.Call;
import org.h2.command.dml.Delete;
import org.h2.command.dml.ExecuteProcedure;
import org.h2.command.dml.Explain;
import org.h2.command.dml.Insert;
import org.h2.command.dml.Merge;
import org.h2.command.dml.NoOperation;
import org.h2.command.dml.Query;
import org.h2.command.dml.Replace;
import org.h2.command.dml.RunScriptCommand;
import org.h2.command.dml.ScriptCommand;
import org.h2.command.dml.Select;
import org.h2.command.dml.SelectOrderBy;
import org.h2.command.dml.SelectUnion;
import org.h2.command.dml.SetTypes;
import org.h2.command.dml.TransactionCommand;
import org.h2.command.dml.Update;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.FunctionAlias;
import org.h2.engine.Mode;
import org.h2.engine.Procedure;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.engine.User;
import org.h2.engine.UserAggregate;
import org.h2.engine.UserDataType;
import org.h2.expression.Aggregate;
import org.h2.expression.Alias;
import org.h2.expression.CompareLike;
import org.h2.expression.Comparison;
import org.h2.expression.ConditionAndOr;
import org.h2.expression.ConditionExists;
import org.h2.expression.ConditionIn;
import org.h2.expression.ConditionInSelect;
import org.h2.expression.ConditionNot;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ExpressionList;
import org.h2.expression.Function;
import org.h2.expression.FunctionCall;
import org.h2.expression.JavaAggregate;
import org.h2.expression.JavaFunction;
import org.h2.expression.Operation;
import org.h2.expression.Parameter;
import org.h2.expression.Rownum;
import org.h2.expression.SequenceValue;
import org.h2.expression.Subquery;
import org.h2.expression.TableFunction;
import org.h2.expression.ValueExpression;
import org.h2.expression.Variable;
import org.h2.expression.Wildcard;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.schema.Sequence;
import org.h2.table.Column;
import org.h2.table.FunctionTable;
import org.h2.table.IndexColumn;
import org.h2.table.RangeTable;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.table.TableFilter.TableFilterVisitor;
import org.h2.table.TableView;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.CompareMode;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueBytes;
import org.h2.value.ValueDate;
import org.h2.value.ValueDecimal;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimestamp;

public class Parser
{
  private static final int CHAR_END = 1;
  private static final int CHAR_VALUE = 2;
  private static final int CHAR_QUOTED = 3;
  private static final int CHAR_NAME = 4;
  private static final int CHAR_SPECIAL_1 = 5;
  private static final int CHAR_SPECIAL_2 = 6;
  private static final int CHAR_STRING = 7;
  private static final int CHAR_DOT = 8;
  private static final int CHAR_DOLLAR_QUOTED_STRING = 9;
  private static final int KEYWORD = 1;
  private static final int IDENTIFIER = 2;
  private static final int PARAMETER = 3;
  private static final int END = 4;
  private static final int VALUE = 5;
  private static final int EQUAL = 6;
  private static final int BIGGER_EQUAL = 7;
  private static final int BIGGER = 8;
  private static final int SMALLER = 9;
  private static final int SMALLER_EQUAL = 10;
  private static final int NOT_EQUAL = 11;
  private static final int AT = 12;
  private static final int MINUS = 13;
  private static final int PLUS = 14;
  private static final int STRING_CONCAT = 15;
  private static final int OPEN = 16;
  private static final int CLOSE = 17;
  private static final int NULL = 18;
  private static final int TRUE = 19;
  private static final int FALSE = 20;
  private static final int CURRENT_TIMESTAMP = 21;
  private static final int CURRENT_DATE = 22;
  private static final int CURRENT_TIME = 23;
  private static final int ROWNUM = 24;
  private static final int SPATIAL_INTERSECTS = 25;
  private final Database database;
  private final Session session;
  private final boolean identifiersToUpper;
  private int[] characterTypes;
  private int currentTokenType;
  private String currentToken;
  private boolean currentTokenQuoted;
  private Value currentValue;
  private String originalSQL;
  private String sqlCommand;
  private char[] sqlCommandChars;
  private int lastParseIndex;
  private int parseIndex;
  private CreateView createView;
  private Prepared currentPrepared;
  private Select currentSelect;
  private ArrayList<Parameter> parameters;
  private String schemaName;
  private ArrayList<String> expectedList;
  private boolean rightsChecked;
  private boolean recompileAlways;
  private ArrayList<Parameter> indexedParameterList;
  
  public Parser(Session paramSession)
  {
    this.database = paramSession.getDatabase();
    this.identifiersToUpper = this.database.getSettings().databaseToUpper;
    this.session = paramSession;
  }
  
  public Prepared prepare(String paramString)
  {
    Prepared localPrepared = parse(paramString);
    localPrepared.prepare();
    if (this.currentTokenType != 4) {
      throw getSyntaxError();
    }
    return localPrepared;
  }
  
  public Command prepareCommand(String paramString)
  {
    try
    {
      Prepared localPrepared = parse(paramString);
      boolean bool = isToken(";");
      if ((!bool) && (this.currentTokenType != 4)) {
        throw getSyntaxError();
      }
      localPrepared.prepare();
      Object localObject = new CommandContainer(this, paramString, localPrepared);
      CommandList localCommandList;
      if (bool)
      {
        String str = this.originalSQL.substring(this.parseIndex);
        if (str.trim().length() != 0) {
          localCommandList = new CommandList(this, paramString, (Command)localObject, str);
        }
      }
      return localCommandList;
    }
    catch (DbException localDbException)
    {
      throw localDbException.addSQL(this.originalSQL);
    }
  }
  
  Prepared parse(String paramString)
  {
    Prepared localPrepared;
    try
    {
      localPrepared = parse(paramString, false);
    }
    catch (DbException localDbException)
    {
      if (localDbException.getErrorCode() == 42000) {
        localPrepared = parse(paramString, true);
      } else {
        throw localDbException.addSQL(paramString);
      }
    }
    localPrepared.setPrepareAlways(this.recompileAlways);
    localPrepared.setParameterList(this.parameters);
    return localPrepared;
  }
  
  private Prepared parse(String paramString, boolean paramBoolean)
  {
    initialize(paramString);
    if (paramBoolean) {
      this.expectedList = New.arrayList();
    } else {
      this.expectedList = null;
    }
    this.parameters = New.arrayList();
    this.currentSelect = null;
    this.currentPrepared = null;
    this.createView = null;
    this.recompileAlways = false;
    this.indexedParameterList = null;
    read();
    return parsePrepared();
  }
  
  private Prepared parsePrepared()
  {
    int i = this.lastParseIndex;
    Object localObject = null;
    String str = this.currentToken;
    if (str.length() == 0)
    {
      localObject = new NoOperation(this.session);
    }
    else
    {
      int j = str.charAt(0);
      switch (j)
      {
      case 63: 
        readTerm();
        
        ((Parameter)this.parameters.get(0)).setValue(ValueNull.INSTANCE);
        read("=");
        read("CALL");
        localObject = parseCall();
        break;
      case 40: 
        localObject = parseSelect();
        break;
      case 65: 
      case 97: 
        if (readIf("ALTER")) {
          localObject = parseAlter();
        } else if (readIf("ANALYZE")) {
          localObject = parseAnalyze();
        }
        break;
      case 66: 
      case 98: 
        if (readIf("BACKUP")) {
          localObject = parseBackup();
        } else if (readIf("BEGIN")) {
          localObject = parseBegin();
        }
        break;
      case 67: 
      case 99: 
        if (readIf("COMMIT")) {
          localObject = parseCommit();
        } else if (readIf("CREATE")) {
          localObject = parseCreate();
        } else if (readIf("CALL")) {
          localObject = parseCall();
        } else if (readIf("CHECKPOINT")) {
          localObject = parseCheckpoint();
        } else if (readIf("COMMENT")) {
          localObject = parseComment();
        }
        break;
      case 68: 
      case 100: 
        if (readIf("DELETE")) {
          localObject = parseDelete();
        } else if (readIf("DROP")) {
          localObject = parseDrop();
        } else if (readIf("DECLARE")) {
          localObject = parseCreate();
        } else if (readIf("DEALLOCATE")) {
          localObject = parseDeallocate();
        }
        break;
      case 69: 
      case 101: 
        if (readIf("EXPLAIN")) {
          localObject = parseExplain();
        } else if (readIf("EXECUTE")) {
          localObject = parseExecute();
        }
        break;
      case 70: 
      case 102: 
        if (isToken("FROM")) {
          localObject = parseSelect();
        }
        break;
      case 71: 
      case 103: 
        if (readIf("GRANT")) {
          localObject = parseGrantRevoke(49);
        }
        break;
      case 72: 
      case 104: 
        if (readIf("HELP")) {
          localObject = parseHelp();
        }
        break;
      case 73: 
      case 105: 
        if (readIf("INSERT")) {
          localObject = parseInsert();
        }
        break;
      case 77: 
      case 109: 
        if (readIf("MERGE")) {
          localObject = parseMerge();
        }
        break;
      case 80: 
      case 112: 
        if (readIf("PREPARE")) {
          localObject = parsePrepare();
        }
        break;
      case 82: 
      case 114: 
        if (readIf("ROLLBACK")) {
          localObject = parseRollback();
        } else if (readIf("REVOKE")) {
          localObject = parseGrantRevoke(50);
        } else if (readIf("RUNSCRIPT")) {
          localObject = parseRunScript();
        } else if (readIf("RELEASE")) {
          localObject = parseReleaseSavepoint();
        } else if (readIf("REPLACE")) {
          localObject = parseReplace();
        }
        break;
      case 83: 
      case 115: 
        if (isToken("SELECT")) {
          localObject = parseSelect();
        } else if (readIf("SET")) {
          localObject = parseSet();
        } else if (readIf("SAVEPOINT")) {
          localObject = parseSavepoint();
        } else if (readIf("SCRIPT")) {
          localObject = parseScript();
        } else if (readIf("SHUTDOWN")) {
          localObject = parseShutdown();
        } else if (readIf("SHOW")) {
          localObject = parseShow();
        }
        break;
      case 84: 
      case 116: 
        if (readIf("TRUNCATE")) {
          localObject = parseTruncate();
        }
        break;
      case 85: 
      case 117: 
        if (readIf("UPDATE")) {
          localObject = parseUpdate();
        } else if (readIf("USE")) {
          localObject = parseUse();
        }
        break;
      case 86: 
      case 118: 
        if (readIf("VALUES")) {
          localObject = parseValues();
        }
        break;
      case 87: 
      case 119: 
        if (readIf("WITH")) {
          localObject = parseWith();
        }
        break;
      case 59: 
        localObject = new NoOperation(this.session);
        break;
      case 41: 
      case 42: 
      case 43: 
      case 44: 
      case 45: 
      case 46: 
      case 47: 
      case 48: 
      case 49: 
      case 50: 
      case 51: 
      case 52: 
      case 53: 
      case 54: 
      case 55: 
      case 56: 
      case 57: 
      case 58: 
      case 60: 
      case 61: 
      case 62: 
      case 64: 
      case 74: 
      case 75: 
      case 76: 
      case 78: 
      case 79: 
      case 81: 
      case 88: 
      case 89: 
      case 90: 
      case 91: 
      case 92: 
      case 93: 
      case 94: 
      case 95: 
      case 96: 
      case 106: 
      case 107: 
      case 108: 
      case 110: 
      case 111: 
      case 113: 
      default: 
        throw getSyntaxError();
      }
      int k;
      if (this.indexedParameterList != null)
      {
        k = 0;int m = this.indexedParameterList.size();
        for (; k < m; k++) {
          if (this.indexedParameterList.get(k) == null) {
            this.indexedParameterList.set(k, new Parameter(k));
          }
        }
        this.parameters = this.indexedParameterList;
      }
      if (readIf("{"))
      {
        Parameter localParameter;
        do
        {
          k = (int)readLong() - 1;
          if ((k < 0) || (k >= this.parameters.size())) {
            throw getSyntaxError();
          }
          localParameter = (Parameter)this.parameters.get(k);
          if (localParameter == null) {
            throw getSyntaxError();
          }
          read(":");
          Expression localExpression = readExpression();
          localExpression = localExpression.optimize(this.session);
          localParameter.setValue(localExpression.getValue(this.session));
        } while (readIf(","));
        read("}");
        for (Iterator localIterator = this.parameters.iterator(); localIterator.hasNext();)
        {
          localParameter = (Parameter)localIterator.next();
          localParameter.checkSet();
        }
        this.parameters.clear();
      }
    }
    if (localObject == null) {
      throw getSyntaxError();
    }
    setSQL((Prepared)localObject, null, i);
    return (Prepared)localObject;
  }
  
  private DbException getSyntaxError()
  {
    if ((this.expectedList == null) || (this.expectedList.size() == 0)) {
      return DbException.getSyntaxError(this.sqlCommand, this.parseIndex);
    }
    StatementBuilder localStatementBuilder = new StatementBuilder();
    for (String str : this.expectedList)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(str);
    }
    return DbException.getSyntaxError(this.sqlCommand, this.parseIndex, localStatementBuilder.toString());
  }
  
  private Prepared parseBackup()
  {
    BackupCommand localBackupCommand = new BackupCommand(this.session);
    read("TO");
    localBackupCommand.setFileName(readExpression());
    return localBackupCommand;
  }
  
  private Prepared parseAnalyze()
  {
    Analyze localAnalyze = new Analyze(this.session);
    if (readIf("SAMPLE_SIZE")) {
      localAnalyze.setTop(readPositiveInt());
    }
    return localAnalyze;
  }
  
  private TransactionCommand parseBegin()
  {
    if (!readIf("WORK")) {
      readIf("TRANSACTION");
    }
    TransactionCommand localTransactionCommand = new TransactionCommand(this.session, 83);
    return localTransactionCommand;
  }
  
  private TransactionCommand parseCommit()
  {
    if (readIf("TRANSACTION"))
    {
      localTransactionCommand = new TransactionCommand(this.session, 78);
      
      localTransactionCommand.setTransactionName(readUniqueIdentifier());
      return localTransactionCommand;
    }
    TransactionCommand localTransactionCommand = new TransactionCommand(this.session, 71);
    
    readIf("WORK");
    return localTransactionCommand;
  }
  
  private TransactionCommand parseShutdown()
  {
    int i = 80;
    if (readIf("IMMEDIATELY")) {
      i = 81;
    } else if (readIf("COMPACT")) {
      i = 82;
    } else if (readIf("DEFRAG")) {
      i = 84;
    } else {
      readIf("SCRIPT");
    }
    return new TransactionCommand(this.session, i);
  }
  
  private TransactionCommand parseRollback()
  {
    TransactionCommand localTransactionCommand;
    if (readIf("TRANSACTION"))
    {
      localTransactionCommand = new TransactionCommand(this.session, 79);
      
      localTransactionCommand.setTransactionName(readUniqueIdentifier());
      return localTransactionCommand;
    }
    if (readIf("TO"))
    {
      read("SAVEPOINT");
      localTransactionCommand = new TransactionCommand(this.session, 75);
      
      localTransactionCommand.setSavepointName(readUniqueIdentifier());
    }
    else
    {
      readIf("WORK");
      localTransactionCommand = new TransactionCommand(this.session, 72);
    }
    return localTransactionCommand;
  }
  
  private Prepared parsePrepare()
  {
    if (readIf("COMMIT"))
    {
      localObject1 = new TransactionCommand(this.session, 77);
      
      ((TransactionCommand)localObject1).setTransactionName(readUniqueIdentifier());
      return (Prepared)localObject1;
    }
    Object localObject1 = readAliasIdentifier();
    if (readIf("("))
    {
      localObject2 = New.arrayList();
      for (int i = 0;; i++)
      {
        Column localColumn = parseColumnForTable("C" + i, true);
        ((ArrayList)localObject2).add(localColumn);
        if (readIf(")")) {
          break;
        }
        read(",");
      }
    }
    read("AS");
    Object localObject2 = parsePrepared();
    PrepareProcedure localPrepareProcedure = new PrepareProcedure(this.session);
    localPrepareProcedure.setProcedureName((String)localObject1);
    localPrepareProcedure.setPrepared((Prepared)localObject2);
    return localPrepareProcedure;
  }
  
  private TransactionCommand parseSavepoint()
  {
    TransactionCommand localTransactionCommand = new TransactionCommand(this.session, 74);
    
    localTransactionCommand.setSavepointName(readUniqueIdentifier());
    return localTransactionCommand;
  }
  
  private Prepared parseReleaseSavepoint()
  {
    NoOperation localNoOperation = new NoOperation(this.session);
    readIf("SAVEPOINT");
    readUniqueIdentifier();
    return localNoOperation;
  }
  
  private Schema getSchema(String paramString)
  {
    if (paramString == null) {
      return null;
    }
    Schema localSchema = this.database.findSchema(paramString);
    if (localSchema == null) {
      if (equalsToken("SESSION", paramString)) {
        localSchema = this.database.getSchema(this.session.getCurrentSchemaName());
      } else if ((!this.database.getMode().sysDummy1) || (!"SYSIBM".equals(paramString))) {
        throw DbException.get(90079, paramString);
      }
    }
    return localSchema;
  }
  
  private Schema getSchema()
  {
    return getSchema(this.schemaName);
  }
  
  private Column readTableColumn(TableFilter paramTableFilter)
  {
    Object localObject1 = null;
    String str = readColumnIdentifier();
    if (readIf("."))
    {
      localObject1 = str;
      str = readColumnIdentifier();
      if (readIf("."))
      {
        Object localObject2 = localObject1;
        localObject1 = str;
        str = readColumnIdentifier();
        if (readIf("."))
        {
          Object localObject3 = localObject2;
          localObject2 = localObject1;
          localObject1 = str;
          str = readColumnIdentifier();
          if (!equalsToken((String)localObject3, this.database.getShortName())) {
            throw DbException.get(90013, (String)localObject3);
          }
        }
        if (!equalsToken((String)localObject2, paramTableFilter.getTable().getSchema().getName())) {
          throw DbException.get(90079, (String)localObject2);
        }
      }
      if (!equalsToken((String)localObject1, paramTableFilter.getTableAlias())) {
        throw DbException.get(42102, (String)localObject1);
      }
    }
    if ((this.database.getSettings().rowId) && 
      ("_ROWID_".equals(str))) {
      return paramTableFilter.getRowIdColumn();
    }
    return paramTableFilter.getTable().getColumn(str);
  }
  
  private Update parseUpdate()
  {
    Update localUpdate = new Update(this.session);
    this.currentPrepared = localUpdate;
    int i = this.lastParseIndex;
    TableFilter localTableFilter = readSimpleTableFilter();
    localUpdate.setTableFilter(localTableFilter);
    read("SET");
    Object localObject1;
    Object localObject2;
    if (readIf("("))
    {
      localObject1 = New.arrayList();
      do
      {
        localObject2 = readTableColumn(localTableFilter);
        ((ArrayList)localObject1).add(localObject2);
      } while (readIf(","));
      read(")");
      read("=");
      localObject2 = readExpression();
      if (((ArrayList)localObject1).size() == 1)
      {
        localUpdate.setAssignment((Column)((ArrayList)localObject1).get(0), (Expression)localObject2);
      }
      else
      {
        int j = 0;
        for (int k = ((ArrayList)localObject1).size(); j < k; j++)
        {
          Column localColumn = (Column)((ArrayList)localObject1).get(j);
          Function localFunction = Function.getFunction(this.database, "ARRAY_GET");
          localFunction.setParameter(0, (Expression)localObject2);
          localFunction.setParameter(1, ValueExpression.get(ValueInt.get(j + 1)));
          localFunction.doneWithParameters();
          localUpdate.setAssignment(localColumn, localFunction);
        }
      }
    }
    else
    {
      do
      {
        localObject1 = readTableColumn(localTableFilter);
        read("=");
        if (readIf("DEFAULT")) {
          localObject2 = ValueExpression.getDefault();
        } else {
          localObject2 = readExpression();
        }
        localUpdate.setAssignment((Column)localObject1, (Expression)localObject2);
      } while (readIf(","));
    }
    if (readIf("WHERE"))
    {
      localObject1 = readExpression();
      localUpdate.setCondition((Expression)localObject1);
    }
    if (readIf("ORDER"))
    {
      read("BY");
      parseSimpleOrderList();
    }
    if (readIf("LIMIT"))
    {
      localObject1 = readTerm().optimize(this.session);
      localUpdate.setLimit((Expression)localObject1);
    }
    setSQL(localUpdate, "UPDATE", i);
    return localUpdate;
  }
  
  private TableFilter readSimpleTableFilter()
  {
    Table localTable = readTableOrView();
    String str = null;
    if (readIf("AS")) {
      str = readAliasIdentifier();
    } else if ((this.currentTokenType == 2) && 
      (!equalsToken("SET", this.currentToken))) {
      str = readAliasIdentifier();
    }
    return new TableFilter(this.session, localTable, str, this.rightsChecked, this.currentSelect);
  }
  
  private Delete parseDelete()
  {
    Delete localDelete = new Delete(this.session);
    Expression localExpression1 = null;
    if (readIf("TOP")) {
      localExpression1 = readTerm().optimize(this.session);
    }
    this.currentPrepared = localDelete;
    int i = this.lastParseIndex;
    readIf("FROM");
    TableFilter localTableFilter = readSimpleTableFilter();
    localDelete.setTableFilter(localTableFilter);
    if (readIf("WHERE"))
    {
      Expression localExpression2 = readExpression();
      localDelete.setCondition(localExpression2);
    }
    if ((readIf("LIMIT")) && (localExpression1 == null)) {
      localExpression1 = readTerm().optimize(this.session);
    }
    localDelete.setLimit(localExpression1);
    setSQL(localDelete, "DELETE", i);
    return localDelete;
  }
  
  private IndexColumn[] parseIndexColumnList()
  {
    ArrayList localArrayList = New.arrayList();
    do
    {
      IndexColumn localIndexColumn = new IndexColumn();
      localIndexColumn.columnName = readColumnIdentifier();
      localArrayList.add(localIndexColumn);
      if (!readIf("ASC")) {
        if (readIf("DESC")) {
          localIndexColumn.sortType = 1;
        }
      }
      if (readIf("NULLS")) {
        if (readIf("FIRST"))
        {
          localIndexColumn.sortType |= 0x2;
        }
        else
        {
          read("LAST");
          localIndexColumn.sortType |= 0x4;
        }
      }
    } while (readIf(","));
    read(")");
    return (IndexColumn[])localArrayList.toArray(new IndexColumn[localArrayList.size()]);
  }
  
  private String[] parseColumnList()
  {
    ArrayList localArrayList = New.arrayList();
    do
    {
      String str = readColumnIdentifier();
      localArrayList.add(str);
    } while (readIfMore());
    return (String[])localArrayList.toArray(new String[localArrayList.size()]);
  }
  
  private Column[] parseColumnList(Table paramTable)
  {
    ArrayList localArrayList = New.arrayList();
    HashSet localHashSet = New.hashSet();
    if (!readIf(")")) {
      do
      {
        Column localColumn = parseColumn(paramTable);
        if (!localHashSet.add(localColumn)) {
          throw DbException.get(42121, localColumn.getSQL());
        }
        localArrayList.add(localColumn);
      } while (readIfMore());
    }
    return (Column[])localArrayList.toArray(new Column[localArrayList.size()]);
  }
  
  private Column parseColumn(Table paramTable)
  {
    String str = readColumnIdentifier();
    if ((this.database.getSettings().rowId) && ("_ROWID_".equals(str))) {
      return paramTable.getRowIdColumn();
    }
    return paramTable.getColumn(str);
  }
  
  private boolean readIfMore()
  {
    if (readIf(",")) {
      return !readIf(")");
    }
    read(")");
    return false;
  }
  
  private Prepared parseHelp()
  {
    StringBuilder localStringBuilder = new StringBuilder("SELECT * FROM INFORMATION_SCHEMA.HELP");
    
    int i = 0;
    ArrayList localArrayList = New.arrayList();
    while (this.currentTokenType != 4)
    {
      String str = this.currentToken;
      read();
      if (i == 0) {
        localStringBuilder.append(" WHERE ");
      } else {
        localStringBuilder.append(" AND ");
      }
      i++;
      localStringBuilder.append("UPPER(TOPIC) LIKE ?");
      localArrayList.add(ValueString.get("%" + str + "%"));
    }
    return prepare(this.session, localStringBuilder.toString(), localArrayList);
  }
  
  private Prepared parseShow()
  {
    ArrayList localArrayList = New.arrayList();
    StringBuilder localStringBuilder = new StringBuilder("SELECT ");
    Object localObject1;
    if (readIf("CLIENT_ENCODING"))
    {
      localStringBuilder.append("'UNICODE' AS CLIENT_ENCODING FROM DUAL");
    }
    else if (readIf("DEFAULT_TRANSACTION_ISOLATION"))
    {
      localStringBuilder.append("'read committed' AS DEFAULT_TRANSACTION_ISOLATION FROM DUAL");
    }
    else if (readIf("TRANSACTION"))
    {
      read("ISOLATION");
      read("LEVEL");
      localStringBuilder.append("'read committed' AS TRANSACTION_ISOLATION FROM DUAL");
    }
    else if (readIf("DATESTYLE"))
    {
      localStringBuilder.append("'ISO' AS DATESTYLE FROM DUAL");
    }
    else if (readIf("SERVER_VERSION"))
    {
      localStringBuilder.append("'8.1.4' AS SERVER_VERSION FROM DUAL");
    }
    else if (readIf("SERVER_ENCODING"))
    {
      localStringBuilder.append("'UTF8' AS SERVER_ENCODING FROM DUAL");
    }
    else
    {
      String str;
      if (readIf("TABLES"))
      {
        str = "PUBLIC";
        if (readIf("FROM")) {
          str = readUniqueIdentifier();
        }
        localStringBuilder.append("TABLE_NAME, TABLE_SCHEMA FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=? ORDER BY TABLE_NAME");
        
        localArrayList.add(ValueString.get(str));
      }
      else if (readIf("COLUMNS"))
      {
        read("FROM");
        str = readIdentifierWithSchema();
        localObject1 = getSchema().getName();
        localArrayList.add(ValueString.get(str));
        if (readIf("FROM")) {
          localObject1 = readUniqueIdentifier();
        }
        localStringBuilder.append("C.COLUMN_NAME FIELD, C.TYPE_NAME || '(' || C.NUMERIC_PRECISION || ')' TYPE, C.IS_NULLABLE \"NULL\", CASE (SELECT MAX(I.INDEX_TYPE_NAME) FROM INFORMATION_SCHEMA.INDEXES I WHERE I.TABLE_SCHEMA=C.TABLE_SCHEMA AND I.TABLE_NAME=C.TABLE_NAME AND I.COLUMN_NAME=C.COLUMN_NAME)WHEN 'PRIMARY KEY' THEN 'PRI' WHEN 'UNIQUE INDEX' THEN 'UNI' ELSE '' END KEY, IFNULL(COLUMN_DEFAULT, 'NULL') DEFAULT FROM INFORMATION_SCHEMA.COLUMNS C WHERE C.TABLE_NAME=? AND C.TABLE_SCHEMA=? ORDER BY C.ORDINAL_POSITION");
        
        localArrayList.add(ValueString.get((String)localObject1));
      }
      else if ((readIf("DATABASES")) || (readIf("SCHEMAS")))
      {
        localStringBuilder.append("SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA");
      }
    }
    boolean bool = this.session.getAllowLiterals();
    try
    {
      this.session.setAllowLiterals(true);
      return prepare(this.session, localStringBuilder.toString(), localArrayList);
    }
    finally
    {
      this.session.setAllowLiterals(bool);
    }
  }
  
  private static Prepared prepare(Session paramSession, String paramString, ArrayList<Value> paramArrayList)
  {
    Prepared localPrepared = paramSession.prepare(paramString);
    ArrayList localArrayList = localPrepared.getParameters();
    if (localArrayList != null)
    {
      int i = 0;
      for (int j = localArrayList.size(); i < j; i++)
      {
        Parameter localParameter = (Parameter)localArrayList.get(i);
        localParameter.setValue((Value)paramArrayList.get(i));
      }
    }
    return localPrepared;
  }
  
  private boolean isSelect()
  {
    int i = this.lastParseIndex;
    while (readIf("(")) {}
    boolean bool = (isToken("SELECT")) || (isToken("FROM"));
    this.parseIndex = i;
    read();
    return bool;
  }
  
  private Merge parseMerge()
  {
    Merge localMerge = new Merge(this.session);
    this.currentPrepared = localMerge;
    read("INTO");
    Table localTable = readTableOrView();
    localMerge.setTable(localTable);
    Object localObject;
    if (readIf("("))
    {
      if (isSelect())
      {
        localMerge.setQuery(parseSelect());
        read(")");
        return localMerge;
      }
      localObject = parseColumnList(localTable);
      localMerge.setColumns((Column[])localObject);
    }
    if (readIf("KEY"))
    {
      read("(");
      localObject = parseColumnList(localTable);
      localMerge.setKeys((Column[])localObject);
    }
    if (readIf("VALUES")) {
      do
      {
        localObject = New.arrayList();
        read("(");
        if (!readIf(")")) {
          do
          {
            if (readIf("DEFAULT")) {
              ((ArrayList)localObject).add(null);
            } else {
              ((ArrayList)localObject).add(readExpression());
            }
          } while (readIfMore());
        }
        localMerge.addRow((Expression[])((ArrayList)localObject).toArray(new Expression[((ArrayList)localObject).size()]));
      } while (readIf(","));
    } else {
      localMerge.setQuery(parseSelect());
    }
    return localMerge;
  }
  
  private Insert parseInsert()
  {
    Insert localInsert = new Insert(this.session);
    this.currentPrepared = localInsert;
    read("INTO");
    Table localTable = readTableOrView();
    localInsert.setTable(localTable);
    Column[] arrayOfColumn = null;
    if (readIf("("))
    {
      if (isSelect())
      {
        localInsert.setQuery(parseSelect());
        read(")");
        return localInsert;
      }
      arrayOfColumn = parseColumnList(localTable);
      localInsert.setColumns(arrayOfColumn);
    }
    if (readIf("DIRECT")) {
      localInsert.setInsertFromSelect(true);
    }
    if (readIf("SORTED")) {
      localInsert.setSortedInsertMode(true);
    }
    Object localObject1;
    Object localObject2;
    if (readIf("DEFAULT"))
    {
      read("VALUES");
      localObject1 = new Expression[0];
      localInsert.addRow((Expression[])localObject1);
    }
    else if (readIf("VALUES"))
    {
      read("(");
      do
      {
        localObject1 = New.arrayList();
        if (!readIf(")")) {
          do
          {
            if (readIf("DEFAULT")) {
              ((ArrayList)localObject1).add(null);
            } else {
              ((ArrayList)localObject1).add(readExpression());
            }
          } while (readIfMore());
        }
        localInsert.addRow((Expression[])((ArrayList)localObject1).toArray(new Expression[((ArrayList)localObject1).size()]));
        if (!readIf(",")) {
          break;
        }
      } while (readIf("("));
    }
    else if (readIf("SET"))
    {
      if (arrayOfColumn != null) {
        throw getSyntaxError();
      }
      localObject1 = New.arrayList();
      localObject2 = New.arrayList();
      do
      {
        ((ArrayList)localObject1).add(parseColumn(localTable));
        read("=");
        Object localObject3;
        if (readIf("DEFAULT")) {
          localObject3 = ValueExpression.getDefault();
        } else {
          localObject3 = readExpression();
        }
        ((ArrayList)localObject2).add(localObject3);
      } while (readIf(","));
      localInsert.setColumns((Column[])((ArrayList)localObject1).toArray(new Column[((ArrayList)localObject1).size()]));
      localInsert.addRow((Expression[])((ArrayList)localObject2).toArray(new Expression[((ArrayList)localObject2).size()]));
    }
    else
    {
      localInsert.setQuery(parseSelect());
    }
    if ((this.database.getMode().onDuplicateKeyUpdate) && 
      (readIf("ON")))
    {
      read("DUPLICATE");
      read("KEY");
      read("UPDATE");
      do
      {
        localObject1 = parseColumn(localTable);
        read("=");
        if (readIf("DEFAULT")) {
          localObject2 = ValueExpression.getDefault();
        } else {
          localObject2 = readExpression();
        }
        localInsert.addAssignmentForDuplicate((Column)localObject1, (Expression)localObject2);
      } while (readIf(","));
    }
    if (this.database.getMode().isolationLevelInSelectOrInsertStatement) {
      parseIsolationClause();
    }
    return localInsert;
  }
  
  private Replace parseReplace()
  {
    Replace localReplace = new Replace(this.session);
    this.currentPrepared = localReplace;
    read("INTO");
    Table localTable = readTableOrView();
    localReplace.setTable(localTable);
    Object localObject;
    if (readIf("("))
    {
      if (isSelect())
      {
        localReplace.setQuery(parseSelect());
        read(")");
        return localReplace;
      }
      localObject = parseColumnList(localTable);
      localReplace.setColumns((Column[])localObject);
    }
    if (readIf("VALUES")) {
      do
      {
        localObject = New.arrayList();
        read("(");
        if (!readIf(")")) {
          do
          {
            if (readIf("DEFAULT")) {
              ((ArrayList)localObject).add(null);
            } else {
              ((ArrayList)localObject).add(readExpression());
            }
          } while (readIfMore());
        }
        localReplace.addRow((Expression[])((ArrayList)localObject).toArray(new Expression[((ArrayList)localObject).size()]));
      } while (readIf(","));
    } else {
      localReplace.setQuery(parseSelect());
    }
    return localReplace;
  }
  
  private TableFilter readTableFilter(boolean paramBoolean)
  {
    String str = null;
    Object localObject2;
    Object localObject3;
    Object localObject1;
    if (readIf("("))
    {
      if (isSelect())
      {
        localObject2 = parseSelectUnion();
        read(")");
        ((Query)localObject2).setParameterList(New.arrayList(this.parameters));
        ((Query)localObject2).init();
        if (this.createView != null) {
          localObject3 = this.database.getSystemSession();
        } else {
          localObject3 = this.session;
        }
        str = this.session.getNextSystemIdentifier(this.sqlCommand);
        localObject1 = TableView.createTempView((Session)localObject3, this.session.getUser(), str, (Query)localObject2, this.currentSelect);
      }
      else
      {
        if (this.database.getSettings().nestedJoins)
        {
          localObject2 = readTableFilter(false);
          localObject2 = readJoin((TableFilter)localObject2, this.currentSelect, false, false);
          localObject2 = getNested((TableFilter)localObject2);
        }
        else
        {
          localObject2 = readTableFilter(paramBoolean);
          localObject2 = readJoin((TableFilter)localObject2, this.currentSelect, false, paramBoolean);
        }
        read(")");
        str = readFromAlias(null);
        if (str != null) {
          ((TableFilter)localObject2).setAlias(str);
        }
        return (TableFilter)localObject2;
      }
    }
    else if (readIf("VALUES"))
    {
      localObject1 = parseValuesTable().getTable();
    }
    else
    {
      localObject2 = readIdentifierWithSchema(null);
      localObject3 = getSchema();
      boolean bool = readIf("(");
      if ((bool) && (readIf("INDEX")))
      {
        readIdentifierWithSchema(null);
        read(")");
        bool = false;
      }
      if (bool)
      {
        Schema localSchema = this.database.getSchema("PUBLIC");
        Expression localExpression;
        Object localObject4;
        if (equalsToken((String)localObject2, "SYSTEM_RANGE"))
        {
          localExpression = readExpression();
          read(",");
          localObject4 = readExpression();
          read(")");
          localObject1 = new RangeTable(localSchema, localExpression, (Expression)localObject4, false);
        }
        else
        {
          localExpression = readFunction((Schema)localObject3, (String)localObject2);
          if (!(localExpression instanceof FunctionCall)) {
            throw getSyntaxError();
          }
          localObject4 = (FunctionCall)localExpression;
          if (!((FunctionCall)localObject4).isDeterministic()) {
            this.recompileAlways = true;
          }
          localObject1 = new FunctionTable(localSchema, this.session, localExpression, (FunctionCall)localObject4);
        }
      }
      else if (equalsToken("DUAL", (String)localObject2))
      {
        localObject1 = getDualTable(false);
      }
      else if ((this.database.getMode().sysDummy1) && (equalsToken("SYSDUMMY1", (String)localObject2)))
      {
        localObject1 = getDualTable(false);
      }
      else
      {
        localObject1 = readTableOrView((String)localObject2);
      }
    }
    str = readFromAlias(str);
    return new TableFilter(this.session, (Table)localObject1, str, this.rightsChecked, this.currentSelect);
  }
  
  private String readFromAlias(String paramString)
  {
    if (readIf("AS")) {
      paramString = readAliasIdentifier();
    } else if (this.currentTokenType == 2) {
      if ((!isToken("LEFT")) && (!isToken("RIGHT")) && (!isToken("FULL"))) {
        paramString = readAliasIdentifier();
      }
    }
    return paramString;
  }
  
  private Prepared parseTruncate()
  {
    read("TABLE");
    Table localTable = readTableOrView();
    TruncateTable localTruncateTable = new TruncateTable(this.session);
    localTruncateTable.setTable(localTable);
    return localTruncateTable;
  }
  
  private boolean readIfExists(boolean paramBoolean)
  {
    if (readIf("IF"))
    {
      read("EXISTS");
      paramBoolean = true;
    }
    return paramBoolean;
  }
  
  private Prepared parseComment()
  {
    int i = 0;
    read("ON");
    int j = 0;
    if ((readIf("TABLE")) || (readIf("VIEW")))
    {
      i = 0;
    }
    else if (readIf("COLUMN"))
    {
      j = 1;
      i = 0;
    }
    else if (readIf("CONSTANT"))
    {
      i = 11;
    }
    else if (readIf("CONSTRAINT"))
    {
      i = 5;
    }
    else if (readIf("ALIAS"))
    {
      i = 9;
    }
    else if (readIf("INDEX"))
    {
      i = 1;
    }
    else if (readIf("ROLE"))
    {
      i = 7;
    }
    else if (readIf("SCHEMA"))
    {
      i = 10;
    }
    else if (readIf("SEQUENCE"))
    {
      i = 3;
    }
    else if (readIf("TRIGGER"))
    {
      i = 4;
    }
    else if (readIf("USER"))
    {
      i = 2;
    }
    else if (readIf("DOMAIN"))
    {
      i = 12;
    }
    else
    {
      throw getSyntaxError();
    }
    SetComment localSetComment = new SetComment(this.session);
    String str;
    if (j != 0)
    {
      ArrayList localArrayList = New.arrayList();
      do
      {
        localArrayList.add(readUniqueIdentifier());
      } while (readIf("."));
      this.schemaName = this.session.getCurrentSchemaName();
      if (localArrayList.size() == 4)
      {
        if (!equalsToken(this.database.getShortName(), (String)localArrayList.get(0))) {
          throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "database name");
        }
        localArrayList.remove(0);
      }
      if (localArrayList.size() == 3)
      {
        this.schemaName = ((String)localArrayList.get(0));
        localArrayList.remove(0);
      }
      if (localArrayList.size() != 2) {
        throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "table.column");
      }
      str = (String)localArrayList.get(0);
      localSetComment.setColumn(true);
      localSetComment.setColumnName((String)localArrayList.get(1));
    }
    else
    {
      str = readIdentifierWithSchema();
    }
    localSetComment.setSchemaName(this.schemaName);
    localSetComment.setObjectName(str);
    localSetComment.setObjectType(i);
    read("IS");
    localSetComment.setCommentExpression(readExpression());
    return localSetComment;
  }
  
  private Prepared parseDrop()
  {
    boolean bool;
    Object localObject1;
    Object localObject2;
    Object localObject3;
    if (readIf("TABLE"))
    {
      bool = readIfExists(false);
      localObject1 = readIdentifierWithSchema();
      localObject2 = new DropTable(this.session, getSchema());
      ((DropTable)localObject2).setTableName((String)localObject1);
      while (readIf(","))
      {
        localObject1 = readIdentifierWithSchema();
        localObject3 = new DropTable(this.session, getSchema());
        ((DropTable)localObject3).setTableName((String)localObject1);
        ((DropTable)localObject2).addNextDropTable((DropTable)localObject3);
      }
      bool = readIfExists(bool);
      ((DropTable)localObject2).setIfExists(bool);
      if (readIf("CASCADE"))
      {
        ((DropTable)localObject2).setDropAction(1);
        readIf("CONSTRAINTS");
      }
      else if (readIf("RESTRICT"))
      {
        ((DropTable)localObject2).setDropAction(0);
      }
      else if (readIf("IGNORE"))
      {
        ((DropTable)localObject2).setDropAction(2);
      }
      return (Prepared)localObject2;
    }
    if (readIf("INDEX"))
    {
      bool = readIfExists(false);
      localObject1 = readIdentifierWithSchema();
      localObject2 = new DropIndex(this.session, getSchema());
      ((DropIndex)localObject2).setIndexName((String)localObject1);
      bool = readIfExists(bool);
      ((DropIndex)localObject2).setIfExists(bool);
      return (Prepared)localObject2;
    }
    if (readIf("USER"))
    {
      bool = readIfExists(false);
      localObject1 = new DropUser(this.session);
      ((DropUser)localObject1).setUserName(readUniqueIdentifier());
      bool = readIfExists(bool);
      readIf("CASCADE");
      ((DropUser)localObject1).setIfExists(bool);
      return (Prepared)localObject1;
    }
    if (readIf("SEQUENCE"))
    {
      bool = readIfExists(false);
      localObject1 = readIdentifierWithSchema();
      localObject2 = new DropSequence(this.session, getSchema());
      ((DropSequence)localObject2).setSequenceName((String)localObject1);
      bool = readIfExists(bool);
      ((DropSequence)localObject2).setIfExists(bool);
      return (Prepared)localObject2;
    }
    if (readIf("CONSTANT"))
    {
      bool = readIfExists(false);
      localObject1 = readIdentifierWithSchema();
      localObject2 = new DropConstant(this.session, getSchema());
      ((DropConstant)localObject2).setConstantName((String)localObject1);
      bool = readIfExists(bool);
      ((DropConstant)localObject2).setIfExists(bool);
      return (Prepared)localObject2;
    }
    if (readIf("TRIGGER"))
    {
      bool = readIfExists(false);
      localObject1 = readIdentifierWithSchema();
      localObject2 = new DropTrigger(this.session, getSchema());
      ((DropTrigger)localObject2).setTriggerName((String)localObject1);
      bool = readIfExists(bool);
      ((DropTrigger)localObject2).setIfExists(bool);
      return (Prepared)localObject2;
    }
    if (readIf("VIEW"))
    {
      bool = readIfExists(false);
      localObject1 = readIdentifierWithSchema();
      localObject2 = new DropView(this.session, getSchema());
      ((DropView)localObject2).setViewName((String)localObject1);
      bool = readIfExists(bool);
      ((DropView)localObject2).setIfExists(bool);
      localObject3 = parseCascadeOrRestrict();
      if (localObject3 != null) {
        ((DropView)localObject2).setDropAction(((Integer)localObject3).intValue());
      }
      return (Prepared)localObject2;
    }
    if (readIf("ROLE"))
    {
      bool = readIfExists(false);
      localObject1 = new DropRole(this.session);
      ((DropRole)localObject1).setRoleName(readUniqueIdentifier());
      bool = readIfExists(bool);
      ((DropRole)localObject1).setIfExists(bool);
      return (Prepared)localObject1;
    }
    if (readIf("ALIAS"))
    {
      bool = readIfExists(false);
      localObject1 = readIdentifierWithSchema();
      localObject2 = new DropFunctionAlias(this.session, getSchema());
      
      ((DropFunctionAlias)localObject2).setAliasName((String)localObject1);
      bool = readIfExists(bool);
      ((DropFunctionAlias)localObject2).setIfExists(bool);
      return (Prepared)localObject2;
    }
    if (readIf("SCHEMA"))
    {
      bool = readIfExists(false);
      localObject1 = new DropSchema(this.session);
      ((DropSchema)localObject1).setSchemaName(readUniqueIdentifier());
      bool = readIfExists(bool);
      ((DropSchema)localObject1).setIfExists(bool);
      return (Prepared)localObject1;
    }
    if (readIf("ALL"))
    {
      read("OBJECTS");
      DropDatabase localDropDatabase = new DropDatabase(this.session);
      localDropDatabase.setDropAllObjects(true);
      if (readIf("DELETE"))
      {
        read("FILES");
        localDropDatabase.setDeleteFiles(true);
      }
      return localDropDatabase;
    }
    if (readIf("DOMAIN")) {
      return parseDropUserDataType();
    }
    if (readIf("TYPE")) {
      return parseDropUserDataType();
    }
    if (readIf("DATATYPE")) {
      return parseDropUserDataType();
    }
    if (readIf("AGGREGATE")) {
      return parseDropAggregate();
    }
    throw getSyntaxError();
  }
  
  private DropUserDataType parseDropUserDataType()
  {
    boolean bool = readIfExists(false);
    DropUserDataType localDropUserDataType = new DropUserDataType(this.session);
    localDropUserDataType.setTypeName(readUniqueIdentifier());
    bool = readIfExists(bool);
    localDropUserDataType.setIfExists(bool);
    return localDropUserDataType;
  }
  
  private DropAggregate parseDropAggregate()
  {
    boolean bool = readIfExists(false);
    DropAggregate localDropAggregate = new DropAggregate(this.session);
    localDropAggregate.setName(readUniqueIdentifier());
    bool = readIfExists(bool);
    localDropAggregate.setIfExists(bool);
    return localDropAggregate;
  }
  
  private TableFilter readJoin(TableFilter paramTableFilter, Select paramSelect, boolean paramBoolean1, boolean paramBoolean2)
  {
    int i = 0;
    Object localObject1 = paramTableFilter;
    boolean bool = this.database.getSettings().nestedJoins;
    for (;;)
    {
      TableFilter localTableFilter;
      Object localObject2;
      if (readIf("RIGHT"))
      {
        readIf("OUTER");
        read("JOIN");
        i = 1;
        
        localTableFilter = readTableFilter(paramBoolean2);
        localTableFilter = readJoin(localTableFilter, paramSelect, paramBoolean1, true);
        localObject2 = null;
        if (readIf("ON")) {
          localObject2 = readExpression();
        }
        if (bool)
        {
          paramTableFilter = getNested(paramTableFilter);
          localTableFilter.addJoin(paramTableFilter, true, false, (Expression)localObject2);
        }
        else
        {
          localTableFilter.addJoin(paramTableFilter, true, false, (Expression)localObject2);
        }
        paramTableFilter = localTableFilter;
        localObject1 = localTableFilter;
      }
      else if (readIf("LEFT"))
      {
        readIf("OUTER");
        read("JOIN");
        i = 1;
        localTableFilter = readTableFilter(true);
        if (bool) {
          localTableFilter = readJoin(localTableFilter, paramSelect, true, true);
        } else {
          paramTableFilter = readJoin(paramTableFilter, paramSelect, false, true);
        }
        localObject2 = null;
        if (readIf("ON")) {
          localObject2 = readExpression();
        }
        paramTableFilter.addJoin(localTableFilter, true, false, (Expression)localObject2);
        localObject1 = localTableFilter;
      }
      else
      {
        if (readIf("FULL")) {
          throw getSyntaxError();
        }
        if (readIf("INNER"))
        {
          read("JOIN");
          i = 1;
          localTableFilter = readTableFilter(paramBoolean2);
          paramTableFilter = readJoin(paramTableFilter, paramSelect, false, false);
          localObject2 = null;
          if (readIf("ON")) {
            localObject2 = readExpression();
          }
          if (bool) {
            paramTableFilter.addJoin(localTableFilter, false, false, (Expression)localObject2);
          } else {
            paramTableFilter.addJoin(localTableFilter, paramBoolean2, false, (Expression)localObject2);
          }
          localObject1 = localTableFilter;
        }
        else if (readIf("JOIN"))
        {
          i = 1;
          localTableFilter = readTableFilter(paramBoolean2);
          paramTableFilter = readJoin(paramTableFilter, paramSelect, false, false);
          localObject2 = null;
          if (readIf("ON")) {
            localObject2 = readExpression();
          }
          if (bool) {
            paramTableFilter.addJoin(localTableFilter, false, false, (Expression)localObject2);
          } else {
            paramTableFilter.addJoin(localTableFilter, paramBoolean2, false, (Expression)localObject2);
          }
          localObject1 = localTableFilter;
        }
        else if (readIf("CROSS"))
        {
          read("JOIN");
          i = 1;
          localTableFilter = readTableFilter(paramBoolean2);
          if (bool) {
            paramTableFilter.addJoin(localTableFilter, false, false, null);
          } else {
            paramTableFilter.addJoin(localTableFilter, paramBoolean2, false, null);
          }
          localObject1 = localTableFilter;
        }
        else
        {
          if (!readIf("NATURAL")) {
            break;
          }
          read("JOIN");
          i = 1;
          localTableFilter = readTableFilter(paramBoolean2);
          localObject2 = ((TableFilter)localObject1).getTable().getColumns();
          Column[] arrayOfColumn1 = localTableFilter.getTable().getColumns();
          String str1 = ((TableFilter)localObject1).getTable().getSchema().getName();
          String str2 = localTableFilter.getTable().getSchema().getName();
          Object localObject3 = null;
          for (Object localObject5 : localObject2)
          {
            String str3 = ((Column)localObject5).getName();
            for (Column localColumn : arrayOfColumn1)
            {
              String str4 = localColumn.getName();
              if (equalsToken(str3, str4))
              {
                localTableFilter.addNaturalJoinColumn(localColumn);
                ExpressionColumn localExpressionColumn1 = new ExpressionColumn(this.database, str1, ((TableFilter)localObject1).getTableAlias(), str3);
                
                ExpressionColumn localExpressionColumn2 = new ExpressionColumn(this.database, str2, localTableFilter.getTableAlias(), str4);
                
                Comparison localComparison = new Comparison(this.session, 0, localExpressionColumn1, localExpressionColumn2);
                if (localObject3 == null) {
                  localObject3 = localComparison;
                } else {
                  localObject3 = new ConditionAndOr(0, (Expression)localObject3, localComparison);
                }
              }
            }
          }
          if (bool) {
            paramTableFilter.addJoin(localTableFilter, false, paramBoolean1, (Expression)localObject3);
          } else {
            paramTableFilter.addJoin(localTableFilter, paramBoolean2, false, (Expression)localObject3);
          }
          localObject1 = localTableFilter;
        }
      }
    }
    if ((paramBoolean1) && (i != 0)) {
      paramTableFilter = getNested(paramTableFilter);
    }
    return paramTableFilter;
  }
  
  private TableFilter getNested(TableFilter paramTableFilter)
  {
    String str = "SYSTEM_JOIN_" + this.parseIndex;
    TableFilter localTableFilter = new TableFilter(this.session, getDualTable(true), str, this.rightsChecked, this.currentSelect);
    
    localTableFilter.addJoin(paramTableFilter, false, true, null);
    return localTableFilter;
  }
  
  private Prepared parseExecute()
  {
    ExecuteProcedure localExecuteProcedure = new ExecuteProcedure(this.session);
    String str = readAliasIdentifier();
    Procedure localProcedure = this.session.getProcedure(str);
    if (localProcedure == null) {
      throw DbException.get(90077, str);
    }
    localExecuteProcedure.setProcedure(localProcedure);
    if (readIf("(")) {
      for (int i = 0;; i++)
      {
        localExecuteProcedure.setExpression(i, readExpression());
        if (readIf(")")) {
          break;
        }
        read(",");
      }
    }
    return localExecuteProcedure;
  }
  
  private DeallocateProcedure parseDeallocate()
  {
    readIf("PLAN");
    String str = readAliasIdentifier();
    DeallocateProcedure localDeallocateProcedure = new DeallocateProcedure(this.session);
    localDeallocateProcedure.setProcedureName(str);
    return localDeallocateProcedure;
  }
  
  private Explain parseExplain()
  {
    Explain localExplain = new Explain(this.session);
    if (readIf("ANALYZE")) {
      localExplain.setExecuteCommand(true);
    } else if (readIf("PLAN")) {
      readIf("FOR");
    }
    if ((isToken("SELECT")) || (isToken("FROM")) || (isToken("("))) {
      localExplain.setCommand(parseSelect());
    } else if (readIf("DELETE")) {
      localExplain.setCommand(parseDelete());
    } else if (readIf("UPDATE")) {
      localExplain.setCommand(parseUpdate());
    } else if (readIf("INSERT")) {
      localExplain.setCommand(parseInsert());
    } else if (readIf("MERGE")) {
      localExplain.setCommand(parseMerge());
    } else if (readIf("WITH")) {
      localExplain.setCommand(parseWith());
    } else {
      throw getSyntaxError();
    }
    return localExplain;
  }
  
  private Query parseSelect()
  {
    int i = this.parameters.size();
    Query localQuery = parseSelectUnion();
    ArrayList localArrayList = New.arrayList();
    int j = i;
    for (int k = this.parameters.size(); j < k; j++) {
      localArrayList.add(this.parameters.get(j));
    }
    localQuery.setParameterList(localArrayList);
    localQuery.init();
    return localQuery;
  }
  
  private Query parseSelectUnion()
  {
    int i = this.lastParseIndex;
    Query localQuery = parseSelectSub();
    return parseSelectUnionExtension(localQuery, i, false);
  }
  
  private Query parseSelectUnionExtension(Query paramQuery, int paramInt, boolean paramBoolean)
  {
    for (;;)
    {
      SelectUnion localSelectUnion;
      if (readIf("UNION"))
      {
        localSelectUnion = new SelectUnion(this.session, paramQuery);
        if (readIf("ALL"))
        {
          localSelectUnion.setUnionType(1);
        }
        else
        {
          readIf("DISTINCT");
          localSelectUnion.setUnionType(0);
        }
        localSelectUnion.setRight(parseSelectSub());
        paramQuery = localSelectUnion;
      }
      else if ((readIf("MINUS")) || (readIf("EXCEPT")))
      {
        localSelectUnion = new SelectUnion(this.session, paramQuery);
        localSelectUnion.setUnionType(2);
        localSelectUnion.setRight(parseSelectSub());
        paramQuery = localSelectUnion;
      }
      else
      {
        if (!readIf("INTERSECT")) {
          break;
        }
        localSelectUnion = new SelectUnion(this.session, paramQuery);
        localSelectUnion.setUnionType(3);
        localSelectUnion.setRight(parseSelectSub());
        paramQuery = localSelectUnion;
      }
    }
    if (!paramBoolean) {
      parseEndOfQuery(paramQuery);
    }
    setSQL(paramQuery, null, paramInt);
    return paramQuery;
  }
  
  private void parseEndOfQuery(Query paramQuery)
  {
    Select localSelect;
    Object localObject1;
    if (readIf("ORDER"))
    {
      read("BY");
      localSelect = this.currentSelect;
      if ((paramQuery instanceof Select)) {
        this.currentSelect = ((Select)paramQuery);
      }
      localObject1 = New.arrayList();
      do
      {
        int i = 1;
        if (readIf("=")) {
          i = 0;
        }
        SelectOrderBy localSelectOrderBy = new SelectOrderBy();
        Expression localExpression = readExpression();
        if ((i != 0) && ((localExpression instanceof ValueExpression)) && (localExpression.getType() == 4))
        {
          localSelectOrderBy.columnIndexExpr = localExpression;
        }
        else if ((localExpression instanceof Parameter))
        {
          this.recompileAlways = true;
          localSelectOrderBy.columnIndexExpr = localExpression;
        }
        else
        {
          localSelectOrderBy.expression = localExpression;
        }
        if (readIf("DESC")) {
          localSelectOrderBy.descending = true;
        } else {
          readIf("ASC");
        }
        if (readIf("NULLS")) {
          if (readIf("FIRST"))
          {
            localSelectOrderBy.nullsFirst = true;
          }
          else
          {
            read("LAST");
            localSelectOrderBy.nullsLast = true;
          }
        }
        ((ArrayList)localObject1).add(localSelectOrderBy);
      } while (readIf(","));
      paramQuery.setOrder((ArrayList)localObject1);
      this.currentSelect = localSelect;
    }
    if (this.database.getMode().supportOffsetFetch)
    {
      localSelect = this.currentSelect;
      this.currentSelect = null;
      if (readIf("OFFSET"))
      {
        paramQuery.setOffset(readExpression().optimize(this.session));
        if (!readIf("ROW")) {
          read("ROWS");
        }
      }
      if (readIf("FETCH"))
      {
        if (!readIf("FIRST")) {
          read("NEXT");
        }
        if (readIf("ROW"))
        {
          paramQuery.setLimit(ValueExpression.get(ValueInt.get(1)));
        }
        else
        {
          localObject1 = readExpression().optimize(this.session);
          paramQuery.setLimit((Expression)localObject1);
          if (!readIf("ROW")) {
            read("ROWS");
          }
        }
        read("ONLY");
      }
      this.currentSelect = localSelect;
    }
    if (readIf("LIMIT"))
    {
      localSelect = this.currentSelect;
      
      this.currentSelect = null;
      localObject1 = readExpression().optimize(this.session);
      paramQuery.setLimit((Expression)localObject1);
      Object localObject2;
      if (readIf("OFFSET"))
      {
        localObject2 = readExpression().optimize(this.session);
        paramQuery.setOffset((Expression)localObject2);
      }
      else if (readIf(","))
      {
        localObject2 = localObject1;
        localObject1 = readExpression().optimize(this.session);
        paramQuery.setOffset((Expression)localObject2);
        paramQuery.setLimit((Expression)localObject1);
      }
      if (readIf("SAMPLE_SIZE"))
      {
        localObject2 = readExpression().optimize(this.session);
        paramQuery.setSampleSize((Expression)localObject2);
      }
      this.currentSelect = localSelect;
    }
    if (readIf("FOR")) {
      if (readIf("UPDATE"))
      {
        if (readIf("OF")) {
          do
          {
            readIdentifierWithSchema();
          } while (readIf(","));
        } else if (!readIf("NOWAIT")) {}
        paramQuery.setForUpdate(true);
      }
      else if ((readIf("READ")) || (readIf("FETCH")))
      {
        read("ONLY");
      }
    }
    if (this.database.getMode().isolationLevelInSelectOrInsertStatement) {
      parseIsolationClause();
    }
  }
  
  private void parseIsolationClause()
  {
    if (readIf("WITH")) {
      if ((readIf("RR")) || (readIf("RS")))
      {
        if (readIf("USE"))
        {
          read("AND");
          read("KEEP");
          if ((!readIf("SHARE")) && (!readIf("UPDATE")) && (readIf("EXCLUSIVE"))) {}
          read("LOCKS");
        }
      }
      else if ((readIf("CS")) || (!readIf("UR"))) {}
    }
  }
  
  private Query parseSelectSub()
  {
    if (readIf("("))
    {
      localObject = parseSelectUnion();
      read(")");
      return (Query)localObject;
    }
    Object localObject = parseSelectSimple();
    return (Query)localObject;
  }
  
  private void parseSelectSimpleFromPart(Select paramSelect)
  {
    do
    {
      TableFilter localTableFilter = readTableFilter(false);
      parseJoinTableFilter(localTableFilter, paramSelect);
    } while (readIf(","));
  }
  
  private void parseJoinTableFilter(TableFilter paramTableFilter, final Select paramSelect)
  {
    paramTableFilter = readJoin(paramTableFilter, paramSelect, false, paramTableFilter.isJoinOuter());
    paramSelect.addTableFilter(paramTableFilter, true);
    boolean bool = false;
    for (;;)
    {
      TableFilter localTableFilter1 = paramTableFilter.getNestedJoin();
      if (localTableFilter1 != null) {
        localTableFilter1.visit(new TableFilter.TableFilterVisitor()
        {
          public void accept(TableFilter paramAnonymousTableFilter)
          {
            paramSelect.addTableFilter(paramAnonymousTableFilter, false);
          }
        });
      }
      TableFilter localTableFilter2 = paramTableFilter.getJoin();
      if (localTableFilter2 == null) {
        break;
      }
      bool |= localTableFilter2.isJoinOuter();
      if (bool)
      {
        paramSelect.addTableFilter(localTableFilter2, false);
      }
      else
      {
        Expression localExpression = localTableFilter2.getJoinCondition();
        if (localExpression != null) {
          paramSelect.addCondition(localExpression);
        }
        localTableFilter2.removeJoinCondition();
        paramTableFilter.removeJoin();
        paramSelect.addTableFilter(localTableFilter2, true);
      }
      paramTableFilter = localTableFilter2;
    }
  }
  
  private void parseSelectSimpleSelectPart(Select paramSelect)
  {
    Select localSelect = this.currentSelect;
    
    this.currentSelect = null;
    Object localObject2;
    if (readIf("TOP"))
    {
      localObject1 = readTerm().optimize(this.session);
      paramSelect.setLimit((Expression)localObject1);
    }
    else if (readIf("LIMIT"))
    {
      localObject1 = readTerm().optimize(this.session);
      paramSelect.setOffset((Expression)localObject1);
      localObject2 = readTerm().optimize(this.session);
      paramSelect.setLimit((Expression)localObject2);
    }
    this.currentSelect = localSelect;
    if (readIf("DISTINCT")) {
      paramSelect.setDistinct(true);
    } else {
      readIf("ALL");
    }
    Object localObject1 = New.arrayList();
    do
    {
      if (readIf("*"))
      {
        ((ArrayList)localObject1).add(new Wildcard(null, null));
      }
      else
      {
        localObject2 = readExpression();
        if ((readIf("AS")) || (this.currentTokenType == 2))
        {
          String str = readAliasIdentifier();
          boolean bool = this.database.getSettings().aliasColumnName;
          bool |= this.database.getMode().aliasColumnName;
          localObject2 = new Alias((Expression)localObject2, str, bool);
        }
        ((ArrayList)localObject1).add(localObject2);
      }
    } while (readIf(","));
    paramSelect.setExpressions((ArrayList)localObject1);
  }
  
  private Select parseSelectSimple()
  {
    int i;
    if (readIf("SELECT")) {
      i = 0;
    } else if (readIf("FROM")) {
      i = 1;
    } else {
      throw getSyntaxError();
    }
    Select localSelect1 = new Select(this.session);
    int j = this.lastParseIndex;
    Select localSelect2 = this.currentSelect;
    this.currentSelect = localSelect1;
    this.currentPrepared = localSelect1;
    Object localObject1;
    Object localObject2;
    if (i != 0)
    {
      parseSelectSimpleFromPart(localSelect1);
      read("SELECT");
      parseSelectSimpleSelectPart(localSelect1);
    }
    else
    {
      parseSelectSimpleSelectPart(localSelect1);
      if (!readIf("FROM"))
      {
        localObject1 = getDualTable(false);
        localObject2 = new TableFilter(this.session, (Table)localObject1, null, this.rightsChecked, this.currentSelect);
        
        localSelect1.addTableFilter((TableFilter)localObject2, true);
      }
      else
      {
        parseSelectSimpleFromPart(localSelect1);
      }
    }
    if (readIf("WHERE"))
    {
      localObject1 = readExpression();
      localSelect1.addCondition((Expression)localObject1);
    }
    this.currentSelect = localSelect2;
    if (readIf("GROUP"))
    {
      read("BY");
      localSelect1.setGroupQuery();
      localObject1 = New.arrayList();
      do
      {
        localObject2 = readExpression();
        ((ArrayList)localObject1).add(localObject2);
      } while (readIf(","));
      localSelect1.setGroupBy((ArrayList)localObject1);
    }
    this.currentSelect = localSelect1;
    if (readIf("HAVING"))
    {
      localSelect1.setGroupQuery();
      localObject1 = readExpression();
      localSelect1.setHaving((Expression)localObject1);
    }
    localSelect1.setParameterList(this.parameters);
    this.currentSelect = localSelect2;
    setSQL(localSelect1, "SELECT", j);
    return localSelect1;
  }
  
  private Table getDualTable(boolean paramBoolean)
  {
    Schema localSchema = this.database.findSchema("PUBLIC");
    ValueExpression localValueExpression = ValueExpression.get(ValueLong.get(1L));
    return new RangeTable(localSchema, localValueExpression, localValueExpression, paramBoolean);
  }
  
  private void setSQL(Prepared paramPrepared, String paramString, int paramInt)
  {
    String str = this.originalSQL.substring(paramInt, this.lastParseIndex).trim();
    if (paramString != null) {
      str = paramString + " " + str;
    }
    paramPrepared.setSQL(str);
  }
  
  private Expression readExpression()
  {
    Object localObject = readAnd();
    while (readIf("OR")) {
      localObject = new ConditionAndOr(1, (Expression)localObject, readAnd());
    }
    return (Expression)localObject;
  }
  
  private Expression readAnd()
  {
    Object localObject = readCondition();
    while (readIf("AND")) {
      localObject = new ConditionAndOr(0, (Expression)localObject, readCondition());
    }
    return (Expression)localObject;
  }
  
  private Expression readCondition()
  {
    if (readIf("NOT")) {
      return new ConditionNot(readCondition());
    }
    if (readIf("EXISTS"))
    {
      read("(");
      localObject1 = parseSelect();
      
      read(")");
      return new ConditionExists((Query)localObject1);
    }
    if (readIf("INTERSECTS"))
    {
      read("(");
      localObject1 = readConcat();
      read(",");
      Expression localExpression = readConcat();
      read(")");
      return new Comparison(this.session, 11, (Expression)localObject1, localExpression);
    }
    Object localObject1 = readConcat();
    for (;;)
    {
      int i = this.parseIndex;
      int j = 0;
      if (readIf("NOT"))
      {
        j = 1;
        if (isToken("NULL"))
        {
          this.parseIndex = i;
          this.currentToken = "NOT";
          break;
        }
      }
      Object localObject2;
      Object localObject3;
      if (readIf("LIKE"))
      {
        localObject2 = readConcat();
        localObject3 = null;
        if (readIf("ESCAPE")) {
          localObject3 = readConcat();
        }
        this.recompileAlways = true;
        localObject1 = new CompareLike(this.database, (Expression)localObject1, (Expression)localObject2, (Expression)localObject3, false);
      }
      else if (readIf("REGEXP"))
      {
        localObject2 = readConcat();
        localObject1 = new CompareLike(this.database, (Expression)localObject1, (Expression)localObject2, null, true);
      }
      else if (readIf("IS"))
      {
        if (readIf("NOT"))
        {
          if (readIf("NULL"))
          {
            localObject1 = new Comparison(this.session, 7, (Expression)localObject1, null);
          }
          else if (readIf("DISTINCT"))
          {
            read("FROM");
            localObject1 = new Comparison(this.session, 16, (Expression)localObject1, readConcat());
          }
          else
          {
            localObject1 = new Comparison(this.session, 21, (Expression)localObject1, readConcat());
          }
        }
        else if (readIf("NULL"))
        {
          localObject1 = new Comparison(this.session, 6, (Expression)localObject1, null);
        }
        else if (readIf("DISTINCT"))
        {
          read("FROM");
          localObject1 = new Comparison(this.session, 21, (Expression)localObject1, readConcat());
        }
        else
        {
          localObject1 = new Comparison(this.session, 16, (Expression)localObject1, readConcat());
        }
      }
      else
      {
        Object localObject4;
        Object localObject5;
        if (readIf("IN"))
        {
          read("(");
          if (readIf(")"))
          {
            localObject1 = ValueExpression.get(ValueBoolean.get(false));
          }
          else
          {
            if (isSelect())
            {
              localObject2 = parseSelect();
              localObject1 = new ConditionInSelect(this.database, (Expression)localObject1, (Query)localObject2, false, 0);
            }
            else
            {
              localObject2 = New.arrayList();
              do
              {
                localObject3 = readExpression();
                ((ArrayList)localObject2).add(localObject3);
              } while (readIf(","));
              if ((((ArrayList)localObject2).size() == 1) && ((localObject3 instanceof Subquery)))
              {
                localObject4 = (Subquery)localObject3;
                localObject5 = ((Subquery)localObject4).getQuery();
                localObject1 = new ConditionInSelect(this.database, (Expression)localObject1, (Query)localObject5, false, 0);
              }
              else
              {
                localObject1 = new ConditionIn(this.database, (Expression)localObject1, (ArrayList)localObject2);
              }
            }
            read(")");
          }
        }
        else if (readIf("BETWEEN"))
        {
          localObject2 = readConcat();
          read("AND");
          localObject3 = readConcat();
          localObject4 = new Comparison(this.session, 3, (Expression)localObject2, (Expression)localObject1);
          
          localObject5 = new Comparison(this.session, 1, (Expression)localObject3, (Expression)localObject1);
          
          localObject1 = new ConditionAndOr(0, (Expression)localObject4, (Expression)localObject5);
        }
        else
        {
          int k = getCompareType(this.currentTokenType);
          if (k < 0) {
            break;
          }
          read();
          if (readIf("ALL"))
          {
            read("(");
            localObject3 = parseSelect();
            localObject1 = new ConditionInSelect(this.database, (Expression)localObject1, (Query)localObject3, true, k);
            
            read(")");
          }
          else if ((readIf("ANY")) || (readIf("SOME")))
          {
            read("(");
            localObject3 = parseSelect();
            localObject1 = new ConditionInSelect(this.database, (Expression)localObject1, (Query)localObject3, false, k);
            
            read(")");
          }
          else
          {
            localObject3 = readConcat();
            if ((SysProperties.OLD_STYLE_OUTER_JOIN) && (readIf("(")) && (readIf("+")) && (readIf(")")))
            {
              if (((localObject1 instanceof ExpressionColumn)) && ((localObject3 instanceof ExpressionColumn)))
              {
                localObject4 = (ExpressionColumn)localObject1;
                localObject5 = (ExpressionColumn)localObject3;
                ArrayList localArrayList = this.currentSelect.getTopFilters();
                for (Object localObject6 = localArrayList.iterator(); ((Iterator)localObject6).hasNext();)
                {
                  localTableFilter = (TableFilter)((Iterator)localObject6).next();
                  while (localTableFilter != null)
                  {
                    ((ExpressionColumn)localObject4).mapColumns(localTableFilter, 0);
                    ((ExpressionColumn)localObject5).mapColumns(localTableFilter, 0);
                    localTableFilter = localTableFilter.getJoin();
                  }
                }
                localObject6 = ((ExpressionColumn)localObject4).getTableFilter();
                TableFilter localTableFilter = ((ExpressionColumn)localObject5).getTableFilter();
                localObject1 = new Comparison(this.session, k, (Expression)localObject1, (Expression)localObject3);
                if ((localObject6 != null) && (localTableFilter != null))
                {
                  int m = localArrayList.indexOf(localTableFilter);
                  if (m >= 0)
                  {
                    localArrayList.remove(m);
                    ((TableFilter)localObject6).addJoin(localTableFilter, true, false, (Expression)localObject1);
                  }
                  else
                  {
                    localTableFilter.mapAndAddFilter((Expression)localObject1);
                  }
                  localObject1 = ValueExpression.get(ValueBoolean.get(true));
                }
              }
            }
            else {
              localObject1 = new Comparison(this.session, k, (Expression)localObject1, (Expression)localObject3);
            }
          }
        }
      }
      if (j != 0) {
        localObject1 = new ConditionNot((Expression)localObject1);
      }
    }
    return (Expression)localObject1;
  }
  
  private Expression readConcat()
  {
    Object localObject = readSum();
    for (;;)
    {
      if (readIf("||"))
      {
        localObject = new Operation(0, (Expression)localObject, readSum());
      }
      else
      {
        Function localFunction;
        if (readIf("~"))
        {
          if (readIf("*"))
          {
            localFunction = Function.getFunction(this.database, "CAST");
            localFunction.setDataType(new Column("X", 14));
            
            localFunction.setParameter(0, (Expression)localObject);
            localObject = localFunction;
          }
          localObject = new CompareLike(this.database, (Expression)localObject, readSum(), null, true);
        }
        else
        {
          if (!readIf("!~")) {
            break;
          }
          if (readIf("*"))
          {
            localFunction = Function.getFunction(this.database, "CAST");
            localFunction.setDataType(new Column("X", 14));
            
            localFunction.setParameter(0, (Expression)localObject);
            localObject = localFunction;
          }
          localObject = new ConditionNot(new CompareLike(this.database, (Expression)localObject, readSum(), null, true));
        }
      }
    }
    return (Expression)localObject;
  }
  
  private Expression readSum()
  {
    Object localObject = readFactor();
    for (;;)
    {
      if (readIf("+"))
      {
        localObject = new Operation(1, (Expression)localObject, readFactor());
      }
      else
      {
        if (!readIf("-")) {
          break;
        }
        localObject = new Operation(2, (Expression)localObject, readFactor());
      }
    }
    return (Expression)localObject;
  }
  
  private Expression readFactor()
  {
    Object localObject = readTerm();
    for (;;)
    {
      if (readIf("*"))
      {
        localObject = new Operation(3, (Expression)localObject, readTerm());
      }
      else if (readIf("/"))
      {
        localObject = new Operation(4, (Expression)localObject, readTerm());
      }
      else
      {
        if (!readIf("%")) {
          break;
        }
        localObject = new Operation(6, (Expression)localObject, readTerm());
      }
    }
    return (Expression)localObject;
  }
  
  private Expression readAggregate(int paramInt)
  {
    if (this.currentSelect == null) {
      throw getSyntaxError();
    }
    this.currentSelect.setGroupQuery();
    Object localObject1;
    boolean bool;
    Object localObject2;
    if (paramInt == 1)
    {
      if (readIf("*"))
      {
        localObject1 = new Aggregate(0, null, this.currentSelect, false);
      }
      else
      {
        bool = readIf("DISTINCT");
        localObject2 = readExpression();
        if (((localObject2 instanceof Wildcard)) && (!bool)) {
          localObject1 = new Aggregate(0, null, this.currentSelect, false);
        } else {
          localObject1 = new Aggregate(1, (Expression)localObject2, this.currentSelect, bool);
        }
      }
    }
    else if (paramInt == 2)
    {
      bool = readIf("DISTINCT");
      localObject2 = new Aggregate(2, readExpression(), this.currentSelect, bool);
      if (readIf("ORDER"))
      {
        read("BY");
        ((Aggregate)localObject2).setGroupConcatOrder(parseSimpleOrderList());
      }
      if (readIf("SEPARATOR")) {
        ((Aggregate)localObject2).setGroupConcatSeparator(readExpression());
      }
      localObject1 = localObject2;
    }
    else
    {
      bool = readIf("DISTINCT");
      localObject1 = new Aggregate(paramInt, readExpression(), this.currentSelect, bool);
    }
    read(")");
    return (Expression)localObject1;
  }
  
  private ArrayList<SelectOrderBy> parseSimpleOrderList()
  {
    ArrayList localArrayList = New.arrayList();
    do
    {
      SelectOrderBy localSelectOrderBy = new SelectOrderBy();
      Expression localExpression = readExpression();
      localSelectOrderBy.expression = localExpression;
      if (readIf("DESC")) {
        localSelectOrderBy.descending = true;
      } else {
        readIf("ASC");
      }
      localArrayList.add(localSelectOrderBy);
    } while (readIf(","));
    return localArrayList;
  }
  
  private JavaFunction readJavaFunction(Schema paramSchema, String paramString)
  {
    FunctionAlias localFunctionAlias = null;
    if (paramSchema != null) {
      localFunctionAlias = paramSchema.findFunction(paramString);
    } else {
      localFunctionAlias = findFunctionAlias(this.session.getCurrentSchemaName(), paramString);
    }
    if (localFunctionAlias == null) {
      throw DbException.get(90022, paramString);
    }
    ArrayList localArrayList = New.arrayList();
    int i = 0;
    while (!readIf(")"))
    {
      if (i++ > 0) {
        read(",");
      }
      localArrayList.add(readExpression());
    }
    Expression[] arrayOfExpression = new Expression[i];
    localArrayList.toArray(arrayOfExpression);
    JavaFunction localJavaFunction = new JavaFunction(localFunctionAlias, arrayOfExpression);
    return localJavaFunction;
  }
  
  private JavaAggregate readJavaAggregate(UserAggregate paramUserAggregate)
  {
    ArrayList localArrayList = New.arrayList();
    do
    {
      localArrayList.add(readExpression());
    } while (readIf(","));
    read(")");
    Expression[] arrayOfExpression = new Expression[localArrayList.size()];
    localArrayList.toArray(arrayOfExpression);
    JavaAggregate localJavaAggregate = new JavaAggregate(paramUserAggregate, arrayOfExpression, this.currentSelect);
    this.currentSelect.setGroupQuery();
    return localJavaAggregate;
  }
  
  private int getAggregateType(String paramString)
  {
    if (!this.identifiersToUpper) {
      paramString = StringUtils.toUpperEnglish(paramString);
    }
    return Aggregate.getAggregateType(paramString);
  }
  
  private Expression readFunction(Schema paramSchema, String paramString)
  {
    if (paramSchema != null) {
      return readJavaFunction(paramSchema, paramString);
    }
    int i = getAggregateType(paramString);
    if (i >= 0) {
      return readAggregate(i);
    }
    Function localFunction = Function.getFunction(this.database, paramString);
    Object localObject1;
    if (localFunction == null)
    {
      localObject1 = this.database.findAggregate(paramString);
      if (localObject1 != null) {
        return readJavaAggregate((UserAggregate)localObject1);
      }
      return readJavaFunction(null, paramString);
    }
    Object localObject2;
    int j;
    switch (localFunction.getFunctionType())
    {
    case 203: 
      localFunction.setParameter(0, readExpression());
      read("AS");
      localObject1 = parseColumnWithType(null);
      localFunction.setDataType((Column)localObject1);
      read(")");
      break;
    case 202: 
      if (this.database.getMode().swapConvertFunctionParameters)
      {
        localObject1 = parseColumnWithType(null);
        localFunction.setDataType((Column)localObject1);
        read(",");
        localFunction.setParameter(0, readExpression());
        read(")");
      }
      else
      {
        localFunction.setParameter(0, readExpression());
        read(",");
        localObject1 = parseColumnWithType(null);
        localFunction.setDataType((Column)localObject1);
        read(")");
      }
      break;
    case 120: 
      localFunction.setParameter(0, ValueExpression.get(ValueString.get(this.currentToken)));
      
      read();
      read("FROM");
      localFunction.setParameter(1, readExpression());
      read(")");
      break;
    case 102: 
    case 103: 
      if (Function.isDatePart(this.currentToken))
      {
        localFunction.setParameter(0, ValueExpression.get(ValueString.get(this.currentToken)));
        
        read();
      }
      else
      {
        localFunction.setParameter(0, readExpression());
      }
      read(",");
      localFunction.setParameter(1, readExpression());
      read(",");
      localFunction.setParameter(2, readExpression());
      read(")");
      break;
    case 73: 
      localFunction.setParameter(0, readExpression());
      if (readIf("FROM"))
      {
        localFunction.setParameter(1, readExpression());
        if (readIf("FOR")) {
          localFunction.setParameter(2, readExpression());
        }
      }
      else if (readIf("FOR"))
      {
        localFunction.setParameter(1, ValueExpression.get(ValueInt.get(0)));
        localFunction.setParameter(2, readExpression());
      }
      else
      {
        read(",");
        localFunction.setParameter(1, readExpression());
        if (readIf(",")) {
          localFunction.setParameter(2, readExpression());
        }
      }
      read(")");
      break;
    case 77: 
      localFunction.setParameter(0, readConcat());
      if (!readIf(",")) {
        read("IN");
      }
      localFunction.setParameter(1, readExpression());
      read(")");
      break;
    case 78: 
      localObject1 = null;
      if (readIf("LEADING"))
      {
        localFunction = Function.getFunction(this.database, "LTRIM");
        if (!readIf("FROM"))
        {
          localObject1 = readExpression();
          read("FROM");
        }
      }
      else if (readIf("TRAILING"))
      {
        localFunction = Function.getFunction(this.database, "RTRIM");
        if (!readIf("FROM"))
        {
          localObject1 = readExpression();
          read("FROM");
        }
      }
      else if ((readIf("BOTH")) && 
        (!readIf("FROM")))
      {
        localObject1 = readExpression();
        read("FROM");
      }
      localObject2 = readExpression();
      if (readIf(","))
      {
        localObject1 = readExpression();
      }
      else if (readIf("FROM"))
      {
        localObject1 = localObject2;
        localObject2 = readExpression();
      }
      localFunction.setParameter(0, (Expression)localObject2);
      if (localObject1 != null) {
        localFunction.setParameter(1, (Expression)localObject1);
      }
      read(")");
      break;
    case 223: 
    case 224: 
      j = 0;
      localObject2 = New.arrayList();
      do
      {
        localObject3 = readAliasIdentifier();
        Column localColumn = parseColumnWithType((String)localObject3);
        ((ArrayList)localObject2).add(localColumn);
        read("=");
        localFunction.setParameter(j, readExpression());
        j++;
      } while (readIf(","));
      read(")");
      Object localObject3 = (TableFunction)localFunction;
      ((TableFunction)localObject3).setColumns((ArrayList)localObject2);
      break;
    case 300: 
      read(")");
      read("OVER");
      read("(");
      read(")");
      return new Rownum(this.currentSelect == null ? this.currentPrepared : this.currentSelect);
    default: 
      if (!readIf(")"))
      {
        j = 0;
        do
        {
          localFunction.setParameter(j++, readExpression());
        } while (readIf(","));
        read(")");
      }
      break;
    }
    localFunction.doneWithParameters();
    return localFunction;
  }
  
  private Function readFunctionWithoutParameters(String paramString)
  {
    if (readIf("(")) {
      read(")");
    }
    Function localFunction = Function.getFunction(this.database, paramString);
    localFunction.doneWithParameters();
    return localFunction;
  }
  
  private Expression readWildcardOrSequenceValue(String paramString1, String paramString2)
  {
    if (readIf("*")) {
      return new Wildcard(paramString1, paramString2);
    }
    if (paramString1 == null) {
      paramString1 = this.session.getCurrentSchemaName();
    }
    Sequence localSequence;
    if (readIf("NEXTVAL"))
    {
      localSequence = findSequence(paramString1, paramString2);
      if (localSequence != null) {
        return new SequenceValue(localSequence);
      }
    }
    else if (readIf("CURRVAL"))
    {
      localSequence = findSequence(paramString1, paramString2);
      if (localSequence != null)
      {
        Function localFunction = Function.getFunction(this.database, "CURRVAL");
        localFunction.setParameter(0, ValueExpression.get(ValueString.get(localSequence.getSchema().getName())));
        
        localFunction.setParameter(1, ValueExpression.get(ValueString.get(localSequence.getName())));
        
        localFunction.doneWithParameters();
        return localFunction;
      }
    }
    return null;
  }
  
  private Expression readTermObjectDot(String paramString)
  {
    Expression localExpression = readWildcardOrSequenceValue(null, paramString);
    if (localExpression != null) {
      return localExpression;
    }
    String str1 = readColumnIdentifier();
    Schema localSchema = this.database.findSchema(paramString);
    if (((!SysProperties.OLD_STYLE_OUTER_JOIN) || (localSchema != null)) && (readIf("("))) {
      return readFunction(localSchema, str1);
    }
    if (readIf("."))
    {
      String str2 = paramString;
      paramString = str1;
      localExpression = readWildcardOrSequenceValue(str2, paramString);
      if (localExpression != null) {
        return localExpression;
      }
      str1 = readColumnIdentifier();
      String str3;
      if (readIf("("))
      {
        str3 = str2;
        if (!equalsToken(this.database.getShortName(), str3)) {
          throw DbException.get(90013, str3);
        }
        str2 = paramString;
        return readFunction(this.database.getSchema(str2), str1);
      }
      if (readIf("."))
      {
        str3 = str2;
        if (!equalsToken(this.database.getShortName(), str3)) {
          throw DbException.get(90013, str3);
        }
        str2 = paramString;
        paramString = str1;
        localExpression = readWildcardOrSequenceValue(str2, paramString);
        if (localExpression != null) {
          return localExpression;
        }
        str1 = readColumnIdentifier();
        return new ExpressionColumn(this.database, str2, paramString, str1);
      }
      return new ExpressionColumn(this.database, str2, paramString, str1);
    }
    return new ExpressionColumn(this.database, null, paramString, str1);
  }
  
  private Expression readTerm()
  {
    Object localObject1;
    Object localObject3;
    Object localObject4;
    Object localObject5;
    switch (this.currentTokenType)
    {
    case 12: 
      read();
      localObject1 = new Variable(this.session, readAliasIdentifier());
      if (readIf(":="))
      {
        Expression localExpression = readExpression();
        localObject3 = Function.getFunction(this.database, "SET");
        ((Function)localObject3).setParameter(0, (Expression)localObject1);
        ((Function)localObject3).setParameter(1, localExpression);
        localObject1 = localObject3;
      }
      break;
    case 3: 
      boolean bool = Character.isDigit(this.sqlCommandChars[this.parseIndex]);
      read();
      if ((bool) && (this.currentTokenType == 5) && (this.currentValue.getType() == 4))
      {
        if (this.indexedParameterList == null)
        {
          if (this.parameters == null) {
            throw getSyntaxError();
          }
          if (this.parameters.size() > 0) {
            throw DbException.get(90123);
          }
          this.indexedParameterList = New.arrayList();
        }
        int i = this.currentValue.getInt() - 1;
        if ((i < 0) || (i >= 100000)) {
          throw DbException.getInvalidValueException("parameter index", Integer.valueOf(i));
        }
        if (this.indexedParameterList.size() <= i)
        {
          this.indexedParameterList.ensureCapacity(i + 1);
          while (this.indexedParameterList.size() <= i) {
            this.indexedParameterList.add(null);
          }
        }
        localObject3 = (Parameter)this.indexedParameterList.get(i);
        if (localObject3 == null)
        {
          localObject3 = new Parameter(i);
          this.indexedParameterList.set(i, localObject3);
        }
        read();
      }
      else
      {
        if (this.indexedParameterList != null) {
          throw DbException.get(90123);
        }
        localObject3 = new Parameter(this.parameters.size());
      }
      this.parameters.add(localObject3);
      localObject1 = localObject3;
      break;
    case 1: 
      if ((isToken("SELECT")) || (isToken("FROM")))
      {
        localObject4 = parseSelect();
        localObject1 = new Subquery((Query)localObject4);
      }
      else
      {
        throw getSyntaxError();
      }
      break;
    case 2: 
      localObject4 = this.currentToken;
      if (this.currentTokenQuoted)
      {
        read();
        if (readIf("(")) {
          localObject1 = readFunction(null, (String)localObject4);
        } else if (readIf(".")) {
          localObject1 = readTermObjectDot((String)localObject4);
        } else {
          localObject1 = new ExpressionColumn(this.database, null, null, (String)localObject4);
        }
      }
      else
      {
        read();
        if (readIf("."))
        {
          localObject1 = readTermObjectDot((String)localObject4);
        }
        else if (equalsToken("CASE", (String)localObject4))
        {
          localObject1 = readCase();
        }
        else if (readIf("("))
        {
          localObject1 = readFunction(null, (String)localObject4);
        }
        else if (equalsToken("CURRENT_USER", (String)localObject4))
        {
          localObject1 = readFunctionWithoutParameters("USER");
        }
        else if (equalsToken("CURRENT", (String)localObject4))
        {
          if (readIf("TIMESTAMP")) {
            localObject1 = readFunctionWithoutParameters("CURRENT_TIMESTAMP");
          } else if (readIf("TIME")) {
            localObject1 = readFunctionWithoutParameters("CURRENT_TIME");
          } else if (readIf("DATE")) {
            localObject1 = readFunctionWithoutParameters("CURRENT_DATE");
          } else {
            localObject1 = new ExpressionColumn(this.database, null, null, (String)localObject4);
          }
        }
        else if ((equalsToken("NEXT", (String)localObject4)) && (readIf("VALUE")))
        {
          read("FOR");
          localObject5 = readSequence();
          localObject1 = new SequenceValue((Sequence)localObject5);
        }
        else if ((this.currentTokenType == 5) && (this.currentValue.getType() == 13))
        {
          if ((equalsToken("DATE", (String)localObject4)) || (equalsToken("D", (String)localObject4)))
          {
            localObject5 = this.currentValue.getString();
            read();
            localObject1 = ValueExpression.get(ValueDate.parse((String)localObject5));
          }
          else if ((equalsToken("TIME", (String)localObject4)) || (equalsToken("T", (String)localObject4)))
          {
            localObject5 = this.currentValue.getString();
            read();
            localObject1 = ValueExpression.get(ValueTime.parse((String)localObject5));
          }
          else if ((equalsToken("TIMESTAMP", (String)localObject4)) || (equalsToken("TS", (String)localObject4)))
          {
            localObject5 = this.currentValue.getString();
            read();
            localObject1 = ValueExpression.get(ValueTimestamp.parse((String)localObject5));
          }
          else if (equalsToken("X", (String)localObject4))
          {
            read();
            localObject5 = StringUtils.convertHexToBytes(this.currentValue.getString());
            
            localObject1 = ValueExpression.get(ValueBytes.getNoCopy((byte[])localObject5));
          }
          else if (equalsToken("E", (String)localObject4))
          {
            localObject5 = this.currentValue.getString();
            
            localObject5 = StringUtils.replaceAll((String)localObject5, "\\\\", "\\");
            read();
            localObject1 = ValueExpression.get(ValueString.get((String)localObject5));
          }
          else if (equalsToken("N", (String)localObject4))
          {
            localObject5 = this.currentValue.getString();
            read();
            localObject1 = ValueExpression.get(ValueString.get((String)localObject5));
          }
          else
          {
            localObject1 = new ExpressionColumn(this.database, null, null, (String)localObject4);
          }
        }
        else
        {
          localObject1 = new ExpressionColumn(this.database, null, null, (String)localObject4);
        }
      }
      break;
    case 13: 
      read();
      if (this.currentTokenType == 5)
      {
        localObject1 = ValueExpression.get(this.currentValue.negate());
        if ((((Expression)localObject1).getType() == 5) && (((Expression)localObject1).getValue(this.session).getLong() == -2147483648L)) {
          localObject1 = ValueExpression.get(ValueInt.get(Integer.MIN_VALUE));
        } else if ((((Expression)localObject1).getType() == 6) && (((Expression)localObject1).getValue(this.session).getBigDecimal().compareTo(ValueLong.MIN_BD) == 0)) {
          localObject1 = ValueExpression.get(ValueLong.get(Long.MIN_VALUE));
        }
        read();
      }
      else
      {
        localObject1 = new Operation(5, readTerm(), null);
      }
      break;
    case 14: 
      read();
      localObject1 = readTerm();
      break;
    case 16: 
      read();
      if (readIf(")"))
      {
        localObject1 = new ExpressionList(new Expression[0]);
      }
      else
      {
        localObject1 = readExpression();
        if (readIf(","))
        {
          localObject5 = New.arrayList();
          ((ArrayList)localObject5).add(localObject1);
          while (!readIf(")"))
          {
            localObject1 = readExpression();
            ((ArrayList)localObject5).add(localObject1);
            if (!readIf(",")) {
              read(")");
            }
          }
          Expression[] arrayOfExpression = new Expression[((ArrayList)localObject5).size()];
          ((ArrayList)localObject5).toArray(arrayOfExpression);
          localObject1 = new ExpressionList(arrayOfExpression);
        }
        else
        {
          read(")");
        }
      }
      break;
    case 19: 
      read();
      localObject1 = ValueExpression.get(ValueBoolean.get(true));
      break;
    case 20: 
      read();
      localObject1 = ValueExpression.get(ValueBoolean.get(false));
      break;
    case 23: 
      read();
      localObject1 = readFunctionWithoutParameters("CURRENT_TIME");
      break;
    case 22: 
      read();
      localObject1 = readFunctionWithoutParameters("CURRENT_DATE");
      break;
    case 21: 
      localObject5 = Function.getFunction(this.database, "CURRENT_TIMESTAMP");
      
      read();
      if ((readIf("(")) && 
        (!readIf(")")))
      {
        ((Function)localObject5).setParameter(0, readExpression());
        read(")");
      }
      ((Function)localObject5).doneWithParameters();
      localObject1 = localObject5;
      break;
    case 24: 
      read();
      if (readIf("(")) {
        read(")");
      }
      localObject1 = new Rownum(this.currentSelect == null ? this.currentPrepared : this.currentSelect);
      
      break;
    case 18: 
      read();
      localObject1 = ValueExpression.getNull();
      break;
    case 5: 
      localObject1 = ValueExpression.get(this.currentValue);
      read();
      break;
    case 4: 
    case 6: 
    case 7: 
    case 8: 
    case 9: 
    case 10: 
    case 11: 
    case 15: 
    case 17: 
    default: 
      throw getSyntaxError();
    }
    Object localObject2;
    if (readIf("["))
    {
      localObject2 = Function.getFunction(this.database, "ARRAY_GET");
      ((Function)localObject2).setParameter(0, (Expression)localObject1);
      localObject1 = readExpression();
      localObject1 = new Operation(1, (Expression)localObject1, ValueExpression.get(ValueInt.get(1)));
      
      ((Function)localObject2).setParameter(1, (Expression)localObject1);
      localObject1 = localObject2;
      read("]");
    }
    if (readIf("::"))
    {
      if (isToken("PG_CATALOG"))
      {
        read("PG_CATALOG");
        read(".");
      }
      if (readIf("REGCLASS"))
      {
        localObject2 = findFunctionAlias("PUBLIC", "PG_GET_OID");
        if (localObject2 == null) {
          throw getSyntaxError();
        }
        localObject3 = new Expression[] { localObject1 };
        localObject4 = new JavaFunction((FunctionAlias)localObject2, (Expression[])localObject3);
        localObject1 = localObject4;
      }
      else
      {
        localObject2 = parseColumnWithType(null);
        localObject3 = Function.getFunction(this.database, "CAST");
        ((Function)localObject3).setDataType((Column)localObject2);
        ((Function)localObject3).setParameter(0, (Expression)localObject1);
        localObject1 = localObject3;
      }
    }
    return (Expression)localObject1;
  }
  
  private Expression readCase()
  {
    if (readIf("END"))
    {
      readIf("CASE");
      return ValueExpression.getNull();
    }
    if (readIf("ELSE"))
    {
      Expression localExpression1 = readExpression().optimize(this.session);
      read("END");
      readIf("CASE");
      return localExpression1;
    }
    Function localFunction;
    int i;
    if (readIf("WHEN"))
    {
      localFunction = Function.getFunction(this.database, "CASE");
      localFunction.setParameter(0, null);
      i = 1;
      do
      {
        localFunction.setParameter(i++, readExpression());
        read("THEN");
        localFunction.setParameter(i++, readExpression());
      } while (readIf("WHEN"));
    }
    else
    {
      Expression localExpression2 = readExpression();
      if (readIf("END"))
      {
        readIf("CASE");
        return ValueExpression.getNull();
      }
      if (readIf("ELSE"))
      {
        Expression localExpression3 = readExpression().optimize(this.session);
        read("END");
        readIf("CASE");
        return localExpression3;
      }
      localFunction = Function.getFunction(this.database, "CASE");
      localFunction.setParameter(0, localExpression2);
      i = 1;
      read("WHEN");
      do
      {
        localFunction.setParameter(i++, readExpression());
        read("THEN");
        localFunction.setParameter(i++, readExpression());
      } while (readIf("WHEN"));
    }
    if (readIf("ELSE")) {
      localFunction.setParameter(i, readExpression());
    }
    read("END");
    readIf("CASE");
    localFunction.doneWithParameters();
    return localFunction;
  }
  
  private int readPositiveInt()
  {
    int i = readInt();
    if (i < 0) {
      throw DbException.getInvalidValueException("positive integer", Integer.valueOf(i));
    }
    return i;
  }
  
  private int readInt()
  {
    int i = 0;
    if (this.currentTokenType == 13)
    {
      i = 1;
      read();
    }
    else if (this.currentTokenType == 14)
    {
      read();
    }
    if (this.currentTokenType != 5) {
      throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "integer");
    }
    if (i != 0) {
      this.currentValue = this.currentValue.negate();
    }
    int j = this.currentValue.getInt();
    read();
    return j;
  }
  
  private long readLong()
  {
    int i = 0;
    if (this.currentTokenType == 13)
    {
      i = 1;
      read();
    }
    else if (this.currentTokenType == 14)
    {
      read();
    }
    if (this.currentTokenType != 5) {
      throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "long");
    }
    if (i != 0) {
      this.currentValue = this.currentValue.negate();
    }
    long l = this.currentValue.getLong();
    read();
    return l;
  }
  
  private boolean readBooleanSetting()
  {
    if (this.currentTokenType == 5)
    {
      boolean bool = this.currentValue.getBoolean().booleanValue();
      read();
      return bool;
    }
    if ((readIf("TRUE")) || (readIf("ON"))) {
      return true;
    }
    if ((readIf("FALSE")) || (readIf("OFF"))) {
      return false;
    }
    throw getSyntaxError();
  }
  
  private String readString()
  {
    Expression localExpression = readExpression().optimize(this.session);
    if (!(localExpression instanceof ValueExpression)) {
      throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "string");
    }
    String str = localExpression.getValue(this.session).getString();
    return str;
  }
  
  private String readIdentifierWithSchema(String paramString)
  {
    if (this.currentTokenType != 2) {
      throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "identifier");
    }
    String str = this.currentToken;
    read();
    this.schemaName = paramString;
    if (readIf("."))
    {
      this.schemaName = str;
      if (this.currentTokenType != 2) {
        throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "identifier");
      }
      str = this.currentToken;
      read();
    }
    if ((equalsToken(".", this.currentToken)) && 
      (equalsToken(this.schemaName, this.database.getShortName())))
    {
      read(".");
      this.schemaName = str;
      if (this.currentTokenType != 2) {
        throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "identifier");
      }
      str = this.currentToken;
      read();
    }
    return str;
  }
  
  private String readIdentifierWithSchema()
  {
    return readIdentifierWithSchema(this.session.getCurrentSchemaName());
  }
  
  private String readAliasIdentifier()
  {
    return readColumnIdentifier();
  }
  
  private String readUniqueIdentifier()
  {
    return readColumnIdentifier();
  }
  
  private String readColumnIdentifier()
  {
    if (this.currentTokenType != 2) {
      throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "identifier");
    }
    String str = this.currentToken;
    read();
    return str;
  }
  
  private void read(String paramString)
  {
    if ((this.currentTokenQuoted) || (!equalsToken(paramString, this.currentToken)))
    {
      addExpected(paramString);
      throw getSyntaxError();
    }
    read();
  }
  
  private boolean readIf(String paramString)
  {
    if ((!this.currentTokenQuoted) && (equalsToken(paramString, this.currentToken)))
    {
      read();
      return true;
    }
    addExpected(paramString);
    return false;
  }
  
  private boolean isToken(String paramString)
  {
    int i = (equalsToken(paramString, this.currentToken)) && (!this.currentTokenQuoted) ? 1 : 0;
    if (i != 0) {
      return true;
    }
    addExpected(paramString);
    return false;
  }
  
  private boolean equalsToken(String paramString1, String paramString2)
  {
    if (paramString1 == null) {
      return paramString2 == null;
    }
    if (paramString1.equals(paramString2)) {
      return true;
    }
    if ((!this.identifiersToUpper) && (paramString1.equalsIgnoreCase(paramString2))) {
      return true;
    }
    return false;
  }
  
  private void addExpected(String paramString)
  {
    if (this.expectedList != null) {
      this.expectedList.add(paramString);
    }
  }
  
  private void read()
  {
    this.currentTokenQuoted = false;
    if (this.expectedList != null) {
      this.expectedList.clear();
    }
    int[] arrayOfInt = this.characterTypes;
    this.lastParseIndex = this.parseIndex;
    int i = this.parseIndex;
    int j = arrayOfInt[i];
    while (j == 0) {
      j = arrayOfInt[(++i)];
    }
    int k = i;
    char[] arrayOfChar = this.sqlCommandChars;
    int m = arrayOfChar[(i++)];
    this.currentToken = "";
    String str2;
    int i1;
    switch (j)
    {
    case 4: 
      for (;;)
      {
        j = arrayOfInt[i];
        if ((j != 4) && (j != 2)) {
          break;
        }
        i++;
      }
      this.currentToken = StringUtils.fromCacheOrNew(this.sqlCommand.substring(k, i));
      
      this.currentTokenType = getTokenType(this.currentToken);
      this.parseIndex = i;
      return;
    case 3: 
      String str1 = null;
      for (;;)
      {
        for (int n = i;; i++) {
          if (arrayOfChar[i] == '"')
          {
            if (str1 == null)
            {
              str1 = this.sqlCommand.substring(n, i); break;
            }
            str1 = str1 + this.sqlCommand.substring(n - 1, i);
            
            break;
          }
        }
        if (arrayOfChar[(++i)] != '"') {
          break;
        }
        i++;
      }
      this.currentToken = StringUtils.fromCacheOrNew(str1);
      this.parseIndex = i;
      this.currentTokenQuoted = true;
      this.currentTokenType = 2;
      return;
    case 6: 
      if (arrayOfInt[i] == 6) {
        i++;
      }
      this.currentToken = this.sqlCommand.substring(k, i);
      this.currentTokenType = getSpecialType(this.currentToken);
      this.parseIndex = i;
      return;
    case 5: 
      this.currentToken = this.sqlCommand.substring(k, i);
      this.currentTokenType = getSpecialType(this.currentToken);
      this.parseIndex = i;
      return;
    case 2: 
      if ((m == 48) && (arrayOfChar[i] == 'X'))
      {
        l = 0L;
        k += 2;
        i++;
        for (;;)
        {
          m = arrayOfChar[i];
          if (((m < 48) || (m > 57)) && ((m < 65) || (m > 70)))
          {
            checkLiterals(false);
            this.currentValue = ValueInt.get((int)l);
            this.currentTokenType = 5;
            this.currentToken = "0";
            this.parseIndex = i;
            return;
          }
          l = (l << 4) + m - (m >= 65 ? 55 : 48);
          if (l > 2147483647L)
          {
            readHexDecimal(k, i);
            return;
          }
          i++;
        }
      }
      long l = m - 48;
      for (;;)
      {
        m = arrayOfChar[i];
        if ((m < 48) || (m > 57))
        {
          if ((m == 46) || (m == 69) || (m == 76))
          {
            readDecimal(k, i);
            break;
          }
          checkLiterals(false);
          this.currentValue = ValueInt.get((int)l);
          this.currentTokenType = 5;
          this.currentToken = "0";
          this.parseIndex = i;
          break;
        }
        l = l * 10L + (m - 48);
        if (l > 2147483647L)
        {
          readDecimal(k, i);
          break;
        }
        i++;
      }
      return;
    case 8: 
      if (arrayOfInt[i] != 2)
      {
        this.currentTokenType = 1;
        this.currentToken = ".";
        this.parseIndex = i;
        return;
      }
      readDecimal(i - 1, i);
      return;
    case 7: 
      str2 = null;
      for (;;)
      {
        for (i1 = i;; i++) {
          if (arrayOfChar[i] == '\'')
          {
            if (str2 == null)
            {
              str2 = this.sqlCommand.substring(i1, i); break;
            }
            str2 = str2 + this.sqlCommand.substring(i1 - 1, i);
            
            break;
          }
        }
        if (arrayOfChar[(++i)] != '\'') {
          break;
        }
        i++;
      }
      this.currentToken = "'";
      checkLiterals(true);
      this.currentValue = ValueString.get(StringUtils.fromCacheOrNew(str2), this.database.getMode().treatEmptyStringsAsNull);
      
      this.parseIndex = i;
      this.currentTokenType = 5;
      return;
    case 9: 
      str2 = null;
      i1 = i - 1;
      while (arrayOfInt[i] == 9) {
        i++;
      }
      str2 = this.sqlCommand.substring(i1, i);
      this.currentToken = "'";
      checkLiterals(true);
      this.currentValue = ValueString.get(StringUtils.fromCacheOrNew(str2), this.database.getMode().treatEmptyStringsAsNull);
      
      this.parseIndex = i;
      this.currentTokenType = 5;
      return;
    case 1: 
      this.currentToken = "";
      this.currentTokenType = 4;
      this.parseIndex = i;
      return;
    }
    throw getSyntaxError();
  }
  
  private void checkLiterals(boolean paramBoolean)
  {
    if (!this.session.getAllowLiterals())
    {
      int i = this.database.getAllowLiterals();
      if ((i == 0) || ((paramBoolean) && (i != 2))) {
        throw DbException.get(90116);
      }
    }
  }
  
  private void readHexDecimal(int paramInt1, int paramInt2)
  {
    char[] arrayOfChar = this.sqlCommandChars;
    int i;
    do
    {
      i = arrayOfChar[(++paramInt2)];
    } while (((i >= 48) && (i <= 57)) || ((i >= 65) && (i <= 70)));
    this.parseIndex = paramInt2;
    String str = this.sqlCommand.substring(paramInt1, paramInt2);
    BigDecimal localBigDecimal = new BigDecimal(new BigInteger(str, 16));
    checkLiterals(false);
    this.currentValue = ValueDecimal.get(localBigDecimal);
    this.currentTokenType = 5;
  }
  
  private void readDecimal(int paramInt1, int paramInt2)
  {
    char[] arrayOfChar = this.sqlCommandChars;
    int[] arrayOfInt = this.characterTypes;
    for (;;)
    {
      i = arrayOfInt[paramInt2];
      if ((i != 8) && (i != 2)) {
        break;
      }
      paramInt2++;
    }
    int i = 0;
    if ((arrayOfChar[paramInt2] == 'E') || (arrayOfChar[paramInt2] == 'e'))
    {
      i = 1;
      paramInt2++;
      if ((arrayOfChar[paramInt2] == '+') || (arrayOfChar[paramInt2] == '-')) {
        paramInt2++;
      }
      if (arrayOfInt[paramInt2] != 2) {
        throw getSyntaxError();
      }
      while (arrayOfInt[(++paramInt2)] == 2) {}
    }
    this.parseIndex = paramInt2;
    String str = this.sqlCommand.substring(paramInt1, paramInt2);
    checkLiterals(false);
    Object localObject;
    if ((i == 0) && (str.indexOf('.') < 0))
    {
      localObject = new BigInteger(str);
      if (((BigInteger)localObject).compareTo(ValueLong.MAX) <= 0)
      {
        if (arrayOfChar[paramInt2] == 'L') {
          this.parseIndex += 1;
        }
        this.currentValue = ValueLong.get(((BigInteger)localObject).longValue());
        this.currentTokenType = 5;
        return;
      }
    }
    try
    {
      localObject = new BigDecimal(str);
    }
    catch (NumberFormatException localNumberFormatException)
    {
      throw DbException.get(22018, localNumberFormatException, new String[] { str });
    }
    this.currentValue = ValueDecimal.get((BigDecimal)localObject);
    this.currentTokenType = 5;
  }
  
  public Session getSession()
  {
    return this.session;
  }
  
  private void initialize(String paramString)
  {
    if (paramString == null) {
      paramString = "";
    }
    this.originalSQL = paramString;
    this.sqlCommand = paramString;
    int i = paramString.length() + 1;
    char[] arrayOfChar = new char[i];
    int[] arrayOfInt = new int[i];
    i--;
    paramString.getChars(0, i, arrayOfChar, 0);
    int j = 0;
    arrayOfChar[i] = ' ';
    int k = 0;
    int m = 0;
    for (int n = 0; n < i; n++)
    {
      char c1 = arrayOfChar[n];
      int i1 = 0;
      switch (c1)
      {
      case '/': 
        if (arrayOfChar[(n + 1)] == '*')
        {
          j = 1;
          arrayOfChar[n] = ' ';
          arrayOfChar[(n + 1)] = ' ';
          k = n;
          n += 2;
          checkRunOver(n, i, k);
          while ((arrayOfChar[n] != '*') || (arrayOfChar[(n + 1)] != '/'))
          {
            arrayOfChar[(n++)] = ' ';
            checkRunOver(n, i, k);
          }
          arrayOfChar[n] = ' ';
          arrayOfChar[(n + 1)] = ' ';
          n++;
        }
        else
        {
          if (arrayOfChar[(n + 1)] == '/')
          {
            j = 1;
            k = n;
            for (;;)
            {
              c1 = arrayOfChar[n];
              if ((c1 == '\n') || (c1 == '\r') || (n >= i - 1)) {
                break;
              }
              arrayOfChar[(n++)] = ' ';
              checkRunOver(n, i, k);
            }
          }
          i1 = 5;
        }
        break;
      case '-': 
        if (arrayOfChar[(n + 1)] == '-')
        {
          j = 1;
          k = n;
          for (;;)
          {
            c1 = arrayOfChar[n];
            if ((c1 == '\n') || (c1 == '\r') || (n >= i - 1)) {
              break;
            }
            arrayOfChar[(n++)] = ' ';
            checkRunOver(n, i, k);
          }
        }
        i1 = 5;
        
        break;
      case '$': 
        if ((arrayOfChar[(n + 1)] == '$') && ((n == 0) || (arrayOfChar[(n - 1)] <= ' ')))
        {
          j = 1;
          arrayOfChar[n] = ' ';
          arrayOfChar[(n + 1)] = ' ';
          k = n;
          n += 2;
          checkRunOver(n, i, k);
          while ((arrayOfChar[n] != '$') || (arrayOfChar[(n + 1)] != '$'))
          {
            arrayOfInt[(n++)] = 9;
            checkRunOver(n, i, k);
          }
          arrayOfChar[n] = ' ';
          arrayOfChar[(n + 1)] = ' ';
          n++;
        }
        else if ((m == 4) || (m == 2))
        {
          i1 = 4;
        }
        else
        {
          i1 = 5;
        }
        break;
      case '%': 
      case '(': 
      case ')': 
      case '*': 
      case '+': 
      case ',': 
      case ';': 
      case '?': 
      case '@': 
      case ']': 
      case '{': 
      case '}': 
        i1 = 5;
        break;
      case '!': 
      case '&': 
      case ':': 
      case '<': 
      case '=': 
      case '>': 
      case '|': 
      case '~': 
        i1 = 6;
        break;
      case '.': 
        i1 = 8;
        break;
      case '\'': 
        i1 = arrayOfInt[n] = 7;
        k = n;
      case '[': 
      case '`': 
      case '"': 
      case '_': 
      case '#': 
      case '0': 
      case '1': 
      case '2': 
      case '3': 
      case '4': 
      case '5': 
      case '6': 
      case '7': 
      case '8': 
      case '9': 
      case 'A': 
      case 'B': 
      case 'C': 
      case 'D': 
      case 'E': 
      case 'F': 
      case 'G': 
      case 'H': 
      case 'I': 
      case 'J': 
      case 'K': 
      case 'L': 
      case 'M': 
      case 'N': 
      case 'O': 
      case 'P': 
      case 'Q': 
      case 'R': 
      case 'S': 
      case 'T': 
      case 'U': 
      case 'V': 
      case 'W': 
      case 'X': 
      case 'Y': 
      case 'Z': 
      case '\\': 
      case '^': 
      case 'a': 
      case 'b': 
      case 'c': 
      case 'd': 
      case 'e': 
      case 'f': 
      case 'g': 
      case 'h': 
      case 'i': 
      case 'j': 
      case 'k': 
      case 'l': 
      case 'm': 
      case 'n': 
      case 'o': 
      case 'p': 
      case 'q': 
      case 'r': 
      case 's': 
      case 't': 
      case 'u': 
      case 'v': 
      case 'w': 
      case 'x': 
      case 'y': 
      case 'z': 
      default: 
        while (arrayOfChar[(++n)] != '\'')
        {
          checkRunOver(n, i, k); continue;
          if (this.database.getMode().squareBracketQuotedNames)
          {
            arrayOfChar[n] = '"';
            j = 1;
            i1 = arrayOfInt[n] = 3;
            k = n;
            while (arrayOfChar[(++n)] != ']') {
              checkRunOver(n, i, k);
            }
            arrayOfChar[n] = '"';
          }
          else
          {
            i1 = 5;
            
            break;
            
            arrayOfChar[n] = '"';
            j = 1;
            i1 = arrayOfInt[n] = 3;
            k = n;
            while (arrayOfChar[(++n)] != '`')
            {
              checkRunOver(n, i, k);
              c1 = arrayOfChar[n];
              arrayOfChar[n] = Character.toUpperCase(c1);
            }
            arrayOfChar[n] = '"';
            break;
            
            i1 = arrayOfInt[n] = 3;
            k = n;
            while (arrayOfChar[(++n)] != '"')
            {
              checkRunOver(n, i, k); continue;
              
              i1 = 4;
              break;
              if ((c1 >= 'a') && (c1 <= 'z'))
              {
                if (this.identifiersToUpper)
                {
                  arrayOfChar[n] = ((char)(c1 - ' '));
                  j = 1;
                }
                i1 = 4;
              }
              else if ((c1 >= 'A') && (c1 <= 'Z'))
              {
                i1 = 4;
              }
              else if ((c1 >= '0') && (c1 <= '9'))
              {
                i1 = 2;
              }
              else if ((c1 > ' ') && (!Character.isSpaceChar(c1)))
              {
                if (Character.isJavaIdentifierPart(c1))
                {
                  i1 = 4;
                  if (this.identifiersToUpper)
                  {
                    char c2 = Character.toUpperCase(c1);
                    if (c2 != c1)
                    {
                      arrayOfChar[n] = c2;
                      j = 1;
                    }
                  }
                }
                else
                {
                  i1 = 5;
                }
              }
            }
          }
        }
      }
      arrayOfInt[n] = i1;
      m = i1;
    }
    this.sqlCommandChars = arrayOfChar;
    arrayOfInt[i] = 1;
    this.characterTypes = arrayOfInt;
    if (j != 0) {
      this.sqlCommand = new String(arrayOfChar);
    }
    this.parseIndex = 0;
  }
  
  private void checkRunOver(int paramInt1, int paramInt2, int paramInt3)
  {
    if (paramInt1 >= paramInt2)
    {
      this.parseIndex = paramInt3;
      throw getSyntaxError();
    }
  }
  
  private int getSpecialType(String paramString)
  {
    int i = paramString.charAt(0);
    if (paramString.length() == 1) {
      switch (i)
      {
      case 36: 
      case 63: 
        return 3;
      case 64: 
        return 12;
      case 43: 
        return 14;
      case 45: 
        return 13;
      case 37: 
      case 42: 
      case 44: 
      case 47: 
      case 58: 
      case 59: 
      case 91: 
      case 93: 
      case 123: 
      case 125: 
      case 126: 
        return 1;
      case 40: 
        return 16;
      case 41: 
        return 17;
      case 60: 
        return 9;
      case 62: 
        return 8;
      case 61: 
        return 6;
      }
    } else if (paramString.length() == 2) {
      switch (i)
      {
      case 58: 
        if ("::".equals(paramString)) {
          return 1;
        }
        if (":=".equals(paramString)) {
          return 1;
        }
        break;
      case 62: 
        if (">=".equals(paramString)) {
          return 7;
        }
        break;
      case 60: 
        if ("<=".equals(paramString)) {
          return 10;
        }
        if ("<>".equals(paramString)) {
          return 11;
        }
        break;
      case 33: 
        if ("!=".equals(paramString)) {
          return 11;
        }
        if ("!~".equals(paramString)) {
          return 1;
        }
        break;
      case 124: 
        if ("||".equals(paramString)) {
          return 15;
        }
        break;
      case 38: 
        if ("&&".equals(paramString)) {
          return 25;
        }
        break;
      }
    }
    throw getSyntaxError();
  }
  
  private int getTokenType(String paramString)
  {
    int i = paramString.length();
    if (i == 0) {
      throw getSyntaxError();
    }
    if (!this.identifiersToUpper) {
      paramString = StringUtils.toUpperEnglish(paramString);
    }
    return getSaveTokenType(paramString, this.database.getMode().supportOffsetFetch);
  }
  
  private boolean isKeyword(String paramString)
  {
    if (!this.identifiersToUpper) {
      paramString = StringUtils.toUpperEnglish(paramString);
    }
    return isKeyword(paramString, false);
  }
  
  public static boolean isKeyword(String paramString, boolean paramBoolean)
  {
    if ((paramString == null) || (paramString.length() == 0)) {
      return false;
    }
    return getSaveTokenType(paramString, paramBoolean) != 2;
  }
  
  private static int getSaveTokenType(String paramString, boolean paramBoolean)
  {
    switch (paramString.charAt(0))
    {
    case 'C': 
      if (paramString.equals("CURRENT_TIMESTAMP")) {
        return 21;
      }
      if (paramString.equals("CURRENT_TIME")) {
        return 23;
      }
      if (paramString.equals("CURRENT_DATE")) {
        return 22;
      }
      return getKeywordOrIdentifier(paramString, "CROSS", 1);
    case 'D': 
      return getKeywordOrIdentifier(paramString, "DISTINCT", 1);
    case 'E': 
      if ("EXCEPT".equals(paramString)) {
        return 1;
      }
      return getKeywordOrIdentifier(paramString, "EXISTS", 1);
    case 'F': 
      if ("FROM".equals(paramString)) {
        return 1;
      }
      if ("FOR".equals(paramString)) {
        return 1;
      }
      if ("FULL".equals(paramString)) {
        return 1;
      }
      if ((paramBoolean) && ("FETCH".equals(paramString))) {
        return 1;
      }
      return getKeywordOrIdentifier(paramString, "FALSE", 20);
    case 'G': 
      return getKeywordOrIdentifier(paramString, "GROUP", 1);
    case 'H': 
      return getKeywordOrIdentifier(paramString, "HAVING", 1);
    case 'I': 
      if ("INNER".equals(paramString)) {
        return 1;
      }
      if ("INTERSECT".equals(paramString)) {
        return 1;
      }
      return getKeywordOrIdentifier(paramString, "IS", 1);
    case 'J': 
      return getKeywordOrIdentifier(paramString, "JOIN", 1);
    case 'L': 
      if ("LIMIT".equals(paramString)) {
        return 1;
      }
      return getKeywordOrIdentifier(paramString, "LIKE", 1);
    case 'M': 
      return getKeywordOrIdentifier(paramString, "MINUS", 1);
    case 'N': 
      if ("NOT".equals(paramString)) {
        return 1;
      }
      if ("NATURAL".equals(paramString)) {
        return 1;
      }
      return getKeywordOrIdentifier(paramString, "NULL", 18);
    case 'O': 
      if ("ON".equals(paramString)) {
        return 1;
      }
      if ((paramBoolean) && ("OFFSET".equals(paramString))) {
        return 1;
      }
      return getKeywordOrIdentifier(paramString, "ORDER", 1);
    case 'P': 
      return getKeywordOrIdentifier(paramString, "PRIMARY", 1);
    case 'R': 
      return getKeywordOrIdentifier(paramString, "ROWNUM", 24);
    case 'S': 
      if (paramString.equals("SYSTIMESTAMP")) {
        return 21;
      }
      if (paramString.equals("SYSTIME")) {
        return 23;
      }
      if (paramString.equals("SYSDATE")) {
        return 21;
      }
      return getKeywordOrIdentifier(paramString, "SELECT", 1);
    case 'T': 
      if ("TODAY".equals(paramString)) {
        return 22;
      }
      return getKeywordOrIdentifier(paramString, "TRUE", 19);
    case 'U': 
      if ("UNIQUE".equals(paramString)) {
        return 1;
      }
      return getKeywordOrIdentifier(paramString, "UNION", 1);
    case 'W': 
      if ("WITH".equals(paramString)) {
        return 1;
      }
      return getKeywordOrIdentifier(paramString, "WHERE", 1);
    }
    return 2;
  }
  
  private static int getKeywordOrIdentifier(String paramString1, String paramString2, int paramInt)
  {
    if (paramString1.equals(paramString2)) {
      return paramInt;
    }
    return 2;
  }
  
  private Column parseColumnForTable(String paramString, boolean paramBoolean)
  {
    int i = 0;
    Column localColumn;
    if ((readIf("IDENTITY")) || (readIf("BIGSERIAL")))
    {
      localColumn = new Column(paramString, 5);
      localColumn.setOriginalSQL("IDENTITY");
      parseAutoIncrement(localColumn);
      if (!this.database.getMode().serialColumnIsNotPK) {
        localColumn.setPrimaryKey(true);
      }
    }
    else if (readIf("SERIAL"))
    {
      localColumn = new Column(paramString, 4);
      localColumn.setOriginalSQL("SERIAL");
      parseAutoIncrement(localColumn);
      if (!this.database.getMode().serialColumnIsNotPK) {
        localColumn.setPrimaryKey(true);
      }
    }
    else
    {
      localColumn = parseColumnWithType(paramString);
    }
    if (readIf("NOT"))
    {
      read("NULL");
      localColumn.setNullable(false);
    }
    else if (readIf("NULL"))
    {
      localColumn.setNullable(true);
    }
    else
    {
      localColumn.setNullable(paramBoolean & localColumn.isNullable());
    }
    Expression localExpression;
    if (readIf("AS"))
    {
      if (i != 0) {
        getSyntaxError();
      }
      localExpression = readExpression();
      localColumn.setComputedExpression(localExpression);
    }
    else if (readIf("DEFAULT"))
    {
      localExpression = readExpression();
      localColumn.setDefaultExpression(this.session, localExpression);
    }
    else if (readIf("GENERATED"))
    {
      if (!readIf("ALWAYS"))
      {
        read("BY");
        read("DEFAULT");
      }
      read("AS");
      read("IDENTITY");
      long l1 = 1L;long l2 = 1L;
      if (readIf("("))
      {
        read("START");
        readIf("WITH");
        l1 = readLong();
        readIf(",");
        if (readIf("INCREMENT"))
        {
          readIf("BY");
          l2 = readLong();
        }
        read(")");
      }
      localColumn.setPrimaryKey(true);
      localColumn.setAutoIncrement(true, l1, l2);
    }
    if (readIf("NOT"))
    {
      read("NULL");
      localColumn.setNullable(false);
    }
    else
    {
      readIf("NULL");
    }
    if ((readIf("AUTO_INCREMENT")) || (readIf("BIGSERIAL")) || (readIf("SERIAL")))
    {
      parseAutoIncrement(localColumn);
      if (readIf("NOT")) {
        read("NULL");
      }
    }
    else if (readIf("IDENTITY"))
    {
      parseAutoIncrement(localColumn);
      localColumn.setPrimaryKey(true);
      if (readIf("NOT")) {
        read("NULL");
      }
    }
    if (readIf("NULL_TO_DEFAULT")) {
      localColumn.setConvertNullToDefault(true);
    }
    if (readIf("SEQUENCE"))
    {
      Sequence localSequence = readSequence();
      localColumn.setSequence(localSequence);
    }
    if (readIf("SELECTIVITY"))
    {
      int j = readPositiveInt();
      localColumn.setSelectivity(j);
    }
    String str = readCommentIf();
    if (str != null) {
      localColumn.setComment(str);
    }
    return localColumn;
  }
  
  private void parseAutoIncrement(Column paramColumn)
  {
    long l1 = 1L;long l2 = 1L;
    if (readIf("("))
    {
      l1 = readLong();
      if (readIf(",")) {
        l2 = readLong();
      }
      read(")");
    }
    paramColumn.setAutoIncrement(true, l1, l2);
  }
  
  private String readCommentIf()
  {
    if (readIf("COMMENT"))
    {
      readIf("IS");
      return readString();
    }
    return null;
  }
  
  private Column parseColumnWithType(String paramString)
  {
    String str1 = this.currentToken;
    int i = 0;
    if (readIf("LONG"))
    {
      if (readIf("RAW")) {
        str1 = str1 + " RAW";
      }
    }
    else if (readIf("DOUBLE"))
    {
      if (readIf("PRECISION")) {
        str1 = str1 + " PRECISION";
      }
    }
    else if (readIf("CHARACTER"))
    {
      if (readIf("VARYING")) {
        str1 = str1 + " VARYING";
      }
    }
    else {
      i = 1;
    }
    long l1 = -1L;
    int j = -1;
    int k = -1;
    String str2 = null;
    Column localColumn1 = null;
    if (!this.identifiersToUpper) {
      str1 = StringUtils.toUpperEnglish(str1);
    }
    UserDataType localUserDataType = this.database.findUserDataType(str1);
    DataType localDataType;
    if (localUserDataType != null)
    {
      localColumn1 = localUserDataType.getColumn();
      localDataType = DataType.getDataType(localColumn1.getType());
      str2 = localColumn1.getComment();
      str1 = localColumn1.getOriginalSQL();
      l1 = localColumn1.getPrecision();
      j = localColumn1.getDisplaySize();
      k = localColumn1.getScale();
    }
    else
    {
      localDataType = DataType.getTypeByName(str1);
      if (localDataType == null) {
        throw DbException.get(50004, this.currentToken);
      }
    }
    if ((this.database.getIgnoreCase()) && (localDataType.type == 13) && (!equalsToken("VARCHAR_CASESENSITIVE", str1)))
    {
      str1 = "VARCHAR_IGNORECASE";
      localDataType = DataType.getTypeByName(str1);
    }
    if (i != 0) {
      read();
    }
    l1 = l1 == -1L ? localDataType.defaultPrecision : l1;
    j = j == -1 ? localDataType.defaultDisplaySize : j;
    
    k = k == -1 ? localDataType.defaultScale : k;
    if ((localDataType.supportsPrecision) || (localDataType.supportsScale))
    {
      if (readIf("("))
      {
        if (!readIf("MAX"))
        {
          long l2 = readLong();
          if (readIf("K")) {
            l2 *= 1024L;
          } else if (readIf("M")) {
            l2 *= 1048576L;
          } else if (readIf("G")) {
            l2 *= 1073741824L;
          }
          if (l2 > Long.MAX_VALUE) {
            l2 = Long.MAX_VALUE;
          }
          str1 = str1 + "(" + l2;
          
          readIf("CHAR");
          if (localDataType.supportsScale) {
            if (readIf(","))
            {
              k = readInt();
              str1 = str1 + ", " + k;
            }
            else if (localDataType.type == 11)
            {
              k = MathUtils.convertLongToInt(l2);
              l2 = l1;
            }
            else
            {
              k = 0;
            }
          }
          l1 = l2;
          j = MathUtils.convertLongToInt(l1);
          str1 = str1 + ")";
        }
        read(")");
      }
    }
    else if (readIf("("))
    {
      readPositiveInt();
      read(")");
    }
    if (readIf("FOR"))
    {
      read("BIT");
      read("DATA");
      if (localDataType.type == 13) {
        localDataType = DataType.getTypeByName("BINARY");
      }
    }
    readIf("UNSIGNED");
    int m = localDataType.type;
    if (k > l1) {
      throw DbException.get(90008, new String[] { Integer.toString(k), "scale (precision = " + l1 + ")" });
    }
    Column localColumn2 = new Column(paramString, m, l1, k, j);
    if (localColumn1 != null)
    {
      localColumn2.setNullable(localColumn1.isNullable());
      localColumn2.setDefaultExpression(this.session, localColumn1.getDefaultExpression());
      
      int n = localColumn1.getSelectivity();
      if (n != 50) {
        localColumn2.setSelectivity(n);
      }
      Expression localExpression = localColumn1.getCheckConstraint(this.session, paramString);
      
      localColumn2.addCheckConstraint(this.session, localExpression);
    }
    localColumn2.setComment(str2);
    localColumn2.setOriginalSQL(str1);
    return localColumn2;
  }
  
  private Prepared parseCreate()
  {
    boolean bool1 = false;
    if (readIf("OR"))
    {
      read("REPLACE");
      bool1 = true;
    }
    boolean bool2 = readIf("FORCE");
    if (readIf("VIEW")) {
      return parseCreateView(bool2, bool1);
    }
    if (readIf("ALIAS")) {
      return parseCreateFunctionAlias(bool2);
    }
    if (readIf("SEQUENCE")) {
      return parseCreateSequence();
    }
    if (readIf("USER")) {
      return parseCreateUser();
    }
    if (readIf("TRIGGER")) {
      return parseCreateTrigger(bool2);
    }
    if (readIf("ROLE")) {
      return parseCreateRole();
    }
    if (readIf("SCHEMA")) {
      return parseCreateSchema();
    }
    if (readIf("CONSTANT")) {
      return parseCreateConstant();
    }
    if (readIf("DOMAIN")) {
      return parseCreateUserDataType();
    }
    if (readIf("TYPE")) {
      return parseCreateUserDataType();
    }
    if (readIf("DATATYPE")) {
      return parseCreateUserDataType();
    }
    if (readIf("AGGREGATE")) {
      return parseCreateAggregate(bool2);
    }
    if (readIf("LINKED")) {
      return parseCreateLinkedTable(false, false, bool2);
    }
    int i = 0;boolean bool3 = false;
    if (readIf("MEMORY")) {
      i = 1;
    } else if (readIf("CACHED")) {
      bool3 = true;
    }
    if (readIf("LOCAL"))
    {
      read("TEMPORARY");
      if (readIf("LINKED")) {
        return parseCreateLinkedTable(true, false, bool2);
      }
      read("TABLE");
      return parseCreateTable(true, false, bool3);
    }
    if (readIf("GLOBAL"))
    {
      read("TEMPORARY");
      if (readIf("LINKED")) {
        return parseCreateLinkedTable(true, true, bool2);
      }
      read("TABLE");
      return parseCreateTable(true, true, bool3);
    }
    if ((readIf("TEMP")) || (readIf("TEMPORARY")))
    {
      if (readIf("LINKED")) {
        return parseCreateLinkedTable(true, true, bool2);
      }
      read("TABLE");
      return parseCreateTable(true, true, bool3);
    }
    if (readIf("TABLE"))
    {
      if ((!bool3) && (i == 0)) {
        bool3 = this.database.getDefaultTableType() == 0;
      }
      return parseCreateTable(false, false, bool3);
    }
    boolean bool4 = false;boolean bool5 = false;
    boolean bool6 = false;boolean bool7 = false;
    String str1 = null;
    Schema localSchema = null;
    boolean bool8 = false;
    if (readIf("PRIMARY"))
    {
      read("KEY");
      if (readIf("HASH")) {
        bool4 = true;
      }
      bool5 = true;
      if (!isToken("ON"))
      {
        bool8 = readIfNoExists();
        str1 = readIdentifierWithSchema(null);
        localSchema = getSchema();
      }
    }
    else
    {
      if (readIf("UNIQUE")) {
        bool6 = true;
      }
      if (readIf("HASH")) {
        bool4 = true;
      }
      if (readIf("SPATIAL")) {
        bool7 = true;
      }
      if (readIf("INDEX"))
      {
        if (!isToken("ON"))
        {
          bool8 = readIfNoExists();
          str1 = readIdentifierWithSchema(null);
          localSchema = getSchema();
        }
      }
      else {
        throw getSyntaxError();
      }
    }
    read("ON");
    String str2 = readIdentifierWithSchema();
    checkSchema(localSchema);
    CreateIndex localCreateIndex = new CreateIndex(this.session, getSchema());
    localCreateIndex.setIfNotExists(bool8);
    localCreateIndex.setHash(bool4);
    localCreateIndex.setSpatial(bool7);
    localCreateIndex.setPrimaryKey(bool5);
    localCreateIndex.setTableName(str2);
    localCreateIndex.setUnique(bool6);
    localCreateIndex.setIndexName(str1);
    localCreateIndex.setComment(readCommentIf());
    read("(");
    localCreateIndex.setIndexColumns(parseIndexColumnList());
    return localCreateIndex;
  }
  
  private boolean addRoleOrRight(GrantRevoke paramGrantRevoke)
  {
    if (readIf("SELECT"))
    {
      paramGrantRevoke.addRight(1);
      return true;
    }
    if (readIf("DELETE"))
    {
      paramGrantRevoke.addRight(2);
      return true;
    }
    if (readIf("INSERT"))
    {
      paramGrantRevoke.addRight(4);
      return true;
    }
    if (readIf("UPDATE"))
    {
      paramGrantRevoke.addRight(8);
      return true;
    }
    if (readIf("ALL"))
    {
      paramGrantRevoke.addRight(15);
      return true;
    }
    if (readIf("ALTER"))
    {
      read("ANY");
      read("SCHEMA");
      paramGrantRevoke.addRight(16);
      paramGrantRevoke.addTable(null);
      return false;
    }
    if (readIf("CONNECT")) {
      return true;
    }
    if (readIf("RESOURCE")) {
      return true;
    }
    paramGrantRevoke.addRoleName(readUniqueIdentifier());
    return false;
  }
  
  private GrantRevoke parseGrantRevoke(int paramInt)
  {
    GrantRevoke localGrantRevoke = new GrantRevoke(this.session);
    localGrantRevoke.setOperationType(paramInt);
    boolean bool = addRoleOrRight(localGrantRevoke);
    while (readIf(","))
    {
      addRoleOrRight(localGrantRevoke);
      if ((localGrantRevoke.isRightMode()) && (localGrantRevoke.isRoleMode())) {
        throw DbException.get(90072);
      }
    }
    if ((bool) && 
      (readIf("ON"))) {
      do
      {
        Table localTable = readTableOrView();
        localGrantRevoke.addTable(localTable);
      } while (readIf(","));
    }
    if (paramInt == 49) {
      read("TO");
    } else {
      read("FROM");
    }
    localGrantRevoke.setGranteeName(readUniqueIdentifier());
    return localGrantRevoke;
  }
  
  private Select parseValues()
  {
    Select localSelect = new Select(this.session);
    this.currentSelect = localSelect;
    TableFilter localTableFilter = parseValuesTable();
    ArrayList localArrayList = New.arrayList();
    localArrayList.add(new Wildcard(null, null));
    localSelect.setExpressions(localArrayList);
    localSelect.addTableFilter(localTableFilter, true);
    localSelect.init();
    return localSelect;
  }
  
  private TableFilter parseValuesTable()
  {
    Schema localSchema = this.database.getSchema("PUBLIC");
    TableFunction localTableFunction = (TableFunction)Function.getFunction(this.database, "TABLE");
    
    ArrayList localArrayList1 = New.arrayList();
    ArrayList localArrayList2 = New.arrayList();
    do
    {
      i = 0;
      ArrayList localArrayList3 = New.arrayList();
      k = readIf("(");
      do
      {
        localObject1 = readExpression();
        localObject1 = ((Expression)localObject1).optimize(this.session);
        int n = ((Expression)localObject1).getType();
        
        String str = "C" + (i + 1);
        if (localArrayList2.size() == 0)
        {
          if (n == -1) {
            n = 13;
          }
          localObject2 = DataType.getDataType(n);
          l = ((DataType)localObject2).defaultPrecision;
          i2 = ((DataType)localObject2).defaultScale;
          i3 = ((DataType)localObject2).defaultDisplaySize;
          localColumn = new Column(str, n, l, i2, i3);
          
          localArrayList1.add(localColumn);
        }
        long l = ((Expression)localObject1).getPrecision();
        int i2 = ((Expression)localObject1).getScale();
        int i3 = ((Expression)localObject1).getDisplaySize();
        if (i >= localArrayList1.size()) {
          throw DbException.get(21002);
        }
        Object localObject2 = (Column)localArrayList1.get(i);
        n = Value.getHigherOrder(((Column)localObject2).getType(), n);
        l = Math.max(((Column)localObject2).getPrecision(), l);
        i2 = Math.max(((Column)localObject2).getScale(), i2);
        i3 = Math.max(((Column)localObject2).getDisplaySize(), i3);
        Column localColumn = new Column(str, n, l, i2, i3);
        localArrayList1.set(i, localColumn);
        localArrayList3.add(localObject1);
        i++;
      } while ((k != 0) && (readIf(",")));
      if (k != 0) {
        read(")");
      }
      localArrayList2.add(localArrayList3);
    } while (readIf(","));
    int i = localArrayList1.size();
    int j = localArrayList2.size();
    for (int k = 0; k < j; k++) {
      if (((ArrayList)localArrayList2.get(k)).size() != i) {
        throw DbException.get(21002);
      }
    }
    for (int m = 0; m < i; m++)
    {
      localObject1 = (Column)localArrayList1.get(m);
      if (((Column)localObject1).getType() == -1)
      {
        localObject1 = new Column(((Column)localObject1).getName(), 13, 0L, 0, 0);
        localArrayList1.set(m, localObject1);
      }
      Expression[] arrayOfExpression = new Expression[j];
      for (int i1 = 0; i1 < j; i1++) {
        arrayOfExpression[i1] = ((Expression)((ArrayList)localArrayList2.get(i1)).get(m));
      }
      ExpressionList localExpressionList = new ExpressionList(arrayOfExpression);
      localTableFunction.setParameter(m, localExpressionList);
    }
    localTableFunction.setColumns(localArrayList1);
    localTableFunction.doneWithParameters();
    FunctionTable localFunctionTable = new FunctionTable(localSchema, this.session, localTableFunction, localTableFunction);
    Object localObject1 = new TableFilter(this.session, localFunctionTable, null, this.rightsChecked, this.currentSelect);
    
    return (TableFilter)localObject1;
  }
  
  private Call parseCall()
  {
    Call localCall = new Call(this.session);
    this.currentPrepared = localCall;
    localCall.setExpression(readExpression());
    return localCall;
  }
  
  private CreateRole parseCreateRole()
  {
    CreateRole localCreateRole = new CreateRole(this.session);
    localCreateRole.setIfNotExists(readIfNoExists());
    localCreateRole.setRoleName(readUniqueIdentifier());
    return localCreateRole;
  }
  
  private CreateSchema parseCreateSchema()
  {
    CreateSchema localCreateSchema = new CreateSchema(this.session);
    localCreateSchema.setIfNotExists(readIfNoExists());
    localCreateSchema.setSchemaName(readUniqueIdentifier());
    if (readIf("AUTHORIZATION")) {
      localCreateSchema.setAuthorization(readUniqueIdentifier());
    } else {
      localCreateSchema.setAuthorization(this.session.getUser().getName());
    }
    return localCreateSchema;
  }
  
  private CreateSequence parseCreateSequence()
  {
    boolean bool = readIfNoExists();
    String str = readIdentifierWithSchema();
    CreateSequence localCreateSequence = new CreateSequence(this.session, getSchema());
    localCreateSequence.setIfNotExists(bool);
    localCreateSequence.setSequenceName(str);
    for (;;)
    {
      if (readIf("START"))
      {
        readIf("WITH");
        localCreateSequence.setStartWith(readExpression());
      }
      else if (readIf("INCREMENT"))
      {
        readIf("BY");
        localCreateSequence.setIncrement(readExpression());
      }
      else if (readIf("MINVALUE"))
      {
        localCreateSequence.setMinValue(readExpression());
      }
      else if (readIf("NOMINVALUE"))
      {
        localCreateSequence.setMinValue(null);
      }
      else if (readIf("MAXVALUE"))
      {
        localCreateSequence.setMaxValue(readExpression());
      }
      else if (readIf("NOMAXVALUE"))
      {
        localCreateSequence.setMaxValue(null);
      }
      else if (readIf("CYCLE"))
      {
        localCreateSequence.setCycle(true);
      }
      else if (readIf("NOCYCLE"))
      {
        localCreateSequence.setCycle(false);
      }
      else if (readIf("NO"))
      {
        if (readIf("MINVALUE"))
        {
          localCreateSequence.setMinValue(null);
        }
        else if (readIf("MAXVALUE"))
        {
          localCreateSequence.setMaxValue(null);
        }
        else if (readIf("CYCLE"))
        {
          localCreateSequence.setCycle(false);
        }
        else
        {
          if (!readIf("CACHE")) {
            break;
          }
          localCreateSequence.setCacheSize(ValueExpression.get(ValueLong.get(1L)));
        }
      }
      else if (readIf("CACHE"))
      {
        localCreateSequence.setCacheSize(readExpression());
      }
      else if (readIf("NOCACHE"))
      {
        localCreateSequence.setCacheSize(ValueExpression.get(ValueLong.get(1L)));
      }
      else
      {
        if (!readIf("BELONGS_TO_TABLE")) {
          break;
        }
        localCreateSequence.setBelongsToTable(true);
      }
    }
    return localCreateSequence;
  }
  
  private boolean readIfNoExists()
  {
    if (readIf("IF"))
    {
      read("NOT");
      read("EXISTS");
      return true;
    }
    return false;
  }
  
  private CreateConstant parseCreateConstant()
  {
    boolean bool = readIfNoExists();
    String str = readIdentifierWithSchema();
    Schema localSchema = getSchema();
    if (isKeyword(str)) {
      throw DbException.get(90114, str);
    }
    read("VALUE");
    Expression localExpression = readExpression();
    CreateConstant localCreateConstant = new CreateConstant(this.session, localSchema);
    localCreateConstant.setConstantName(str);
    localCreateConstant.setExpression(localExpression);
    localCreateConstant.setIfNotExists(bool);
    return localCreateConstant;
  }
  
  private CreateAggregate parseCreateAggregate(boolean paramBoolean)
  {
    boolean bool = readIfNoExists();
    CreateAggregate localCreateAggregate = new CreateAggregate(this.session);
    localCreateAggregate.setForce(paramBoolean);
    String str = readIdentifierWithSchema();
    if ((isKeyword(str)) || (Function.getFunction(this.database, str) != null) || (getAggregateType(str) >= 0)) {
      throw DbException.get(90076, str);
    }
    localCreateAggregate.setName(str);
    localCreateAggregate.setSchema(getSchema());
    localCreateAggregate.setIfNotExists(bool);
    read("FOR");
    localCreateAggregate.setJavaClassMethod(readUniqueIdentifier());
    return localCreateAggregate;
  }
  
  private CreateUserDataType parseCreateUserDataType()
  {
    boolean bool = readIfNoExists();
    CreateUserDataType localCreateUserDataType = new CreateUserDataType(this.session);
    localCreateUserDataType.setTypeName(readUniqueIdentifier());
    read("AS");
    Column localColumn = parseColumnForTable("VALUE", true);
    if (readIf("CHECK"))
    {
      Expression localExpression = readExpression();
      localColumn.addCheckConstraint(this.session, localExpression);
    }
    localColumn.rename(null);
    localCreateUserDataType.setColumn(localColumn);
    localCreateUserDataType.setIfNotExists(bool);
    return localCreateUserDataType;
  }
  
  private CreateTrigger parseCreateTrigger(boolean paramBoolean)
  {
    boolean bool1 = readIfNoExists();
    String str1 = readIdentifierWithSchema(null);
    Schema localSchema = getSchema();
    boolean bool3;
    boolean bool2;
    if (readIf("INSTEAD"))
    {
      read("OF");
      bool3 = true;
      bool2 = true;
    }
    else if (readIf("BEFORE"))
    {
      bool2 = false;
      bool3 = true;
    }
    else
    {
      read("AFTER");
      bool2 = false;
      bool3 = false;
    }
    int i = 0;
    boolean bool4 = false;
    do
    {
      if (readIf("INSERT")) {
        i |= 0x1;
      } else if (readIf("UPDATE")) {
        i |= 0x2;
      } else if (readIf("DELETE")) {
        i |= 0x4;
      } else if (readIf("SELECT")) {
        i |= 0x8;
      } else if (readIf("ROLLBACK")) {
        bool4 = true;
      } else {
        throw getSyntaxError();
      }
    } while (readIf(","));
    read("ON");
    String str2 = readIdentifierWithSchema();
    checkSchema(localSchema);
    CreateTrigger localCreateTrigger = new CreateTrigger(this.session, getSchema());
    localCreateTrigger.setForce(paramBoolean);
    localCreateTrigger.setTriggerName(str1);
    localCreateTrigger.setIfNotExists(bool1);
    localCreateTrigger.setInsteadOf(bool2);
    localCreateTrigger.setBefore(bool3);
    localCreateTrigger.setOnRollback(bool4);
    localCreateTrigger.setTypeMask(i);
    localCreateTrigger.setTableName(str2);
    if (readIf("FOR"))
    {
      read("EACH");
      read("ROW");
      localCreateTrigger.setRowBased(true);
    }
    else
    {
      localCreateTrigger.setRowBased(false);
    }
    if (readIf("QUEUE")) {
      localCreateTrigger.setQueueSize(readPositiveInt());
    }
    localCreateTrigger.setNoWait(readIf("NOWAIT"));
    read("CALL");
    localCreateTrigger.setTriggerClassName(readUniqueIdentifier());
    return localCreateTrigger;
  }
  
  private CreateUser parseCreateUser()
  {
    CreateUser localCreateUser = new CreateUser(this.session);
    localCreateUser.setIfNotExists(readIfNoExists());
    localCreateUser.setUserName(readUniqueIdentifier());
    localCreateUser.setComment(readCommentIf());
    if (readIf("PASSWORD"))
    {
      localCreateUser.setPassword(readExpression());
    }
    else if (readIf("SALT"))
    {
      localCreateUser.setSalt(readExpression());
      read("HASH");
      localCreateUser.setHash(readExpression());
    }
    else if (readIf("IDENTIFIED"))
    {
      read("BY");
      
      localCreateUser.setPassword(ValueExpression.get(ValueString.get(readColumnIdentifier())));
    }
    else
    {
      throw getSyntaxError();
    }
    if (readIf("ADMIN")) {
      localCreateUser.setAdmin(true);
    }
    return localCreateUser;
  }
  
  private CreateFunctionAlias parseCreateFunctionAlias(boolean paramBoolean)
  {
    boolean bool = readIfNoExists();
    String str = readIdentifierWithSchema();
    if ((isKeyword(str)) || (Function.getFunction(this.database, str) != null) || (getAggregateType(str) >= 0)) {
      throw DbException.get(90076, str);
    }
    CreateFunctionAlias localCreateFunctionAlias = new CreateFunctionAlias(this.session, getSchema());
    
    localCreateFunctionAlias.setForce(paramBoolean);
    localCreateFunctionAlias.setAliasName(str);
    localCreateFunctionAlias.setIfNotExists(bool);
    localCreateFunctionAlias.setDeterministic(readIf("DETERMINISTIC"));
    localCreateFunctionAlias.setBufferResultSetToLocalTemp(!readIf("NOBUFFER"));
    if (readIf("AS"))
    {
      localCreateFunctionAlias.setSource(readString());
    }
    else
    {
      read("FOR");
      localCreateFunctionAlias.setJavaClassMethod(readUniqueIdentifier());
    }
    return localCreateFunctionAlias;
  }
  
  private Query parseWith()
  {
    readIf("RECURSIVE");
    String str1 = readIdentifierWithSchema();
    Schema localSchema = getSchema();
    
    read("(");
    ArrayList localArrayList = New.arrayList();
    String[] arrayOfString = parseColumnList();
    Object localObject3;
    for (localObject3 : arrayOfString) {
      localArrayList.add(new Column((String)localObject3, 13));
    }
    ??? = this.session.findLocalTempTable(str1);
    if (??? != null)
    {
      if (!(??? instanceof TableView)) {
        throw DbException.get(42101, str1);
      }
      localObject2 = (TableView)???;
      if (!((TableView)localObject2).isTableExpression()) {
        throw DbException.get(42101, str1);
      }
      this.session.removeLocalTempTable((Table)???);
    }
    Object localObject2 = new CreateTableData();
    ((CreateTableData)localObject2).id = this.database.allocateObjectId();
    ((CreateTableData)localObject2).columns = localArrayList;
    ((CreateTableData)localObject2).tableName = str1;
    ((CreateTableData)localObject2).temporary = true;
    ((CreateTableData)localObject2).persistData = true;
    ((CreateTableData)localObject2).persistIndexes = false;
    ((CreateTableData)localObject2).create = true;
    ((CreateTableData)localObject2).session = this.session;
    Table localTable = localSchema.createTable((CreateTableData)localObject2);
    this.session.addLocalTempTable(localTable);
    String str2;
    try
    {
      read("AS");
      read("(");
      localObject3 = parseSelect();
      read(")");
      ((Query)localObject3).prepare();
      str2 = StringUtils.fromCacheOrNew(((Query)localObject3).getPlanSQL());
    }
    finally
    {
      this.session.removeLocalTempTable(localTable);
    }
    int k = this.database.allocateObjectId();
    TableView localTableView = new TableView(localSchema, k, str1, str2, null, arrayOfString, this.session, true);
    
    localTableView.setTableExpression(true);
    localTableView.setTemporary(true);
    this.session.addLocalTempTable(localTableView);
    localTableView.setOnCommitDrop(true);
    Query localQuery = parseSelect();
    localQuery.setPrepareAlways(true);
    return localQuery;
  }
  
  private CreateView parseCreateView(boolean paramBoolean1, boolean paramBoolean2)
  {
    boolean bool = readIfNoExists();
    String str = readIdentifierWithSchema();
    CreateView localCreateView = new CreateView(this.session, getSchema());
    this.createView = localCreateView;
    localCreateView.setViewName(str);
    localCreateView.setIfNotExists(bool);
    localCreateView.setComment(readCommentIf());
    localCreateView.setOrReplace(paramBoolean2);
    localCreateView.setForce(paramBoolean1);
    if (readIf("("))
    {
      localObject = parseColumnList();
      localCreateView.setColumnNames((String[])localObject);
    }
    Object localObject = StringUtils.fromCacheOrNew(this.sqlCommand.substring(this.parseIndex));
    
    read("AS");
    try
    {
      Query localQuery = parseSelect();
      localQuery.prepare();
      localCreateView.setSelect(localQuery);
    }
    catch (DbException localDbException)
    {
      if (paramBoolean1)
      {
        localCreateView.setSelectSQL((String)localObject);
        while (this.currentTokenType != 4) {
          read();
        }
      }
      throw localDbException;
    }
    return localCreateView;
  }
  
  private TransactionCommand parseCheckpoint()
  {
    TransactionCommand localTransactionCommand;
    if (readIf("SYNC")) {
      localTransactionCommand = new TransactionCommand(this.session, 76);
    } else {
      localTransactionCommand = new TransactionCommand(this.session, 73);
    }
    return localTransactionCommand;
  }
  
  private Prepared parseAlter()
  {
    if (readIf("TABLE")) {
      return parseAlterTable();
    }
    if (readIf("USER")) {
      return parseAlterUser();
    }
    if (readIf("INDEX")) {
      return parseAlterIndex();
    }
    if (readIf("SCHEMA")) {
      return parseAlterSchema();
    }
    if (readIf("SEQUENCE")) {
      return parseAlterSequence();
    }
    if (readIf("VIEW")) {
      return parseAlterView();
    }
    throw getSyntaxError();
  }
  
  private void checkSchema(Schema paramSchema)
  {
    if ((paramSchema != null) && (getSchema() != paramSchema)) {
      throw DbException.get(90080);
    }
  }
  
  private AlterIndexRename parseAlterIndex()
  {
    String str1 = readIdentifierWithSchema();
    Schema localSchema = getSchema();
    AlterIndexRename localAlterIndexRename = new AlterIndexRename(this.session);
    localAlterIndexRename.setOldIndex(getSchema().getIndex(str1));
    read("RENAME");
    read("TO");
    String str2 = readIdentifierWithSchema(localSchema.getName());
    checkSchema(localSchema);
    localAlterIndexRename.setNewName(str2);
    return localAlterIndexRename;
  }
  
  private AlterView parseAlterView()
  {
    AlterView localAlterView = new AlterView(this.session);
    String str = readIdentifierWithSchema();
    Table localTable = getSchema().findTableOrView(this.session, str);
    if (!(localTable instanceof TableView)) {
      throw DbException.get(90037, str);
    }
    TableView localTableView = (TableView)localTable;
    localAlterView.setView(localTableView);
    read("RECOMPILE");
    return localAlterView;
  }
  
  private AlterSchemaRename parseAlterSchema()
  {
    String str1 = readIdentifierWithSchema();
    Schema localSchema = getSchema();
    AlterSchemaRename localAlterSchemaRename = new AlterSchemaRename(this.session);
    localAlterSchemaRename.setOldSchema(getSchema(str1));
    read("RENAME");
    read("TO");
    String str2 = readIdentifierWithSchema(localSchema.getName());
    checkSchema(localSchema);
    localAlterSchemaRename.setNewName(str2);
    return localAlterSchemaRename;
  }
  
  private AlterSequence parseAlterSequence()
  {
    String str = readIdentifierWithSchema();
    Sequence localSequence = getSchema().getSequence(str);
    AlterSequence localAlterSequence = new AlterSequence(this.session, localSequence.getSchema());
    localAlterSequence.setSequence(localSequence);
    for (;;)
    {
      if (readIf("RESTART"))
      {
        read("WITH");
        localAlterSequence.setStartWith(readExpression());
      }
      else if (readIf("INCREMENT"))
      {
        read("BY");
        localAlterSequence.setIncrement(readExpression());
      }
      else if (readIf("MINVALUE"))
      {
        localAlterSequence.setMinValue(readExpression());
      }
      else if (readIf("NOMINVALUE"))
      {
        localAlterSequence.setMinValue(null);
      }
      else if (readIf("MAXVALUE"))
      {
        localAlterSequence.setMaxValue(readExpression());
      }
      else if (readIf("NOMAXVALUE"))
      {
        localAlterSequence.setMaxValue(null);
      }
      else if (readIf("CYCLE"))
      {
        localAlterSequence.setCycle(Boolean.valueOf(true));
      }
      else if (readIf("NOCYCLE"))
      {
        localAlterSequence.setCycle(Boolean.valueOf(false));
      }
      else if (readIf("NO"))
      {
        if (readIf("MINVALUE"))
        {
          localAlterSequence.setMinValue(null);
        }
        else if (readIf("MAXVALUE"))
        {
          localAlterSequence.setMaxValue(null);
        }
        else if (readIf("CYCLE"))
        {
          localAlterSequence.setCycle(Boolean.valueOf(false));
        }
        else
        {
          if (!readIf("CACHE")) {
            break;
          }
          localAlterSequence.setCacheSize(ValueExpression.get(ValueLong.get(1L)));
        }
      }
      else if (readIf("CACHE"))
      {
        localAlterSequence.setCacheSize(readExpression());
      }
      else
      {
        if (!readIf("NOCACHE")) {
          break;
        }
        localAlterSequence.setCacheSize(ValueExpression.get(ValueLong.get(1L)));
      }
    }
    return localAlterSequence;
  }
  
  private AlterUser parseAlterUser()
  {
    String str = readUniqueIdentifier();
    AlterUser localAlterUser;
    if (readIf("SET"))
    {
      localAlterUser = new AlterUser(this.session);
      localAlterUser.setType(19);
      localAlterUser.setUser(this.database.getUser(str));
      if (readIf("PASSWORD"))
      {
        localAlterUser.setPassword(readExpression());
      }
      else if (readIf("SALT"))
      {
        localAlterUser.setSalt(readExpression());
        read("HASH");
        localAlterUser.setHash(readExpression());
      }
      else
      {
        throw getSyntaxError();
      }
      return localAlterUser;
    }
    Object localObject;
    if (readIf("RENAME"))
    {
      read("TO");
      localAlterUser = new AlterUser(this.session);
      localAlterUser.setType(18);
      localAlterUser.setUser(this.database.getUser(str));
      localObject = readUniqueIdentifier();
      localAlterUser.setNewName((String)localObject);
      return localAlterUser;
    }
    if (readIf("ADMIN"))
    {
      localAlterUser = new AlterUser(this.session);
      localAlterUser.setType(17);
      localObject = this.database.getUser(str);
      localAlterUser.setUser((User)localObject);
      if (readIf("TRUE")) {
        localAlterUser.setAdmin(true);
      } else if (readIf("FALSE")) {
        localAlterUser.setAdmin(false);
      } else {
        throw getSyntaxError();
      }
      return localAlterUser;
    }
    throw getSyntaxError();
  }
  
  private void readIfEqualOrTo()
  {
    if (!readIf("=")) {
      readIf("TO");
    }
  }
  
  private Prepared parseSet()
  {
    if (readIf("@"))
    {
      org.h2.command.dml.Set localSet1 = new org.h2.command.dml.Set(this.session, 35);
      localSet1.setString(readAliasIdentifier());
      readIfEqualOrTo();
      localSet1.setExpression(readExpression());
      return localSet1;
    }
    boolean bool1;
    if (readIf("AUTOCOMMIT"))
    {
      readIfEqualOrTo();
      bool1 = readBooleanSetting();
      int j = bool1 ? 69 : 70;
      
      return new TransactionCommand(this.session, j);
    }
    if (readIf("MVCC"))
    {
      readIfEqualOrTo();
      bool1 = readBooleanSetting();
      localObject2 = new org.h2.command.dml.Set(this.session, 31);
      ((org.h2.command.dml.Set)localObject2).setInt(bool1 ? 1 : 0);
      return (Prepared)localObject2;
    }
    if (readIf("EXCLUSIVE"))
    {
      readIfEqualOrTo();
      org.h2.command.dml.Set localSet2 = new org.h2.command.dml.Set(this.session, 33);
      localSet2.setExpression(readExpression());
      return localSet2;
    }
    if (readIf("IGNORECASE"))
    {
      readIfEqualOrTo();
      boolean bool2 = readBooleanSetting();
      localObject2 = new org.h2.command.dml.Set(this.session, 1);
      ((org.h2.command.dml.Set)localObject2).setInt(bool2 ? 1 : 0);
      return (Prepared)localObject2;
    }
    Object localObject1;
    if (readIf("PASSWORD"))
    {
      readIfEqualOrTo();
      localObject1 = new AlterUser(this.session);
      ((AlterUser)localObject1).setType(19);
      ((AlterUser)localObject1).setUser(this.session.getUser());
      ((AlterUser)localObject1).setPassword(readExpression());
      return (Prepared)localObject1;
    }
    if (readIf("SALT"))
    {
      readIfEqualOrTo();
      localObject1 = new AlterUser(this.session);
      ((AlterUser)localObject1).setType(19);
      ((AlterUser)localObject1).setUser(this.session.getUser());
      ((AlterUser)localObject1).setSalt(readExpression());
      read("HASH");
      ((AlterUser)localObject1).setHash(readExpression());
      return (Prepared)localObject1;
    }
    if (readIf("MODE"))
    {
      readIfEqualOrTo();
      localObject1 = new org.h2.command.dml.Set(this.session, 3);
      ((org.h2.command.dml.Set)localObject1).setString(readAliasIdentifier());
      return (Prepared)localObject1;
    }
    if (readIf("COMPRESS_LOB"))
    {
      readIfEqualOrTo();
      localObject1 = new org.h2.command.dml.Set(this.session, 23);
      if (this.currentTokenType == 5) {
        ((org.h2.command.dml.Set)localObject1).setString(readString());
      } else {
        ((org.h2.command.dml.Set)localObject1).setString(readUniqueIdentifier());
      }
      return (Prepared)localObject1;
    }
    if (readIf("DATABASE"))
    {
      readIfEqualOrTo();
      read("COLLATION");
      return parseSetCollation();
    }
    if (readIf("COLLATION"))
    {
      readIfEqualOrTo();
      return parseSetCollation();
    }
    if (readIf("BINARY_COLLATION"))
    {
      readIfEqualOrTo();
      return parseSetBinaryCollation();
    }
    if (readIf("CLUSTER"))
    {
      readIfEqualOrTo();
      localObject1 = new org.h2.command.dml.Set(this.session, 13);
      ((org.h2.command.dml.Set)localObject1).setString(readString());
      return (Prepared)localObject1;
    }
    if (readIf("DATABASE_EVENT_LISTENER"))
    {
      readIfEqualOrTo();
      localObject1 = new org.h2.command.dml.Set(this.session, 15);
      ((org.h2.command.dml.Set)localObject1).setString(readString());
      return (Prepared)localObject1;
    }
    if (readIf("ALLOW_LITERALS"))
    {
      readIfEqualOrTo();
      localObject1 = new org.h2.command.dml.Set(this.session, 24);
      if (readIf("NONE")) {
        ((org.h2.command.dml.Set)localObject1).setInt(0);
      } else if (readIf("ALL")) {
        ((org.h2.command.dml.Set)localObject1).setInt(2);
      } else if (readIf("NUMBERS")) {
        ((org.h2.command.dml.Set)localObject1).setInt(1);
      } else {
        ((org.h2.command.dml.Set)localObject1).setInt(readPositiveInt());
      }
      return (Prepared)localObject1;
    }
    if (readIf("DEFAULT_TABLE_TYPE"))
    {
      readIfEqualOrTo();
      localObject1 = new org.h2.command.dml.Set(this.session, 7);
      if (readIf("MEMORY")) {
        ((org.h2.command.dml.Set)localObject1).setInt(1);
      } else if (readIf("CACHED")) {
        ((org.h2.command.dml.Set)localObject1).setInt(0);
      } else {
        ((org.h2.command.dml.Set)localObject1).setInt(readPositiveInt());
      }
      return (Prepared)localObject1;
    }
    if (readIf("CREATE"))
    {
      readIfEqualOrTo();
      
      read();
      return new NoOperation(this.session);
    }
    if (readIf("HSQLDB.DEFAULT_TABLE_TYPE"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("PAGE_STORE"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("CACHE_TYPE"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("FILE_LOCK"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("DB_CLOSE_ON_EXIT"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("AUTO_SERVER"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("AUTO_SERVER_PORT"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("AUTO_RECONNECT"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("ASSERT"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("ACCESS_MODE_DATA"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("OPEN_NEW"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("JMX"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("PAGE_SIZE"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("RECOVER"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("NAMES"))
    {
      readIfEqualOrTo();
      read();
      return new NoOperation(this.session);
    }
    if (readIf("SCHEMA"))
    {
      readIfEqualOrTo();
      localObject1 = new org.h2.command.dml.Set(this.session, 26);
      ((org.h2.command.dml.Set)localObject1).setString(readAliasIdentifier());
      return (Prepared)localObject1;
    }
    if (readIf("DATESTYLE"))
    {
      readIfEqualOrTo();
      if (!readIf("ISO"))
      {
        localObject1 = readString();
        if (!equalsToken((String)localObject1, "ISO")) {
          throw getSyntaxError();
        }
      }
      return new NoOperation(this.session);
    }
    if ((readIf("SEARCH_PATH")) || (readIf(SetTypes.getTypeName(28))))
    {
      readIfEqualOrTo();
      localObject1 = new org.h2.command.dml.Set(this.session, 28);
      localObject2 = New.arrayList();
      ((ArrayList)localObject2).add(readAliasIdentifier());
      while (readIf(",")) {
        ((ArrayList)localObject2).add(readAliasIdentifier());
      }
      String[] arrayOfString = new String[((ArrayList)localObject2).size()];
      ((ArrayList)localObject2).toArray(arrayOfString);
      ((org.h2.command.dml.Set)localObject1).setStringArray(arrayOfString);
      return (Prepared)localObject1;
    }
    if (readIf("JAVA_OBJECT_SERIALIZER"))
    {
      readIfEqualOrTo();
      return parseSetJavaObjectSerializer();
    }
    if (isToken("LOGSIZE")) {
      this.currentToken = SetTypes.getTypeName(2);
    }
    if (isToken("FOREIGN_KEY_CHECKS")) {
      this.currentToken = SetTypes.getTypeName(30);
    }
    int i = SetTypes.getType(this.currentToken);
    if (i < 0) {
      throw getSyntaxError();
    }
    read();
    readIfEqualOrTo();
    Object localObject2 = new org.h2.command.dml.Set(this.session, i);
    ((org.h2.command.dml.Set)localObject2).setExpression(readExpression());
    return (Prepared)localObject2;
  }
  
  private Prepared parseUse()
  {
    readIfEqualOrTo();
    org.h2.command.dml.Set localSet = new org.h2.command.dml.Set(this.session, 26);
    localSet.setString(readAliasIdentifier());
    return localSet;
  }
  
  private org.h2.command.dml.Set parseSetCollation()
  {
    org.h2.command.dml.Set localSet = new org.h2.command.dml.Set(this.session, 12);
    String str = readAliasIdentifier();
    localSet.setString(str);
    if (equalsToken(str, "OFF")) {
      return localSet;
    }
    Collator localCollator = CompareMode.getCollator(str);
    if (localCollator == null) {
      throw DbException.getInvalidValueException("collation", str);
    }
    if (readIf("STRENGTH"))
    {
      if (readIf("PRIMARY")) {
        localSet.setInt(0);
      } else if (readIf("SECONDARY")) {
        localSet.setInt(1);
      } else if (readIf("TERTIARY")) {
        localSet.setInt(2);
      } else if (readIf("IDENTICAL")) {
        localSet.setInt(3);
      }
    }
    else {
      localSet.setInt(localCollator.getStrength());
    }
    return localSet;
  }
  
  private org.h2.command.dml.Set parseSetBinaryCollation()
  {
    org.h2.command.dml.Set localSet = new org.h2.command.dml.Set(this.session, 38);
    String str = readAliasIdentifier();
    localSet.setString(str);
    if ((equalsToken(str, "UNSIGNED")) || (equalsToken(str, "SIGNED"))) {
      return localSet;
    }
    throw DbException.getInvalidValueException("BINARY_COLLATION", str);
  }
  
  private org.h2.command.dml.Set parseSetJavaObjectSerializer()
  {
    org.h2.command.dml.Set localSet = new org.h2.command.dml.Set(this.session, 39);
    String str = readString();
    localSet.setString(str);
    return localSet;
  }
  
  private RunScriptCommand parseRunScript()
  {
    RunScriptCommand localRunScriptCommand = new RunScriptCommand(this.session);
    read("FROM");
    localRunScriptCommand.setFileNameExpr(readExpression());
    if (readIf("COMPRESSION")) {
      localRunScriptCommand.setCompressionAlgorithm(readUniqueIdentifier());
    }
    if (readIf("CIPHER"))
    {
      localRunScriptCommand.setCipher(readUniqueIdentifier());
      if (readIf("PASSWORD")) {
        localRunScriptCommand.setPassword(readExpression());
      }
    }
    if (readIf("CHARSET")) {
      localRunScriptCommand.setCharset(Charset.forName(readString()));
    }
    return localRunScriptCommand;
  }
  
  private ScriptCommand parseScript()
  {
    ScriptCommand localScriptCommand = new ScriptCommand(this.session);
    boolean bool1 = true;boolean bool2 = true;boolean bool3 = true;
    boolean bool4 = false;boolean bool5 = false;
    if (readIf("SIMPLE")) {
      bool5 = true;
    }
    if (readIf("NODATA")) {
      bool1 = false;
    }
    if (readIf("NOPASSWORDS")) {
      bool2 = false;
    }
    if (readIf("NOSETTINGS")) {
      bool3 = false;
    }
    if (readIf("DROP")) {
      bool4 = true;
    }
    if (readIf("BLOCKSIZE"))
    {
      long l = readLong();
      localScriptCommand.setLobBlockSize(l);
    }
    localScriptCommand.setData(bool1);
    localScriptCommand.setPasswords(bool2);
    localScriptCommand.setSettings(bool3);
    localScriptCommand.setDrop(bool4);
    localScriptCommand.setSimple(bool5);
    if (readIf("TO"))
    {
      localScriptCommand.setFileNameExpr(readExpression());
      if (readIf("COMPRESSION")) {
        localScriptCommand.setCompressionAlgorithm(readUniqueIdentifier());
      }
      if (readIf("CIPHER"))
      {
        localScriptCommand.setCipher(readUniqueIdentifier());
        if (readIf("PASSWORD")) {
          localScriptCommand.setPassword(readExpression());
        }
      }
      if (readIf("CHARSET")) {
        localScriptCommand.setCharset(Charset.forName(readString()));
      }
    }
    Object localObject;
    if (readIf("SCHEMA"))
    {
      localObject = New.hashSet();
      do
      {
        ((HashSet)localObject).add(readUniqueIdentifier());
      } while (readIf(","));
      localScriptCommand.setSchemaNames((java.util.Set)localObject);
    }
    else if (readIf("TABLE"))
    {
      localObject = New.arrayList();
      do
      {
        ((ArrayList)localObject).add(readTableOrView());
      } while (readIf(","));
      localScriptCommand.setTables((Collection)localObject);
    }
    return localScriptCommand;
  }
  
  private Table readTableOrView()
  {
    return readTableOrView(readIdentifierWithSchema(null));
  }
  
  private Table readTableOrView(String paramString)
  {
    if (this.schemaName != null) {
      return getSchema().getTableOrView(this.session, paramString);
    }
    Table localTable = this.database.getSchema(this.session.getCurrentSchemaName()).findTableOrView(this.session, paramString);
    if (localTable != null) {
      return localTable;
    }
    String[] arrayOfString1 = this.session.getSchemaSearchPath();
    if (arrayOfString1 != null) {
      for (String str : arrayOfString1)
      {
        Schema localSchema = this.database.getSchema(str);
        localTable = localSchema.findTableOrView(this.session, paramString);
        if (localTable != null) {
          return localTable;
        }
      }
    }
    throw DbException.get(42102, paramString);
  }
  
  private FunctionAlias findFunctionAlias(String paramString1, String paramString2)
  {
    FunctionAlias localFunctionAlias = this.database.getSchema(paramString1).findFunction(paramString2);
    if (localFunctionAlias != null) {
      return localFunctionAlias;
    }
    String[] arrayOfString1 = this.session.getSchemaSearchPath();
    if (arrayOfString1 != null) {
      for (String str : arrayOfString1)
      {
        localFunctionAlias = this.database.getSchema(str).findFunction(paramString2);
        if (localFunctionAlias != null) {
          return localFunctionAlias;
        }
      }
    }
    return null;
  }
  
  private Sequence findSequence(String paramString1, String paramString2)
  {
    Sequence localSequence = this.database.getSchema(paramString1).findSequence(paramString2);
    if (localSequence != null) {
      return localSequence;
    }
    String[] arrayOfString1 = this.session.getSchemaSearchPath();
    if (arrayOfString1 != null) {
      for (String str : arrayOfString1)
      {
        localSequence = this.database.getSchema(str).findSequence(paramString2);
        if (localSequence != null) {
          return localSequence;
        }
      }
    }
    return null;
  }
  
  private Sequence readSequence()
  {
    String str = readIdentifierWithSchema(null);
    if (this.schemaName != null) {
      return getSchema().getSequence(str);
    }
    Sequence localSequence = findSequence(this.session.getCurrentSchemaName(), str);
    if (localSequence != null) {
      return localSequence;
    }
    throw DbException.get(90036, str);
  }
  
  private Prepared parseAlterTable()
  {
    Table localTable = readTableOrView();
    if (readIf("ADD"))
    {
      DefineCommand localDefineCommand = parseAlterTableAddConstraintIf(localTable.getName(), localTable.getSchema());
      if (localDefineCommand != null) {
        return localDefineCommand;
      }
      return parseAlterTableAddColumn(localTable);
    }
    Object localObject3;
    if (readIf("SET"))
    {
      read("REFERENTIAL_INTEGRITY");
      int i = 55;
      boolean bool3 = readBooleanSetting();
      localObject3 = new AlterTableSet(this.session, localTable.getSchema(), i, bool3);
      
      ((AlterTableSet)localObject3).setTableName(localTable.getName());
      if (readIf("CHECK")) {
        ((AlterTableSet)localObject3).setCheckExisting(true);
      } else if (readIf("NOCHECK")) {
        ((AlterTableSet)localObject3).setCheckExisting(false);
      }
      return (Prepared)localObject3;
    }
    Object localObject2;
    if (readIf("RENAME"))
    {
      read("TO");
      String str1 = readIdentifierWithSchema(localTable.getSchema().getName());
      
      checkSchema(localTable.getSchema());
      localObject2 = new AlterTableRename(this.session, getSchema());
      
      ((AlterTableRename)localObject2).setOldTable(localTable);
      ((AlterTableRename)localObject2).setNewTableName(str1);
      ((AlterTableRename)localObject2).setHidden(readIf("HIDDEN"));
      return (Prepared)localObject2;
    }
    if (readIf("DROP"))
    {
      if (readIf("CONSTRAINT"))
      {
        boolean bool1 = readIfExists(false);
        localObject2 = readIdentifierWithSchema(localTable.getSchema().getName());
        
        bool1 = readIfExists(bool1);
        checkSchema(localTable.getSchema());
        localObject3 = new AlterTableDropConstraint(this.session, getSchema(), bool1);
        
        ((AlterTableDropConstraint)localObject3).setConstraintName((String)localObject2);
        return (Prepared)localObject3;
      }
      Object localObject1;
      if (readIf("FOREIGN"))
      {
        read("KEY");
        localObject1 = readIdentifierWithSchema(localTable.getSchema().getName());
        
        checkSchema(localTable.getSchema());
        localObject2 = new AlterTableDropConstraint(this.session, getSchema(), false);
        
        ((AlterTableDropConstraint)localObject2).setConstraintName((String)localObject1);
        return (Prepared)localObject2;
      }
      if (readIf("INDEX"))
      {
        localObject1 = readIdentifierWithSchema();
        localObject2 = new DropIndex(this.session, getSchema());
        ((DropIndex)localObject2).setIndexName((String)localObject1);
        return (Prepared)localObject2;
      }
      if (readIf("PRIMARY"))
      {
        read("KEY");
        localObject1 = localTable.getPrimaryKey();
        localObject2 = new DropIndex(this.session, localTable.getSchema());
        ((DropIndex)localObject2).setIndexName(((Index)localObject1).getName());
        return (Prepared)localObject2;
      }
      readIf("COLUMN");
      boolean bool2 = readIfExists(false);
      localObject2 = new AlterTableAlterColumn(this.session, localTable.getSchema());
      
      ((AlterTableAlterColumn)localObject2).setType(12);
      localObject3 = readColumnIdentifier();
      ((AlterTableAlterColumn)localObject2).setTable(localTable);
      if ((bool2) && (!localTable.doesColumnExist((String)localObject3))) {
        return new NoOperation(this.session);
      }
      ((AlterTableAlterColumn)localObject2).setOldColumn(localTable.getColumn((String)localObject3));
      return (Prepared)localObject2;
    }
    String str2;
    Object localObject4;
    if (readIf("CHANGE"))
    {
      readIf("COLUMN");
      str2 = readColumnIdentifier();
      localObject2 = localTable.getColumn(str2);
      localObject3 = readColumnIdentifier();
      
      parseColumnForTable((String)localObject3, ((Column)localObject2).isNullable());
      localObject4 = new AlterTableRenameColumn(this.session);
      ((AlterTableRenameColumn)localObject4).setTable(localTable);
      ((AlterTableRenameColumn)localObject4).setColumn((Column)localObject2);
      ((AlterTableRenameColumn)localObject4).setNewColumnName((String)localObject3);
      return (Prepared)localObject4;
    }
    if (readIf("MODIFY"))
    {
      readIf("COLUMN");
      str2 = readColumnIdentifier();
      localObject2 = localTable.getColumn(str2);
      return parseAlterTableAlterColumnType(localTable, str2, (Column)localObject2);
    }
    if (readIf("ALTER"))
    {
      readIf("COLUMN");
      str2 = readColumnIdentifier();
      localObject2 = localTable.getColumn(str2);
      if (readIf("RENAME"))
      {
        read("TO");
        localObject3 = new AlterTableRenameColumn(this.session);
        
        ((AlterTableRenameColumn)localObject3).setTable(localTable);
        ((AlterTableRenameColumn)localObject3).setColumn((Column)localObject2);
        localObject4 = readColumnIdentifier();
        ((AlterTableRenameColumn)localObject3).setNewColumnName((String)localObject4);
        return (Prepared)localObject3;
      }
      if (readIf("DROP"))
      {
        if (readIf("DEFAULT"))
        {
          localObject3 = new AlterTableAlterColumn(this.session, localTable.getSchema());
          
          ((AlterTableAlterColumn)localObject3).setTable(localTable);
          ((AlterTableAlterColumn)localObject3).setOldColumn((Column)localObject2);
          ((AlterTableAlterColumn)localObject3).setType(10);
          ((AlterTableAlterColumn)localObject3).setDefaultExpression(null);
          return (Prepared)localObject3;
        }
        read("NOT");
        read("NULL");
        localObject3 = new AlterTableAlterColumn(this.session, localTable.getSchema());
        
        ((AlterTableAlterColumn)localObject3).setTable(localTable);
        ((AlterTableAlterColumn)localObject3).setOldColumn((Column)localObject2);
        ((AlterTableAlterColumn)localObject3).setType(9);
        return (Prepared)localObject3;
      }
      if (readIf("TYPE")) {
        return parseAlterTableAlterColumnType(localTable, str2, (Column)localObject2);
      }
      if (readIf("SET"))
      {
        if (readIf("DATA"))
        {
          read("TYPE");
          return parseAlterTableAlterColumnType(localTable, str2, (Column)localObject2);
        }
        localObject3 = new AlterTableAlterColumn(this.session, localTable.getSchema());
        
        ((AlterTableAlterColumn)localObject3).setTable(localTable);
        ((AlterTableAlterColumn)localObject3).setOldColumn((Column)localObject2);
        if (readIf("NULL"))
        {
          ((AlterTableAlterColumn)localObject3).setType(9);
          return (Prepared)localObject3;
        }
        if (readIf("NOT"))
        {
          read("NULL");
          ((AlterTableAlterColumn)localObject3).setType(8);
          return (Prepared)localObject3;
        }
        if (readIf("DEFAULT"))
        {
          localObject4 = readExpression();
          ((AlterTableAlterColumn)localObject3).setType(10);
          ((AlterTableAlterColumn)localObject3).setDefaultExpression((Expression)localObject4);
          return (Prepared)localObject3;
        }
      }
      else
      {
        if (readIf("RESTART"))
        {
          readIf("WITH");
          localObject3 = readExpression();
          localObject4 = new AlterSequence(this.session, localTable.getSchema());
          
          ((AlterSequence)localObject4).setColumn((Column)localObject2);
          ((AlterSequence)localObject4).setStartWith((Expression)localObject3);
          return (Prepared)localObject4;
        }
        if (readIf("SELECTIVITY"))
        {
          localObject3 = new AlterTableAlterColumn(this.session, localTable.getSchema());
          
          ((AlterTableAlterColumn)localObject3).setTable(localTable);
          ((AlterTableAlterColumn)localObject3).setType(13);
          ((AlterTableAlterColumn)localObject3).setOldColumn((Column)localObject2);
          ((AlterTableAlterColumn)localObject3).setSelectivity(readExpression());
          return (Prepared)localObject3;
        }
        return parseAlterTableAlterColumnType(localTable, str2, (Column)localObject2);
      }
    }
    throw getSyntaxError();
  }
  
  private AlterTableAlterColumn parseAlterTableAlterColumnType(Table paramTable, String paramString, Column paramColumn)
  {
    Column localColumn = parseColumnForTable(paramString, paramColumn.isNullable());
    AlterTableAlterColumn localAlterTableAlterColumn = new AlterTableAlterColumn(this.session, paramTable.getSchema());
    
    localAlterTableAlterColumn.setTable(paramTable);
    localAlterTableAlterColumn.setType(11);
    localAlterTableAlterColumn.setOldColumn(paramColumn);
    localAlterTableAlterColumn.setNewColumn(localColumn);
    return localAlterTableAlterColumn;
  }
  
  private AlterTableAlterColumn parseAlterTableAddColumn(Table paramTable)
  {
    readIf("COLUMN");
    Schema localSchema = paramTable.getSchema();
    AlterTableAlterColumn localAlterTableAlterColumn = new AlterTableAlterColumn(this.session, localSchema);
    
    localAlterTableAlterColumn.setType(7);
    localAlterTableAlterColumn.setTable(paramTable);
    ArrayList localArrayList = New.arrayList();
    Object localObject;
    if (readIf("("))
    {
      localAlterTableAlterColumn.setIfNotExists(false);
      do
      {
        String str = readColumnIdentifier();
        localObject = parseColumnForTable(str, true);
        localArrayList.add(localObject);
      } while (readIf(","));
      read(")");
      localAlterTableAlterColumn.setNewColumns(localArrayList);
    }
    else
    {
      boolean bool = readIfNoExists();
      localAlterTableAlterColumn.setIfNotExists(bool);
      localObject = readColumnIdentifier();
      Column localColumn = parseColumnForTable((String)localObject, true);
      localArrayList.add(localColumn);
      if (readIf("BEFORE")) {
        localAlterTableAlterColumn.setAddBefore(readColumnIdentifier());
      } else if (readIf("AFTER")) {
        localAlterTableAlterColumn.setAddAfter(readColumnIdentifier());
      }
    }
    localAlterTableAlterColumn.setNewColumns(localArrayList);
    return localAlterTableAlterColumn;
  }
  
  private int parseAction()
  {
    Integer localInteger = parseCascadeOrRestrict();
    if (localInteger != null) {
      return localInteger.intValue();
    }
    if (readIf("NO"))
    {
      read("ACTION");
      return 0;
    }
    read("SET");
    if (readIf("NULL")) {
      return 3;
    }
    read("DEFAULT");
    return 2;
  }
  
  private Integer parseCascadeOrRestrict()
  {
    if (readIf("CASCADE")) {
      return Integer.valueOf(1);
    }
    if (readIf("RESTRICT")) {
      return Integer.valueOf(0);
    }
    return null;
  }
  
  private DefineCommand parseAlterTableAddConstraintIf(String paramString, Schema paramSchema)
  {
    String str1 = null;String str2 = null;
    boolean bool1 = false;
    boolean bool2 = this.database.getMode().indexDefinitionInCreateTable;
    if (readIf("CONSTRAINT"))
    {
      bool1 = readIfNoExists();
      str1 = readIdentifierWithSchema(paramSchema.getName());
      checkSchema(paramSchema);
      str2 = readCommentIf();
      bool2 = true;
    }
    Object localObject;
    if (readIf("PRIMARY"))
    {
      read("KEY");
      AlterTableAddConstraint localAlterTableAddConstraint1 = new AlterTableAddConstraint(this.session, paramSchema, bool1);
      
      localAlterTableAddConstraint1.setType(6);
      localAlterTableAddConstraint1.setComment(str2);
      localAlterTableAddConstraint1.setConstraintName(str1);
      localAlterTableAddConstraint1.setTableName(paramString);
      if (readIf("HASH")) {
        localAlterTableAddConstraint1.setPrimaryKeyHash(true);
      }
      read("(");
      localAlterTableAddConstraint1.setIndexColumns(parseIndexColumnList());
      if (readIf("INDEX"))
      {
        localObject = readIdentifierWithSchema();
        localAlterTableAddConstraint1.setIndex(getSchema().findIndex(this.session, (String)localObject));
      }
      return localAlterTableAddConstraint1;
    }
    if ((bool2) && ((isToken("INDEX")) || (isToken("KEY"))))
    {
      int i = this.lastParseIndex;
      read();
      if (DataType.getTypeByName(this.currentToken) != null)
      {
        this.parseIndex = i;
        read();
        return null;
      }
      localObject = new CreateIndex(this.session, paramSchema);
      ((CreateIndex)localObject).setComment(str2);
      ((CreateIndex)localObject).setTableName(paramString);
      if (!readIf("("))
      {
        ((CreateIndex)localObject).setIndexName(readUniqueIdentifier());
        read("(");
      }
      ((CreateIndex)localObject).setIndexColumns(parseIndexColumnList());
      if (readIf("USING")) {
        read("BTREE");
      }
      return (DefineCommand)localObject;
    }
    AlterTableAddConstraint localAlterTableAddConstraint2;
    if (readIf("CHECK"))
    {
      localAlterTableAddConstraint2 = new AlterTableAddConstraint(this.session, paramSchema, bool1);
      localAlterTableAddConstraint2.setType(3);
      localAlterTableAddConstraint2.setCheckExpression(readExpression());
    }
    else if (readIf("UNIQUE"))
    {
      readIf("KEY");
      readIf("INDEX");
      localAlterTableAddConstraint2 = new AlterTableAddConstraint(this.session, paramSchema, bool1);
      localAlterTableAddConstraint2.setType(4);
      if (!readIf("("))
      {
        str1 = readUniqueIdentifier();
        read("(");
      }
      localAlterTableAddConstraint2.setIndexColumns(parseIndexColumnList());
      if (readIf("INDEX"))
      {
        localObject = readIdentifierWithSchema();
        localAlterTableAddConstraint2.setIndex(getSchema().findIndex(this.session, (String)localObject));
      }
      if (readIf("USING")) {
        read("BTREE");
      }
    }
    else if (readIf("FOREIGN"))
    {
      localAlterTableAddConstraint2 = new AlterTableAddConstraint(this.session, paramSchema, bool1);
      localAlterTableAddConstraint2.setType(5);
      read("KEY");
      read("(");
      localAlterTableAddConstraint2.setIndexColumns(parseIndexColumnList());
      if (readIf("INDEX"))
      {
        localObject = readIdentifierWithSchema();
        localAlterTableAddConstraint2.setIndex(paramSchema.findIndex(this.session, (String)localObject));
      }
      read("REFERENCES");
      parseReferences(localAlterTableAddConstraint2, paramSchema, paramString);
    }
    else
    {
      if (str1 != null) {
        throw getSyntaxError();
      }
      return null;
    }
    if (readIf("NOCHECK"))
    {
      localAlterTableAddConstraint2.setCheckExisting(false);
    }
    else
    {
      readIf("CHECK");
      localAlterTableAddConstraint2.setCheckExisting(true);
    }
    localAlterTableAddConstraint2.setTableName(paramString);
    localAlterTableAddConstraint2.setConstraintName(str1);
    localAlterTableAddConstraint2.setComment(str2);
    return localAlterTableAddConstraint2;
  }
  
  private void parseReferences(AlterTableAddConstraint paramAlterTableAddConstraint, Schema paramSchema, String paramString)
  {
    String str;
    if (readIf("("))
    {
      paramAlterTableAddConstraint.setRefTableName(paramSchema, paramString);
      paramAlterTableAddConstraint.setRefIndexColumns(parseIndexColumnList());
    }
    else
    {
      str = readIdentifierWithSchema(paramSchema.getName());
      paramAlterTableAddConstraint.setRefTableName(getSchema(), str);
      if (readIf("(")) {
        paramAlterTableAddConstraint.setRefIndexColumns(parseIndexColumnList());
      }
    }
    if (readIf("INDEX"))
    {
      str = readIdentifierWithSchema();
      paramAlterTableAddConstraint.setRefIndex(getSchema().findIndex(this.session, str));
    }
    while (readIf("ON")) {
      if (readIf("DELETE"))
      {
        paramAlterTableAddConstraint.setDeleteAction(parseAction());
      }
      else
      {
        read("UPDATE");
        paramAlterTableAddConstraint.setUpdateAction(parseAction());
      }
    }
    if (readIf("NOT")) {
      read("DEFERRABLE");
    } else {
      readIf("DEFERRABLE");
    }
  }
  
  private CreateLinkedTable parseCreateLinkedTable(boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    read("TABLE");
    boolean bool = readIfNoExists();
    String str1 = readIdentifierWithSchema();
    CreateLinkedTable localCreateLinkedTable = new CreateLinkedTable(this.session, getSchema());
    localCreateLinkedTable.setTemporary(paramBoolean1);
    localCreateLinkedTable.setGlobalTemporary(paramBoolean2);
    localCreateLinkedTable.setForce(paramBoolean3);
    localCreateLinkedTable.setIfNotExists(bool);
    localCreateLinkedTable.setTableName(str1);
    localCreateLinkedTable.setComment(readCommentIf());
    read("(");
    localCreateLinkedTable.setDriver(readString());
    read(",");
    localCreateLinkedTable.setUrl(readString());
    read(",");
    localCreateLinkedTable.setUser(readString());
    read(",");
    localCreateLinkedTable.setPassword(readString());
    read(",");
    String str2 = readString();
    if (readIf(","))
    {
      localCreateLinkedTable.setOriginalSchema(str2);
      str2 = readString();
    }
    localCreateLinkedTable.setOriginalTable(str2);
    read(")");
    if (readIf("EMIT"))
    {
      read("UPDATES");
      localCreateLinkedTable.setEmitUpdates(true);
    }
    else if (readIf("READONLY"))
    {
      localCreateLinkedTable.setReadOnly(true);
    }
    return localCreateLinkedTable;
  }
  
  private CreateTable parseCreateTable(boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    boolean bool1 = readIfNoExists();
    String str1 = readIdentifierWithSchema();
    if ((paramBoolean1) && (paramBoolean2) && (equalsToken("SESSION", this.schemaName)))
    {
      this.schemaName = this.session.getCurrentSchemaName();
      paramBoolean2 = false;
    }
    Schema localSchema = getSchema();
    CreateTable localCreateTable = new CreateTable(this.session, localSchema);
    localCreateTable.setPersistIndexes(paramBoolean3);
    localCreateTable.setTemporary(paramBoolean1);
    localCreateTable.setGlobalTemporary(paramBoolean2);
    localCreateTable.setIfNotExists(bool1);
    localCreateTable.setTableName(str1);
    localCreateTable.setComment(readCommentIf());
    Object localObject1;
    if ((readIf("(")) && 
      (!readIf(")"))) {
      do
      {
        localObject1 = parseAlterTableAddConstraintIf(str1, localSchema);
        if (localObject1 != null)
        {
          localCreateTable.addConstraintCommand((DefineCommand)localObject1);
        }
        else
        {
          String str2 = readColumnIdentifier();
          Column localColumn = parseColumnForTable(str2, true);
          if ((localColumn.isAutoIncrement()) && (localColumn.isPrimaryKey()))
          {
            localColumn.setPrimaryKey(false);
            localObject2 = new IndexColumn[] { new IndexColumn() };
            localObject2[0].columnName = localColumn.getName();
            AlterTableAddConstraint localAlterTableAddConstraint1 = new AlterTableAddConstraint(this.session, localSchema, false);
            
            localAlterTableAddConstraint1.setType(6);
            localAlterTableAddConstraint1.setTableName(str1);
            localAlterTableAddConstraint1.setIndexColumns((IndexColumn[])localObject2);
            localCreateTable.addConstraintCommand(localAlterTableAddConstraint1);
          }
          localCreateTable.addColumn(localColumn);
          Object localObject2 = null;
          if (readIf("CONSTRAINT")) {
            localObject2 = readColumnIdentifier();
          }
          IndexColumn[] arrayOfIndexColumn;
          Object localObject3;
          if (readIf("PRIMARY"))
          {
            read("KEY");
            boolean bool2 = readIf("HASH");
            arrayOfIndexColumn = new IndexColumn[] { new IndexColumn() };
            arrayOfIndexColumn[0].columnName = localColumn.getName();
            AlterTableAddConstraint localAlterTableAddConstraint2 = new AlterTableAddConstraint(this.session, localSchema, false);
            
            localAlterTableAddConstraint2.setPrimaryKeyHash(bool2);
            localAlterTableAddConstraint2.setType(6);
            localAlterTableAddConstraint2.setTableName(str1);
            localAlterTableAddConstraint2.setIndexColumns(arrayOfIndexColumn);
            localCreateTable.addConstraintCommand(localAlterTableAddConstraint2);
            if (readIf("AUTO_INCREMENT")) {
              parseAutoIncrement(localColumn);
            }
          }
          else if (readIf("UNIQUE"))
          {
            localObject3 = new AlterTableAddConstraint(this.session, localSchema, false);
            
            ((AlterTableAddConstraint)localObject3).setConstraintName((String)localObject2);
            ((AlterTableAddConstraint)localObject3).setType(4);
            arrayOfIndexColumn = new IndexColumn[] { new IndexColumn() };
            arrayOfIndexColumn[0].columnName = str2;
            ((AlterTableAddConstraint)localObject3).setIndexColumns(arrayOfIndexColumn);
            ((AlterTableAddConstraint)localObject3).setTableName(str1);
            localCreateTable.addConstraintCommand((DefineCommand)localObject3);
          }
          if (readIf("NOT"))
          {
            read("NULL");
            localColumn.setNullable(false);
          }
          else
          {
            readIf("NULL");
          }
          if (readIf("CHECK"))
          {
            localObject3 = readExpression();
            localColumn.addCheckConstraint(this.session, (Expression)localObject3);
          }
          if (readIf("REFERENCES"))
          {
            localObject3 = new AlterTableAddConstraint(this.session, localSchema, false);
            
            ((AlterTableAddConstraint)localObject3).setConstraintName((String)localObject2);
            ((AlterTableAddConstraint)localObject3).setType(5);
            arrayOfIndexColumn = new IndexColumn[] { new IndexColumn() };
            arrayOfIndexColumn[0].columnName = str2;
            ((AlterTableAddConstraint)localObject3).setIndexColumns(arrayOfIndexColumn);
            ((AlterTableAddConstraint)localObject3).setTableName(str1);
            parseReferences((AlterTableAddConstraint)localObject3, localSchema, str1);
            localCreateTable.addConstraintCommand((DefineCommand)localObject3);
          }
        }
      } while (readIfMore());
    }
    if ((readIf("COMMENT")) && 
      (readIf("="))) {
      readString();
    }
    if (readIf("ENGINE"))
    {
      if (readIf("="))
      {
        localObject1 = readUniqueIdentifier();
        if (!"InnoDb".equalsIgnoreCase((String)localObject1)) {
          if (!"MyISAM".equalsIgnoreCase((String)localObject1)) {
            throw DbException.getUnsupportedException((String)localObject1);
          }
        }
      }
      else
      {
        localCreateTable.setTableEngine(readUniqueIdentifier());
        if (readIf("WITH"))
        {
          localObject1 = New.arrayList();
          do
          {
            ((ArrayList)localObject1).add(readUniqueIdentifier());
          } while (readIf(","));
          localCreateTable.setTableEngineParams((ArrayList)localObject1);
        }
      }
    }
    else if (this.database.getSettings().defaultTableEngine != null) {
      localCreateTable.setTableEngine(this.database.getSettings().defaultTableEngine);
    }
    if (readIf("AUTO_INCREMENT"))
    {
      read("=");
      if ((this.currentTokenType != 5) || (this.currentValue.getType() != 4)) {
        throw DbException.getSyntaxError(this.sqlCommand, this.parseIndex, "integer");
      }
      read();
    }
    readIf("DEFAULT");
    if (readIf("CHARSET"))
    {
      read("=");
      read("UTF8");
    }
    if (paramBoolean1)
    {
      if (readIf("ON"))
      {
        read("COMMIT");
        if (readIf("DROP"))
        {
          localCreateTable.setOnCommitDrop();
        }
        else if (readIf("DELETE"))
        {
          read("ROWS");
          localCreateTable.setOnCommitTruncate();
        }
      }
      else if (readIf("NOT"))
      {
        if (readIf("PERSISTENT")) {
          localCreateTable.setPersistData(false);
        } else {
          read("LOGGED");
        }
      }
      if (readIf("TRANSACTIONAL")) {
        localCreateTable.setTransactional(true);
      }
    }
    else if ((!paramBoolean3) && (readIf("NOT")))
    {
      read("PERSISTENT");
      localCreateTable.setPersistData(false);
    }
    if (readIf("HIDDEN")) {
      localCreateTable.setHidden(true);
    }
    if (readIf("AS"))
    {
      if (readIf("SORTED")) {
        localCreateTable.setSortedInsertMode(true);
      }
      localCreateTable.setQuery(parseSelect());
    }
    if ((readIf("ROW_FORMAT")) && 
      (readIf("="))) {
      readColumnIdentifier();
    }
    return localCreateTable;
  }
  
  private static int getCompareType(int paramInt)
  {
    switch (paramInt)
    {
    case 6: 
      return 0;
    case 7: 
      return 1;
    case 8: 
      return 2;
    case 9: 
      return 4;
    case 10: 
      return 3;
    case 11: 
      return 5;
    case 25: 
      return 11;
    }
    return -1;
  }
  
  public static String quoteIdentifier(String paramString)
  {
    if ((paramString == null) || (paramString.length() == 0)) {
      return "\"\"";
    }
    char c = paramString.charAt(0);
    if (((!Character.isLetter(c)) && (c != '_')) || (Character.isLowerCase(c))) {
      return StringUtils.quoteIdentifier(paramString);
    }
    int i = 1;
    for (int j = paramString.length(); i < j; i++)
    {
      c = paramString.charAt(i);
      if (((!Character.isLetterOrDigit(c)) && (c != '_')) || (Character.isLowerCase(c))) {
        return StringUtils.quoteIdentifier(paramString);
      }
    }
    if (isKeyword(paramString, true)) {
      return StringUtils.quoteIdentifier(paramString);
    }
    return paramString;
  }
  
  public void setRightsChecked(boolean paramBoolean)
  {
    this.rightsChecked = paramBoolean;
  }
  
  public Expression parseExpression(String paramString)
  {
    this.parameters = New.arrayList();
    initialize(paramString);
    read();
    return readExpression();
  }
  
  public Table parseTableName(String paramString)
  {
    this.parameters = New.arrayList();
    initialize(paramString);
    read();
    return readTableOrView();
  }
}
