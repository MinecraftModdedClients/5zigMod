package org.objectweb.asm.commons;

class SerialVersionUIDAdder$Item
  implements Comparable
{
  final String name;
  final int access;
  final String desc;
  
  SerialVersionUIDAdder$Item(String paramString1, int paramInt, String paramString2)
  {
    this.name = paramString1;
    this.access = paramInt;
    this.desc = paramString2;
  }
  
  public int compareTo(Item paramItem)
  {
    int i = this.name.compareTo(paramItem.name);
    if (i == 0) {
      i = this.desc.compareTo(paramItem.desc);
    }
    return i;
  }
  
  public boolean equals(Object paramObject)
  {
    if ((paramObject instanceof Item)) {
      return compareTo((Item)paramObject) == 0;
    }
    return false;
  }
  
  public int hashCode()
  {
    return (this.name + this.desc).hashCode();
  }
}
