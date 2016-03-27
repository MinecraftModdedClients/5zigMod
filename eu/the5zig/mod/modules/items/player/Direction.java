package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ConfigNew.BracketsFormatting;
import eu.the5zig.mod.config.ConfigNew.DirectionStyle;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.config.items.EnumItem;
import eu.the5zig.mod.config.items.Item;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.util.IVariables;

public class Direction
  extends StringItem
{
  public Direction()
  {
    addSetting(new EnumItem("directionStyle", "direction", ConfigNew.DirectionStyle.STRING, ConfigNew.DirectionStyle.class));
    addSetting(new BoolItem("showDirectionTowards", "direction", Boolean.valueOf(false)));
  }
  
  protected Object getValue(boolean dummy)
  {
    return getF(dummy);
  }
  
  private String getF(boolean dummy)
  {
    float rotationYaw = dummy ? 0.0F : The5zigMod.getVars().getPlayerRotationYaw();
    ConfigNew.DirectionStyle directionStyle = (ConfigNew.DirectionStyle)getSetting("directionStyle").get();
    if (directionStyle == ConfigNew.DirectionStyle.DEGREE) {
      return shorten(Math.abs(rotationYaw) % 360.0D) + "°";
    }
    float fDir = rotationYaw / 360.0F * 4.0F;
    fDir %= 4.0F;
    if (fDir < 0.0F) {
      fDir = Math.abs(-4.0F - fDir);
    }
    String result;
    String result;
    if (((fDir >= 3.75D) && (fDir <= 4.0D)) || ((fDir >= 0.0D) && (fDir <= 0.25D)))
    {
      String s = shorten(fDir);
      if (s.startsWith("4")) {
        s = "0" + s.substring(1);
      }
      result = toDirection(s, I18n.translate("ingame.f.south"), "Z+");
    }
    else
    {
      String result;
      if ((fDir > 0.25D) && (fDir < 0.75D))
      {
        result = toDirection(shorten(fDir), I18n.translate("ingame.f.south_west"), "X-, Z+");
      }
      else
      {
        String result;
        if ((fDir >= 0.75D) && (fDir <= 1.25D))
        {
          result = toDirection(shorten(fDir), I18n.translate("ingame.f.west"), "X-");
        }
        else
        {
          String result;
          if ((fDir > 1.25D) && (fDir < 1.75D))
          {
            result = toDirection(shorten(fDir), I18n.translate("ingame.f.north_west"), "X-, Z-");
          }
          else
          {
            String result;
            if ((fDir >= 1.75D) && (fDir <= 2.25D))
            {
              result = toDirection(shorten(fDir), I18n.translate("ingame.f.north"), "Z-");
            }
            else
            {
              String result;
              if ((fDir > 2.25D) && (fDir < 2.75D))
              {
                result = toDirection(shorten(fDir), I18n.translate("ingame.f.north_east"), "X+, Z-");
              }
              else
              {
                String result;
                if ((fDir >= 2.75D) && (fDir <= 3.25D))
                {
                  result = toDirection(shorten(fDir), I18n.translate("ingame.f.east"), "X+");
                }
                else
                {
                  String result;
                  if ((fDir > 3.25D) && (fDir < 3.75D))
                  {
                    String s = shorten(fDir);
                    if (s.startsWith("4")) {
                      s = "0" + s.substring(1);
                    }
                    result = toDirection(s, I18n.translate("ingame.f.south_east"), "X+, Z+");
                  }
                  else
                  {
                    result = I18n.translate("error");
                  }
                }
              }
            }
          }
        }
      }
    }
    return result;
  }
  
  private String toDirection(String number, String direction, String towards)
  {
    DisplayRenderer renderer = The5zigMod.getRenderer();
    ConfigNew.DirectionStyle directionStyle = (ConfigNew.DirectionStyle)getSetting("directionStyle").get();
    String rightBr = ((ConfigNew.BracketsFormatting)The5zigMod.getConfig().getEnum("formattingBrackets", ConfigNew.BracketsFormatting.class)).hasFirst() ? renderer.getBracketsRight() : "";
    towards = " " + renderer.getBrackets() + renderer.getBracketsLeft() + renderer.getPrefix() + towards + renderer.getBrackets() + rightBr;
    
    boolean directionTowards = ((Boolean)getSetting("showDirectionTowards").get()).booleanValue();
    if (directionStyle == ConfigNew.DirectionStyle.NUMBER) {
      return number + (directionTowards ? towards : "");
    }
    if (directionStyle == ConfigNew.DirectionStyle.STRING) {
      return direction + (directionTowards ? towards : "");
    }
    return direction + " (" + number + ") " + (directionTowards ? towards : "");
  }
  
  public String getName()
  {
    return "F";
  }
}
