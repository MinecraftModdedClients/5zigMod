import com.google.common.collect.Multimap;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

public class WrappedItemStack
  implements ItemStack
{
  private static final UUID ITEM_MODIFIER_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
  private adq item;
  
  public WrappedItemStack(adq item)
  {
    this.item = item;
  }
  
  public int getMaxDurability()
  {
    return this.item.j();
  }
  
  public int getCurrentDurability()
  {
    return this.item.h();
  }
  
  public String getKey()
  {
    return ((kk)ado.f.b(this.item.b())).a();
  }
  
  public String getDisplayName()
  {
    return this.item.q();
  }
  
  public List<String> getLore()
  {
    return this.item.a(((Variables)MinecraftFactory.getVars()).getPlayer(), false);
  }
  
  public void render(int x, int y, boolean withGenericAttributes)
  {
    if (this.item == null) {
      return;
    }
    ((Variables)MinecraftFactory.getVars()).renderItem(this.item, x, y);
    if (withGenericAttributes) {
      for (rw modifier : rw.values())
      {
        Multimap<String, sn> multimap = this.item.a(modifier);
        for (Map.Entry<String, sn> entry : multimap.entries())
        {
          sn attribute = (sn)entry.getValue();
          double value = attribute.d();
          if (ITEM_MODIFIER_UUID.equals(attribute.a())) {
            if (((Variables)MinecraftFactory.getVars()).getPlayer() == null)
            {
              value += 1.0D;
              value += ago.a(this.item, sf.a);
            }
            else
            {
              value += ((Variables)MinecraftFactory.getVars()).getPlayer().a(yt.e).b();
              value += ago.a(this.item, sf.a);
            }
          }
          if ((((String)entry.getKey()).equals("generic.attackDamage")) || (((String)entry.getKey()).equals("generic.armor")))
          {
            GLUtil.disableDepth();
            GLUtil.pushMatrix();
            GLUtil.translate(x + 8, y + 10, 1.0F);
            GLUtil.scale(0.7F, 0.7F, 0.7F);
            MinecraftFactory.getVars().drawString(ChatColor.BLUE + "+" + Math.round(value), 0, 0);
            GLUtil.popMatrix();
            GLUtil.enableDepth();
          }
        }
      }
    }
  }
}
