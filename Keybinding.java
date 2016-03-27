import eu.the5zig.mod.util.IKeybinding;

public class Keybinding
  extends bcc
  implements IKeybinding
{
  public Keybinding(String description, int keyCode, String category)
  {
    super(description, keyCode, category);
  }
  
  public boolean isPressed()
  {
    return g();
  }
  
  public int getKeyCode()
  {
    return j();
  }
  
  public int compareTo(bcc o)
  {
    return super.a(o);
  }
}
