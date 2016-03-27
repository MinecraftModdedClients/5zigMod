import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.elements.ITextfield;

public class Textfield
  extends bdd
  implements ITextfield
{
  private final int width;
  private final int height;
  
  public Textfield(int id, int x, int y, int width, int height, int maxStringLength)
  {
    super(id, ((Variables)MinecraftFactory.getVars()).getFontrenderer(), x, y, width, height);
    this.width = width;
    this.height = height;
    setMaxStringLength(maxStringLength);
  }
  
  public Textfield(int id, int x, int y, int width, int height)
  {
    this(id, x, y, width, height, 32);
  }
  
  public int getId()
  {
    return d();
  }
  
  public void setSelected(boolean selected)
  {
    b(selected);
  }
  
  public boolean isFocused()
  {
    return m();
  }
  
  public void setFocused(boolean focused)
  {
    b(focused);
  }
  
  public boolean isBackgroundDrawing()
  {
    return j();
  }
  
  public int getX()
  {
    return this.a;
  }
  
  public void setX(int x)
  {
    this.a = x;
  }
  
  public int getY()
  {
    return this.f;
  }
  
  public void setY(int y)
  {
    this.f = y;
  }
  
  public int getWidth()
  {
    return this.width;
  }
  
  public int getHeight()
  {
    return this.height;
  }
  
  public int getMaxStringLength()
  {
    return h();
  }
  
  public void setMaxStringLength(int length)
  {
    f(length);
  }
  
  public String getText()
  {
    return b();
  }
  
  public void setText(String string)
  {
    a(string);
  }
  
  public void mouseClicked(int x, int y, int button)
  {
    a(x, y, button);
  }
  
  public boolean keyTyped(char character, int key)
  {
    return a(character, key);
  }
  
  public void tick()
  {
    a();
  }
  
  public void draw()
  {
    g();
  }
}
