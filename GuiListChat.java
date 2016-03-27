import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.GuiListChatCallback;
import eu.the5zig.mod.util.IVariables;
import java.util.List;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class GuiListChat<E extends Row>
  extends GuiList<E>
{
  private final GuiListChatCallback callback;
  private int w;
  private int h;
  
  public GuiListChat(int width, int height, int top, int bottom, int left, int right, int scrollx, List<E> rows, GuiListChatCallback callback)
  {
    super(null, width, height, top, bottom, left, right, rows);
    this.callback = callback;
    setBottomPadding(6);
    setLeftbound(true);
    setDrawSelection(false);
    setScrollX(scrollx);
    if (callback.getResourceLocation() != null)
    {
      double w = callback.getImageWidth();
      double h = callback.getImageHeight();
      int listWidth = getRight() - getLeft();
      int listHeight = getBottom() - getTop();
      while ((w > listWidth) && (h > listHeight))
      {
        w -= 1.0D;
        h -= h / w;
      }
      while ((w < listWidth) || (h < listHeight))
      {
        w += 1.0D;
        h += h / w;
      }
      this.w = ((int)w);
      this.h = ((int)h);
    }
  }
  
  public int getRowWidth()
  {
    return getRight() - getLeft() - 5;
  }
  
  public void a(int mouseX, int mouseY, float partialTicks)
  {
    if (this.q)
    {
      this.i = mouseX;
      this.j = mouseY;
      a();
      int var3 = d();
      int var4 = var3 + 6;
      l();
      bni.g();
      bni.p();
      bnu tesselator = bnu.a();
      bmz worldRenderer = tesselator.c();
      bni.c(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.callback.drawDefaultBackground())
      {
        MinecraftFactory.getVars().bindTexture(bcu.b);
        float var7 = 32.0F;
        worldRenderer.a(7, bvp.i);
        worldRenderer.b(this.g, this.e, 0.0D).a(this.g / var7, (this.e + (int)this.n) / var7).b(32, 32, 32, 255).d();
        worldRenderer.b(this.f, this.e, 0.0D).a(this.f / var7, (this.e + (int)this.n) / var7).b(32, 32, 32, 255).d();
        worldRenderer.b(this.f, this.d, 0.0D).a(this.f / var7, (this.d + (int)this.n) / var7).b(32, 32, 32, 255).d();
        worldRenderer.b(this.g, this.d, 0.0D).a(this.g / var7, (this.d + (int)this.n) / var7).b(32, 32, 32, 255).d();
        tesselator.b();
      }
      else if (this.callback.getResourceLocation() != null)
      {
        MinecraftFactory.getVars().bindTexture(this.callback.getResourceLocation());
        Gui.drawModalRectWithCustomSizedTexture(getLeft(), getTop(), 0.0F, 0.0F, getRight() - getLeft(), getBottom() - getTop(), this.w, this.h);
      }
      int var8 = this.g + this.b / 2 - c() / 2 + 2;
      int var9 = this.d + 4 - (int)this.n;
      if (this.s) {
        a(var8, var9, tesselator);
      }
      b(var8, var9, mouseX, mouseY);
      bni.j();
      byte var10 = 4;
      c(0, this.d, 255, 255);
      
      drawOverlay(100, 10, 0, getBottom());
      drawOverlay(getRight(), 10, 0, getBottom());
      
      c(this.e, this.c, 255, 255);
      bni.m();
      bni.a(770, 771, 0, 1);
      bni.d();
      bni.j(7425);
      bni.z();
      
      worldRenderer.a(7, bvp.i);
      worldRenderer.b(this.g, this.d + var10, 0.0D).a(0.0D, 1.0D).b(0, 0, 0, 0).d();
      worldRenderer.b(this.f, this.d + var10, 0.0D).a(1.0D, 1.0D).b(0, 0, 0, 0).d();
      worldRenderer.b(this.f, this.d, 0.0D).a(1.0D, 0.0D).b(0, 0, 0, 255).d();
      worldRenderer.b(this.g, this.d, 0.0D).a(0.0D, 0.0D).b(0, 0, 0, 255).d();
      tesselator.b();
      worldRenderer.a(7, bvp.i);
      worldRenderer.b(this.g, this.e, 0.0D).a(0.0D, 1.0D).b(0, 0, 0, 255).d();
      worldRenderer.b(this.f, this.e, 0.0D).a(1.0D, 1.0D).b(0, 0, 0, 255).d();
      worldRenderer.b(this.f, this.e - var10, 0.0D).a(1.0D, 0.0D).b(0, 0, 0, 0).d();
      worldRenderer.b(this.g, this.e - var10, 0.0D).a(0.0D, 0.0D).b(0, 0, 0, 0).d();
      tesselator.b();
      int var11 = m();
      if (var11 > 0)
      {
        int var12 = (this.e - this.d) * (this.e - this.d) / k();
        var12 = on.a(var12, 32, this.e - this.d - 8);
        int var13 = (int)this.n * (this.e - this.d - var12) / var11 + this.d;
        if (var13 < this.d) {
          var13 = this.d;
        }
        worldRenderer.a(7, bvp.i);
        worldRenderer.b(var3, this.e, 0.0D).a(0.0D, 1.0D).b(0, 0, 0, 255).d();
        worldRenderer.b(var4, this.e, 0.0D).a(1.0D, 1.0D).b(0, 0, 0, 255).d();
        worldRenderer.b(var4, this.d, 0.0D).a(1.0D, 0.0D).b(0, 0, 0, 255).d();
        worldRenderer.b(var3, this.d, 0.0D).a(0.0D, 0.0D).b(0, 0, 0, 255).d();
        tesselator.b();
        worldRenderer.a(7, bvp.i);
        worldRenderer.b(var3, var13 + var12, 0.0D).a(0.0D, 1.0D).b(128, 128, 128, 255).d();
        worldRenderer.b(var4, var13 + var12, 0.0D).a(1.0D, 1.0D).b(128, 128, 128, 255).d();
        worldRenderer.b(var4, var13, 0.0D).a(1.0D, 0.0D).b(128, 128, 128, 255).d();
        worldRenderer.b(var3, var13, 0.0D).a(0.0D, 0.0D).b(128, 128, 128, 255).d();
        tesselator.b();
        worldRenderer.a(7, bvp.i);
        worldRenderer.b(var3, var13 + var12 - 1, 0.0D).a(0.0D, 1.0D).b(192, 192, 192, 255).d();
        worldRenderer.b(var4 - 1, var13 + var12 - 1, 0.0D).a(1.0D, 1.0D).b(192, 192, 192, 255).d();
        worldRenderer.b(var4 - 1, var13, 0.0D).a(1.0D, 0.0D).b(192, 192, 192, 255).d();
        worldRenderer.b(var3, var13, 0.0D).a(0.0D, 0.0D).b(192, 192, 192, 255).d();
        tesselator.b();
      }
      b(mouseX, mouseY);
      bni.y();
      bni.j(7424);
      bni.e();
      bni.j();
    }
  }
  
  protected void drawOverlay(int x0, int width, int y0, int height)
  {
    bnu tesselator = bnu.a();
    bmz worldRenderer = tesselator.c();
    MinecraftFactory.getVars().bindTexture(bcu.b);
    bni.c(1.0F, 1.0F, 1.0F, 1.0F);
    worldRenderer.a(7, bvp.i);
    worldRenderer.b(x0, height, 0.0D).a(0.0D, height / 32.0F).b(64, 64, 64, 255).d();
    worldRenderer.b(x0 + width, height, 0.0D).a(width / 32.0F, height / 32.0F).b(64, 64, 64, 255).d();
    worldRenderer.b(x0 + width, y0, 0.0D).a(width / 32.0F, y0 / 32.0F).b(64, 64, 64, 255).d();
    worldRenderer.b(x0, y0, 0.0D).a(0.0D, y0 / 32.0F).b(64, 64, 64, 255).d();
    tesselator.b();
  }
  
  public void handleMouseInput()
  {
    super.handleMouseInput();
    if (!Display.isActive()) {
      return;
    }
    int mouseX = this.i;
    int mouseY = this.j;
    if ((g(mouseY)) && 
      (Mouse.isButtonDown(0)) && (q()))
    {
      int y = mouseY - this.d - this.t + (int)this.n - 4;
      int id = -1;
      int minY = -1;
      for (int i1 = 0; i1 < this.heightMap.size(); i1++)
      {
        Integer integer = (Integer)this.heightMap.get(i1);
        Row line = (Row)this.rows.get(i1);
        if ((y >= integer.intValue() - 2) && (y <= integer.intValue() + line.getLineHeight() - 2))
        {
          id = i1;
          minY = integer.intValue();
          break;
        }
      }
      if ((id < 0) || (id >= this.rows.size())) {
        return;
      }
      this.callback.chatLineClicked((Row)this.rows.get(id), mouseX, y, minY, getLeft());
    }
  }
}
