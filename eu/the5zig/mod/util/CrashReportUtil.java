package eu.the5zig.mod.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class CrashReportUtil
{
  private static final Method createCrashReport;
  private static final Method createCrashCategory;
  private static final Constructor<?> reportedException;
  
  static
  {
    try
    {
      Class<?> crashReportClass = Class.forName("b");
      createCrashReport = crashReportClass.getMethod("a", new Class[] { Throwable.class, String.class });
      createCrashCategory = crashReportClass.getMethod("a", new Class[] { String.class });
      reportedException = Class.forName("e").getConstructor(new Class[] { crashReportClass });
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public static void makeCrashReport(Throwable throwable, String reason)
  {
    Object crashReport = ReflectionUtil.invoke(createCrashReport, new Object[] { throwable, reason });
    ReflectionUtil.invoke(crashReport, createCrashCategory, new Object[] { "The 5zig Mod" });
    throw ((RuntimeException)ReflectionUtil.newInstance(reportedException, new Object[] { crashReport }));
  }
}
