import com.google.common.collect.Lists;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.elements.IFileSelector;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import java.io.File;
import java.util.List;
import javax.swing.filechooser.FileSystemView;
import org.lwjgl.input.Mouse;

public class FileSelector
  implements IFileSelector
{
  private final FileSystemView fsv = FileSystemView.getFileSystemView();
  private final Callback<File> callback;
  private File currentDir;
  private int selectedFile;
  private List<File> files = Lists.newArrayList();
  private int width;
  private int height;
  private int left;
  private int right;
  private int top;
  private int bottom;
  private int columnCount;
  private int mouseX;
  private int mouseY;
  private int selectionBoxWidth = 100;
  private float amountScrolled;
  private float initialClickY = -1.0F;
  private float scrollMultiplier = -1.0F;
  private long lastClicked;
  
  public FileSelector(File currentDir, int width, int height, int left, int right, int top, int bottom, Callback<File> callback)
  {
    updateDir(currentDir);
    this.width = width;
    this.height = height;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.callback = callback;
    
    this.columnCount = ((int)Math.floor((right - left) / this.selectionBoxWidth));
  }
  
  public void goUp()
  {
    if (this.currentDir == null) {
      return;
    }
    File parent = this.currentDir.getParentFile();
    if (parent == null)
    {
      this.currentDir = null;
      this.files.clear();
      File[] a = File.listRoots();
      if (a == null) {
        return;
      }
      for (File file : a) {
        if ((this.fsv.isDrive(file)) && (!this.fsv.getSystemDisplayName(file).isEmpty())) {
          this.files.add(file);
        }
      }
      this.selectedFile = (this.files.isEmpty() ? -1 : 0);
      return;
    }
    updateDir(parent);
  }
  
  public void updateDir(File dir)
  {
    this.currentDir = dir;
    this.files.clear();
    if (dir == null)
    {
      File[] a = File.listRoots();
      if (a == null) {
        return;
      }
      for (File file : a) {
        if ((this.fsv.isDrive(file)) && (!this.fsv.getSystemDisplayName(file).isEmpty())) {
          this.files.add(file);
        }
      }
      this.selectedFile = (this.files.isEmpty() ? -1 : 0);
      return;
    }
    File[] a = dir.listFiles();
    if (a == null) {
      return;
    }
    for (File file : a) {
      if (!file.isHidden()) {
        this.files.add(file);
      }
    }
    this.selectedFile = (this.files.isEmpty() ? -1 : 0);
  }
  
  public void draw(int mouseX, int mouseY, float partialTicks)
  {
    this.mouseX = mouseX;
    this.mouseY = mouseY;
    
    int scrollX0 = getScrollBarX();
    int scrollX1 = scrollX0 + 6;
    bindAmountScrolled();
    GLUtil.disableLighting();
    GLUtil.disableFog();
    bnu tessellator = bnu.a();
    bmz worldRenderer = tessellator.c();
    MinecraftFactory.getVars().bindTexture(bcu.b);
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
    float var7 = 32.0F;
    
    worldRenderer.a(7, bvp.i);
    worldRenderer.b(this.left, this.bottom, 0.0D).a(this.left / var7, (this.bottom + (int)this.amountScrolled) / var7).b(32, 32, 32, 255).d();
    worldRenderer.b(this.right, this.bottom, 0.0D).a(this.right / var7, (this.bottom + (int)this.amountScrolled) / var7).b(32, 32, 32, 255).d();
    worldRenderer.b(this.right, this.top, 0.0D).a(this.right / var7, (this.top + (int)this.amountScrolled) / var7).b(32, 32, 32, 255).d();
    worldRenderer.b(this.left, this.top, 0.0D).a(this.left / var7, (this.top + (int)this.amountScrolled) / var7).b(32, 32, 32, 255).d();
    tessellator.b();
    
    int selectionX0 = this.left;
    int selectionY0 = this.top + 4 - (int)this.amountScrolled;
    drawSelectionBox(selectionX0, selectionY0, mouseX, mouseY);
    GLUtil.disableDepth();
    byte var10 = 4;
    overlayBackground(0, this.top, 255, 255);
    overlayBackground(this.bottom, this.height, 255, 255);
    bni.m();
    GLUtil.tryBlendFuncSeparate(770, 771, 0, 1);
    bni.d();
    bni.j(7425);
    bni.z();
    
    worldRenderer.a(7, bvp.i);
    worldRenderer.b(this.left, this.top + var10, 0.0D).a(0.0D, 1.0D).b(0, 0, 0, 0).d();
    worldRenderer.b(this.right, this.top + var10, 0.0D).a(1.0D, 1.0D).b(0, 0, 0, 0).d();
    worldRenderer.b(this.right, this.top, 0.0D).a(1.0D, 0.0D).b(0, 0, 0, 255).d();
    worldRenderer.b(this.left, this.top, 0.0D).a(0.0D, 0.0D).b(0, 0, 0, 255).d();
    tessellator.b();
    worldRenderer.a(7, bvp.i);
    worldRenderer.b(this.left, this.bottom, 0.0D).a(0.0D, 1.0D).b(0, 0, 0, 255).d();
    worldRenderer.b(this.right, this.bottom, 0.0D).a(1.0D, 1.0D).b(0, 0, 0, 255).d();
    worldRenderer.b(this.right, this.bottom - var10, 0.0D).a(1.0D, 0.0D).b(0, 0, 0, 0).d();
    worldRenderer.b(this.left, this.bottom - var10, 0.0D).a(0.0D, 0.0D).b(0, 0, 0, 0).d();
    tessellator.b();
    int var11 = m();
    if (var11 > 0)
    {
      int var12 = (this.bottom - this.top) * (this.bottom - this.top) / getContentHeight();
      var12 = on.a(var12, 32, this.bottom - this.top - 8);
      int var13 = (int)this.amountScrolled * (this.bottom - this.top - var12) / var11 + this.top;
      if (var13 < this.top) {
        var13 = this.top;
      }
      worldRenderer.a(7, bvp.i);
      worldRenderer.b(scrollX0, this.bottom, 0.0D).a(0.0D, 1.0D).b(0, 0, 0, 255).d();
      worldRenderer.b(scrollX1, this.bottom, 0.0D).a(1.0D, 1.0D).b(0, 0, 0, 255).d();
      worldRenderer.b(scrollX1, this.top, 0.0D).a(1.0D, 0.0D).b(0, 0, 0, 255).d();
      worldRenderer.b(scrollX0, this.top, 0.0D).a(0.0D, 0.0D).b(0, 0, 0, 255).d();
      tessellator.b();
      worldRenderer.a(7, bvp.i);
      worldRenderer.b(scrollX0, var13 + var12, 0.0D).a(0.0D, 1.0D).b(128, 128, 128, 255).d();
      worldRenderer.b(scrollX1, var13 + var12, 0.0D).a(1.0D, 1.0D).b(128, 128, 128, 255).d();
      worldRenderer.b(scrollX1, var13, 0.0D).a(1.0D, 0.0D).b(128, 128, 128, 255).d();
      worldRenderer.b(scrollX0, var13, 0.0D).a(0.0D, 0.0D).b(128, 128, 128, 255).d();
      tessellator.b();
      worldRenderer.a(7, bvp.i);
      worldRenderer.b(scrollX0, var13 + var12 - 1, 0.0D).a(0.0D, 1.0D).b(192, 192, 192, 255).d();
      worldRenderer.b(scrollX1 - 1, var13 + var12 - 1, 0.0D).a(1.0D, 1.0D).b(192, 192, 192, 255).d();
      worldRenderer.b(scrollX1 - 1, var13, 0.0D).a(1.0D, 0.0D).b(192, 192, 192, 255).d();
      worldRenderer.b(scrollX0, var13, 0.0D).a(0.0D, 0.0D).b(192, 192, 192, 255).d();
      tessellator.b();
    }
    bni.y();
    bni.j(7424);
    bni.e();
    GLUtil.disableBlend();
    
    int x = mouseX - this.left;
    int y = mouseY - this.top + (int)this.amountScrolled - 4;
    int idX = x / this.selectionBoxWidth;
    int idY = y / getSlotHeight();
    int id = idX + idY * this.columnCount;
    if ((id < this.files.size()) && (id >= 0) && (mouseX >= 0) && (mouseY >= 0) && (idX < this.columnCount)) {
      elementHovered(id, mouseX, mouseY);
    }
  }
  
  protected void drawSelectionBox(int x, int y, int mouseX, int mouseY)
  {
    int rows = (int)Math.ceil(this.files.size() / this.columnCount);
    int columns = this.columnCount;
    bnu tessellator = bnu.a();
    bmz worldRenderer = tessellator.c();
    for (int i = 0; i < rows; i++)
    {
      int ny = y + i * getSlotHeight();
      int padding = getSlotHeight() - 4;
      for (int j = 0; j < this.columnCount; j++)
      {
        int nx = x + j * this.selectionBoxWidth;
        int nx1 = nx + this.selectionBoxWidth;
        
        int id = j + i * columns;
        if (isSelected(id))
        {
          GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
          bni.z();
          worldRenderer.a(7, bvp.i);
          worldRenderer.b(nx, ny + padding + 2, 0.0D).a(0.0D, 1.0D).b(128, 128, 128, 255).d();
          worldRenderer.b(nx1, ny + padding + 2, 0.0D).a(1.0D, 1.0D).b(128, 128, 128, 255).d();
          worldRenderer.b(nx1, ny - 2, 0.0D).a(1.0D, 0.0D).b(128, 128, 128, 255).d();
          worldRenderer.b(nx, ny - 2, 0.0D).a(0.0D, 0.0D).b(128, 128, 128, 255).d();
          worldRenderer.b(nx + 1, ny + padding + 1, 0.0D).a(0.0D, 1.0D).b(0, 0, 0, 255).d();
          worldRenderer.b(nx1 - 1, ny + padding + 1, 0.0D).a(1.0D, 1.0D).b(0, 0, 0, 255).d();
          worldRenderer.b(nx1 - 1, ny - 1, 0.0D).a(1.0D, 0.0D).b(0, 0, 0, 255).d();
          worldRenderer.b(nx + 1, ny - 1, 0.0D).a(0.0D, 0.0D).b(0, 0, 0, 255).d();
          tessellator.b();
          bni.y();
        }
        drawSlot(id, nx, ny, padding, mouseX, mouseY);
      }
    }
  }
  
  protected void drawSlot(int id, int x, int y, int padding, int mouseX, int mouseY)
  {
    File file = getFile(id);
    if (file == null) {
      return;
    }
    String name = file.getName();
    if (this.currentDir == null) {
      name = this.fsv.getSystemDisplayName(file);
    }
    MinecraftFactory.getVars().drawString(MinecraftFactory.getVars().shortenToWidth(name, this.selectionBoxWidth - 18), x + 2, y + 2);
  }
  
  public boolean isSelected(int id)
  {
    return this.selectedFile == id;
  }
  
  public File getFile(int i)
  {
    if ((i < 0) || (i >= this.files.size())) {
      return null;
    }
    return (File)this.files.get(i);
  }
  
  public File getSelectedFile()
  {
    return getFile(this.selectedFile);
  }
  
  protected void overlayBackground(int var, int var1, int var2, int var3)
  {
    bnu var4 = bnu.a();
    bmz var5 = var4.c();
    MinecraftFactory.getVars().bindTexture(bcu.b);
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
    var5.a(7, bvp.i);
    var5.b(this.left, var1, 0.0D).a(0.0D, var1 / 32.0F).b(64, 64, 64, var3).d();
    var5.b(this.left + (this.right - this.left), var1, 0.0D).a((this.right - this.left) / 32.0F, var1 / 32.0F).b(64, 64, 64, var3).d();
    var5.b(this.left + (this.right - this.left), var, 0.0D).a((this.right - this.left) / 32.0F, var / 32.0F).b(64, 64, 64, var2).d();
    var5.b(this.left, var, 0.0D).a(0.0D, var / 32.0F).b(64, 64, 64, var2).d();
    var4.b();
  }
  
  public int getContentHeight()
  {
    return (int)Math.ceil(this.files.size() / this.columnCount) * getSlotHeight();
  }
  
  public int getSlotHeight()
  {
    return 18;
  }
  
  protected void bindAmountScrolled()
  {
    this.amountScrolled = on.a(this.amountScrolled, 0.0F, m());
  }
  
  public int m()
  {
    return Math.max(0, getContentHeight() - (this.bottom - this.top - 4));
  }
  
  public boolean isMouseWithinSlotBounds()
  {
    return (this.mouseY >= this.top) && (this.mouseY <= this.bottom) && (this.mouseX >= this.left) && (this.mouseX <= this.right);
  }
  
  public int getScrollBarX()
  {
    return this.right - 10;
  }
  
  public void handleMouseInput()
  {
    if (!isMouseWithinSlotBounds()) {
      return;
    }
    if ((Mouse.getEventButton() == 0) && (Mouse.getEventButtonState()) && (this.mouseY >= this.top) && (this.mouseY <= this.bottom))
    {
      int x = this.mouseX - this.left;
      int y = this.mouseY - this.top + (int)this.amountScrolled - 4;
      int idY = y / getSlotHeight();
      int idX = x / this.selectionBoxWidth;
      int id = idX + idY * this.columnCount;
      if ((id < this.files.size()) && (id >= 0) && (x >= 0) && (y >= 0) && (idX < this.columnCount))
      {
        this.selectedFile = id;
        elementClicked(id, false, this.mouseX, this.mouseY);
      }
      else if (id >= 0) {}
    }
    if (Mouse.isButtonDown(0))
    {
      if (this.initialClickY == -1.0F)
      {
        boolean var9 = true;
        if ((this.mouseY >= this.top) && (this.mouseY <= this.bottom))
        {
          int x = this.mouseX - this.left;
          int y = this.mouseY - this.top + (int)this.amountScrolled - 4;
          int idY = y / getSlotHeight();
          int idX = x / this.selectionBoxWidth;
          int id = idX + idY * this.columnCount;
          if ((id < this.files.size()) && (id >= 0) && (x >= 0) && (y >= 0) && (idX < this.columnCount))
          {
            boolean doubleClick = (id == this.selectedFile) && (MinecraftFactory.getVars().getSystemTime() - this.lastClicked < 250L);
            this.selectedFile = id;
            elementClicked(id, doubleClick, this.mouseX, this.mouseY);
            this.lastClicked = MinecraftFactory.getVars().getSystemTime();
          }
          else if (id < 0)
          {
            var9 = false;
          }
          int scrollBarX0 = getScrollBarX();
          int scrollBarX1 = scrollBarX0 + 6;
          if ((this.mouseX >= scrollBarX0) && (this.mouseX <= scrollBarX1))
          {
            this.scrollMultiplier = -1.0F;
            int var7 = m();
            if (var7 < 1) {
              var7 = 1;
            }
            int var8 = (int)((this.bottom - this.top) * (this.bottom - this.top) / getContentHeight());
            var8 = on.a(var8, 32, this.bottom - this.top - 8);
            this.scrollMultiplier /= (this.bottom - this.top - var8) / var7;
          }
          else
          {
            this.scrollMultiplier = 1.0F;
          }
          if (var9) {
            this.initialClickY = this.mouseY;
          } else {
            this.initialClickY = -2.0F;
          }
        }
        else
        {
          this.initialClickY = -2.0F;
        }
      }
      else if (this.initialClickY >= 0.0F)
      {
        this.amountScrolled -= (this.mouseY - this.initialClickY) * this.scrollMultiplier;
        this.initialClickY = this.mouseY;
      }
    }
    else {
      this.initialClickY = -1.0F;
    }
    int var = Mouse.getEventDWheel();
    if (var != 0)
    {
      if (var > 0) {
        var = -1;
      } else if (var < 0) {
        var = 1;
      }
      this.amountScrolled += var * getSlotHeight() / 2;
    }
  }
  
  protected void elementClicked(int id, boolean doubleClick, int mouseX, int mouseY)
  {
    if ((doubleClick) && (getSelectedFile() != null) && (getSelectedFile().isDirectory())) {
      updateDir(getSelectedFile());
    } else if ((doubleClick) && (getSelectedFile() != null)) {
      this.callback.call(getSelectedFile());
    }
  }
  
  protected void elementHovered(int id, int mouseX, int mouseY)
  {
    File file = getFile(id);
    if (file == null) {
      return;
    }
    String name = file.getName();
    if (this.currentDir == null) {
      name = this.fsv.getSystemDisplayName(file);
    }
    if (MinecraftFactory.getVars().getStringWidth(name) > this.selectionBoxWidth - 18 - MinecraftFactory.getVars().getStringWidth("..."))
    {
      MinecraftFactory.getVars().getCurrentScreen().drawHoveringText(MinecraftFactory.getVars().splitStringToWidth(name, 200), mouseX, mouseY);
      GLUtil.disableLighting();
    }
  }
  
  public File getCurrentDir()
  {
    return this.currentDir;
  }
}
