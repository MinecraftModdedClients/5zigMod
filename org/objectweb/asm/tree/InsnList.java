package org.objectweb.asm.tree;

import java.util.ListIterator;
import org.objectweb.asm.MethodVisitor;

public class InsnList
{
  private int size;
  private AbstractInsnNode first;
  private AbstractInsnNode last;
  AbstractInsnNode[] cache;
  
  public int size()
  {
    return this.size;
  }
  
  public AbstractInsnNode getFirst()
  {
    return this.first;
  }
  
  public AbstractInsnNode getLast()
  {
    return this.last;
  }
  
  public AbstractInsnNode get(int paramInt)
  {
    if ((paramInt < 0) || (paramInt >= this.size)) {
      throw new IndexOutOfBoundsException();
    }
    if (this.cache == null) {
      this.cache = toArray();
    }
    return this.cache[paramInt];
  }
  
  public boolean contains(AbstractInsnNode paramAbstractInsnNode)
  {
    for (AbstractInsnNode localAbstractInsnNode = this.first; (localAbstractInsnNode != null) && (localAbstractInsnNode != paramAbstractInsnNode); localAbstractInsnNode = localAbstractInsnNode.next) {}
    return localAbstractInsnNode != null;
  }
  
  public int indexOf(AbstractInsnNode paramAbstractInsnNode)
  {
    if (this.cache == null) {
      this.cache = toArray();
    }
    return paramAbstractInsnNode.index;
  }
  
  public void accept(MethodVisitor paramMethodVisitor)
  {
    for (AbstractInsnNode localAbstractInsnNode = this.first; localAbstractInsnNode != null; localAbstractInsnNode = localAbstractInsnNode.next) {
      localAbstractInsnNode.accept(paramMethodVisitor);
    }
  }
  
  public ListIterator iterator()
  {
    return iterator(0);
  }
  
  public ListIterator iterator(int paramInt)
  {
    return new InsnList.InsnListIterator(this, paramInt);
  }
  
  public AbstractInsnNode[] toArray()
  {
    int i = 0;
    AbstractInsnNode localAbstractInsnNode = this.first;
    AbstractInsnNode[] arrayOfAbstractInsnNode = new AbstractInsnNode[this.size];
    while (localAbstractInsnNode != null)
    {
      arrayOfAbstractInsnNode[i] = localAbstractInsnNode;
      localAbstractInsnNode.index = (i++);
      localAbstractInsnNode = localAbstractInsnNode.next;
    }
    return arrayOfAbstractInsnNode;
  }
  
  public void set(AbstractInsnNode paramAbstractInsnNode1, AbstractInsnNode paramAbstractInsnNode2)
  {
    AbstractInsnNode localAbstractInsnNode1 = paramAbstractInsnNode1.next;
    paramAbstractInsnNode2.next = localAbstractInsnNode1;
    if (localAbstractInsnNode1 != null) {
      localAbstractInsnNode1.prev = paramAbstractInsnNode2;
    } else {
      this.last = paramAbstractInsnNode2;
    }
    AbstractInsnNode localAbstractInsnNode2 = paramAbstractInsnNode1.prev;
    paramAbstractInsnNode2.prev = localAbstractInsnNode2;
    if (localAbstractInsnNode2 != null) {
      localAbstractInsnNode2.next = paramAbstractInsnNode2;
    } else {
      this.first = paramAbstractInsnNode2;
    }
    if (this.cache != null)
    {
      int i = paramAbstractInsnNode1.index;
      this.cache[i] = paramAbstractInsnNode2;
      paramAbstractInsnNode2.index = i;
    }
    else
    {
      paramAbstractInsnNode2.index = 0;
    }
    paramAbstractInsnNode1.index = -1;
    paramAbstractInsnNode1.prev = null;
    paramAbstractInsnNode1.next = null;
  }
  
  public void add(AbstractInsnNode paramAbstractInsnNode)
  {
    this.size += 1;
    if (this.last == null)
    {
      this.first = paramAbstractInsnNode;
      this.last = paramAbstractInsnNode;
    }
    else
    {
      this.last.next = paramAbstractInsnNode;
      paramAbstractInsnNode.prev = this.last;
    }
    this.last = paramAbstractInsnNode;
    this.cache = null;
    paramAbstractInsnNode.index = 0;
  }
  
  public void add(InsnList paramInsnList)
  {
    if (paramInsnList.size == 0) {
      return;
    }
    this.size += paramInsnList.size;
    if (this.last == null)
    {
      this.first = paramInsnList.first;
      this.last = paramInsnList.last;
    }
    else
    {
      AbstractInsnNode localAbstractInsnNode = paramInsnList.first;
      this.last.next = localAbstractInsnNode;
      localAbstractInsnNode.prev = this.last;
      this.last = paramInsnList.last;
    }
    this.cache = null;
    paramInsnList.removeAll(false);
  }
  
  public void insert(AbstractInsnNode paramAbstractInsnNode)
  {
    this.size += 1;
    if (this.first == null)
    {
      this.first = paramAbstractInsnNode;
      this.last = paramAbstractInsnNode;
    }
    else
    {
      this.first.prev = paramAbstractInsnNode;
      paramAbstractInsnNode.next = this.first;
    }
    this.first = paramAbstractInsnNode;
    this.cache = null;
    paramAbstractInsnNode.index = 0;
  }
  
  public void insert(InsnList paramInsnList)
  {
    if (paramInsnList.size == 0) {
      return;
    }
    this.size += paramInsnList.size;
    if (this.first == null)
    {
      this.first = paramInsnList.first;
      this.last = paramInsnList.last;
    }
    else
    {
      AbstractInsnNode localAbstractInsnNode = paramInsnList.last;
      this.first.prev = localAbstractInsnNode;
      localAbstractInsnNode.next = this.first;
      this.first = paramInsnList.first;
    }
    this.cache = null;
    paramInsnList.removeAll(false);
  }
  
