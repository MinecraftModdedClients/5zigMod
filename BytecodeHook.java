import com.mojang.authlib.GameProfile;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.ingame.IGui2ndChat;
import eu.the5zig.mod.listener.EventListener;
import eu.the5zig.mod.listener.InventoryListener;
import eu.the5zig.mod.manager.AutoReconnectManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.SearchManager;
import eu.the5zig.mod.render.ChatSymbolsRenderer;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.render.GuiIngame;
import eu.the5zig.mod.render.PotionIndicatorRenderer;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;
import eu.the5zig.mod.util.PreciseCounter;
import eu.the5zig.mod.util.TabList;
import io.netty.buffer.ByteBuf;
import java.io.File;
import java.util.List;

public class BytecodeHook
{
  public static void onDispatchKeyPresses()
  {
    The5zigMod.getListener().dispatchKeypresses();
  }
  
  public static void onShutdown() {}
  
  public static void appendCrashCategory(Object crashReport)
  {
    ClassProxy.appendCategoryToCrashReport(crashReport);
  }
  
  public static void onDisplayCrashReport(Throwable cause, File crashFile)
  {
    ClassProxy.publishCrashReport(cause, crashFile);
  }
  
  public static void onAbstractClientPlayerInit(GameProfile gameProfile, Object instance)
  {
    ClassProxy.setupPlayerTextures(gameProfile, instance);
  }
  
  public static void onTick()
  {
    ClassProxy.renderSnow();
    The5zigMod.getListener().onRenderOverlay();
  }
  
  public static void onLeftClickMouse()
  {
    The5zigMod.getDataManager().getCpsCalculator().incrementCount();
  }
  
  public static void onChatMouseInput(int scroll)
  {
    The5zigMod.getVars().get2ndChat().scroll(scroll);
  }
  
  public static boolean onChatMouseClicked(int mouseX, int mouseY, int button)
  {
    return (The5zigMod.getRenderer().getChatSymbolsRenderer().mouseClicked(mouseX, mouseY)) || (The5zigMod.getVars().get2ndChat().mouseClicked(button));
  }
  
  public static void onChatClosed()
  {
    The5zigMod.getVars().get2ndChat().resetScroll();
  }
  
  public static void onChatKeyTyped(int key)
  {
    The5zigMod.getVars().get2ndChat().keyTyped(key);
  }
  
  public static void onChatDrawScreen(int mouseX, int mouseY)
  {
    The5zigMod.getVars().get2ndChat().drawComponentHover(mouseX, mouseY);
  }
  
  public static void onDrawChat(int updateCounter)
  {
    The5zigMod.getVars().get2ndChat().draw(updateCounter);
  }
  
  public static void onRenderGameOverlay()
  {
    The5zigMod.getGuiIngame().renderGameOverlay();
  }
  
  public static void onRenderHotbar()
  {
    The5zigMod.getGuiIngame().onRenderHotbar();
  }
  
  public static void onIngameTick()
  {
    The5zigMod.getGuiIngame().tick();
  }
  
  public static void onRenderFood()
  {
    The5zigMod.getGuiIngame().onRenderFood();
  }
  
  public static void onRenderVignette()
  {
    The5zigMod.getRenderer().getPotionIndicatorRenderer().render();
  }
  
  public static void onInsertSingleMultiplayerButton(Object instance, int i, int i2, boolean isForge)
  {
    ClassProxy.patchMainMenu(instance, i, i2, isForge);
  }
  
  public static void onMainActionPerformed(Object button)
  {
    ClassProxy.guiMainActionPerformed(button);
  }
  
  public static void onMainStatic() {}
  
  public static IButton get5zigOptionButton(Object instance)
  {
    return ClassProxy.getThe5zigModButton(instance);
  }
  
  public static void onOptionsActionPerformed(Object instance, Object button)
  {
    ClassProxy.guiOptionsActionPerformed(instance, button);
  }
  
  public static void onOptionsTick(Object instance)
  {
    ClassProxy.fixOptionButtons(instance);
  }
  
  public static void onCustomPayload(String channel, ByteBuf byteBuf)
  {
    The5zigMod.getListener().handlePluginMessage(channel, byteBuf);
  }
  
  public static ByteBuf packetBufferToByteBuf(Object packetBuffer)
  {
    return ClassProxy.packetBufferToByteBuf(packetBuffer);
  }
  
  public static void onPlayerListHeaderFooter(TabList tabList)
  {
    The5zigMod.getListener().onPlayerListHeaderFooter(tabList);
  }
  
