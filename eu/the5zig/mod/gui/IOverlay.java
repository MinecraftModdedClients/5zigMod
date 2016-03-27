package eu.the5zig.mod.gui;

public abstract interface IOverlay
{
  public abstract void displayMessage(String paramString1, String paramString2);
  
  public abstract void displayMessage(String paramString);
  
  public abstract void displayMessageAndSplit(String paramString);
}