  public void insert(AbstractInsnNode paramAbstractInsnNode1, AbstractInsnNode paramAbstractInsnNode2)
  {
    this.size += 1;
    AbstractInsnNode localAbstractInsnNode = paramAbstractInsnNode1.next;
    if (localAbstractInsnNode == null) {
      this.last = paramAbstractInsnNode2;
    } else {
      localAbstractInsnNode.prev = paramAbstractInsnNode2;
    }
    paramAbstractInsnNode1.next = paramAbstractInsnNode2;
    paramAbstractInsnNode2.next = localAbstractInsnNode;
    paramAbstractInsnNode2.prev = paramAbstractInsnNode1;
    this.cache = null;
    paramAbstractInsnNode2.index = 0;
  }
  
  public void insert(AbstractInsnNode paramAbstractInsnNode, InsnList paramInsnList)
  {
    if (paramInsnList.size == 0) {
      return;
    }
    this.size += paramInsnList.size;
    AbstractInsnNode localAbstractInsnNode1 = paramInsnList.first;
    AbstractInsnNode localAbstractInsnNode2 = paramInsnList.last;
    AbstractInsnNode localAbstractInsnNode3 = paramAbstractInsnNode.next;
    if (localAbstractInsnNode3 == null) {
      this.last = localAbstractInsnNode2;
    } else {
      localAbstractInsnNode3.prev = localAbstractInsnNode2;
    }
    paramAbstractInsnNode.next = localAbstractInsnNode1;
    localAbstractInsnNode2.next = localAbstractInsnNode3;
    localAbstractInsnNode1.prev = paramAbstractInsnNode;
    this.cache = null;
    paramInsnList.removeAll(false);
  }
  
  public void insertBefore(AbstractInsnNode paramAbstractInsnNode1, AbstractInsnNode paramAbstractInsnNode2)
  {
    this.size += 1;
    AbstractInsnNode localAbstractInsnNode = paramAbstractInsnNode1.prev;
    if (localAbstractInsnNode == null) {
      this.first = paramAbstractInsnNode2;
    } else {
      localAbstractInsnNode.next = paramAbstractInsnNode2;
    }
    paramAbstractInsnNode1.prev = paramAbstractInsnNode2;
    paramAbstractInsnNode2.next = paramAbstractInsnNode1;
    paramAbstractInsnNode2.prev = localAbstractInsnNode;
    this.cache = null;
    paramAbstractInsnNode2.index = 0;
  }
  
  public void insertBefore(AbstractInsnNode paramAbstractInsnNode, InsnList paramInsnList)
  {
    if (paramInsnList.size == 0) {
      return;
    }
    this.size += paramInsnList.size;
    AbstractInsnNode localAbstractInsnNode1 = paramInsnList.first;
    AbstractInsnNode localAbstractInsnNode2 = paramInsnList.last;
    AbstractInsnNode localAbstractInsnNode3 = paramAbstractInsnNode.prev;
    if (localAbstractInsnNode3 == null) {
      this.first = localAbstractInsnNode1;
    } else {
      localAbstractInsnNode3.next = localAbstractInsnNode1;
    }
    paramAbstractInsnNode.prev = localAbstractInsnNode2;
    localAbstractInsnNode2.next = paramAbstractInsnNode;
    localAbstractInsnNode1.prev = localAbstractInsnNode3;
    this.cache = null;
    paramInsnList.removeAll(false);
  }
  
  public void remove(AbstractInsnNode paramAbstractInsnNode)
  {
    this.size -= 1;
    AbstractInsnNode localAbstractInsnNode1 = paramAbstractInsnNode.next;
    AbstractInsnNode localAbstractInsnNode2 = paramAbstractInsnNode.prev;
    if (localAbstractInsnNode1 == null)
    {
      if (localAbstractInsnNode2 == null)
      {
        this.first = null;
        this.last = null;
      }
      else
      {
        localAbstractInsnNode2.next = null;
        this.last = localAbstractInsnNode2;
      }
    }
    else if (localAbstractInsnNode2 == null)
    {
      this.first = localAbstractInsnNode1;
      localAbstractInsnNode1.prev = null;
    }
    else
    {
      localAbstractInsnNode2.next = localAbstractInsnNode1;
      localAbstractInsnNode1.prev = localAbstractInsnNode2;
    }
    this.cache = null;
    paramAbstractInsnNode.index = -1;
    paramAbstractInsnNode.prev = null;
    paramAbstractInsnNode.next = null;
  }
  
  void removeAll(boolean paramBoolean)
  {
    if (paramBoolean)
    {
      AbstractInsnNode localAbstractInsnNode;
      for (Object localObject = this.first; localObject != null; localObject = localAbstractInsnNode)
      {
        localAbstractInsnNode = ((AbstractInsnNode)localObject).next;
        ((AbstractInsnNode)localObject).index = -1;
        ((AbstractInsnNode)localObject).prev = null;
        ((AbstractInsnNode)localObject).next = null;
      }
    }
    this.size = 0;
    this.first = null;
    this.last = null;
    this.cache = null;
  }
  
  public void clear()
  {
    removeAll(false);
  }
  
  public void resetLabels()
  {
    for (AbstractInsnNode localAbstractInsnNode = this.first; localAbstractInsnNode != null; localAbstractInsnNode = localAbstractInsnNode.next) {
      if ((localAbstractInsnNode instanceof LabelNode)) {
        ((LabelNode)localAbstractInsnNode).resetLabel();
      }
    }
  }
}
