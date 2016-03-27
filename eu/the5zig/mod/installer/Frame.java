package eu.the5zig.mod.installer;

import eu.the5zig.util.Utils;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Frame
  extends JFrame
  implements ActionListener
{
  private File installDirectory;
  private JProgressBar progressBar;
  private JLabel cancellbl;
  private JButton cancel;
  private JLabel installlbl;
  private JButton install;
  private JLabel changeDirlbl;
  private JButton changeDir;
  private JLabel text;
  private InstallerNew installer;
  private final File thisFile;
  private static String minecraftVersion = "Unknown";
  private static String modVersion = "Unknown";
  private static final long serialVersionUID = -4340832085024558089L;
  
  public Frame()
    throws IOException, IllegalAccessException
  {
    Utils.setUI(UIManager.getSystemLookAndFeelClassName());
    
    this.thisFile = Utils.getRunningJar();
    if (this.thisFile == null) {
      throw new IOException("Could not find current file!");
    }
    modVersion = "3.5.3";
    minecraftVersion = "1.9";
    System.out.println("Loading " + InstallerNew.getVersionName(minecraftVersion) + "...");
    
    JFrame loading = new JFrame("Loading");
    loading.setSize(240, 40);
    loading.setUndecorated(true);
    loading.setDefaultCloseOperation(3);
    loading.setLocationRelativeTo(null);
    JLabel label = new JLabel("Loading The 5zig Mod v" + modVersion + "...", 0);
    label.setBounds(0, 0, 240, 40);
    loading.add(label);
    loading.setVisible(true);
    
    Images.load();
    InstallerUtils.checkMD5(this.thisFile, minecraftVersion + "/" + modVersion.replace(" ", "%20"));
    loading.dispose();
    this.installDirectory = InstallerUtils.getMinecraftDirectory();
    init();
  }
  
  public static void main(String[] args)
  {
    try
    {
      new Frame();
    }
    catch (Throwable throwable)
    {
      InstallerUtils.exitWithException(throwable, modVersion, minecraftVersion);
    }
  }
  
  private void init()
  {
    setTitle("The 5zig Mod v" + modVersion);
    setSize(500, 350);
    setLocationRelativeTo(null);
    setResizable(false);
    setDefaultCloseOperation(3);
    setIconImage(Images.iconImage.getImage());
    setContentPane(new JLabel(Images.backgroundImage));
    
    JLabel title = new JLabel("The 5zig Mod v" + modVersion, 0);
    title.setBounds(0, 10, 500, 35);
    title.setFont(new Font("Verdana", 1, 26));
    add(title);
    
    JLabel subTitle = new JLabel("for Minecraft " + minecraftVersion, 0);
    subTitle.setBounds(0, 36, 500, 35);
    subTitle.setFont(new Font("Verdana", 1, 20));
    add(subTitle);
    
    this.text = new JLabel("", 0);
    this.text.setBounds(40, 50, 420, 170);
    this.text.setFont(new Font("Helvetica", 1, 18));
    this.text.setText("<html>Welcome to the 5zig Mod installer!<br><br>Click on \"Install\" to install the mod! Select optionally other mods to install them together.<br>More information on <u>https://www.5zig.eu</u></html>");
    
    add(this.text);
    
    this.changeDir = new JButton(Images.installBox);
    this.changeDir.setBounds(30, 220, 430, 30);
    this.changeDir.setBorderPainted(false);
    this.changeDir.setFocusPainted(false);
    this.changeDir.setContentAreaFilled(false);
    this.changeDirlbl = new JLabel("Installing to " + this.installDirectory.getAbsolutePath(), 0);
    this.changeDirlbl.setBounds(35, 220, 420, 30);
    this.changeDirlbl.setFont(new Font("Arial", 0, 16));
    this.changeDirlbl.setForeground(Color.lightGray);
    this.changeDirlbl.addMouseListener(new MouseAdapter()
    {
      public void mouseEntered(MouseEvent e)
      {
        Frame.this.changeDirlbl.setText("Click to change...");
        Frame.this.changeDirlbl.setFont(new Font("Arial", 1, 16));
      }
      
      public void mouseExited(MouseEvent e)
      {
        Frame.this.changeDirlbl.setText("Installing to " + Frame.this.installDirectory.getAbsolutePath());
        Frame.this.changeDirlbl.setFont(new Font("Arial", 0, 16));
      }
      
      public void mouseClicked(MouseEvent e)
      {
        JFileChooser chooser = new JFileChooser(Frame.this.installDirectory);
        chooser.setDialogTitle("Select Minecraft Installation Directory");
        chooser.setFileSelectionMode(1);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(null) == 0)
        {
          Frame.this.installDirectory = chooser.getSelectedFile();
          Frame.this.changeDirlbl.setText("Installing to " + Frame.this.installDirectory.getAbsolutePath());
          Frame.this.changeDirlbl.setFont(new Font("Arial", 0, 16));
        }
      }
    });
    add(this.changeDirlbl);
    add(this.changeDir);
    
    this.install = new JButton(Images.installImage);
    this.install.setRolloverIcon(Images.installHoverImage);
    this.install.setRolloverSelectedIcon(Images.installHoverImage);
    this.install.setPressedIcon(Images.installHoverImage);
    this.install.setSelectedIcon(Images.installHoverImage);
    this.install.setDisabledIcon(Images.installDisabledImage);
    this.install.setBounds(30, 260, 200, 40);
    this.install.setBorderPainted(false);
    this.install.setFocusPainted(false);
    this.install.setContentAreaFilled(false);
    this.install.addActionListener(this);
    this.installlbl = new JLabel("Install", 0);
    this.installlbl.setBounds(30, 260, 200, 40);
    this.installlbl.setFont(new Font("Verdana", 0, 20));
    this.installlbl.setForeground(Color.WHITE);
    add(this.installlbl);
    add(this.install);
    
    this.cancel = new JButton(Images.installImage);
    this.cancel.setRolloverIcon(Images.installHoverImage);
    this.cancel.setRolloverSelectedIcon(Images.installHoverImage);
    this.cancel.setPressedIcon(Images.installHoverImage);
    this.cancel.setSelectedIcon(Images.installHoverImage);
    this.cancel.setDisabledIcon(Images.installDisabledImage);
    this.cancel.setBounds(260, 260, 200, 40);
    this.cancel.setBorderPainted(false);
    this.cancel.setFocusPainted(false);
    this.cancel.setContentAreaFilled(false);
    this.cancel.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        System.exit(0);
      }
    });
    this.cancellbl = new JLabel("Cancel", 0);
    this.cancellbl.setBounds(260, 260, 200, 40);
    this.cancellbl.setFont(new Font("Verdana", 0, 20));
    this.cancellbl.setForeground(Color.WHITE);
    add(this.cancellbl);
    add(this.cancel);
    
    this.progressBar = new JProgressBar();
    this.progressBar.setBounds(20, 210, 450, 20);
    this.progressBar.setVisible(false);
    add(this.progressBar);
    
    setVisible(true);
  }
  
  public void actionPerformed(ActionEvent e)
  {
    try
    {
      final InstallerNew installer = new InstallerNew(this.installDirectory, modVersion, minecraftVersion);
      this.install.setEnabled(false);
      this.cancel.setEnabled(false);
      this.text.setText("Waiting.");
      this.changeDir.setVisible(false);
      this.changeDirlbl.setVisible(false);
      
      final ProcessCallback callback = new ProcessCallback()
      {
        public void progress(float percentage)
        {
          Frame.this.progressBar.setValue((int)(percentage * 100.0D));
        }
        
        public void message(String message)
        {
          Frame.this.text.setText(message);
          System.out.println(" ");
          System.out.println(message);
        }
      };
      int installWithMods = JOptionPane.showOptionDialog(null, "Do you want to install the 5zig Mod with another Mod?\nYou can select multiple Files as well.", "The 5zig Mod v" + modVersion, 0, 3, Images.iconImage, new String[] { "Yes", "No" }, "default");
      if (installWithMods == 0)
      {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home") + File.separator + "Downloads");
        chooser.setMultiSelectionEnabled(true);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("ZIP and JAR Archives", new String[] { "zip", "jar" });
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == 0) {
          installer.setOtherMods(chooser.getSelectedFiles());
        }
      }
      this.text.setText("Installing... Please wait.");
      this.progressBar.setVisible(true);
      new Thread(new Runnable()
      {
        public void run()
        {
          long start = System.currentTimeMillis();
          try
          {
            installer.install(callback);
          }
          catch (MinecraftNotFoundException e)
          {
            JOptionPane.showMessageDialog(null, "Minecraft version not found: " + 
              Frame.minecraftVersion + ".\nPlease start Minecraft " + Frame.minecraftVersion + " manually via the Minecraft Launcher!", "The 5zig Mod v" + 
              Frame.modVersion, 0, Images.iconImage);
            System.exit(1);
          }
          catch (Exception e)
          {
            InstallerUtils.exitWithException(e, Frame.modVersion, Frame.minecraftVersion);
          }
          Frame.this.text.setText("Done.");
          System.out.println();
          System.out.println("===========================================");
          System.out.println("Finished Installation after " + (System.currentTimeMillis() - start) + "ms.");
          System.out.println("===========================================");
          JOptionPane.showMessageDialog(null, "The 5zig Mod has been successfully installed.", "The 5zig Mod v" + Frame.modVersion, 1, Images.iconImage);
          Frame.this.dispose();
        }
      })
      
        .start();
    }
    catch (MinecraftNotFoundException ex)
    {
      JOptionPane.showMessageDialog(null, "Minecraft version not found: " + minecraftVersion + ".\nPlease start Minecraft " + minecraftVersion + " manually via the Minecraft Launcher!", "The 5zig Mod v" + modVersion, 0, Images.iconImage);
      
      System.exit(1);
    }
    catch (Exception ex)
    {
      throw new RuntimeException("Could not access Mod & Minecraft Version!", ex);
    }
  }
}
