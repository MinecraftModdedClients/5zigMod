package eu.the5zig.mod.util;

import eu.the5zig.util.Utils;
import net.minecraft.launchwrapper.Launch;

public class Start
{
  public static void main(String[] args)
  {
    Launch.main((String[])Utils.concat(new String[] { "--version", "1.9", "--assetsDir", "assets", "--assetIndex", "1.9", "--userProperties", "{}", "--tweakClass", "eu.the5zig.mod.asm.ClassTweaker" }, args));
  }
}