  public static boolean onChat(String message, Object chatComponent)
  {
    return The5zigMod.getListener().onServerChat(message, chatComponent);
  }
  
  public static boolean onActionBar(String message)
  {
    return The5zigMod.getListener().onActionBar(message);
  }
  
  public static void onSetSlot(int slot, ItemStack itemStack)
  {
    The5zigMod.getDataManager().getInventoryListener().handleInventorySetSlot(slot, itemStack);
  }
  
  public static void onPlayerInfo(Object action, int ping, GameProfile gameProfile)
  {
    ClassProxy.handlePlayerInfo(action, ping, gameProfile);
  }
  
  public static void onGuiDisconnectedInit(Object parentScreen)
  {
    The5zigMod.getDataManager().getAutoReconnectManager().startCountdown(parentScreen);
  }
  
  public static void onGuiDisconnectedDraw(Object instance)
  {
    ClassProxy.handleGuiDisconnectedDraw(instance);
  }
  
  public static void onGuiConnecting(Object serverData)
  {
    ClassProxy.setServerData(serverData);
  }
  
  public static void onTitle(String title, String subTitle)
  {
    The5zigMod.getListener().onTitle(title, subTitle);
  }
  
  public static void onGuiResourcePacksInit(Object instance, List list, List list2)
  {
    ClassProxy.handleGuiResourcePackInit(instance, list, list2);
  }
  
  public static void onGuiResourcePacksClosed()
  {
    The5zigMod.getDataManager().getSearchManager().onGuiClose();
  }
  
  public static void onGuiResourcePacksDraw()
  {
    The5zigMod.getDataManager().getSearchManager().draw();
  }
  
  public static void onGuiResourcePacksKey(char character, int code)
  {
    The5zigMod.getDataManager().getSearchManager().keyTyped(character, code);
  }
  
  public static void onGuiResourcePacksMouseClicked(int mouseX, int mouseY, int button)
  {
    The5zigMod.getDataManager().getSearchManager().mouseClicked(mouseX, mouseY, button);
  }
  
  public static void onGuiMultiplayerInit(Object instance, Object serverSelectionListInstance)
  {
    ClassProxy.handleGuiMultiplayerInit(instance, serverSelectionListInstance);
  }
  
  public static void onGuiMultiplayerClosed()
  {
    The5zigMod.getDataManager().getSearchManager().onGuiClose();
  }
  
  public static void onGuiMultiplayerDraw()
  {
    The5zigMod.getDataManager().getSearchManager().draw();
  }
  
  public static void onGuiMultiplayerKey(char character, int code)
  {
    The5zigMod.getDataManager().getSearchManager().keyTyped(character, code);
  }
  
  public static void onGuiMultiplayerMouseClicked(int mouseX, int mouseY, int button)
  {
    The5zigMod.getDataManager().getSearchManager().mouseClicked(mouseX, mouseY, button);
  }
  
  public static void onGuiSelectWorldInit(Object instance, List list)
  {
    ClassProxy.handleGuiSelectWorldInit(instance, list);
  }
  
  public static void onGuiSelectWorldClosed()
  {
    The5zigMod.getDataManager().getSearchManager().onGuiClose();
  }
  
  public static void onGuiSelectWorldDraw()
  {
    The5zigMod.getDataManager().getSearchManager().draw();
  }
  
  public static void onGuiSelectWorldKey(char character, int code)
  {
    The5zigMod.getDataManager().getSearchManager().keyTyped(character, code);
  }
  
  public static void onGuiSelectWorldMouseClicked(int mouseX, int mouseY, int button)
  {
    The5zigMod.getDataManager().getSearchManager().mouseClicked(mouseX, mouseY, button);
  }
  
  public static void onRealTick()
  {
    The5zigMod.getListener().onTick();
  }
  
  public static boolean onRenderItemPerson(Object instance, Object itemStack, Object entityPlayer, Object cameraTransformType, boolean leftHand)
  {
    return ClassProxy.onRenderItemPerson(instance, itemStack, entityPlayer, cameraTransformType, leftHand);
  }
  
  public static boolean onRenderItemPerson(Object instance, Object itemStack, Object entityPlayer, Object cameraTransformType)
  {
    return ClassProxy.onRenderItemPerson(instance, itemStack, entityPlayer, cameraTransformType, false);
  }
  
  public static boolean onRenderItemInventory(Object instance, Object itemStack, int x, int y)
  {
    return ClassProxy.onRenderItemInventory(instance, itemStack, x, y);
  }
}
