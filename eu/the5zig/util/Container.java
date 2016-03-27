package eu.the5zig.util;

public class Container<V>
{
  private V value;
  
  public Container(V value)
  {
    this.value = value;
  }
  
  public Container() {}
  
  public void setValue(V value)
  {
    this.value = value;
  }
  
  public V getValue()
  {
    return (V)this.value;
  }
  
  public String toString()
  {
    return String.valueOf(getValue());
  }
}
