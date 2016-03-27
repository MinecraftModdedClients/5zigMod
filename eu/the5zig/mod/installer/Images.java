package eu.the5zig.mod.installer;

import java.io.PrintStream;
import javax.swing.ImageIcon;

public class Images
{
  public static ImageIcon iconImage;
  public static ImageIcon backgroundImage;
  public static ImageIcon installImage;
  public static ImageIcon installHoverImage;
  public static ImageIcon installDisabledImage;
  public static ImageIcon installBox;
  
  public static void load()
  {
    System.out.println("Loading Images...");
    iconImage = new ImageIcon(Images.class.getResource("/images/5zig-icon.png"));
    backgroundImage = new ImageIcon(Images.class.getResource("/images/background.jpg"));
    installImage = new ImageIcon(Images.class.getResource("/images/install.png"));
    installHoverImage = new ImageIcon(Images.class.getResource("/images/install_hover.png"));
    installDisabledImage = new ImageIcon(Images.class.getResource("/images/install_disabled.png"));
    installBox = new ImageIcon(Images.class.getResource("/images/install-box.png"));
  }
}
