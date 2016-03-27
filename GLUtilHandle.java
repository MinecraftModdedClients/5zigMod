import eu.the5zig.mod.util.IGLUtil;

public class GLUtilHandle
  implements IGLUtil
{
  public void enableBlend() {}
  
  public void disableBlend() {}
  
  public void scale(float x, float y, float z)
  {
    bni.b(x, y, z);
  }
  
  public void translate(float x, float y, float z)
  {
    bni.c(x, y, z);
  }
  
  public void color(float r, float g, float b, float a)
  {
    bni.c(r, g, b, a);
  }
  
  public void color(float r, float g, float b)
  {
    color(r, g, b, 1.0F);
  }
  
  public void pushMatrix() {}
  
  public void popMatrix() {}
  
  public void matrixMode(int mode)
  {
    bni.n(mode);
  }
  
  public void loadIdentity() {}
  
  public void clear(int i)
  {
    bni.m(i);
  }
  
  public void disableDepth() {}
  
  public void enableDepth() {}
  
  public void depthMask(boolean b)
  {
    bni.a(b);
  }
  
  public void disableLighting() {}
  
  public void enableLighting() {}
  
  public void disableFog() {}
  
  public void tryBlendFuncSeparate(int i, int i1, int i2, int i3)
  {
    bni.a(i, i1, i2, i3);
  }
  
  public void disableAlpha() {}
}
