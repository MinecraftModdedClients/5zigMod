package org.objectweb.asm.tree;

import java.util.ListIterator;
import java.util.NoSuchElementException;

final class InsnList$InsnListIterator
  implements ListIterator
{
  AbstractInsnNode next;
  AbstractInsnNode prev;
  AbstractInsnNode remove;
  final InsnList this$0;
  
  InsnList$InsnListIterator(InsnList paramInsnList, int paramInt)
  {
    if (paramInt == paramInsnList.size())
    {
      this.next = null;
      this.prev = paramInsnList.getLast();
    }
    else
    {
      this.next = paramInsnList.get(paramInt);
      this.prev = this.next.prev;
    }
  }
  
  public boolean hasNext()
  {
    return this.next != null;
  }
  
  public Object next()
  {
    if (this.next == null) {
      throw new NoSuchElementException();
    }
    AbstractInsnNode localAbstractInsnNode = this.next;
    this.prev = localAbstractInsnNode;
    this.next = localAbstractInsnNode.next;
    this.remove = localAbstractInsnNode;
    return localAbstractInsnNode;
  }
  
  public void remove()
  {
    if (this.remove != null)
    {
      if (this.remove == this.next) {
        this.next = this.next.next;
      } else {
        this.prev = this.prev.prev;
      }
      this.this$0.remove(this.remove);
      this.remove = null;
    }
    else
    {
      throw new IllegalStateException();
    }
  }
  
  public boolean hasPrevious()
  {
    return this.prev != null;
  }
  
  public Object previous()
  {
    AbstractInsnNode localAbstractInsnNode = this.prev;
    this.next = localAbstractInsnNode;
    this.prev = localAbstractInsnNode.prev;
    this.remove = localAbstractInsnNode;
    return localAbstractInsnNode;
  }
  
  public int nextIndex()
  {
    if (this.next == null) {
      return this.this$0.size();
    }
    if (this.this$0.cache == null) {
      this.this$0.cache = this.this$0.toArray();
    }
    return this.next.index;
  }
  
  public int previousIndex()
  {
    if (this.prev == null) {
      return -1;
    }
    if (this.this$0.cache == null) {
      this.this$0.cache = this.this$0.toArray();
    }
    return this.prev.index;
  }
  
  public void add(Object paramObject)
  {
    this.this$0.insertBefore(this.next, (AbstractInsnNode)paramObject);
    this.prev = ((AbstractInsnNode)paramObject);
    this.remove = null;
  }
  
  public void set(Object paramObject)
  {
    this.this$0.set(this.next.prev, (AbstractInsnNode)paramObject);
    this.prev = ((AbstractInsnNode)paramObject);
  }
}
