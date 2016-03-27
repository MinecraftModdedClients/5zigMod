package org.h2.index;

import org.h2.result.Row;
import org.h2.result.SearchRow;

public class TreeCursor
  implements Cursor
{
  private final TreeIndex tree;
  private TreeNode node;
  private boolean beforeFirst;
  private final SearchRow first;
  private final SearchRow last;
  
  TreeCursor(TreeIndex paramTreeIndex, TreeNode paramTreeNode, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    this.tree = paramTreeIndex;
    this.node = paramTreeNode;
    this.first = paramSearchRow1;
    this.last = paramSearchRow2;
    this.beforeFirst = true;
  }
  
  public Row get()
  {
    return this.node == null ? null : this.node.row;
  }
  
  public SearchRow getSearchRow()
  {
    return get();
  }
  
  public boolean next()
  {
    if (this.beforeFirst)
    {
      this.beforeFirst = false;
      if (this.node == null) {
        return false;
      }
      if ((this.first != null) && (this.tree.compareRows(this.node.row, this.first) < 0)) {
        this.node = next(this.node);
      }
    }
    else
    {
      this.node = next(this.node);
    }
    if ((this.node != null) && (this.last != null) && 
      (this.tree.compareRows(this.node.row, this.last) > 0)) {
      this.node = null;
    }
    return this.node != null;
  }
  
  public boolean previous()
  {
    this.node = previous(this.node);
    return this.node != null;
  }
  
  private static TreeNode next(TreeNode paramTreeNode)
  {
    if (paramTreeNode == null) {
      return null;
    }
    TreeNode localTreeNode1 = paramTreeNode.right;
    if (localTreeNode1 != null)
    {
      paramTreeNode = localTreeNode1;
      localTreeNode2 = paramTreeNode.left;
      while (localTreeNode2 != null)
      {
        paramTreeNode = localTreeNode2;
        localTreeNode2 = paramTreeNode.left;
      }
      return paramTreeNode;
    }
    TreeNode localTreeNode2 = paramTreeNode;
    paramTreeNode = paramTreeNode.parent;
    while ((paramTreeNode != null) && (localTreeNode2 == paramTreeNode.right))
    {
      localTreeNode2 = paramTreeNode;
      paramTreeNode = paramTreeNode.parent;
    }
    return paramTreeNode;
  }
  
  private static TreeNode previous(TreeNode paramTreeNode)
  {
    if (paramTreeNode == null) {
      return null;
    }
    TreeNode localTreeNode1 = paramTreeNode.left;
    if (localTreeNode1 != null)
    {
      paramTreeNode = localTreeNode1;
      localTreeNode2 = paramTreeNode.right;
      while (localTreeNode2 != null)
      {
        paramTreeNode = localTreeNode2;
        localTreeNode2 = paramTreeNode.right;
      }
      return paramTreeNode;
    }
    TreeNode localTreeNode2 = paramTreeNode;
    paramTreeNode = paramTreeNode.parent;
    while ((paramTreeNode != null) && (localTreeNode2 == paramTreeNode.left))
    {
      localTreeNode2 = paramTreeNode;
      paramTreeNode = paramTreeNode.parent;
    }
    return paramTreeNode;
  }
}
