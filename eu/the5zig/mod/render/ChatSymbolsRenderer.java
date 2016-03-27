package eu.the5zig.mod.render;

import com.google.common.base.Charsets;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.items.Item;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.apache.commons.compress.utils.IOUtils;
import org.lwjgl.input.Mouse;

public class ChatSymbolsRenderer
{
  private static final String DEFAULT_SYMBOLS = "♡ℳⒶ❀ℬℒℛℰ★❤™♥✿ℴ☠ღ✌Ⓔ❁ℋ❥♛ℯℜ〖【ℱ℘✧❝ℕℐⓇ☣〗♕ℓⓄ»】❞Ⓣ☢Ⓜ●ⓈⓃ❣Ⓓ♚→✩♔†✈Åℂ☾Ⓘ☮✪❃☛☞Ⓛ☪➳Ⓚ✰✔「✞》ℊ«ℭ☯《ℙ➤⇝ℝ」⋆ϟ━☆『ⒸⒷ➵☼ⓘ✦↬✾©✯Ⓟℑ✘K₳☪℧›☽►Ⓖ☜☚↠✝』☁✽↳‹❦ⒽⓊℵ†☀〔✠╔─♣ⓐ♠↓₮〘ℍ“℣▸ℤ℮☂❛✓✖│❖➊ℌ✉←ℎ❋〙◤⇜▲Ⓨ☭➸™❜↻ⓢ☮❊☯〕☬✯♞〚ツ✎®➽◦║┊◆✌ⓡ✍☸∞”◢↫◄ⓛ▬♋Ⓕ┇✗↑ⓔ℩♦ⓞ❤★☆✰✯✡✪✶✱✲✴✼✻✵❇❈❊❖❄❆❋❂⁂☯✡☨✞✝☮☥☦☧☩☪☫☬☭✌♛♕♚♔♜♖♝♗♞♘♟♙ღ▂▃▅▆▇▇▆▅▃▂⓿❶❷❸❹❺❻❼❽❾∆▽O■□●○▲►▼◄";
  private static String SYMBOLS = "";
  private static final float SCALE = 1.6F;
  
