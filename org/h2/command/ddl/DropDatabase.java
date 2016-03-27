package org.h2.command.ddl;

import java.util.ArrayList;
import java.util.Iterator;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.Role;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.table.Table;
import org.h2.util.New;

public class DropDatabase
  extends DefineCommand
{
  private boolean dropAllObjects;
  private boolean deleteFiles;
  
  public DropDatabase(Session paramSession)
  {
    super(paramSession);
  }
  
  public int update()
  {
    if (this.dropAllObjects) {
      dropAllObjects();
    }
    if (this.deleteFiles) {
      this.session.getDatabase().setDeleteFilesOnDisconnect(true);
    }
    return 0;
  }
  
  private void dropAllObjects()
  {
    this.session.getUser().checkAdmin();
    this.session.commit(true);
    Database localDatabase = this.session.getDatabase();
    localDatabase.lockMeta(this.session);
    Object localObject4;
    int i;
    do
    {
      localObject1 = localDatabase.getAllTablesAndViews(false);
      localObject2 = New.arrayList();
      for (localObject3 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (Table)((Iterator)localObject3).next();
        if ((((Table)localObject4).getName() != null) && ("VIEW".equals(((Table)localObject4).getTableType()))) {
          ((ArrayList)localObject2).add(localObject4);
        }
      }
      for (localObject3 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (Table)((Iterator)localObject3).next();
        if ((((Table)localObject4).getName() != null) && ("TABLE LINK".equals(((Table)localObject4).getTableType()))) {
          ((ArrayList)localObject2).add(localObject4);
        }
      }
      for (localObject3 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (Table)((Iterator)localObject3).next();
        if ((((Table)localObject4).getName() != null) && ("TABLE".equals(((Table)localObject4).getTableType())) && (!((Table)localObject4).isHidden())) {
          ((ArrayList)localObject2).add(localObject4);
        }
      }
      for (localObject3 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (Table)((Iterator)localObject3).next();
        if ((((Table)localObject4).getName() != null) && ("EXTERNAL".equals(((Table)localObject4).getTableType())) && (!((Table)localObject4).isHidden())) {
          ((ArrayList)localObject2).add(localObject4);
        }
      }
      i = 0;
      for (localObject3 = ((ArrayList)localObject2).iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (Table)((Iterator)localObject3).next();
        if (((Table)localObject4).getName() != null) {
          if (localDatabase.getDependentTable((SchemaObject)localObject4, (Table)localObject4) == null) {
            localDatabase.removeSchemaObject(this.session, (SchemaObject)localObject4);
          } else {
            i = 1;
          }
        }
      }
    } while (i != 0);
    for (Object localObject1 = localDatabase.getAllSchemas().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Schema)((Iterator)localObject1).next();
      if (((Schema)localObject2).canDrop()) {
        localDatabase.removeDatabaseObject(this.session, (DbObject)localObject2);
      }
    }
    this.session.findLocalTempTable(null);
    localObject1 = New.arrayList();
    ((ArrayList)localObject1).addAll(localDatabase.getAllSchemaObjects(3));
    
    ((ArrayList)localObject1).addAll(localDatabase.getAllSchemaObjects(5));
    ((ArrayList)localObject1).addAll(localDatabase.getAllSchemaObjects(4));
    ((ArrayList)localObject1).addAll(localDatabase.getAllSchemaObjects(11));
    ((ArrayList)localObject1).addAll(localDatabase.getAllSchemaObjects(9));
    for (Object localObject2 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject2).hasNext();)
    {
      localObject3 = (SchemaObject)((Iterator)localObject2).next();
      if (!((SchemaObject)localObject3).isHidden()) {
        localDatabase.removeSchemaObject(this.session, (SchemaObject)localObject3);
      }
    }
    for (localObject2 = localDatabase.getAllUsers().iterator(); ((Iterator)localObject2).hasNext();)
    {
      localObject3 = (User)((Iterator)localObject2).next();
      if (localObject3 != this.session.getUser()) {
        localDatabase.removeDatabaseObject(this.session, (DbObject)localObject3);
      }
    }
    for (localObject2 = localDatabase.getAllRoles().iterator(); ((Iterator)localObject2).hasNext();)
    {
      localObject3 = (Role)((Iterator)localObject2).next();
      localObject4 = ((Role)localObject3).getCreateSQL();
      if (localObject4 != null) {
        localDatabase.removeDatabaseObject(this.session, (DbObject)localObject3);
      }
    }
    localObject2 = New.arrayList();
    ((ArrayList)localObject2).addAll(localDatabase.getAllRights());
    ((ArrayList)localObject2).addAll(localDatabase.getAllAggregates());
    ((ArrayList)localObject2).addAll(localDatabase.getAllUserDataTypes());
    for (Object localObject3 = ((ArrayList)localObject2).iterator(); ((Iterator)localObject3).hasNext();)
    {
      localObject4 = (DbObject)((Iterator)localObject3).next();
      String str = ((DbObject)localObject4).getCreateSQL();
      if (str != null) {
        localDatabase.removeDatabaseObject(this.session, (DbObject)localObject4);
      }
    }
  }
  
  public void setDropAllObjects(boolean paramBoolean)
  {
    this.dropAllObjects = paramBoolean;
  }
  
  public void setDeleteFiles(boolean paramBoolean)
  {
    this.deleteFiles = paramBoolean;
  }
  
  public int getType()
  {
    return 38;
  }
}
