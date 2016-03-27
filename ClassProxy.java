import com.mojang.authlib.GameProfile;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import eu.the5zig.mod.asm.Transformer;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.manager.SearchEntry;
import eu.the5zig.mod.util.ClassProxyCallback;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import io.netty.buffer.ByteBuf;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

public class ClassProxy
{
  private static final Field byteBuf;
  private static Field buttonList;
  private static final Field serverList;
  private static final Field worldData;
  private static final Field worldDataList;
  private static boolean tryFix = false;
  
  static
  {
    try
    {
      Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(Names.packetBuffer.getName());
      byteBuf = Transformer.FORGE ? c.getDeclaredField("field_150794_a") : c.getDeclaredField("a");
      byteBuf.setAccessible(true);
      if (Transformer.FORGE)
      {
        buttonList = Thread.currentThread().getContextClassLoader().loadClass(Names.guiScreen.getName()).getDeclaredField("field_146292_n");
        buttonList.setAccessible(true);
        
        serverList = bgv.class.getDeclaredField("field_148198_l");
        worldData = bhn.class.getDeclaredField("field_186786_g");
        worldDataList = bho.class.getDeclaredField("field_186799_w");
      }
      else
      {
        serverList = bgv.class.getDeclaredField("v");
        worldData = bhn.class.getDeclaredField("g");
        worldDataList = bho.class.getDeclaredField("w");
      }
      serverList.setAccessible(true);
      worldData.setAccessible(true);
      worldDataList.setAccessible(true);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public static void patchMainMenu(Object instance, int paramInt1, int paramInt2, boolean isForge)
  {
    bfi guiMainMenu = (bfi)instance;
    if (!MinecraftFactory.getClassProxyCallback().isShowLastServer()) {
      return;
    }
    String server = MinecraftFactory.getClassProxyCallback().getLastServer();
    int x = guiMainMenu.l / 2 + 2;
    int y = paramInt1 + paramInt2;
    IButton button;
    IButton button;
    if (server != null)
    {
      String[] parts = server.split(":");
      String host = parts[(parts.length - 2)];
      int port = Integer.parseInt(parts[(parts.length - 1)]);
      String lastServer;
      if (port == 25565) {
        lastServer = host;
      } else {
        lastServer = host + ":" + port;
      }
      String lastServer = MinecraftFactory.getVars().shortenToWidth(lastServer, 88);
      button = MinecraftFactory.getVars().createButton(99, x, y, 98, 20, lastServer);
    }
    else
    {
      button = MinecraftFactory.getVars().createButton(98, x, y, 98, 20, MinecraftFactory.getClassProxyCallback().translate("menu.no_last_server", new Object[0]));
    }
    List<bcz> list;
    if (Transformer.FORGE) {
      try
      {
        list = (List)buttonList.get(guiMainMenu);
      }
      catch (Exception e)
      {
        List<bcz> list;
        throw new RuntimeException(e);
      }
    } else {
      list = guiMainMenu.n;
    }
    list.add((bcz)button);
    for (bcz b : list)
    {
      int id = b.k;
      if (id == 2) {
        b.f = 98;
      }
    }
  }
  
  public static void guiMainActionPerformed(Object b)
  {
    bcz button = (bcz)b;
    if (button.k == 99)
    {
      String server = MinecraftFactory.getClassProxyCallback().getLastServer();
      if (server == null) {
        return;
      }
      String[] parts = server.split(":");
      String host = parts[(parts.length - 2)];
      int port = Integer.parseInt(parts[(parts.length - 1)]);
      MinecraftFactory.getVars().joinServer(host, port);
    }
  }
  
  public static IButton getThe5zigModButton(Object instance)
  {
    bfb guiScreen = (bfb)instance;
    return MinecraftFactory.getVars().createButton(42, guiScreen.l / 2 - 155, guiScreen.m / 6 + 24 - 6, 150, 20, MinecraftFactory.getClassProxyCallback().translate("menu.the5zigMod", new Object[0]));
  }
  
  public static void guiOptionsActionPerformed(Object instance, Object b)
  {
    bfb guiScreen = (bfb)instance;
    bcz button = (bcz)b;
    if (button.k == 42) {
      MinecraftFactory.getClassProxyCallback().displayGuiSettings(new WrappedGui(guiScreen));
    }
  }
  
  public static IButton getMCPVPButton(bfb guiScreen)
  {
    return MinecraftFactory.getVars().createButton(9, guiScreen.l / 2 - 23, guiScreen.m - 28, 46, 20, MinecraftFactory.getClassProxyCallback().translate("menu.mcpvp", new Object[0]));
  }
  
  public static void guiMultiplayerActionPerformed(bfb guiScreen, bcz button)
  {
    if (button.k == 9) {}
  }
  
  public static void setupPlayerTextures(GameProfile gameProfile, Object instance)
  {
    ((Variables)MinecraftFactory.getVars()).getResourceManager().loadPlayerTextures(gameProfile, instance);
  }
  
  public static boolean onRenderItemPerson(Object instance, Object itemStackObject, Object entityPlayerObject, Object cameraTransformTypeObject, boolean leftHand)
  {
    return ((Variables)MinecraftFactory.getVars()).getResourceManager().renderInPersonMode(instance, itemStackObject, entityPlayerObject, cameraTransformTypeObject, leftHand);
  }
  
  public static boolean onRenderItemInventory(Object instance, Object itemStackObject, int x, int y)
  {
    return ((Variables)MinecraftFactory.getVars()).getResourceManager().renderInInventory(instance, itemStackObject, x, y);
  }
  
  public static ByteBuf packetBufferToByteBuf(Object packetBuffer)
  {
    try
    {
      return (ByteBuf)byteBuf.get(packetBuffer);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public static void handlePlayerInfo(Object packetPlayerInfoAction, int ping, GameProfile gameProfile)
  {
    boolean action = packetPlayerInfoAction == gz.a.c;
    MinecraftFactory.getClassProxyCallback().handlePlayerInfo(action, ping, gameProfile);
  }
  
  public static void appendCategoryToCrashReport(Object crashReport)
  {
    ((b)crashReport).g().a("The 5zig Mod Version", "3.5.3");
    ((b)crashReport).g().a("Forge", Boolean.valueOf(Transformer.FORGE));
  }
  
  public static void publishCrashReport(Throwable cause, File crashFile)
  {
    MinecraftFactory.getClassProxyCallback().launchCrashHopper(cause, crashFile);
  }
  
  public static void handleGuiDisconnectedDraw(Object instance)
  {
    bep gui = (bep)instance;
    MinecraftFactory.getClassProxyCallback().checkAutoreconnectCountdown(gui.l, gui.m);
  }
  
  public static void setServerData(Object serverData)
  {
    String host = ((bkx)serverData).b;
    if (!"5zig.eu".equalsIgnoreCase(host)) {
      MinecraftFactory.getClassProxyCallback().setAutoreconnectServerData(serverData);
    } else {
      MinecraftFactory.getClassProxyCallback().setAutoreconnectServerData(null);
    }
  }
  
  public static void fixOptionButtons(Object instance)
  {
    if (!tryFix) {
      return;
    }
    bfb guiScreen = (bfb)instance;
    tryFix = false;
    List<bcz> list;
    if (Transformer.FORGE) {
      try
      {
        list = (List)buttonList.get(guiScreen);
      }
      catch (Exception e)
      {
        List<bcz> list;
        throw new RuntimeException(e);
      }
    } else {
      list = guiScreen.n;
    }
    for (bcz button : list) {
      if ((button.k != 42) && (button.h == guiScreen.l / 2 - 155) && (button.i == guiScreen.m / 6 + 24 - 6))
      {
        button.h = (guiScreen.l / 2 + 5);
        button.f = 150;
      }
    }
  }
  
  public static void handleGuiResourcePackInit(Object instance, List list, List list2)
  {
    bgy gui = (bgy)instance;
    Comparator comparator = new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        if ((!(o1 instanceof bhb)) || (!(o2 instanceof bhb))) {
          return 0;
        }
        bhb resourcePackListEntryFound1 = (bhb)o1;
        bhb resourcePackListEntryFound2 = (bhb)o2;
        return resourcePackListEntryFound1.l().d().toLowerCase().compareTo(resourcePackListEntryFound2.l().d().toLowerCase());
      }
    };
    MinecraftFactory.getClassProxyCallback().addSearch(new SearchEntry(
      MinecraftFactory.getVars().createTextfield(MinecraftFactory.getClassProxyCallback().translate("gui.search", new Object[0]), 9991, gui.l / 2 - 200, gui.m - 70, 170, 16), list, comparator)
      
      new SearchEntry
      {
        public boolean filter(String text, Object o)
        {
          if (!(o instanceof bhb)) {
            return true;
          }
          bhb resourcePackListEntryFound = (bhb)o;
          return (resourcePackListEntryFound.l().d().toLowerCase().contains(text.toLowerCase())) || (resourcePackListEntryFound.l().e().toLowerCase().contains(text.toLowerCase()));
        }
      }, new SearchEntry[] { new SearchEntry(
      
      MinecraftFactory.getVars().createTextfield(MinecraftFactory.getClassProxyCallback().translate("gui.search", new Object[0]), 9992, gui.l / 2 + 8, gui.m - 70, 170, 16), list2, new Comparator()
      {
        public int compare(Object o1, Object o2)
        {
          return 0;
        }
      })
      {
        public boolean filter(String text, Object o)
        {
          if (!(o instanceof bhb)) {
            return true;
          }
          bhb resourcePackListEntryFound = (bhb)o;
          return (resourcePackListEntryFound.l().d().toLowerCase().contains(text.toLowerCase())) || (resourcePackListEntryFound.l().e().toLowerCase().contains(text.toLowerCase()));
        }
        
        protected int getAddIndex()
        {
          return 1;
        }
      } });
  }
  
  public static void handleGuiMultiplayerInit(Object instance, Object serverSelectionListInstance)
  {
    bgr guiMultiplayer = (bgr)instance;
    bgv serverSelectionList = (bgv)serverSelectionListInstance;
    try
    {
      list = (List)serverList.get(serverSelectionList);
    }
    catch (Exception e)
    {
      List list;
      throw new RuntimeException(e);
    }
    List list;
    ITextfield textfield = MinecraftFactory.getVars().createTextfield(MinecraftFactory.getClassProxyCallback().translate("gui.search", new Object[0]), 9991, (guiMultiplayer.l - 305) / 2 + 6, guiMultiplayer.m - 84, 170, 16);
    
    final SearchEntry searchEntry = new SearchEntry(textfield, list)
    {
      public boolean filter(String text, Object o)
      {
        bgu serverListEntry = (bgu)o;
        return (serverListEntry.a().a.toLowerCase().contains(text.toLowerCase())) || (serverListEntry.a().b.toLowerCase().contains(text.toLowerCase()));
      }
    };
    Callback enterCallback = new Callback()
    {
      public void call(Object callback)
      {
        this.val$guiMultiplayer.b(0);
        this.val$guiMultiplayer.f();
        searchEntry.reset();
      }
    };
    searchEntry.setEnterCallback(enterCallback);
    MinecraftFactory.getClassProxyCallback().addSearch(searchEntry, new SearchEntry[0]);
  }
  
  public static void handleGuiSelectWorldInit(Object instance, List l)
  {
    bhm guiSelectWorld = (bhm)instance;
    bho guiList = (bho)l.get(0);
    try
    {
      list = (List)worldDataList.get(guiList);
    }
    catch (IllegalAccessException e)
    {
      List<bhn> list;
      throw new RuntimeException(e);
    }
    List<bhn> list;
    ITextfield textfield = MinecraftFactory.getVars().createTextfield(MinecraftFactory.getClassProxyCallback().translate("gui.search", new Object[0]), 9991, (guiSelectWorld.l - 220) / 2 + 6, guiSelectWorld.m - 84, 170, 16);
    
    SearchEntry searchEntry = new SearchEntry(textfield, list)
    {
      public boolean filter(String text, Object o)
      {
        bhn saveFormatComparator = (bhn)o;
        try
        {
          azl worldData = (azl)ClassProxy.worldData.get(saveFormatComparator);
          return (worldData.a().toLowerCase().contains(text.toLowerCase())) || (worldData.b().toLowerCase().contains(text.toLowerCase()));
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    };
    searchEntry.setEnterCallback(new Callback()
    {
      public void call(Object callback)
      {
        this.val$guiList.d(0);
        bhn f = this.val$guiList.f();
        if (f != null) {
          f.a();
        }
      }
    });
    searchEntry.setComparator(new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        bhn saveFormatComparator1 = (bhn)o1;
        bhn saveFormatComparator2 = (bhn)o2;
        try
        {
          azl worldData1 = (azl)ClassProxy.worldData.get(saveFormatComparator1);
          azl worldData2 = (azl)ClassProxy.worldData.get(saveFormatComparator2);
          return (int)(worldData2.e() - worldData1.e());
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    });
    MinecraftFactory.getClassProxyCallback().addSearch(searchEntry, new SearchEntry[0]);
  }
  
  public static void renderSnow()
  {
    if ((MinecraftFactory.getVars().getMinecraftScreen() instanceof bfh)) {
      MinecraftFactory.getClassProxyCallback().renderSnow(((bfi)MinecraftFactory.getVars().getMinecraftScreen()).l, ((bfi)MinecraftFactory.getVars().getMinecraftScreen()).m);
    }
  }
}
