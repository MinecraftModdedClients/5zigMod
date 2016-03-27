import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import java.util.List;

public class Overlay
  extends bfl
  implements IOverlay
{
  private static final ResourceLocation texture = new ResourceLocation("textures/gui/achievement/achievement_background.png");
  private static Overlay[] activeOverlays;
  public long timeStarted;
  public int offset;
  private bcf mc;
  private String title;
  private String subtitle;
  private int width;
  private int height;
  private int index = -1;
  
  Overlay()
  {
    super(((Variables)MinecraftFactory.getVars()).getMinecraft());
    this.mc = ((Variables)MinecraftFactory.getVars()).getMinecraft();
  }
  
  public static void updateOverlayCount(int count)
  {
    activeOverlays = new Overlay[count];
  }
  
  public static void renderAll()
  {
    int activeOverlaysSize = activeOverlays.length;
    for (int i = activeOverlaysSize - 1; i >= 0; i--)
    {
      Overlay activeOverlay = activeOverlays[i];
      if (activeOverlay != null) {
        activeOverlay.render();
      }
    }
  }
  
  public void displayMessage(String title, String subtitle)
  {
    this.title = MinecraftFactory.getVars().shortenToWidth(title, 140);
    this.subtitle = MinecraftFactory.getVars().shortenToWidth(subtitle, 140);
    
    this.timeStarted = MinecraftFactory.getVars().getSystemTime();
    for (int i = 0; i < activeOverlays.length; i++) {
      if (activeOverlays[i] == null)
      {
        setOffset(i);
        break;
      }
    }
    if (this.index == -1) {
      setOffset(activeOverlays.length - 1);
    }
  }
  
  private void setOffset(int index)
  {
    this.index = index;
    activeOverlays[index] = this;
    this.offset = (index * 32);
  }
  
  public void displayMessage(String message)
  {
    displayMessage("The 5zig Mod", message);
  }
  
  public void displayMessageAndSplit(String message)
  {
    List<String> split = MinecraftFactory.getVars().splitStringToWidth(message, 140);
    String title = null;
    String subTitle = null;
    for (int i = 0; i < split.size(); i++)
    {
      if (i == 0) {
        title = (String)split.get(0);
      }
      if (i == 1) {
        subTitle = (String)split.get(1);
      }
    }
    displayMessage(title, subTitle);
  }
  
  private void updateScale()
  {
    bni.b(0, 0, this.mc.d, this.mc.e);
    GLUtil.matrixMode(5889);
    GLUtil.loadIdentity();
    GLUtil.matrixMode(5888);
    GLUtil.loadIdentity();
    this.width = this.mc.d;
    this.height = this.mc.e;
    bcx scaledResolution = new bcx(this.mc);
    this.width = scaledResolution.a();
    this.height = scaledResolution.b();
    GLUtil.clear(256);
    GLUtil.matrixMode(5889);
    GLUtil.loadIdentity();
    bni.a(0.0D, this.width, this.height, 0.0D, 1000.0D, 3000.0D);
    GLUtil.matrixMode(5888);
    GLUtil.loadIdentity();
    GLUtil.translate(0.0F, 0.0F, -2000.0F);
  }
  
  private void render()
  {
    if ((this.mc == null) || (this.timeStarted == 0L)) {
      return;
    }
    double delta = (MinecraftFactory.getVars().getSystemTime() - this.timeStarted) / 3000.0D;
    if ((delta < 0.0D) || (delta > 1.0D))
    {
      this.timeStarted = 0L;
      activeOverlays[this.index] = null;
      return;
    }
    updateScale();
    
    GLUtil.disableDepth();
    GLUtil.depthMask(false);
    
    delta *= 2.0D;
    if (delta > 1.0D) {
      delta = 2.0D - delta;
    }
    delta *= 4.0D;
    delta = 1.0D - delta;
    if (delta < 0.0D) {
      delta = 0.0D;
    }
    delta = Math.pow(delta, 3.0D);
    
    int x = this.width - 160;
    int y = this.offset - (int)(delta * 32.0D);
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
    bni.y();
    MinecraftFactory.getVars().bindTexture(texture);
    bni.g();
    
    b(x, y, 96, 202, 160, 32);
    if (this.title != null) {
      MinecraftFactory.getVars().drawString(this.title, x + 5, y + 7, 65280);
    }
    if (this.subtitle != null) {
      MinecraftFactory.getVars().drawString(this.subtitle, x + 5, y + 18, -1);
    }
    bcc.c();
    bni.g();
    bni.B();
    bni.g();
    
    bni.f();
    bni.g();
    
    GLUtil.depthMask(true);
    GLUtil.enableDepth();
  }
  
  public void b()
  {
    this.timeStarted = 0L;
    this.title = null;
    this.subtitle = null;
  }
}
