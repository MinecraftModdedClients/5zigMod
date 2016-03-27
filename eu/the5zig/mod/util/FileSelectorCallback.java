package eu.the5zig.mod.util;

import java.io.File;

public abstract interface FileSelectorCallback
{
  public abstract void onDone(File paramFile);
  
  public abstract String getTitle();
}
