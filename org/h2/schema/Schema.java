package org.h2.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.h2.api.TableEngine;
import org.h2.command.ddl.CreateTableData;
import org.h2.constraint.Constraint;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.DbObjectBase;
import org.h2.engine.DbSettings;
import org.h2.engine.FunctionAlias;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.engine.User;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.mvstore.db.MVTableEngine;
import org.h2.table.RegularTable;
import org.h2.table.Table;
import org.h2.util.JdbcUtils;
import org.h2.util.New;

public class Schema
  extends DbObjectBase
{
  private User owner;
  private final boolean system;
  private final HashMap<String, Table> tablesAndViews;
  private final HashMap<String, Index> indexes;
  private final HashMap<String, Sequence> sequences;
  private final HashMap<String, TriggerObject> triggers;
  private final HashMap<String, Constraint> constraints;
  private final HashMap<String, Constant> constants;
  private final HashMap<String, FunctionAlias> functions;
  private final HashSet<String> temporaryUniqueNames = New.hashSet();
  
  public Schema(Database paramDatabase, int paramInt, String paramString, User paramUser, boolean paramBoolean)
  {
    this.tablesAndViews = paramDatabase.newStringMap();
    this.indexes = paramDatabase.newStringMap();
    this.sequences = paramDatabase.newStringMap();
    this.triggers = paramDatabase.newStringMap();
    this.constraints = paramDatabase.newStringMap();
    this.constants = paramDatabase.newStringMap();
    this.functions = paramDatabase.newStringMap();
    initDbObjectBase(paramDatabase, paramInt, paramString, "schema");
    this.owner = paramUser;
    this.system = paramBoolean;
  }
  
  public boolean canDrop()
  {
    return !this.system;
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQL()
  {
    if (this.system) {
      return null;
    }
    return "CREATE SCHEMA IF NOT EXISTS " + getSQL() + " AUTHORIZATION " + this.owner.getSQL();
  }
  
  public int getType()
  {
    return 10;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    Object localObject1;
    while ((this.triggers != null) && (this.triggers.size() > 0))
    {
      localObject1 = (TriggerObject)this.triggers.values().toArray()[0];
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject1);
    }
    while ((this.constraints != null) && (this.constraints.size() > 0))
    {
      localObject1 = (Constraint)this.constraints.values().toArray()[0];
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject1);
    }
    int i = 0;
    Object localObject2;
    do
    {
      i = 0;
      if (this.tablesAndViews != null) {
        for (localObject2 = New.arrayList(this.tablesAndViews.values()).iterator(); ((Iterator)localObject2).hasNext();)
        {
          Table localTable = (Table)((Iterator)localObject2).next();
          if (localTable.getName() != null) {
            if (this.database.getDependentTable(localTable, localTable) == null) {
              this.database.removeSchemaObject(paramSession, localTable);
            } else {
              i = 1;
            }
          }
        }
      }
    } while (i != 0);
    while ((this.indexes != null) && (this.indexes.size() > 0))
    {
      localObject2 = (Index)this.indexes.values().toArray()[0];
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject2);
    }
    while ((this.sequences != null) && (this.sequences.size() > 0))
    {
      localObject2 = (Sequence)this.sequences.values().toArray()[0];
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject2);
    }
    while ((this.constants != null) && (this.constants.size() > 0))
    {
      localObject2 = (Constant)this.constants.values().toArray()[0];
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject2);
    }
    while ((this.functions != null) && (this.functions.size() > 0))
    {
      localObject2 = (FunctionAlias)this.functions.values().toArray()[0];
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject2);
    }
    this.database.removeMeta(paramSession, getId());
    this.owner = null;
    invalidate();
  }
  
  public void checkRename() {}
  
  public User getOwner()
  {
    return this.owner;
  }
  
  private HashMap<String, SchemaObject> getMap(int paramInt)
  {
    HashMap localHashMap;
    switch (paramInt)
    {
    case 0: 
      localHashMap = this.tablesAndViews;
      break;
    case 3: 
      localHashMap = this.sequences;
      break;
    case 1: 
      localHashMap = this.indexes;
      break;
    case 4: 
      localHashMap = this.triggers;
      break;
    case 5: 
      localHashMap = this.constraints;
      break;
    case 11: 
      localHashMap = this.constants;
      break;
    case 9: 
      localHashMap = this.functions;
      break;
    case 2: 
    case 6: 
    case 7: 
    case 8: 
    case 10: 
    default: 
      throw DbException.throwInternalError("type=" + paramInt);
    }
    return localHashMap;
  }
  
  public void add(SchemaObject paramSchemaObject)
  {
    if ((SysProperties.CHECK) && (paramSchemaObject.getSchema() != this)) {
      DbException.throwInternalError("wrong schema");
    }
    String str = paramSchemaObject.getName();
    HashMap localHashMap = getMap(paramSchemaObject.getType());
    if ((SysProperties.CHECK) && (localHashMap.get(str) != null)) {
      DbException.throwInternalError("object already exists: " + str);
    }
    localHashMap.put(str, paramSchemaObject);
    freeUniqueName(str);
  }
  
  public void rename(SchemaObject paramSchemaObject, String paramString)
  {
    int i = paramSchemaObject.getType();
    HashMap localHashMap = getMap(i);
    if (SysProperties.CHECK)
    {
      if (!localHashMap.containsKey(paramSchemaObject.getName())) {
        DbException.throwInternalError("not found: " + paramSchemaObject.getName());
      }
      if ((paramSchemaObject.getName().equals(paramString)) || (localHashMap.containsKey(paramString))) {
        DbException.throwInternalError("object already exists: " + paramString);
      }
    }
    paramSchemaObject.checkRename();
    localHashMap.remove(paramSchemaObject.getName());
    freeUniqueName(paramSchemaObject.getName());
    paramSchemaObject.rename(paramString);
    localHashMap.put(paramString, paramSchemaObject);
    freeUniqueName(paramString);
  }
  
  public Table findTableOrView(Session paramSession, String paramString)
  {
    Table localTable = (Table)this.tablesAndViews.get(paramString);
    if ((localTable == null) && (paramSession != null)) {
      localTable = paramSession.findLocalTempTable(paramString);
    }
    return localTable;
  }
  
  public Index findIndex(Session paramSession, String paramString)
  {
    Index localIndex = (Index)this.indexes.get(paramString);
    if (localIndex == null) {
      localIndex = paramSession.findLocalTempTableIndex(paramString);
    }
    return localIndex;
  }
  
  public TriggerObject findTrigger(String paramString)
  {
    return (TriggerObject)this.triggers.get(paramString);
  }
  
  public Sequence findSequence(String paramString)
  {
    return (Sequence)this.sequences.get(paramString);
  }
  
  public Constraint findConstraint(Session paramSession, String paramString)
  {
    Constraint localConstraint = (Constraint)this.constraints.get(paramString);
    if (localConstraint == null) {
      localConstraint = paramSession.findLocalTempTableConstraint(paramString);
    }
    return localConstraint;
  }
  
  public Constant findConstant(String paramString)
  {
    return (Constant)this.constants.get(paramString);
  }
  
  public FunctionAlias findFunction(String paramString)
  {
    return (FunctionAlias)this.functions.get(paramString);
  }
  
  public void freeUniqueName(String paramString)
  {
    if (paramString != null) {
      synchronized (this.temporaryUniqueNames)
      {
        this.temporaryUniqueNames.remove(paramString);
      }
    }
  }
  
  private String getUniqueName(DbObject paramDbObject, HashMap<String, ? extends SchemaObject> paramHashMap, String paramString)
  {
    String str1 = Integer.toHexString(paramDbObject.getName().hashCode()).toUpperCase();
    String str2 = null;
    synchronized (this.temporaryUniqueNames)
    {
      int i = 1;
      for (int j = str1.length(); i < j; i++)
      {
        str2 = paramString + str1.substring(0, i);
        if ((!paramHashMap.containsKey(str2)) && (!this.temporaryUniqueNames.contains(str2))) {
          break;
        }
        str2 = null;
      }
      if (str2 == null)
      {
        paramString = paramString + str1 + "_";
        for (i = 0;; i++)
        {
          str2 = paramString + i;
          if ((!paramHashMap.containsKey(str2)) && (!this.temporaryUniqueNames.contains(str2))) {
            break;
          }
        }
      }
      this.temporaryUniqueNames.add(str2);
    }
    return str2;
  }
  
  public String getUniqueConstraintName(Session paramSession, Table paramTable)
  {
    HashMap localHashMap;
    if ((paramTable.isTemporary()) && (!paramTable.isGlobalTemporary())) {
      localHashMap = paramSession.getLocalTempTableConstraints();
    } else {
      localHashMap = this.constraints;
    }
    return getUniqueName(paramTable, localHashMap, "CONSTRAINT_");
  }
  
  public String getUniqueIndexName(Session paramSession, Table paramTable, String paramString)
  {
    HashMap localHashMap;
    if ((paramTable.isTemporary()) && (!paramTable.isGlobalTemporary())) {
      localHashMap = paramSession.getLocalTempTableIndexes();
    } else {
      localHashMap = this.indexes;
    }
    return getUniqueName(paramTable, localHashMap, paramString);
  }
  
  public Table getTableOrView(Session paramSession, String paramString)
  {
    Table localTable = (Table)this.tablesAndViews.get(paramString);
    if (localTable == null)
    {
      if (paramSession != null) {
        localTable = paramSession.findLocalTempTable(paramString);
      }
      if (localTable == null) {
        throw DbException.get(42102, paramString);
      }
    }
    return localTable;
  }
  
  public Index getIndex(String paramString)
  {
    Index localIndex = (Index)this.indexes.get(paramString);
    if (localIndex == null) {
      throw DbException.get(42112, paramString);
    }
    return localIndex;
  }
  
  public Constraint getConstraint(String paramString)
  {
    Constraint localConstraint = (Constraint)this.constraints.get(paramString);
    if (localConstraint == null) {
      throw DbException.get(90057, paramString);
    }
    return localConstraint;
  }
  
  public Constant getConstant(String paramString)
  {
    Constant localConstant = (Constant)this.constants.get(paramString);
    if (localConstant == null) {
      throw DbException.get(90115, paramString);
    }
    return localConstant;
  }
  
  public Sequence getSequence(String paramString)
  {
    Sequence localSequence = (Sequence)this.sequences.get(paramString);
    if (localSequence == null) {
      throw DbException.get(90036, paramString);
    }
    return localSequence;
  }
  
  public ArrayList<SchemaObject> getAll()
  {
    ArrayList localArrayList = New.arrayList();
    localArrayList.addAll(getMap(0).values());
    localArrayList.addAll(getMap(3).values());
    localArrayList.addAll(getMap(1).values());
    localArrayList.addAll(getMap(4).values());
    localArrayList.addAll(getMap(5).values());
    localArrayList.addAll(getMap(11).values());
    localArrayList.addAll(getMap(9).values());
    return localArrayList;
  }
  
  public ArrayList<SchemaObject> getAll(int paramInt)
  {
    HashMap localHashMap = getMap(paramInt);
    return New.arrayList(localHashMap.values());
  }
  
  /* Error */
  public ArrayList<Table> getAllTablesAndViews()
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 29	org/h2/schema/Schema:database	Lorg/h2/engine/Database;
    //   4: dup
    //   5: astore_1
    //   6: monitorenter
    //   7: aload_0
    //   8: getfield 5	org/h2/schema/Schema:tablesAndViews	Ljava/util/HashMap;
    //   11: invokevirtual 26	java/util/HashMap:values	()Ljava/util/Collection;
    //   14: invokestatic 32	org/h2/util/New:arrayList	(Ljava/util/Collection;)Ljava/util/ArrayList;
    //   17: aload_1
    //   18: monitorexit
    //   19: areturn
    //   20: astore_2
    //   21: aload_1
    //   22: monitorexit
    //   23: aload_2
    //   24: athrow
    // Line number table:
    //   Java source line #538	-> byte code offset #0
    //   Java source line #539	-> byte code offset #7
    //   Java source line #540	-> byte code offset #20
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	25	0	this	Schema
    //   5	17	1	Ljava/lang/Object;	Object
    //   20	4	2	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   7	19	20	finally
    //   20	23	20	finally
  }
  
  public void remove(SchemaObject paramSchemaObject)
  {
    String str = paramSchemaObject.getName();
    HashMap localHashMap = getMap(paramSchemaObject.getType());
    if ((SysProperties.CHECK) && (!localHashMap.containsKey(str))) {
      DbException.throwInternalError("not found: " + str);
    }
    localHashMap.remove(str);
    freeUniqueName(str);
  }
  
  public Table createTable(CreateTableData paramCreateTableData)
  {
    synchronized (this.database)
    {
      if ((!paramCreateTableData.temporary) || (paramCreateTableData.globalTemporary)) {
        this.database.lockMeta(paramCreateTableData.session);
      }
      paramCreateTableData.schema = this;
      if ((paramCreateTableData.tableEngine == null) && 
        (this.database.getSettings().mvStore)) {
        paramCreateTableData.tableEngine = MVTableEngine.class.getName();
      }
      if (paramCreateTableData.tableEngine != null)
      {
        TableEngine localTableEngine;
        try
        {
          localTableEngine = (TableEngine)JdbcUtils.loadUserClass(paramCreateTableData.tableEngine).newInstance();
        }
        catch (Exception localException)
        {
          throw DbException.convert(localException);
        }
        return localTableEngine.createTable(paramCreateTableData);
      }
      return new RegularTable(paramCreateTableData);
    }
  }
  
  /* Error */
  public org.h2.table.TableLink createTableLink(int paramInt, String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, String paramString6, String paramString7, boolean paramBoolean1, boolean paramBoolean2)
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 29	org/h2/schema/Schema:database	Lorg/h2/engine/Database;
    //   4: dup
    //   5: astore 11
    //   7: monitorenter
    //   8: new 110	org/h2/table/TableLink
    //   11: dup
    //   12: aload_0
    //   13: iload_1
    //   14: aload_2
    //   15: aload_3
    //   16: aload 4
    //   18: aload 5
    //   20: aload 6
    //   22: aload 7
    //   24: aload 8
    //   26: iload 9
    //   28: iload 10
    //   30: invokespecial 111	org/h2/table/TableLink:<init>	(Lorg/h2/schema/Schema;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   33: aload 11
    //   35: monitorexit
    //   36: areturn
    //   37: astore 12
    //   39: aload 11
    //   41: monitorexit
    //   42: aload 12
    //   44: athrow
    // Line number table:
    //   Java source line #606	-> byte code offset #0
    //   Java source line #607	-> byte code offset #8
    //   Java source line #610	-> byte code offset #37
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	45	0	this	Schema
    //   0	45	1	paramInt	int
    //   0	45	2	paramString1	String
    //   0	45	3	paramString2	String
    //   0	45	4	paramString3	String
    //   0	45	5	paramString4	String
    //   0	45	6	paramString5	String
    //   0	45	7	paramString6	String
    //   0	45	8	paramString7	String
    //   0	45	9	paramBoolean1	boolean
    //   0	45	10	paramBoolean2	boolean
    //   5	35	11	Ljava/lang/Object;	Object
    //   37	6	12	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   8	36	37	finally
    //   37	42	37	finally
  }
}
