package eu.the5zig.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringUtil
{
  public static List<String> differentWords(String s1, String s2)
  {
    List<String> result = new ArrayList();
    if ((s1 == null) && (s2 == null)) {
      return result;
    }
    if (s1 == null) {
      return Arrays.asList(s2.split(" "));
    }
    if (s2 == null) {
      return Arrays.asList(s1.split(" "));
    }
    List<String> words1 = Arrays.asList(s1.split(" "));
    List<String> words2 = Arrays.asList(s2.split(" "));
    result.addAll(words1);
    result.removeAll(words2);
    
    return result;
  }
}
