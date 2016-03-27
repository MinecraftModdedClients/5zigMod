package eu.the5zig.mod.modules.items.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.api.ServerAPIBackend;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.render.Base64Renderer;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.render.LargeTextRenderer;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CustomServer
  extends Item
{
  private final Base64Renderer base64Renderer = new Base64Renderer();
  
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    ServerAPIBackend backend = The5zigMod.getServerAPIBackend();
    if (backend.getLargeText() != null)
    {
      List<?> texts = The5zigMod.getVars().splitStringToWidth(backend.getLargeText(), (int)(The5zigMod.getVars().getScaledWidth() / 1.5D / 3.0D * 2.0D));
      int i = 0;
      for (int textsSize = texts.size(); (i < textsSize) && (i <= 9); i++)
      {
        Object o = texts.get(i);
        String text = String.valueOf(o);
        DisplayRenderer.largeTextRenderer.render(text, 1.5F, (int)(The5zigMod.getVars().getScaledHeight() / 4 + i * 15 * The5zigMod.getConfig().getFloat("scale")));
        DisplayRenderer.largeTextRenderer.flush();
      }
    }
    else if ((backend.getCountdownTime() != -1L) && (The5zigMod.getConfig().getBool("showLargeStartCountdown")))
    {
      DisplayRenderer.largeTextRenderer.render(
        The5zigMod.getRenderer().getPrefix() + backend.getCountdownName() + ": " + shorten((backend.getCountdownTime() - System.currentTimeMillis()) / 1000.0D));
    }
    int yy;
    if (The5zigMod.getServerAPIBackend().getBase64() != null)
    {
      if ((this.base64Renderer.getBase64String() == null) || (!this.base64Renderer.getBase64String().equals(The5zigMod.getServerAPIBackend().getBase64()))) {
        this.base64Renderer.setBase64String(The5zigMod.getServerAPIBackend().getBase64(), The5zigMod.getServerAPIBackend().getBase64().substring(0, 16));
      }
      int xx = The5zigMod.getVars().getScaledWidth() - 64;
      yy = (The5zigMod.getVars().getScaledHeight() - 64) / 2;
      this.base64Renderer.renderImage(xx, yy, 64, 64);
    }
    List<String> renderItems = getRenderItems(dummy);
    if (!renderItems.isEmpty())
    {
      The5zigMod.getVars().drawString(The5zigMod.getRenderer().getPrefix() + ChatColor.UNDERLINE + backend.getDisplayName(), x, y);
      y += 12;
      for (String renderItem : renderItems)
      {
        The5zigMod.getVars().drawString(renderItem, x, y);
        y += 10;
      }
    }
  }
  
  public boolean shouldRender(boolean dummy)
  {
    return !getRenderItems(dummy).isEmpty();
  }
  
  public int getWidth(boolean dummy)
  {
    int maxWidth = 0;
    List<String> renderItems = getRenderItems(dummy);
    for (String renderItem : renderItems)
    {
      int width = The5zigMod.getVars().getStringWidth(renderItem);
      if (width > maxWidth) {
        maxWidth = width;
      }
    }
    return maxWidth;
  }
  
  public int getHeight(boolean dummy)
  {
    List<String> renderItems = getRenderItems(dummy);
    return renderItems.isEmpty() ? 0 : 12 + renderItems.size() * 10;
  }
  
  private List<String> getRenderItems(boolean dummy)
  {
    Map<String, String> stats = dummy ? Maps.newHashMap() : The5zigMod.getServerAPIBackend().getStats();
    if (dummy)
    {
      stats.put("Kills", "8");
      stats.put("Deaths", "3");
    }
    if (stats.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> result = Lists.newArrayList();
    for (Map.Entry<String, String> entry : stats.entrySet()) {
      result.add(getPrefix((String)entry.getKey()) + (String)entry.getValue());
    }
    return result;
  }
}
