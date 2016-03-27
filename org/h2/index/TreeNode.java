package org.h2.index;

import org.h2.result.Row;

class TreeNode
{
  int balance;
  TreeNode left;
  TreeNode right;
  TreeNode parent;
  final Row row;
  
  TreeNode(Row paramRow)
  {
    this.row = paramRow;
  }
  
  boolean isFromLeft()
  {
    return (this.parent == null) || (this.parent.left == this);
  }
}
