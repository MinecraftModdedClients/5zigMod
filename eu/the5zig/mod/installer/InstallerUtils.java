package eu.the5zig.mod.installer;

import eu.the5zig.util.Utils;
import eu.the5zig.util.io.FileUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class InstallerUtils
{
  public static File getMinecraftDirectory()
  {
    String userHome = System.getProperty("user.home", ".");
    File workingDirectory;
    File workingDirectory;
    File workingDirectory;
    File workingDirectory;
    switch (Utils.getPlatform())
    {
    case LINUX: 
    case SOLARIS: 
      workingDirectory = new File(userHome, ".minecraft/");
      break;
    case WINDOWS: 
      String applicationData = System.getenv("APPDATA");
      File workingDirectory;
      if (applicationData != null) {
        workingDirectory = new File(applicationData, ".minecraft/");
      } else {
        workingDirectory = new File(userHome, ".minecraft/");
      }
      break;
    case MAC: 
      workingDirectory = new File(userHome, "Library/Application Support/minecraft");
      break;
    default: 
      workingDirectory = new File(userHome, "minecraft/");
    }
    return workingDirectory;
  }
  
  public static void checkMD5(File thisFile, String checkPath)
  {
    try
    {
      String md5 = FileUtils.md5(thisFile);
      System.out.println("Calculated MD5: " + md5);
      String url = "http://5zig.eu/md5/" + checkPath;
      String checksum = Utils.downloadFile(url, 2500);
      if (checksum == null)
      {
        System.out.println("Could not download checksum! Continuing installation at own risk!");
        return;
      }
      System.out.println("Checksum: " + checksum);
      if (!md5.equals(checksum))
      {
        System.out.println("Calculated MD5-String does not match checksum from " + url + "!");
        int installWithMods = JOptionPane.showOptionDialog(null, "The File you downloaded could not be verified as secure!\nThis means that this Modification has been probably modified by another person.\nDo you still want to continue installation?", "Error", 0, 3, Images.iconImage, new String[] { "Yes, I understand the risks.", "No, Exit Installer." }, "default");
        if (installWithMods == 1) {
          System.exit(0);
        }
      }
      else
      {
        System.out.println("Checksum does match calculated md5 String!");
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public static void exitWithException(Throwable throwable, String modVersion, String minecraftVersion)
  {
    throwable.printStackTrace();
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    throwable.printStackTrace(ps);
    String content = baos.toString();
    JTextArea textArea = new JTextArea("The 5zig Mod v" + modVersion, 20, 100);
    textArea.setText("An error occured while installing The 5zig Mod!" + 
      Utils.lineSeparator() + "Please contact info@5zig.eu and paste the contents of this window!" + Utils.lineSeparator(3) + content + 
      seperator() + "User Info:" + Utils.lineSeparator() + "Version:\t" + minecraftVersion + "_" + modVersion + Utils.lineSeparator() + "OS Name:\t" + 
      Utils.getOSName() + Utils.lineSeparator() + "Java Version:\t" + Utils.getJava() + Utils.lineSeparator() + "User Home:\t" + System.getProperty("user.home") + 
      seperator());
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    textArea.setCaretPosition(0);
    textArea.setEditable(false);
    
    JOptionPane.showMessageDialog(null, new JScrollPane(textArea), "The 5zig Mod", -1);
    System.exit(1);
  }
  
  private static String seperator()
  {
    StringBuilder sb = new StringBuilder(Utils.lineSeparator());
    for (int i = 0; i < 100; i++) {
      sb.append("=");
    }
    return Utils.lineSeparator();
  }
}
