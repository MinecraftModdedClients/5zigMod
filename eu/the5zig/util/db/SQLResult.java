package eu.the5zig.util.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SQLResult<T>
{
  private List<T> results = new ArrayList();
  
  public void add(T result)
  {
    this.results.add(result);
  }
  
  public T unique()
  {
    if (this.results.size() == 0) {
      return null;
    }
    return (T)this.results.get(0);
  }
  
  public List<T> getAll()
  {
    return Collections.unmodifiableList(this.results);
  }
}
