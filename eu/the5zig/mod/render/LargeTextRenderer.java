package eu.the5zig.mod.render;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;

public class LargeTextRenderer
{
  private Renderable renderable;
  
  public void render(String text, float scale, int y)
  {
    this.renderable = new Renderable(text, scale, y);
  }
  
  public void render(String text, float scale)
  {
    render(text, scale, The5zigMod.getVars().getScaledHeight() / 4);
  }
  
  public void render(String text)
  {
    render(text, 1.5F);
  }
  
  public void flush()
  {
    if (this.renderable == null) {
      return;
    }
    if (The5zigMod.getVars().isTablistShown()) {
      return;
    }
    float displayScale = this.renderable.scale * The5zigMod.getConfig().getFloat("scale");
    GLUtil.pushMatrix();
    GLUtil.translate((The5zigMod.getVars().getScaledWidth() - The5zigMod.getVars().getStringWidth(this.renderable.text) * displayScale) / 2.0F, this.renderable.y, 2.0F);
    GLUtil.scale(displayScale, displayScale, displayScale);
    The5zigMod.getVars().drawString(this.renderable.text, 0, 0);
    GLUtil.popMatrix();
    
    this.renderable = null;
  }
  
  private class Renderable
  {
    private String text;
    private float scale;
    private int y;
    
    public Renderable(String text, float scale, int y)
    {
      this.text = text;
      this.scale = scale;
      this.y = y;
    }
  }
}
