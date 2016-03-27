import com.google.common.collect.Lists;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.gui.elements.RowExtended;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;
import org.lwjgl.input.Mouse;

public class GuiList<E extends Row>
  extends bdq
  implements IGuiList<E>
{
  protected final List<E> rows;
  private final Clickable<E> clickable;
  private int rowWidth = 95;
  private int bottomPadding;
  private boolean leftbound = false;
  private int scrollX;
  private int selected;
  private IButton selectedButton;
  private String header;
  protected List<Integer> heightMap = Lists.newArrayList();
  
  public GuiList(Clickable<E> clickable, int width, int height, int top, int bottom, int left, int right, List<E> rows)
  {
    this(clickable, width, height, top, bottom, left, right, rows, 18);
  }
  
  @Deprecated
  public GuiList(Clickable<E> clickable, int width, int height, int top, int bottom, int left, int right, List<E> rows, int padding)
  {
    super(((Variables)MinecraftFactory.getVars()).getMinecraft(), width, height, top, bottom, padding);
    
    this.rows = rows;
    this.clickable = clickable;
    setLeft(left);
    setRight(right);
  }
  
  /* Error */
  protected int b()
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 10	GuiList:rows	Ljava/util/List;
    //   4: dup
    //   5: astore_1
    //   6: monitorenter
    //   7: aload_0
    //   8: getfield 10	GuiList:rows	Ljava/util/List;
    //   11: invokeinterface 14 1 0
    //   16: aload_1
    //   17: monitorexit
    //   18: ireturn
    //   19: astore_2
    //   20: aload_1
    //   21: monitorexit
    //   22: aload_2
    //   23: athrow
    // Line number table:
    //   Java source line #46	-> byte code offset #0
    //   Java source line #47	-> byte code offset #7
    //   Java source line #48	-> byte code offset #19
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	24	0	this	GuiList<E>
    //   5	16	1	Ljava/lang/Object;	Object
    //   19	4	2	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   7	18	19	finally
    //   19	22	19	finally
  }
  
  protected int k()
  {
    return getContentHeight();
  }
  
  protected void a(int id, boolean doubleClick, int mouseX, int mouseY)
  {
    this.selected = id;
    boolean var5 = (this.selected >= 0) && (this.selected < b());
    if ((var5) && 
      (this.clickable != null)) {
      synchronized (this.rows)
      {
        onSelect(id, (Row)this.rows.get(id), doubleClick);
      }
    }
  }
  
  public void onSelect(int id, E row, boolean doubleClick)
  {
    setSelectedId(id);
    if ((this.clickable != null) && (row != null)) {
      this.clickable.onSelect(id, row, doubleClick);
    }
  }
  
  protected boolean a(int id)
  {
    return isSelected(id);
  }
  
  public boolean isSelected(int id)
  {
    return this.selected == id;
  }
  
  protected void a() {}
  
  public int c()
  {
    return getRowWidth();
  }
  
  protected void a(int id, int x, int y, int slotHeight, int mouseX, int mouseY)
  {
    drawSlot(id, x, y, slotHeight, mouseX, mouseY);
  }
  
  protected void drawSlot(int id, int x, int y, int slotHeight, int mouseX, int mouseY)
  {
    synchronized (this.rows)
    {
      if ((id < 0) || (id >= this.rows.size())) {
        return;
      }
      Row selectedRow = (Row)this.rows.get(id);
      selectedRow.draw(x, y);
      if ((selectedRow instanceof RowExtended)) {
        ((RowExtended)selectedRow).draw(x, y, slotHeight, mouseX, mouseY);
      }
    }
  }
  
  public void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    a(mouseX, mouseY, partialTicks);
    getSelectedRow();
  }
  
  protected void b(int x, int y, int mouseX, int mouseY)
  {
    calculateHeightMap();
    if (this.leftbound) {
      x = getLeft() + 2;
    }
    bnu tesselator = bnu.a();
    bmz worldRenderer = tesselator.c();
    for (int rowIndex = 0; rowIndex < this.heightMap.size(); rowIndex++)
    {
      int newY = y + ((Integer)this.heightMap.get(rowIndex)).intValue() + getHeaderPadding();
      int slotHeight = ((Row)this.rows.get(rowIndex)).getLineHeight() - 4;
      if ((newY > getBottom()) || (newY + slotHeight < getTop())) {
        a(rowIndex, x, newY);
      }
      if ((isDrawSelection()) && (isSelected(rowIndex)))
      {
        int x2;
        int x1;
        int x2;
        if (this.leftbound)
        {
          int x1 = getLeft();
          x2 = getLeft() + getRowWidth();
        }
        else
        {
          x1 = getLeft() + (getWidth() / 2 - getRowWidth() / 2);
          x2 = getLeft() + getWidth() / 2 + getRowWidth() / 2;
        }
        GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
        bni.z();
        worldRenderer.a(7, bvp.i);
        worldRenderer.b(x1, newY + slotHeight + 2, 0.0D).a(0.0D, 1.0D).b(128, 128, 128, 255).d();
        worldRenderer.b(x2, newY + slotHeight + 2, 0.0D).a(1.0D, 1.0D).b(128, 128, 128, 255).d();
        worldRenderer.b(x2, newY - 2, 0.0D).a(1.0D, 0.0D).b(128, 128, 128, 255).d();
        worldRenderer.b(x1, newY - 2, 0.0D).a(0.0D, 0.0D).b(128, 128, 128, 255).d();
        worldRenderer.b(x1 + 1, newY + slotHeight + 1, 0.0D).a(0.0D, 1.0D).b(0, 0, 0, 255).d();
        worldRenderer.b(x2 - 1, newY + slotHeight + 1, 0.0D).a(1.0D, 1.0D).b(0, 0, 0, 255).d();
        worldRenderer.b(x2 - 1, newY - 1, 0.0D).a(1.0D, 0.0D).b(0, 0, 0, 255).d();
        worldRenderer.b(x1 + 1, newY - 1, 0.0D).a(0.0D, 0.0D).b(0, 0, 0, 255).d();
        tesselator.b();
        bni.y();
      }
      drawSlot(rowIndex, x, newY, slotHeight, mouseX, mouseY);
    }
  }
  
  public void handleMouseInput()
  {
    if (g(getMouseY()))
    {
      if ((Mouse.isButtonDown(0)) && (q()))
      {
        if (this.l == -1.0F)
        {
          boolean var1 = true;
          if ((this.j >= this.d) && (this.j <= this.e))
          {
            int x2;
            int x1;
            int x2;
            if (this.leftbound)
            {
              int x1 = getLeft();
              x2 = getLeft() + getRowWidth();
            }
            else
            {
              x1 = (getWidth() - getRowWidth()) / 2;
              x2 = (getWidth() + getRowWidth()) / 2;
            }
            int var4 = getMouseY() - getTop() - this.t + (int)getCurrentScroll();
            int var5 = -1;
            for (int i1 = 0; i1 < this.heightMap.size(); i1++)
            {
              Integer integer = (Integer)this.heightMap.get(i1);
              Row line = (Row)this.rows.get(i1);
              if ((var4 >= integer.intValue()) && (var4 <= integer.intValue() + line.getLineHeight()))
              {
                var5 = i1;
                break;
              }
            }
            if ((getMouseX() >= x1) && (getMouseX() <= x2) && (var5 >= 0) && (var4 >= 0) && (var5 < b()))
            {
              boolean var6 = (var5 == this.o) && (MinecraftFactory.getVars().getSystemTime() - this.p < 250L);
              a(var5, var6, getMouseX(), getMouseY());
              this.o = var5;
              this.p = MinecraftFactory.getVars().getSystemTime();
            }
            else if ((getMouseX() >= x1) && (getMouseX() <= x2) && (var4 < 0))
            {
              a(getMouseX() - x1, getMouseY() - this.d + (int)this.n - 4);
              var1 = false;
            }
            int var11 = d();
            int var7 = var11 + 6;
            if ((getMouseX() >= var11) && (getMouseX() <= var7))
            {
              this.m = -1.0F;
              int var8 = m();
              if (var8 < 1) {
                var8 = 1;
              }
              int var9 = (int)((this.e - this.d) * (this.e - this.d) / k());
              var9 = on.a(var9, 32, this.e - this.d - 8);
              this.m /= (this.e - this.d - var9) / var8;
            }
            else
            {
              this.m = 1.0F;
            }
            if (var1) {
              this.l = getMouseY();
            } else {
              this.l = -2;
            }
          }
          else
          {
            this.l = -2;
          }
        }
        else if (this.l >= 0.0F)
        {
          this.n -= (getMouseY() - this.l) * this.m;
          this.l = getMouseY();
        }
      }
      else {
        this.l = -1;
      }
      int var10 = Mouse.getEventDWheel();
      if (var10 != 0)
      {
        if (var10 > 0) {
          var10 = -1;
        } else if (var10 < 0) {
          var10 = 1;
        }
        this.n += var10 * this.h / 2;
      }
    }
  }
  
  private int getMouseX()
  {
    return this.i;
  }
  
  private int getMouseY()
  {
    return this.j;
  }
  
  public void mouseClicked(int mouseX, int mouseY)
  {
    synchronized (this.rows)
    {
      for (Row row : this.rows) {
        if ((row instanceof RowExtended))
        {
          IButton pressed = ((RowExtended)row).mousePressed(mouseX, mouseY);
          if (pressed != null)
          {
            if ((this.selectedButton != null) && (pressed != this.selectedButton)) {
              this.selectedButton.mouseClicked(mouseX, mouseY);
            }
            this.selectedButton = pressed;
            return;
          }
        }
      }
    }
  }
  
  protected int d()
  {
    return this.scrollX > 0 ? this.scrollX : super.d();
  }
  
  public int c(int x, int y)
  {
    int var4;
    int var3;
    int var4;
    if (this.leftbound)
    {
      int var3 = getLeft();
      var4 = getLeft() + getRowWidth();
    }
    else
    {
      var3 = getLeft() + getWidth() / 2 - getRowWidth() / 2;
      var4 = getLeft() + getWidth() / 2 + getRowWidth() / 2;
    }
    int var5 = y - getTop() - this.t + (int)getCurrentScroll() - 4;
    int var6 = -1;
    for (int i1 = 0; i1 < this.heightMap.size(); i1++)
    {
      Integer integer = (Integer)this.heightMap.get(i1);
      Row line = (Row)this.rows.get(i1);
      if ((y >= integer.intValue()) && (y <= integer.intValue() + line.getLineHeight()))
      {
        var6 = i1;
        break;
      }
    }
    return (x < d()) && (x >= var3) && (x <= var4) && (var6 >= 0) && (var5 >= 0) && (var6 < b()) ? var6 : -1;
  }
  
  public void mouseReleased(int mouseX, int mouseY, int state)
  {
    if ((this.selectedButton != null) && (state == 0))
    {
      this.selectedButton.mouseReleased(mouseX, mouseY);
      this.selectedButton = null;
    }
  }
  
  public void scrollToBottom()
  {
    scrollTo(k());
  }
  
  public float getCurrentScroll()
  {
    return this.n;
  }
  
  public void scrollTo(float to)
  {
    this.n = to;
  }
  
  public int getContentHeight()
  {
    int height = this.bottomPadding + (getHeaderPadding() > 0 ? getHeaderPadding() + 8 : 0);
    List<E> chatLines = Lists.newArrayList(this.rows);
    for (Row row : chatLines) {
      height += row.getLineHeight();
    }
    return height;
  }
  
  public void calculateHeightMap()
  {
    this.heightMap.clear();
    
    int curHeight = getHeaderPadding();
    List<E> chatLines = Lists.newArrayList(this.rows);
    for (Row row : chatLines)
    {
      this.heightMap.add(Integer.valueOf(curHeight));
      curHeight += row.getLineHeight();
    }
  }
  
  public int getRowWidth()
  {
    return this.rowWidth;
  }
  
  public void setRowWidth(int rowWidth)
  {
    this.rowWidth = rowWidth;
  }
  
  public int getSelectedId()
  {
    synchronized (this.rows)
    {
      if ((this.selected < 0) || (this.selected > this.rows.size())) {
        this.selected = setSelectedId(0);
      }
    }
    return this.selected;
  }
  
  public int setSelectedId(int selected)
  {
    synchronized (this.rows)
    {
      if ((selected < 0) || (selected > this.rows.size())) {
        selected = 0;
      }
    }
    this.selected = selected;
    return selected;
  }
  
  public E getSelectedRow()
  {
    synchronized (this.rows)
    {
      if (this.rows.isEmpty()) {
        return null;
      }
      if (this.selected < 0)
      {
        this.selected = 0;
        return (Row)this.rows.get(0);
      }
      while (this.selected >= this.rows.size()) {
        this.selected -= 1;
      }
      return (Row)this.rows.get(this.selected);
    }
  }
  
  public int getWidth()
  {
    return this.b;
  }
  
  public void setWidth(int width)
  {
    this.b = width;
  }
  
  public int getHeight()
  {
    return this.c;
  }
  
  public void setHeight(int height)
  {
    this.c = height;
  }
  
  public int getHeight(int id)
  {
    return ((Integer)this.heightMap.get(id)).intValue();
  }
  
  public int getTop()
  {
    return this.d;
  }
  
  public void setTop(int top)
  {
    this.d = top;
  }
  
  public int getBottom()
  {
    return this.e;
  }
  
  public void setBottom(int bottom)
  {
    this.e = bottom;
  }
  
  public int getLeft()
  {
    return this.g;
  }
  
  public void setLeft(int left)
  {
    this.g = left;
  }
  
  public int getRight()
  {
    return this.f;
  }
  
  public void setRight(int right)
  {
    this.f = right;
  }
  
  public int getScrollX()
  {
    return this.scrollX;
  }
  
  public void setScrollX(int scrollX)
  {
    this.scrollX = scrollX;
  }
  
  public boolean isLeftbound()
  {
    return this.leftbound;
  }
  
  public void setLeftbound(boolean leftbound)
  {
    this.leftbound = leftbound;
  }
  
  public boolean isDrawSelection()
  {
    return this.r;
  }
  
  public void setDrawSelection(boolean drawSelection)
  {
    this.r = drawSelection;
  }
  
  @Deprecated
  public int getPadding()
  {
    return this.h;
  }
  
  public int getHeaderPadding()
  {
    return this.t;
  }
  
  public void setHeaderPadding(int headerPadding)
  {
    a(headerPadding > 0, headerPadding);
  }
  
  public String getHeader()
  {
    return this.header;
  }
  
  public void setHeader(String header)
  {
    this.header = header;
  }
  
  protected void a(int x, int y, bnu tesselator)
  {
    if (this.header != null) {
      MinecraftFactory.getVars().drawCenteredString(ChatColor.UNDERLINE.toString() + ChatColor.BOLD.toString() + this.header, getLeft() + (getRight() - getLeft()) / 2, 
        Math.min(getTop() + 5, y));
    }
  }
  
  public int getBottomPadding()
  {
    return this.bottomPadding;
  }
  
  public void setBottomPadding(int bottomPadding)
  {
    this.bottomPadding = bottomPadding;
  }
  
  public E getHoverItem(int mouseX, int mouseY)
  {
    int x2;
    int x1;
    int x2;
    if (this.leftbound)
    {
      int x1 = getLeft();
      x2 = getLeft() + getRowWidth();
    }
    else
    {
      x1 = getLeft() + (getWidth() / 2 - getRowWidth() / 2);
      x2 = getLeft() + getWidth() / 2 + getRowWidth() / 2;
    }
    if ((mouseX >= x1) && (mouseX <= x2)) {
      synchronized (this.rows)
      {
        for (int i = 0; i < this.heightMap.size(); i++)
        {
          Integer y = Integer.valueOf((int)(((Integer)this.heightMap.get(i)).intValue() + getTop() + getHeaderPadding() - getCurrentScroll()));
          E element = (Row)this.rows.get(i);
          if ((mouseY >= y.intValue()) && (mouseY <= y.intValue() + element.getLineHeight())) {
            return element;
          }
        }
      }
    }
    return null;
  }
  
  public List<E> getRows()
  {
    return this.rows;
  }
}
