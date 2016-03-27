package org.h2.index;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class TreeIndex
  extends BaseIndex
{
  private TreeNode root;
  private final RegularTable tableData;
  private long rowCount;
  private boolean closed;
  
  public TreeIndex(RegularTable paramRegularTable, int paramInt, String paramString, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType)
  {
    initBaseIndex(paramRegularTable, paramInt, paramString, paramArrayOfIndexColumn, paramIndexType);
    this.tableData = paramRegularTable;
    if (!this.database.isStarting()) {
      checkIndexColumnTypes(paramArrayOfIndexColumn);
    }
  }
  
  public void close(Session paramSession)
  {
    this.root = null;
    this.closed = true;
  }
  
  public void add(Session paramSession, Row paramRow)
  {
    if (this.closed) {
      throw DbException.throwInternalError();
    }
    TreeNode localTreeNode1 = new TreeNode(paramRow);
    TreeNode localTreeNode2 = this.root;TreeNode localTreeNode3 = localTreeNode2;
    boolean bool = true;
    for (;;)
    {
      if (localTreeNode2 == null)
      {
        if (localTreeNode3 == null)
        {
          this.root = localTreeNode1;
          this.rowCount += 1L;
          return;
        }
        set(localTreeNode3, bool, localTreeNode1);
        break;
      }
      Row localRow = localTreeNode2.row;
      int i = compareRows(paramRow, localRow);
      if (i == 0)
      {
        if ((this.indexType.isUnique()) && 
          (!containsNullAndAllowMultipleNull(paramRow))) {
          throw getDuplicateKeyException(paramRow.toString());
        }
        i = compareKeys(paramRow, localRow);
      }
      bool = i < 0;
      localTreeNode3 = localTreeNode2;
      localTreeNode2 = child(localTreeNode3, bool);
    }
    balance(localTreeNode3, bool);
    this.rowCount += 1L;
  }
  
  private void balance(TreeNode paramTreeNode, boolean paramBoolean)
  {
    for (;;)
    {
      int i = paramBoolean ? 1 : -1;
      switch (paramTreeNode.balance * i)
      {
      case 1: 
        paramTreeNode.balance = 0;
        return;
      case 0: 
        paramTreeNode.balance = (-i);
        break;
      case -1: 
        TreeNode localTreeNode1 = child(paramTreeNode, paramBoolean);
        if (localTreeNode1.balance == -i)
        {
          replace(paramTreeNode, localTreeNode1);
          set(paramTreeNode, paramBoolean, child(localTreeNode1, !paramBoolean));
          set(localTreeNode1, !paramBoolean, paramTreeNode);
          paramTreeNode.balance = 0;
          localTreeNode1.balance = 0;
        }
        else
        {
          TreeNode localTreeNode2 = child(localTreeNode1, !paramBoolean);
          replace(paramTreeNode, localTreeNode2);
          set(localTreeNode1, !paramBoolean, child(localTreeNode2, paramBoolean));
          set(localTreeNode2, paramBoolean, localTreeNode1);
          set(paramTreeNode, paramBoolean, child(localTreeNode2, !paramBoolean));
          set(localTreeNode2, !paramBoolean, paramTreeNode);
          int j = localTreeNode2.balance;
          paramTreeNode.balance = (j == -i ? i : 0);
          localTreeNode1.balance = (j == i ? -i : 0);
          localTreeNode2.balance = 0;
        }
        return;
      default: 
        DbException.throwInternalError("b:" + paramTreeNode.balance * i);
      }
      if (paramTreeNode == this.root) {
        return;
      }
      paramBoolean = paramTreeNode.isFromLeft();
      paramTreeNode = paramTreeNode.parent;
    }
  }
  
  private static TreeNode child(TreeNode paramTreeNode, boolean paramBoolean)
  {
    return paramBoolean ? paramTreeNode.left : paramTreeNode.right;
  }
  
  private void replace(TreeNode paramTreeNode1, TreeNode paramTreeNode2)
  {
    if (paramTreeNode1 == this.root)
    {
      this.root = paramTreeNode2;
      if (paramTreeNode2 != null) {
        paramTreeNode2.parent = null;
      }
    }
    else
    {
      set(paramTreeNode1.parent, paramTreeNode1.isFromLeft(), paramTreeNode2);
    }
  }
  
  private static void set(TreeNode paramTreeNode1, boolean paramBoolean, TreeNode paramTreeNode2)
  {
    if (paramBoolean) {
      paramTreeNode1.left = paramTreeNode2;
    } else {
      paramTreeNode1.right = paramTreeNode2;
    }
    if (paramTreeNode2 != null) {
      paramTreeNode2.parent = paramTreeNode1;
    }
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    if (this.closed) {
      throw DbException.throwInternalError();
    }
    Object localObject1 = findFirstNode(paramRow, true);
    if (localObject1 == null) {
      throw DbException.throwInternalError("not found!");
    }
    int i;
    TreeNode localTreeNode2;
    if (((TreeNode)localObject1).left == null)
    {
      localTreeNode1 = ((TreeNode)localObject1).right;
    }
    else if (((TreeNode)localObject1).right == null)
    {
      localTreeNode1 = ((TreeNode)localObject1).left;
    }
    else
    {
      Object localObject2 = localObject1;
      localObject1 = ((TreeNode)localObject1).left;
      for (Object localObject3 = localObject1; (localObject3 = ((TreeNode)localObject3).right) != null;) {
        localObject1 = localObject3;
      }
      localTreeNode1 = ((TreeNode)localObject1).left;
      
      i = ((TreeNode)localObject1).balance;
      ((TreeNode)localObject1).balance = ((TreeNode)localObject2).balance;
      ((TreeNode)localObject2).balance = i;
      
      localTreeNode2 = ((TreeNode)localObject1).parent;
      TreeNode localTreeNode3 = ((TreeNode)localObject2).parent;
      if (localObject2 == this.root) {
        this.root = ((TreeNode)localObject1);
      }
      ((TreeNode)localObject1).parent = localTreeNode3;
      if (localTreeNode3 != null) {
        if (localTreeNode3.right == localObject2) {
          localTreeNode3.right = ((TreeNode)localObject1);
        } else {
          localTreeNode3.left = ((TreeNode)localObject1);
        }
      }
      if (localTreeNode2 == localObject2)
      {
        ((TreeNode)localObject2).parent = ((TreeNode)localObject1);
        if (((TreeNode)localObject2).left == localObject1)
        {
          ((TreeNode)localObject1).left = ((TreeNode)localObject2);
          ((TreeNode)localObject1).right = ((TreeNode)localObject2).right;
        }
        else
        {
          ((TreeNode)localObject1).right = ((TreeNode)localObject2);
          ((TreeNode)localObject1).left = ((TreeNode)localObject2).left;
        }
      }
      else
      {
        ((TreeNode)localObject2).parent = localTreeNode2;
        localTreeNode2.right = ((TreeNode)localObject2);
        ((TreeNode)localObject1).right = ((TreeNode)localObject2).right;
        ((TreeNode)localObject1).left = ((TreeNode)localObject2).left;
      }
      if ((SysProperties.CHECK) && (((TreeNode)localObject1).right == null)) {
        DbException.throwInternalError("tree corrupted");
      }
      ((TreeNode)localObject1).right.parent = ((TreeNode)localObject1);
      ((TreeNode)localObject1).left.parent = ((TreeNode)localObject1);
      
      ((TreeNode)localObject2).left = localTreeNode1;
      if (localTreeNode1 != null) {
        localTreeNode1.parent = ((TreeNode)localObject2);
      }
      ((TreeNode)localObject2).right = null;
      localObject1 = localObject2;
    }
    this.rowCount -= 1L;
    
    boolean bool = ((TreeNode)localObject1).isFromLeft();
    replace((TreeNode)localObject1, localTreeNode1);
    TreeNode localTreeNode1 = ((TreeNode)localObject1).parent;
    while (localTreeNode1 != null)
    {
      localObject1 = localTreeNode1;
      i = bool ? 1 : -1;
      switch (((TreeNode)localObject1).balance * i)
      {
      case -1: 
        ((TreeNode)localObject1).balance = 0;
        break;
      case 0: 
        ((TreeNode)localObject1).balance = i;
        return;
      case 1: 
        localTreeNode2 = child((TreeNode)localObject1, !bool);
        int j = localTreeNode2.balance;
        if (j * i >= 0)
        {
          replace((TreeNode)localObject1, localTreeNode2);
          set((TreeNode)localObject1, !bool, child(localTreeNode2, bool));
          set(localTreeNode2, bool, (TreeNode)localObject1);
          if (j == 0)
          {
            ((TreeNode)localObject1).balance = i;
            localTreeNode2.balance = (-i);
            return;
          }
          ((TreeNode)localObject1).balance = 0;
          localTreeNode2.balance = 0;
          localObject1 = localTreeNode2;
        }
        else
        {
          TreeNode localTreeNode4 = child(localTreeNode2, bool);
          replace((TreeNode)localObject1, localTreeNode4);
          j = localTreeNode4.balance;
          set(localTreeNode2, bool, child(localTreeNode4, !bool));
          set(localTreeNode4, !bool, localTreeNode2);
          set((TreeNode)localObject1, !bool, child(localTreeNode4, bool));
          set(localTreeNode4, bool, (TreeNode)localObject1);
          ((TreeNode)localObject1).balance = (j == i ? -i : 0);
          localTreeNode2.balance = (j == -i ? i : 0);
          localTreeNode4.balance = 0;
          localObject1 = localTreeNode4;
        }
        break;
      default: 
        DbException.throwInternalError("b: " + ((TreeNode)localObject1).balance * i);
      }
      bool = ((TreeNode)localObject1).isFromLeft();
      localTreeNode1 = ((TreeNode)localObject1).parent;
    }
  }
  
  private TreeNode findFirstNode(SearchRow paramSearchRow, boolean paramBoolean)
  {
    TreeNode localTreeNode1 = this.root;TreeNode localTreeNode2 = localTreeNode1;
    while (localTreeNode1 != null)
    {
      localTreeNode2 = localTreeNode1;
      int i = compareRows(localTreeNode1.row, paramSearchRow);
      if ((i == 0) && (paramBoolean)) {
        i = compareKeys(localTreeNode1.row, paramSearchRow);
      }
      if (i == 0)
      {
        if (paramBoolean) {
          return localTreeNode1;
        }
        localTreeNode1 = localTreeNode1.left;
      }
      else if (i > 0)
      {
        localTreeNode1 = localTreeNode1.left;
      }
      else
      {
        localTreeNode1 = localTreeNode1.right;
      }
    }
    return localTreeNode2;
  }
  
  public Cursor find(TableFilter paramTableFilter, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramSearchRow1, paramSearchRow2);
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramSearchRow1, paramSearchRow2);
  }
  
  private Cursor find(SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    if (paramSearchRow1 == null)
    {
      localObject = this.root;
      while (localObject != null)
      {
        TreeNode localTreeNode = ((TreeNode)localObject).left;
        if (localTreeNode == null) {
          break;
        }
        localObject = localTreeNode;
      }
      return new TreeCursor(this, (TreeNode)localObject, null, paramSearchRow2);
    }
    Object localObject = findFirstNode(paramSearchRow1, false);
    return new TreeCursor(this, (TreeNode)localObject, paramSearchRow1, paramSearchRow2);
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    return getCostRangeIndex(paramArrayOfInt, this.tableData.getRowCountApproximation(), paramTableFilter, paramSortOrder);
  }
  
  public void remove(Session paramSession)
  {
    truncate(paramSession);
  }
  
  public void truncate(Session paramSession)
  {
    this.root = null;
    this.rowCount = 0L;
  }
  
  public void checkRename() {}
  
  public boolean needRebuild()
  {
    return true;
  }
  
  public boolean canGetFirstOrLast()
  {
    return true;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    if (this.closed) {
      throw DbException.throwInternalError();
    }
    Object localObject2;
    if (paramBoolean)
    {
      localObject1 = find(paramSession, null, null);
      while (((Cursor)localObject1).next())
      {
        localObject2 = ((Cursor)localObject1).getSearchRow();
        localObject3 = ((SearchRow)localObject2).getValue(this.columnIds[0]);
        if (localObject3 != ValueNull.INSTANCE) {
          return (Cursor)localObject1;
        }
      }
      return (Cursor)localObject1;
    }
    Object localObject1 = this.root;
    while (localObject1 != null)
    {
      localObject2 = ((TreeNode)localObject1).right;
      if (localObject2 == null) {
        break;
      }
      localObject1 = localObject2;
    }
    Object localObject3 = new TreeCursor(this, (TreeNode)localObject1, null, null);
    if (localObject1 == null) {
      return (Cursor)localObject3;
    }
    do
    {
      SearchRow localSearchRow = ((TreeCursor)localObject3).getSearchRow();
      if (localSearchRow == null) {
        break;
      }
      Value localValue = localSearchRow.getValue(this.columnIds[0]);
      if (localValue != ValueNull.INSTANCE) {
        return (Cursor)localObject3;
      }
    } while (((TreeCursor)localObject3).previous());
    return (Cursor)localObject3;
  }
  
  public long getRowCount(Session paramSession)
  {
    return this.rowCount;
  }
  
  public long getRowCountApproximation()
  {
    return this.rowCount;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
}
