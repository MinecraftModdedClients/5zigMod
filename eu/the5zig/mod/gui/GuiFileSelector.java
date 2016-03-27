package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IFileSelector;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.util.FileSelectorCallback;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import java.io.File;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

public class GuiFileSelector
  extends Gui
{
  private IFileSelector fileSelector;
  private FileSelectorCallback callback;
  private final File startDirectory;
  private String[] allowedExtensions;
  private int tabIndex = -1;
  private List<String> fileNames = Lists.newArrayList();
  
  public GuiFileSelector(Gui lastScreen, FileSelectorCallback callback, String... extensions)
  {
    this(lastScreen, callback, new File(System.getProperty("user.home", "Desktop")), extensions);
  }
  
  public GuiFileSelector(Gui lastScreen, FileSelectorCallback callback, File startDirectory, String... extensions)
  {
    super(lastScreen);
    this.callback = callback;
    this.startDirectory = startDirectory;
    this.allowedExtensions = extensions;
  }
  
  public void initGui()
  {
    this.fileSelector = The5zigMod.getVars().createFileSelector(this.startDirectory, getWidth(), getHeight(), getWidth() / 2 - 150, getWidth() / 2 + 160, 60, getHeight() / 6 + 150, new Callback()
    {
      public void call(File callback)
      {
        GuiFileSelector.this.selectFile(callback);
      }
    });
    ITextfield textfield = The5zigMod.getVars().createTextfield(1, getWidth() / 2 - 150, 38, 250, 18, 200);
    textfield.setSelected(false);
    textfield.setText(this.fileSelector.getCurrentDir().getAbsolutePath() + "\\");
    addTextField(textfield);
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 + 105, 37, 50, 20, I18n.translate("file_selector.open")));
    addButton(The5zigMod.getVars().createIconButton(The5zigMod.ITEMS, 16, 0, 2, getWidth() / 2 - 172, 61));
    
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 - 152, getHeight() / 6 + 168, 150, 20, The5zigMod.getVars().translate("gui.cancel", new Object[0])));
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 + 2, getHeight() / 6 + 168, 150, 20, I18n.translate("file_selector.select")));
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      ITextfield textfield = getTextfieldById(1);
      if (textfield.getText().isEmpty())
      {
        this.fileSelector.updateDir(null);
      }
      else
      {
        File file = new File(textfield.getText());
        if ((!file.exists()) || (!file.isDirectory())) {
          return;
        }
        this.fileSelector.updateDir(file);
      }
    }
    if (button.getId() == 2) {
      this.fileSelector.goUp();
    }
    if (button.getId() == 100) {
      selectFile(this.fileSelector.getSelectedFile());
    }
  }
  
  private void selectFile(File selectedFile)
  {
    if (selectedFile == null) {
      return;
    }
    if (!selectedFile.isFile())
    {
      this.fileSelector.updateDir(selectedFile);
      return;
    }
    boolean allow = false;
    String name = FilenameUtils.getExtension(this.fileSelector.getSelectedFile().getName());
    for (String extension : this.allowedExtensions) {
      if (name.equalsIgnoreCase(extension))
      {
        allow = true;
        break;
      }
    }
    if (!allow) {
      return;
    }
    The5zigMod.getVars().displayScreen(this.lastScreen);
    this.callback.onDone(selectedFile);
  }
  
  protected void tick()
  {
    boolean enable = this.fileSelector.getSelectedFile() != null;
    if ((this.fileSelector.getSelectedFile() != null) && (this.fileSelector.getSelectedFile().isFile()))
    {
      enable = false;
      String name = FilenameUtils.getExtension(this.fileSelector.getSelectedFile().getName());
      for (String extension : this.allowedExtensions) {
        if (name.equalsIgnoreCase(extension))
        {
          enable = true;
          break;
        }
      }
      getButtonById(100).setLabel(I18n.translate("file_selector.select"));
    }
    else
    {
      getButtonById(100).setLabel(I18n.translate("file_selector.open"));
    }
    getButtonById(100).setEnabled(enable);
    
    ITextfield textfield = getTextfieldById(1);
    if (!textfield.isFocused())
    {
      File currentDir = this.fileSelector.getCurrentDir();
      if (currentDir == null) {
        textfield.setText("");
      } else {
        textfield.setText(currentDir.getAbsolutePath());
      }
    }
  }
  
  protected void onKeyType(char character, int key)
  {
    if (key == 15)
    {
      ITextfield textfield = getTextfieldById(1);
      if (this.tabIndex != -1)
      {
        this.tabIndex += 1;
        String string = (String)this.fileNames.get(this.tabIndex % this.fileNames.size());
        textfield.setText(string);
      }
      else
      {
        this.fileNames.clear();
        this.tabIndex = -1;
        File currentFile = new File(textfield.getText());
        File currentDir = textfield.getText().endsWith("\\") ? currentFile : currentFile.getParentFile();
        if (currentDir == null) {
          return;
        }
        File[] files = currentDir.listFiles();
        if (files == null) {
          return;
        }
        String text = textfield.getText();
        for (File file : files) {
          if ((file.isDirectory()) && (file.getAbsolutePath().toLowerCase().startsWith(text.toLowerCase()))) {
            this.fileNames.add(file.getAbsolutePath());
          }
        }
        if (!this.fileNames.isEmpty())
        {
          this.tabIndex += 1;
          String string = (String)this.fileNames.get(this.tabIndex % this.fileNames.size());
          textfield.setText(string);
          if (this.fileNames.size() == 1) {
            this.tabIndex = -1;
          }
        }
      }
    }
    else
    {
      this.tabIndex = -1;
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawMenuBackground();
    this.fileSelector.draw(mouseX, mouseY, partialTicks);
  }
  
  protected void handleMouseInput()
  {
    this.fileSelector.handleMouseInput();
  }
  
  public String getTitleName()
  {
    return this.callback.getTitle();
  }
}
