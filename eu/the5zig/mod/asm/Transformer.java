package eu.the5zig.mod.asm;

import eu.the5zig.mod.asm.transformers.PatchAbstractClientPlayer;
import eu.the5zig.mod.asm.transformers.PatchGameSettings;
import eu.the5zig.mod.asm.transformers.PatchGuiAchievement;
import eu.the5zig.mod.asm.transformers.PatchGuiChat;
import eu.the5zig.mod.asm.transformers.PatchGuiConnecting;
import eu.the5zig.mod.asm.transformers.PatchGuiDisconnected;
import eu.the5zig.mod.asm.transformers.PatchGuiIngame;
import eu.the5zig.mod.asm.transformers.PatchGuiIngameForge;
import eu.the5zig.mod.asm.transformers.PatchGuiMainMenu;
import eu.the5zig.mod.asm.transformers.PatchGuiMultiplayer;
import eu.the5zig.mod.asm.transformers.PatchGuiOptions;
import eu.the5zig.mod.asm.transformers.PatchGuiResourcePacks;
import eu.the5zig.mod.asm.transformers.PatchGuiScreen;
import eu.the5zig.mod.asm.transformers.PatchGuiSelectWorld;
import eu.the5zig.mod.asm.transformers.PatchMinecraft;
import eu.the5zig.mod.asm.transformers.PatchNetHandlerPlayClient;
import eu.the5zig.mod.asm.transformers.PatchNetworkManager;
import eu.the5zig.mod.asm.transformers.PatchRenderItem;
import java.io.PrintStream;
import java.util.HashMap;
import net.minecraft.launchwrapper.IClassTransformer;

public class Transformer
  implements IClassTransformer
{
  public static boolean FORGE = false;
  public static HashMap<String, IClassTransformer> obfNames = new HashMap();
  
  static
  {
    obfNames.put(Names.minecraft.getName(), new PatchMinecraft());
    obfNames.put(Names.guiMainMenu.getName(), new PatchGuiMainMenu());
    obfNames.put(Names.guiIngame.getName(), new PatchGuiIngame());
    obfNames.put(Names.guiIngameForge.getName(), new PatchGuiIngameForge());
    obfNames.put(Names.guiAchievement.getName(), new PatchGuiAchievement());
    obfNames.put(Names.gameSettings.getName(), new PatchGameSettings());
    obfNames.put(Names.netHandlerPlayClient.getName(), new PatchNetHandlerPlayClient());
    obfNames.put(Names.guiScreen.getName(), new PatchGuiScreen());
    obfNames.put(Names.guiOptions.getName(), new PatchGuiOptions());
    obfNames.put(Names.abstractClientPlayer.getName(), new PatchAbstractClientPlayer());
    obfNames.put(Names.guiMultiplayer.getName(), new PatchGuiMultiplayer());
    obfNames.put(Names.networkManager.getName(), new PatchNetworkManager());
    obfNames.put(Names.guiChat.getName(), new PatchGuiChat());
    obfNames.put(Names.guiConnecting.getName(), new PatchGuiConnecting());
    obfNames.put(Names.guiDisconnected.getName(), new PatchGuiDisconnected());
    obfNames.put(Names.guiResourcePacks.getName(), new PatchGuiResourcePacks());
    obfNames.put(Names.guiSelectWorld.getName(), new PatchGuiSelectWorld());
    obfNames.put(Names.renderItem.getName(), new PatchRenderItem());
  }
  
  public byte[] transform(String className, String arg1, byte[] bytes)
  {
    if (!obfNames.containsKey(className)) {
      return bytes;
    }
    try
    {
      return ((IClassTransformer)obfNames.get(className)).transform(className, arg1, bytes);
    }
    catch (Exception e)
    {
      System.err.println("[5zig] Failed to Transform Class " + className + "!");
      e.printStackTrace();
    }
    return bytes;
  }
}
