package eu.the5zig.mod.gui.elements;

import java.io.File;

public abstract interface IFileSelector
{
  public abstract File getCurrentDir();
  
  public abstract File getSelectedFile();
  
  public abstract void updateDir(File paramFile);
  
  public abstract void goUp();
  
  public abstract void draw(int paramInt1, int paramInt2, float paramFloat);
  
  public abstract void handleMouseInput();
}
