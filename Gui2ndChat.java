import com.google.common.collect.Lists;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import eu.the5zig.mod.asm.Transformer;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.ingame.IGui2ndChat;
import eu.the5zig.mod.util.ClassProxyCallback;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.input.Mouse;

public class Gui2ndChat
  implements IGui2ndChat
{
  private final List<String> sentMessages = Lists.newArrayList();
  private final List<GuiChatLine> chatLines = Lists.newArrayList();
  private final List<GuiChatLine> singleChatLines = Lists.newArrayList();
  private int scrollPos;
  private boolean isScrolled;
  private static Method clickChatComponent;
  
  static
  {
    if (Transformer.FORGE) {
      try
      {
        clickChatComponent = Names.guiScreen.load().getDeclaredMethod("func_175276_a", new Class[] { Names.chatComponent.load() });
        clickChatComponent.setAccessible(true);
      }
      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }
  }
  
  public void draw(int updateCounter)
  {
    int scaledWidth = MinecraftFactory.getVars().getScaledWidth();
    if (MinecraftFactory.getClassProxyCallback().is2ndChatVisible())
    {
      int lineDisplayCount = getLineCount();
      boolean chatOpened = false;
      int totalChatLines = 0;
      int chatLineCount = this.singleChatLines.size();
      float opacity = MinecraftFactory.getClassProxyCallback().get2ndChatOpacity() * 0.9F + 0.1F;
      if (chatLineCount > 0)
      {
        if (isChatOpened()) {
          chatOpened = true;
        }
        float chatScale = getChatScale();
        int width = on.f(getChatWidth() / chatScale);
        GLUtil.pushMatrix();
        if (Transformer.FORGE) {
          GLUtil.translate(scaledWidth - getChatWidth() - 6.0F * chatScale, MinecraftFactory.getVars().getScaledHeight() - 28.0F, 0.0F);
        } else {
          GLUtil.translate(scaledWidth - getChatWidth() - 6.0F * chatScale, 8.0F, 0.0F);
        }
        GLUtil.scale(chatScale, chatScale, 1.0F);
        for (int lineIndex = 0; (lineIndex + this.scrollPos < this.singleChatLines.size()) && (lineIndex < lineDisplayCount); lineIndex++)
        {
          GuiChatLine chatLine = (GuiChatLine)this.singleChatLines.get(lineIndex + this.scrollPos);
          if (chatLine != null)
          {
            int ticksPassed = updateCounter - chatLine.getUpdatedCounter();
            if ((ticksPassed < 200) || (chatOpened))
            {
              double c = ticksPassed / 200.0D;
              c = 1.0D - c;
              c *= 10.0D;
              c = on.a(c, 0.0D, 1.0D);
              c *= c;
              int alpha = (int)(255.0D * c);
              if (chatOpened) {
                alpha = 255;
              }
              alpha = (int)(alpha * opacity);
              totalChatLines++;
              if (alpha > 3)
              {
                int x = 0;
                int y = -lineIndex * 9;
                Gui.drawRect(x, y - 9, x + width + on.d(4.0F * chatScale), y, alpha / 2 << 24);
                String text = chatLine.getChatComponent().d();
                GLUtil.enableBlend();
                if (MinecraftFactory.getClassProxyCallback().is2ndChatTextLeftbound()) {
                  MinecraftFactory.getVars().drawString(text, x, y - 8, 16777215 + (alpha << 24));
                } else {
                  MinecraftFactory.getVars().drawString(text, x + width + on.d(4.0F * chatScale) - MinecraftFactory.getVars().getStringWidth(text), y - 8, 16777215 + (alpha << 24));
                }
                GLUtil.disableAlpha();
                GLUtil.disableBlend();
              }
            }
          }
        }
        if (chatOpened)
        {
          int fontHeight = MinecraftFactory.getVars().getFontHeight();
          GLUtil.translate(-3.0F, 0.0F, 0.0F);
          int visibleLineHeight = chatLineCount * fontHeight + chatLineCount;
          int totalLineHeight = totalChatLines * fontHeight + totalChatLines;
          int var19 = this.scrollPos * totalLineHeight / chatLineCount;
          int var12 = totalLineHeight * totalLineHeight / visibleLineHeight;
          if (visibleLineHeight != totalLineHeight)
          {
            int alpha = var19 > 0 ? 170 : 96;
            int color = this.isScrolled ? 13382451 : 3355562;
            Gui.drawRect(width + 6, -var19, width + 8, -var19 - var12, color + (alpha << 24));
            Gui.drawRect(width + 8, -var19, width + 7, -var19 - var12, 13421772 + (alpha << 24));
          }
        }
        GLUtil.popMatrix();
      }
    }
  }
  
  public void clearChatMessages()
  {
    this.singleChatLines.clear();
    this.chatLines.clear();
    this.sentMessages.clear();
  }
  
  public void printChatMessage(String message)
  {
    printChatMessage(ChatComponentBuilder.fromLegacyText(message));
  }
  
  public void printChatMessage(Object chatComponent)
  {
    if (!(chatComponent instanceof eu)) {
      throw new IllegalArgumentException(chatComponent.getClass().getName() + " != " + eu.class.getName());
    }
    printChatMessage((eu)chatComponent);
  }
  
  public void printChatMessage(eu chatComponent)
  {
    printChatMessage(chatComponent, 0);
  }
  
  public void printChatMessage(eu chatComponent, int id)
  {
    setChatLine(chatComponent, id, ((Variables)MinecraftFactory.getVars()).getGuiIngame().e(), false);
  }
  
  private void setChatLine(eu chatComponent, int id, int currentUpdateCounter, boolean keep)
  {
    if (id != 0) {
      deleteChatLine(id);
    }
    int lineWidth = on.d(getChatWidth() / getChatScale());
    List<eu> lines = bdb.a(chatComponent, lineWidth, ((Variables)MinecraftFactory.getVars()).getFontrenderer(), false, false);
    boolean var6 = isChatOpened();
    eu lineString;
    for (Iterator<eu> iterator = lines.iterator(); iterator.hasNext(); this.singleChatLines.add(0, new GuiChatLine(currentUpdateCounter, lineString, id)))
    {
      lineString = (eu)iterator.next();
      if ((var6) && (this.scrollPos > 0))
      {
        this.isScrolled = true;
        scroll(1);
      }
    }
    while (this.singleChatLines.size() > 100) {
      this.singleChatLines.remove(this.singleChatLines.size() - 1);
    }
    if (!keep)
    {
      this.chatLines.add(0, new GuiChatLine(currentUpdateCounter, chatComponent, id));
      while (this.chatLines.size() > 100) {
        this.chatLines.remove(this.chatLines.size() - 1);
      }
    }
  }
  
  public void clear()
  {
    this.sentMessages.clear();
    this.singleChatLines.clear();
    this.chatLines.clear();
    resetScroll();
  }
  
  public void refreshChat()
  {
    this.singleChatLines.clear();
    resetScroll();
    for (int i = this.chatLines.size() - 1; i >= 0; i--)
    {
      GuiChatLine chatLine = (GuiChatLine)this.chatLines.get(i);
      setChatLine(chatLine.getChatComponent(), chatLine.getChatLineID(), chatLine.getUpdatedCounter(), true);
    }
  }
  
  public List<String> getSentMessages()
  {
    return this.sentMessages;
  }
  
  public void addToSentMessages(String message)
  {
    if ((this.sentMessages.isEmpty()) || (!((String)this.sentMessages.get(this.sentMessages.size() - 1)).equals(message))) {
      this.sentMessages.add(message);
    }
  }
  
  public void resetScroll()
  {
    this.scrollPos = 0;
    this.isScrolled = false;
  }
  
  public void scroll(int scroll)
  {
    this.scrollPos += scroll;
    int chatLines = this.singleChatLines.size();
    if (this.scrollPos > chatLines - getLineCount()) {
      this.scrollPos = (chatLines - getLineCount());
    }
    if (this.scrollPos <= 0)
    {
      this.scrollPos = 0;
      this.isScrolled = false;
    }
  }
  
  public void drawComponentHover(int mouseX, int mouseY)
  {
    eu chatComponent = getChatComponent(Mouse.getX(), Mouse.getY());
    if ((chatComponent != null) && (chatComponent.b().i() != null)) {
      ((bfb)MinecraftFactory.getVars().getMinecraftScreen()).a(chatComponent, mouseX, mouseY);
    }
  }
  
  public boolean mouseClicked(int button)
  {
    try
    {
      if (button == 0) {
        if (clickChatComponent != null) {
          if (!((Boolean)clickChatComponent.invoke(MinecraftFactory.getVars().getMinecraftScreen(), new Object[] {
            getChatComponent(Mouse.getX(), Mouse.getY()) })).booleanValue()) {
            break label84;
          }
        }
      }
      label84:
      return ((bfb)MinecraftFactory.getVars().getMinecraftScreen()).a(getChatComponent(Mouse.getX(), Mouse.getY()));
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public void keyTyped(int code)
  {
    if (code == 201) {
      scroll(((Variables)MinecraftFactory.getVars()).getGuiIngame().d().i() - 1);
    } else if (code == 209) {
      scroll(-((Variables)MinecraftFactory.getVars()).getGuiIngame().d().i() + 1);
    }
  }
  
  private eu getChatComponent(int mouseX, int mouseY)
  {
    if (!isChatOpened()) {
      return null;
    }
    bcx scaledResolution = new bcx(((Variables)MinecraftFactory.getVars()).getMinecraft());
    int resolutionScaleFactor = scaledResolution.e();
    float chatScale = getChatScale();
    int x = mouseX / resolutionScaleFactor - MinecraftFactory.getVars().getScaledWidth() + getChatWidth() + 6;
    int y = mouseY / resolutionScaleFactor - 27;
    x = on.d(x / chatScale);
    y = on.d(y / chatScale);
    if ((x >= 0) && (y >= 0))
    {
      int lineCount = Math.min(getLineCount(), this.singleChatLines.size());
      if ((x <= on.d(getChatWidth() / getChatScale() + 3.0F / getChatScale())) && (y < MinecraftFactory.getVars().getFontHeight() * lineCount))
      {
        int lineId = y / MinecraftFactory.getVars().getFontHeight() + this.scrollPos;
        if ((lineId >= 0) && (lineId < this.singleChatLines.size()))
        {
          GuiChatLine chatLine = (GuiChatLine)this.singleChatLines.get(lineId);
          int widthCounter = on.d(getChatWidth() / getChatScale() + 3.0F / getChatScale());
          Iterator<eu> iterator = chatLine.getChatComponent().iterator();
          while (iterator.hasNext())
          {
            eu chatComponent = (eu)iterator.next();
            if ((chatComponent instanceof fa))
            {
              widthCounter -= MinecraftFactory.getVars().getStringWidth(bdb.a(((fa)chatComponent).g(), false));
              if (x > widthCounter) {
                return chatComponent;
              }
            }
          }
        }
        return null;
      }
      return null;
    }
    return null;
  }
  
  public boolean isChatOpened()
  {
    return MinecraftFactory.getVars().getMinecraftScreen() instanceof bed;
  }
  
  public void deleteChatLine(int id)
  {
    Iterator<GuiChatLine> iterator = this.singleChatLines.iterator();
    while (iterator.hasNext())
    {
      GuiChatLine chatLine = (GuiChatLine)iterator.next();
      if (chatLine.getChatLineID() == id) {
        iterator.remove();
      }
    }
    iterator = this.chatLines.iterator();
    while (iterator.hasNext())
    {
      GuiChatLine chatLine = (GuiChatLine)iterator.next();
      if (chatLine.getChatLineID() == id) {
        iterator.remove();
      }
    }
  }
  
  public int getChatWidth()
  {
    return on.d(MinecraftFactory.getClassProxyCallback().get2ndChatWidth());
  }
  
  public int getChatHeight()
  {
    return on.d(isChatOpened() ? MinecraftFactory.getClassProxyCallback().get2ndChatHeightFocused() : MinecraftFactory.getClassProxyCallback().get2ndChatHeightUnfocused());
  }
  
  public float getChatScale()
  {
    return MinecraftFactory.getClassProxyCallback().get2ndChatScale();
  }
  
  public int getLineCount()
  {
    return getChatHeight() / 9;
  }
}
