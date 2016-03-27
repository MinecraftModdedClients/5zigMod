package eu.the5zig.mod.util;

public abstract interface IGLUtil
{
  public abstract void enableBlend();
  
  public abstract void disableBlend();
  
  public abstract void scale(float paramFloat1, float paramFloat2, float paramFloat3);
  
  public abstract void translate(float paramFloat1, float paramFloat2, float paramFloat3);
  
  public abstract void color(float paramFloat1, float paramFloat2, float paramFloat3, float paramFloat4);
  
  public abstract void color(float paramFloat1, float paramFloat2, float paramFloat3);
  
  public abstract void pushMatrix();
  
  public abstract void popMatrix();
  
  public abstract void matrixMode(int paramInt);
  
  public abstract void loadIdentity();
  
  public abstract void clear(int paramInt);
  
  public abstract void disableDepth();
  
  public abstract void enableDepth();
  
  public abstract void depthMask(boolean paramBoolean);
  
  public abstract void disableLighting();
  
  public abstract void enableLighting();
  
  public abstract void disableFog();
  
  public abstract void tryBlendFuncSeparate(int paramInt1, int paramInt2, int paramInt3, int paramInt4);
  
  public abstract void disableAlpha();
}
