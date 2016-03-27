package eu.the5zig.util;

import java.util.ArrayList;

public class CaseInsensitiveArrayList<T>
  extends ArrayList<T>
{
  public boolean contains(Object o)
  {
    for (T t : this) {
      if (t.equals(o)) {
        return true;
      }
    }
    return false;
  }
}
