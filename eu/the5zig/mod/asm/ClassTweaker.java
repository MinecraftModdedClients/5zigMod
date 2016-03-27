package eu.the5zig.mod.asm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.main.Main;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;

public class ClassTweaker
  implements ITweaker
{
  private List<String> args;
  private File gameDir;
  private File assetsDir;
  private String version;
  
  public void acceptOptions(List<String> args, File gameDir, File assetsDir, String version)
  {
    this.args = args;
    this.gameDir = gameDir;
    this.assetsDir = assetsDir;
    this.version = version;
    LogWrapper.info("Minecraft Version: " + version, new Object[0]);
    try
    {
      LogWrapper.finest("Checking for Forge", new Object[0]);
      Class.forName("net.minecraftforge.client.GuiIngameForge");
      LogWrapper.info("Forge detected!", new Object[0]);
      Transformer.FORGE = true;
    }
    catch (Exception ignored)
    {
      LogWrapper.info("Forge not found!", new Object[0]);
    }
  }
  
  public void injectIntoClassLoader(LaunchClassLoader classLoader)
  {
    classLoader.registerTransformer(Transformer.class.getName());
  }
  
  public String getLaunchTarget()
  {
    return Main.class.getName();
  }
  
  public String[] getLaunchArguments()
  {
    ArrayList<String> argumentList = (ArrayList)Launch.blackboard.get("ArgumentList");
    if (argumentList.isEmpty())
    {
      if (this.gameDir != null)
      {
        argumentList.add("--gameDir");
        argumentList.add(this.gameDir.getPath());
      }
      if (this.assetsDir != null)
      {
        argumentList.add("--assetsDir");
        argumentList.add(this.assetsDir.getPath());
      }
      argumentList.add("--version");
      argumentList.add(this.version);
      argumentList.addAll(this.args);
    }
    return new String[0];
  }
}
