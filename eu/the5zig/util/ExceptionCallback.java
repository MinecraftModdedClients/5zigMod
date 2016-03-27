package eu.the5zig.util;

public abstract interface ExceptionCallback<T>
{
  public abstract void call(T paramT, Throwable paramThrowable);
}
