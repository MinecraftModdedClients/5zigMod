package eu.the5zig.mod.util;

import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import eu.the5zig.mod.asm.Transformer;
import eu.the5zig.util.Utils;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.logging.log4j.Logger;

public class TrayManager
{
  private final boolean isTraySupported;
  private SystemTray tray;
  private TrayIcon trayIcon;
  private Image minecraftIcon;
  private boolean trayInitialized = false;
  
  public TrayManager()
  {
    this.isTraySupported = SystemTray.isSupported();
    if (this.isTraySupported)
    {
      Utils.setUI(UIManager.getSystemLookAndFeelClassName());
      this.tray = SystemTray.getSystemTray();
      loadMinecraftIcon();
      create();
    }
    else
    {
      MinecraftFactory.getClassProxyCallback().getLogger().warn("System Tray is NOT Supported!");
    }
  }
  
  private void loadMinecraftIcon()
  {
    if (this.minecraftIcon != null) {
      return;
    }
    try
    {
      Class<?> minecraft = Thread.currentThread().getContextClassLoader().loadClass(Names.minecraft.getName());
      InputStream is;
      InputStream is;
      if (Transformer.FORGE)
      {
        Object minecraftInstance = minecraft.getMethod("func_71410_x", new Class[0]).invoke(null, new Object[0]);
        Field resourcePackField = minecraft.getDeclaredField("field_110450_ap");
        resourcePackField.setAccessible(true);
        Object resourceManager = resourcePackField.get(minecraftInstance);
        is = (InputStream)resourceManager.getClass().getMethod("func_152780_c", new Class[] { Thread.currentThread().getContextClassLoader().loadClass(Names.resourceLocation.getName()) }).invoke(resourceManager, new Object[] {
          MinecraftFactory.getVars().createResourceLocation("icons/icon_16x16.png") });
      }
      else
      {
        Object minecraftInstance = minecraft.getMethod("z", new Class[0]).invoke(null, new Object[0]);
        Field aB = minecraft.getDeclaredField("aE");
        aB.setAccessible(true);
        Object resourceManager = aB.get(minecraftInstance);
        is = (InputStream)resourceManager.getClass().getMethod("c", new Class[] { Class.forName(Names.resourceLocation.getName()) }).invoke(resourceManager, new Object[] {MinecraftFactory.getVars()
          .createResourceLocation("icons/icon_16x16.png") });
      }
      this.minecraftIcon = ImageIO.read(is);
      
      this.trayIcon = new TrayIcon(this.minecraftIcon, "The 5zig Mod - " + MinecraftFactory.getVars().getUsername());
      
      PopupMenu popupMenu = new PopupMenu();
      MenuItem about = new MenuItem("About");
      about.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          JOptionPane.showMessageDialog(null, "This Tray Icon is used by The 5zig Mod to notify you, when you receive a new Message.", "Minecraft 1.9", -1, null);
        }
      });
      popupMenu.add(about);
      popupMenu.addSeparator();
      MenuItem disable = new MenuItem("Disable");
      disable.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          MinecraftFactory.getClassProxyCallback().disableTray();
          
          TrayManager.this.destroy();
        }
      });
      popupMenu.add(disable);
      this.trayIcon.setPopupMenu(popupMenu);
    }
    catch (Exception e)
    {
      MinecraftFactory.getClassProxyCallback().getLogger().warn("Could not load Minecraft Icon!", e);
    }
  }
  
  public void create()
  {
    if ((!this.isTraySupported) || (this.trayInitialized) || (!MinecraftFactory.getClassProxyCallback().isTrayEnabled()) || (this.minecraftIcon == null)) {
      return;
    }
    MinecraftFactory.getClassProxyCallback().getLogger().info("Setting up Tray Icon...");
    try
    {
      this.tray.add(this.trayIcon);
      this.trayInitialized = true;
    }
    catch (Exception e)
    {
      MinecraftFactory.getClassProxyCallback().getLogger().warn("Could not Setup Tray Icon!", e);
    }
  }
  
  public boolean isTraySupported()
  {
    return this.isTraySupported;
  }
  
  public void displayMessage(String title, String message)
  {
    if (this.trayInitialized) {
      this.trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
    }
  }
  
  public void destroy()
  {
    if (this.trayInitialized)
    {
      this.tray.remove(this.trayIcon);
      this.trayInitialized = false;
    }
  }
}