  public static void load()
  {
    File symbolsFile = new File("the5zigmod", "symbols.txt");
    if (!symbolsFile.exists())
    {
      BufferedWriter out = null;
      try
      {
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(symbolsFile), Charsets.UTF_8));
        out.write("♡ℳⒶ❀ℬℒℛℰ★❤™♥✿ℴ☠ღ✌Ⓔ❁ℋ❥♛ℯℜ〖【ℱ℘✧❝ℕℐⓇ☣〗♕ℓⓄ»】❞Ⓣ☢Ⓜ●ⓈⓃ❣Ⓓ♚→✩♔†✈Åℂ☾Ⓘ☮✪❃☛☞Ⓛ☪➳Ⓚ✰✔「✞》ℊ«ℭ☯《ℙ➤⇝ℝ」⋆ϟ━☆『ⒸⒷ➵☼ⓘ✦↬✾©✯Ⓟℑ✘K₳☪℧›☽►Ⓖ☜☚↠✝』☁✽↳‹❦ⒽⓊℵ†☀〔✠╔─♣ⓐ♠↓₮〘ℍ“℣▸ℤ℮☂❛✓✖│❖➊ℌ✉←ℎ❋〙◤⇜▲Ⓨ☭➸™❜↻ⓢ☮❊☯〕☬✯♞〚ツ✎®➽◦║┊◆✌ⓡ✍☸∞”◢↫◄ⓛ▬♋Ⓕ┇✗↑ⓔ℩♦ⓞ❤★☆✰✯✡✪✶✱✲✴✼✻✵❇❈❊❖❄❆❋❂⁂☯✡☨✞✝☮☥☦☧☩☪☫☬☭✌♛♕♚♔♜♖♝♗♞♘♟♙ღ▂▃▅▆▇▇▆▅▃▂⓿❶❷❸❹❺❻❼❽❾∆▽O■□●○▲►▼◄");
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      finally
      {
        SYMBOLS = "♡ℳⒶ❀ℬℒℛℰ★❤™♥✿ℴ☠ღ✌Ⓔ❁ℋ❥♛ℯℜ〖【ℱ℘✧❝ℕℐⓇ☣〗♕ℓⓄ»】❞Ⓣ☢Ⓜ●ⓈⓃ❣Ⓓ♚→✩♔†✈Åℂ☾Ⓘ☮✪❃☛☞Ⓛ☪➳Ⓚ✰✔「✞》ℊ«ℭ☯《ℙ➤⇝ℝ」⋆ϟ━☆『ⒸⒷ➵☼ⓘ✦↬✾©✯Ⓟℑ✘K₳☪℧›☽►Ⓖ☜☚↠✝』☁✽↳‹❦ⒽⓊℵ†☀〔✠╔─♣ⓐ♠↓₮〘ℍ“℣▸ℤ℮☂❛✓✖│❖➊ℌ✉←ℎ❋〙◤⇜▲Ⓨ☭➸™❜↻ⓢ☮❊☯〕☬✯♞〚ツ✎®➽◦║┊◆✌ⓡ✍☸∞”◢↫◄ⓛ▬♋Ⓕ┇✗↑ⓔ℩♦ⓞ❤★☆✰✯✡✪✶✱✲✴✼✻✵❇❈❊❖❄❆❋❂⁂☯✡☨✞✝☮☥☦☧☩☪☫☬☭✌♛♕♚♔♜♖♝♗♞♘♟♙ღ▂▃▅▆▇▇▆▅▃▂⓿❶❷❸❹❺❻❼❽❾∆▽O■□●○▲►▼◄";
        IOUtils.closeQuietly(out);
      }
    }
    else
    {
      BufferedReader in = null;
      try
      {
        in = new BufferedReader(new InputStreamReader(new FileInputStream(symbolsFile), Charsets.UTF_8));
        SYMBOLS = in.readLine();
      }
      catch (Exception e)
      {
        e.printStackTrace();
        SYMBOLS = "♡ℳⒶ❀ℬℒℛℰ★❤™♥✿ℴ☠ღ✌Ⓔ❁ℋ❥♛ℯℜ〖【ℱ℘✧❝ℕℐⓇ☣〗♕ℓⓄ»】❞Ⓣ☢Ⓜ●ⓈⓃ❣Ⓓ♚→✩♔†✈Åℂ☾Ⓘ☮✪❃☛☞Ⓛ☪➳Ⓚ✰✔「✞》ℊ«ℭ☯《ℙ➤⇝ℝ」⋆ϟ━☆『ⒸⒷ➵☼ⓘ✦↬✾©✯Ⓟℑ✘K₳☪℧›☽►Ⓖ☜☚↠✝』☁✽↳‹❦ⒽⓊℵ†☀〔✠╔─♣ⓐ♠↓₮〘ℍ“℣▸ℤ℮☂❛✓✖│❖➊ℌ✉←ℎ❋〙◤⇜▲Ⓨ☭➸™❜↻ⓢ☮❊☯〕☬✯♞〚ツ✎®➽◦║┊◆✌ⓡ✍☸∞”◢↫◄ⓛ▬♋Ⓕ┇✗↑ⓔ℩♦ⓞ❤★☆✰✯✡✪✶✱✲✴✼✻✵❇❈❊❖❄❆❋❂⁂☯✡☨✞✝☮☥☦☧☩☪☫☬☭✌♛♕♚♔♜♖♝♗♞♘♟♙ღ▂▃▅▆▇▇▆▅▃▂⓿❶❷❸❹❺❻❼❽❾∆▽O■□●○▲►▼◄";
      }
      finally
      {
        IOUtils.closeQuietly(in);
      }
    }
  }
  
  private boolean opened = false;
  private int scrollOffset;
  private boolean scrollPressed = false;
  private int scrollMouseYOffset;
  private int boxX1;
  private int boxX2;
  private int boxY1;
  private int boxY2;
  private int panelX1;
  private int panelX2;
  private int panelY1;
  private int panelY2;
  
  public void render()
  {
    int mouseX = Mouse.getX() / The5zigMod.getVars().getScaleFactor();
    int height = The5zigMod.getVars().getScaledHeight();
    int mouseY = height - Mouse.getY() / The5zigMod.getVars().getScaleFactor();
    
    this.boxX1 = 2;
    this.boxX2 = (this.boxX1 + 11);
    this.boxY1 = (height - 26);
    this.boxY2 = (this.boxY1 + 11);
    
    this.panelX1 = this.boxX1;
    this.panelX2 = (this.panelX1 + 108);
    this.panelY1 = (this.boxY1 - 1 - 120);
    this.panelY2 = (this.panelY1 + 120);
    
    this.opened = (((mouseX >= this.boxX1) && (mouseX <= this.boxX2) && (mouseY >= this.boxY1) && (mouseY <= this.boxY2)) || (((mouseX >= this.panelX1) && (mouseX <= this.panelX2) && (mouseY >= this.panelY1) && (mouseY <= this.panelY2)) || (((mouseX >= this.panelX1) && (mouseX <= this.panelX2) && (mouseY >= this.panelY2) && (mouseY <= this.boxY2) && (this.opened)) || (this.scrollPressed))));
    
    int rectColor = this.opened ? -1342177280 : Integer.MIN_VALUE;
    Gui.drawRect(this.boxX1, this.boxY1, this.boxX2, this.boxY2, rectColor);
    The5zigMod.getVars().drawString("+", 4, height - 24);
    if (this.opened)
    {
      The5zigMod.getVars().renderTextureOverlay(this.panelX1, this.panelX2, this.panelY1, this.panelY2);
      
      int scrollX1 = this.panelX2 - 8;
      int scrollX2 = scrollX1 + 5;
      int totalRows = Math.max(0, (int)Math.ceil(SYMBOLS.length() / 5.0D) - 6);
      double rowPercent = this.scrollOffset / totalRows;
      int h = totalRows > 0 ? (int)(6.0D / (totalRows + 6) * (this.panelY2 - this.panelY1)) : this.panelY2 - this.panelY1;
      int scrollHeight = Math.max(20, h);
      int scrollY1 = this.panelY1 + (int)(rowPercent * (this.panelY2 - this.panelY1 - scrollHeight));
      int scrollY2 = scrollY1 + scrollHeight;
      if ((mouseX >= scrollX1) && (mouseX <= scrollX2) && (mouseY >= this.panelY1) && (mouseY <= this.panelY2))
      {
        if ((!this.scrollPressed) && (Mouse.isButtonDown(0)) && (mouseY >= scrollY1) && (mouseY <= scrollY2))
        {
          this.scrollPressed = true;
          this.scrollMouseYOffset = ((int)(mouseY - this.panelY1 - Math.ceil(this.scrollOffset / totalRows * (this.panelY2 - this.panelY1 - scrollHeight))));
        }
        else if (!Mouse.isButtonDown(0))
        {
          this.scrollPressed = false;
        }
        if (this.scrollPressed)
        {
          double a = (mouseY - this.scrollMouseYOffset - this.panelY1) / (this.panelY2 - this.panelY1 - scrollHeight) * totalRows;
          this.scrollOffset = ((int)Math.min(Math.max(a, 0.0D), totalRows));
        }
      }
      else if (!Mouse.isButtonDown(0))
      {
        this.scrollPressed = false;
      }
      Gui.drawRect(scrollX1, this.panelY1, scrollX2, this.panelY2, -1154272461);
      Gui.drawRect(scrollX1, scrollY1, scrollX2, scrollY2, -16777216);
      Gui.drawRect(scrollX1, scrollY1, scrollX2 - 1, scrollY2 - 1, this.scrollPressed ? 0xFFFFFF & ((ChatColor)The5zigMod.getConfig().get("colorPrefix").get()).getColor() | 0xFF000000 : -7829368);
      
      GLUtil.pushMatrix();
      GLUtil.translate(this.panelX1, this.panelY1, 0.0F);
      GLUtil.scale(1.6F, 1.6F, 1.6F);
      for (int row = 0; row < 6; row++) {
        for (int col = 0; col < 5; col++)
        {
          int index = col + (row + this.scrollOffset) * 5;
          if ((index < 0) || (index >= SYMBOLS.length())) {
            break;
          }
          char symbol = SYMBOLS.charAt(index);
          int x = 2 + col * 12;
          int y = 2 + row * 12;
          boolean isHover = (mouseX >= x * 1.6F + this.panelX1) && (mouseX <= (x + 10) * 1.6F + this.panelX1) && (mouseY >= y * 1.6F + this.panelY1) && (mouseY <= (y + 10) * 1.6F + this.panelY1);
          int backgroundColor = isHover ? 2005436552 : -1722460843;
          Gui.drawRect(x, y, x + 10, y + 10, backgroundColor);
          String symbolString = String.valueOf(symbol);
          int stringWidth = The5zigMod.getVars().getStringWidth(symbolString);
          The5zigMod.getVars().drawString(symbolString, x + (10 - stringWidth) / 2 + 1, y + 1);
        }
      }
      GLUtil.popMatrix();
    }
  }
  
  public boolean mouseClicked(int mouseX, int mouseY)
  {
    if (this.opened)
    {
      int pressedSymbolIndex = -1;
      for (int row = 0; row < 6; row++) {
        for (int col = 0; col < 5; col++)
        {
          int index = col + (row + this.scrollOffset) * 5;
          if (index >= SYMBOLS.length()) {
            break;
          }
          int x = 2 + col * 12;
          int y = 2 + row * 12;
          boolean isHover = (mouseX >= x * 1.6F + this.panelX1) && (mouseX <= (x + 10) * 1.6F + this.panelX1) && (mouseY >= y * 1.6F + this.panelY1) && (mouseY <= (y + 10) * 1.6F + this.panelY1);
          if ((isHover) && (!this.scrollPressed)) {
            pressedSymbolIndex = index;
          }
        }
      }
      if (pressedSymbolIndex != -1)
      {
        char pressedSymbol = SYMBOLS.charAt(pressedSymbolIndex);
        The5zigMod.getVars().typeInChatGUI(String.valueOf(pressedSymbol));
      }
    }
    return this.opened;
  }
}
