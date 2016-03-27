import eu.the5zig.util.minecraft.ChatColor;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatComponentBuilder
{
  private static final Map<ChatColor, a> TRANSLATE = new HashMap();
  private static final Pattern url = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");
  
  static
  {
    for (int i = 0; i < a.values().length; i++) {
      TRANSLATE.put(ChatColor.values()[i], a.values()[i]);
    }
  }
  
  public static eu fromLegacyText(String message)
  {
    fa base = new fa("");
    StringBuilder builder = new StringBuilder();
    fa currentComponent = new fa("");
    ez currentStyle = new ez();
    currentComponent.a(currentStyle);
    Matcher matcher = url.matcher(message);
    for (int i = 0; i < message.length(); i++)
    {
      char c = message.charAt(i);
      if (c == '§')
      {
        i++;
        c = message.charAt(i);
        if ((c >= 'A') && (c <= 'Z')) {
          c = (char)(c + ' ');
        }
        ChatColor format = ChatColor.getByChar(c);
        if (format != null)
        {
          if (builder.length() > 0)
          {
            fa old = currentComponent;
            currentComponent = old.h();
            currentStyle = currentComponent.b();
            old.a(builder.toString());
            builder = new StringBuilder();
            base.a(old);
          }
          switch (format)
          {
          case BOLD: 
            currentStyle.a(Boolean.valueOf(true));
            break;
          case ITALIC: 
            currentStyle.b(Boolean.valueOf(true));
            break;
          case STRIKETHROUGH: 
            currentStyle.c(Boolean.valueOf(true));
            break;
          case UNDERLINE: 
            currentStyle.d(Boolean.valueOf(true));
            break;
          case MAGIC: 
            currentStyle.e(Boolean.valueOf(true));
            break;
          default: 
            currentStyle.a((a)TRANSLATE.get(format));
            break;
          }
        }
      }
      else
      {
        int pos = message.indexOf(' ', i);
        if (pos == -1) {
          pos = message.length();
        }
        if (matcher.region(i, pos).find())
        {
          if (builder.length() > 0)
          {
            fa old = currentComponent;
            currentComponent = old.h();
            currentStyle = currentComponent.b();
            old.a(builder.toString());
            builder = new StringBuilder();
            base.a(old);
          }
          fa old = currentComponent;
          currentComponent = old.h();
          currentStyle = currentComponent.b();
          String urlStr = message.substring(i, pos);
          if (!urlStr.startsWith("http")) {
            urlStr = "http://" + urlStr;
          }
          et clickEvent = new et(et.a.a, urlStr);
          currentStyle.a(clickEvent);
          base.a(currentComponent);
          i += pos - i - 1;
          currentComponent = old;
          currentStyle = currentComponent.b();
        }
        else
        {
          builder.append(c);
        }
      }
    }
    if (builder.length() > 0)
    {
      currentComponent.a(builder.toString());
      base.a(currentComponent);
    }
    return base;
  }
}
