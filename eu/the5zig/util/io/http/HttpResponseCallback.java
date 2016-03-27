package eu.the5zig.util.io.http;

public abstract interface HttpResponseCallback
{
  public abstract void call(String paramString, int paramInt, Throwable paramThrowable);
}
