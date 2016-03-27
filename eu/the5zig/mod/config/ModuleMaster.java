package eu.the5zig.mod.config;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.modules.Module.RenderType;
import eu.the5zig.mod.modules.items.Item.Color;
import eu.the5zig.mod.modules.items.RegisteredItem;
import eu.the5zig.util.AsyncExecutor;
import eu.the5zig.util.minecraft.ChatColor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

public class ModuleMaster
{
  private final Gson builder = new GsonBuilder().setPrettyPrinting().create();
  private File file;
  private final List<Module> modules = Lists.newArrayList();
  private final List<String> newAvailableItems = Lists.newArrayList();
  private final List<String> activeItems = Lists.newArrayList();
  
  public ModuleMaster(File parent)
  {
    this.file = new File(parent, "modules.json");
    try
    {
      if (this.file.exists()) {
        loadModules();
      } else {
        createDefault();
      }
    }
    catch (Exception e)
    {
      The5zigMod.logger.warn("Error loading modules!", e);
      try
      {
        FileUtils.moveFile(this.file, new File(parent, "modules.old.json"));
        createDefault();
      }
      catch (Exception e1)
      {
        The5zigMod.logger.error("Could not create default modules file!", e1);
      }
    }
  }
  
  private JsonElement parseJson(String json)
    throws Exception
  {
    try
    {
      JsonParser parser = new JsonParser();
      return parser.parse(json);
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not parse json!");
      throw e;
    }
  }
  
  private JsonObject getDefaultRoot()
    throws Exception
  {
    try
    {
      String json = CharStreams.toString(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("core/modules.json")));
      element = parseJson(json);
    }
    catch (Exception e)
    {
      JsonElement element;
      The5zigMod.logger.error("Could not load default module data!");
      throw e;
    }
    JsonElement element;
    String json;
    return element.getAsJsonObject();
  }
  
  private ModuleData parse(JsonObject root)
  {
    List<String> defaultModules = Lists.newArrayList();
    JsonArray defaultModulesArray = root.get("defaultModules").getAsJsonArray();
    for (JsonElement element : defaultModulesArray) {
      defaultModules.add(element.getAsString());
    }
    Object availableItems = Lists.newArrayList();
    JsonArray availableItemsArray;
    if (root.has("availableItems"))
    {
      availableItemsArray = root.get("availableItems").getAsJsonArray();
      for (JsonElement element : availableItemsArray) {
        ((List)availableItems).add(element.getAsString());
      }
    }
    else
    {
      for (RegisteredItem registeredItem : eu.the5zig.mod.modules.items.Item.getRegisteredItems()) {
        ((List)availableItems).add(registeredItem.getKey());
      }
    }
    return new ModuleData(defaultModules, (List)availableItems);
  }
  
  private void loadModules()
    throws Exception
  {
    try
    {
      json = FileUtils.readFileToString(this.file);
    }
    catch (Exception e)
    {
      String json;
      The5zigMod.logger.error("Could not load module file " + this.file + "!");
      throw e;
    }
    try
    {
      String json;
      element = parseJson(json);
    }
    catch (Exception e)
    {
      JsonElement element;
      The5zigMod.logger.error("Could not parse json of " + this.file + "!");
      throw e;
    }
    try
    {
      JsonElement element;
      JsonObject root = element.getAsJsonObject();
      modulesArray = root.get("modules").getAsJsonArray();
    }
    catch (Exception e)
    {
      JsonArray modulesArray;
      The5zigMod.logger.error("Could not parse module list!");
      throw e;
    }
    JsonArray modulesArray;
    JsonObject root;
    this.modules.clear();
    this.activeItems.clear();
    try
    {
      JsonObject defaultRoot = getDefaultRoot();
      ModuleData fileData = parse(root);
      ModuleData resourceData = parse(defaultRoot);
      for (Iterator localIterator1 = resourceData.getDefaultModules().iterator(); localIterator1.hasNext();)
      {
        defaultModule = (String)localIterator1.next();
        if (!fileData.getDefaultModules().contains(defaultModule)) {
          for (JsonElement moduleElement : defaultRoot.getAsJsonArray("modules"))
          {
            JsonObject moduleObject = moduleElement.getAsJsonObject();
            if ((moduleObject.has("id")) && (defaultModule.equals(moduleObject.get("id").getAsString()))) {
              modulesArray.add(moduleObject);
            }
          }
        }
      }
      String defaultModule;
      for (localIterator1 = resourceData.getAvailableItems().iterator(); localIterator1.hasNext();)
      {
        availableItem = (String)localIterator1.next();
        if (!fileData.getAvailableItems().contains(availableItem))
        {
          tryCopyItem(availableItem, defaultRoot, modulesArray);
          this.newAvailableItems.add(availableItem);
        }
      }
      String availableItem;
      if (!this.newAvailableItems.isEmpty()) {
        The5zigMod.logger.info("Found " + this.newAvailableItems.size() + " new available module item(s)!");
      }
      root.add("defaultModules", defaultRoot.get("defaultModules"));
      JsonArray availableItems = new JsonArray();
      for (RegisteredItem registeredItem : eu.the5zig.mod.modules.items.Item.getRegisteredItems()) {
        availableItems.add(new JsonPrimitive(registeredItem.getKey()));
      }
      root.add("availableItems", availableItems);
    }
    catch (Exception e)
    {
      The5zigMod.logger.warn("Could not parse default module file!", e);
    }
    for (JsonElement moduleElement : modulesArray)
    {
      try
      {
        JsonObject moduleObject = moduleElement.getAsJsonObject();
        moduleId = moduleObject.get("id").getAsString();
      }
      catch (Exception e)
      {
        String moduleId;
        The5zigMod.logger.error("Could not load unknown module!", e);
      }
      continue;
      try
      {
        JsonObject moduleObject;
        this.modules.add(parseModule(moduleObject, moduleId));
      }
      catch (Exception e)
      {
        String moduleId;
        The5zigMod.logger.error("Could not load module with id \"" + moduleId + "\"!", e);
      }
    }
    FileWriter writer;
    try
    {
      String toJson = this.builder.toJson(root);
      writer = new FileWriter(this.file);
      writer.write(toJson);
      writer.close();
    }
    catch (IOException e)
    {
      The5zigMod.logger.warn("Could not update Modules!", e);
    }
    int items = 0;
    for (Module module : this.modules) {
      items += module.getItems().size();
    }
    The5zigMod.logger.info("Loaded " + this.modules.size() + " modules containing " + items + " items!");
  }
  
  private void tryCopyItem(String availableItem, JsonObject defaultRoot, JsonArray modulesArray)
  {
    if (!defaultRoot.has("modules")) {
      return;
    }
    for (JsonElement defaultModuleElement : defaultRoot.getAsJsonArray("modules"))
    {
      defaultModuleObject = defaultModuleElement.getAsJsonObject();
      if (defaultModuleObject.has("items")) {
        for (JsonElement defaultItemElement : defaultModuleObject.getAsJsonArray("items"))
        {
          defaultItemObject = defaultItemElement.getAsJsonObject();
          if ((defaultItemObject.has("type")) && (defaultItemObject.get("type").getAsString().equals(availableItem))) {
            for (JsonElement moduleElement : modulesArray)
            {
              JsonObject moduleObject = moduleElement.getAsJsonObject();
              if (moduleObject.get("id").getAsString().equals(defaultModuleObject.get("id").getAsString()))
              {
                moduleObject.getAsJsonArray("items").add(defaultItemObject); return;
              }
            }
          }
        }
      }
    }
    JsonObject defaultModuleObject;
    JsonObject defaultItemObject;
  }
  
  private Module parseModule(JsonObject moduleObject, String moduleId)
  {
    String moduleName = moduleObject.has("name") ? moduleObject.get("name").getAsString() : null;
    String moduleTranslation = moduleObject.has("translation") ? moduleObject.get("translation").getAsString() : null;
    
    float locationX = 0.0F;float locationY = 0.0F;
    ConfigNew.Location location;
    try
    {
      String locationString = moduleObject.get("location").getAsString();
      ConfigNew.Location location = ConfigNew.Location.valueOf(locationString);
      if (location == ConfigNew.Location.CUSTOM) {
        try
        {
          locationX = moduleObject.get("locationX").getAsFloat();
          locationY = moduleObject.get("locationY").getAsFloat();
        }
        catch (Exception e)
        {
          The5zigMod.logger.error("Could not parse location \"" + locationString + "\" for module with id \"" + moduleId + "\"!", e);
          locationX = 0.0F;
          locationY = 0.0F;
        }
      }
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not parse location for module with id \"" + moduleId + "\"!", e);
      location = ConfigNew.Location.TOP_LEFT;
    }
    String server = moduleObject.has("server") ? moduleObject.get("server").getAsString() : null;
    boolean showLabel = (!moduleObject.has("showLabel")) || (moduleObject.get("showLabel").getAsBoolean());
    Module module = new Module(moduleId, moduleName, moduleTranslation, server, showLabel, location, locationX, locationY);
    Module.RenderType renderType = null;
    try
    {
      if (moduleObject.has("render")) {
        renderType = Module.RenderType.valueOf(moduleObject.get("render").getAsString());
      }
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not parse render type!", e);
    }
    module.setRenderType(renderType);
    
    JsonArray items = moduleObject.get("items").getAsJsonArray();
    for (JsonElement jsonElement : items)
    {
      JsonObject itemObject = jsonElement.getAsJsonObject();
      String typeString = itemObject.get("type").getAsString();
      RegisteredItem registeredItem = eu.the5zig.mod.modules.items.Item.byKey(typeString);
      if (registeredItem == null) {
        The5zigMod.logger.error("Could not parse item type \"" + typeString + "\"!");
      } else {
        try
        {
          module.addItem(parseItem(itemObject, registeredItem));
          this.activeItems.add(registeredItem.getKey());
        }
        catch (Exception e)
        {
          The5zigMod.logger.error("Could not load item \"" + typeString + "\"!", e);
        }
      }
    }
    return module;
  }
  
  private eu.the5zig.mod.modules.items.Item parseItem(JsonObject itemObject, RegisteredItem registeredItem)
  {
    try
    {
      item = eu.the5zig.mod.modules.items.Item.create(registeredItem);
    }
    catch (Exception e)
    {
      eu.the5zig.mod.modules.items.Item item;
      The5zigMod.logger.error("Could not parse item type \"" + registeredItem.getKey() + "\"!", e);
      return null;
    }
    eu.the5zig.mod.modules.items.Item item;
    try
    {
      parseSettings(item, itemObject);
      if (itemObject.has("color")) {
        try
        {
          JsonObject colorObject = itemObject.get("color").getAsJsonObject();
          ChatColor prefixFormatting = colorObject.has("prefixFormatting") ? ChatColor.valueOf(colorObject.get("prefixFormatting").getAsString()) : null;
          ChatColor prefixColor = colorObject.has("prefixColor") ? ChatColor.valueOf(colorObject.get("prefixColor").getAsString()) : null;
          ChatColor mainFormatting = colorObject.has("mainFormatting") ? ChatColor.valueOf(colorObject.get("mainFormatting").getAsString()) : null;
          ChatColor mainColor = colorObject.has("mainColor") ? ChatColor.valueOf(colorObject.get("mainColor").getAsString()) : null;
          item.setColor(new Item.Color(prefixFormatting, prefixColor, mainFormatting, mainColor));
        }
        catch (Exception e)
        {
          The5zigMod.logger.error("Could not parse color for item \"" + registeredItem.getKey() + "\"!", e);
        }
      }
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not parse type \"" + registeredItem.getKey() + "\"", e);
    }
    return item;
  }
  
  private void parseSettings(eu.the5zig.mod.modules.items.Item item, JsonObject itemObject)
  {
    JsonArray settingsArray = null;
    if (itemObject.has("settings")) {
      settingsArray = itemObject.get("settings").getAsJsonArray();
    }
    List<eu.the5zig.mod.config.items.Item> missingSettings = Lists.newArrayList();
    for (Iterator localIterator1 = item.getSettings().iterator(); localIterator1.hasNext();)
    {
      configItem = (eu.the5zig.mod.config.items.Item)localIterator1.next();
      if (settingsArray != null)
      {
        boolean contains = false;
        try
        {
          for (JsonElement settingsElement : settingsArray)
          {
            JsonObject settingsObject = settingsElement.getAsJsonObject();
            String settingsName = settingsObject.get("name").getAsString();
            if (configItem.getKey().equals(settingsName))
            {
              JsonObject settingsValue = settingsObject.get("value").getAsJsonObject();
              try
              {
                configItem.deserialize(settingsValue);
              }
              catch (Exception e)
              {
                The5zigMod.logger.warn("Could not deserialize setting " + settingsName + "!", e);
                configItem.reset();
                configItem.serialize(settingsValue);
              }
              contains = true;
              break;
            }
          }
        }
        catch (Exception e)
        {
          The5zigMod.logger.warn("Could not parse setting \"" + configItem.getKey() + "\" for item \"" + eu.the5zig.mod.modules.items.Item.byItem(item.getClass()) + "\"!", e);
        }
        if (!contains)
        {
          configItem.reset();
          missingSettings.add(configItem);
        }
      }
      else
      {
        configItem.reset();
        missingSettings.add(configItem);
      }
    }
    eu.the5zig.mod.config.items.Item configItem;
    if (!missingSettings.isEmpty())
    {
      boolean wasNull = settingsArray == null;
      if (settingsArray == null) {
        settingsArray = new JsonArray();
      }
      for (eu.the5zig.mod.config.items.Item missingSetting : missingSettings) {
        try
        {
          JsonObject missingElement = new JsonObject();
          missingElement.addProperty("name", missingSetting.getKey());
          JsonObject missingValue = new JsonObject();
          missingSetting.serialize(missingValue);
          missingElement.add("value", missingValue);
          settingsArray.add(missingElement);
        }
        catch (Exception e)
        {
          The5zigMod.logger.warn("Could not add missing setting \"" + missingSetting.getKey() + "\" for item \"" + eu.the5zig.mod.modules.items.Item.byItem(item.getClass()) + "\"");
        }
      }
      if (wasNull) {
        itemObject.add("settings", settingsArray);
      }
    }
  }
  
  public void createDefault()
    throws Exception
  {
    FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream("core/modules.json"), this.file);
    The5zigMod.logger.info("Created default module config!");
    loadModules();
  }
  
  public void save()
  {
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        try
        {
          JsonObject root = new JsonObject();
          JsonArray moduleArray = new JsonArray();
          ModuleMaster.this.activeItems.clear();
          List<Module> modules = Lists.newArrayList(ModuleMaster.this.modules);
          for (Module module : modules)
          {
            moduleObject = new JsonObject();
            moduleObject.addProperty("id", module.getId());
            if (module.getName() != null) {
              moduleObject.addProperty("name", module.getName());
            }
            if (module.getTranslation() != null) {
              moduleObject.addProperty("translation", module.getTranslation());
            }
            if (module.getServer() != null) {
              moduleObject.addProperty("server", module.getServer());
            }
            moduleObject.addProperty("showLabel", Boolean.valueOf(module.isShowLabel()));
            moduleObject.addProperty("location", module.getLocation().toString());
            if (module.getLocationX() != 0.0F) {
              moduleObject.addProperty("locationX", Float.valueOf(module.getLocationX()));
            }
            if (module.getLocationY() != 0.0F) {
              moduleObject.addProperty("locationY", Float.valueOf(module.getLocationY()));
            }
            if (module.getRenderType() != null) {
              moduleObject.addProperty("render", module.getRenderType().toString());
            }
            JsonArray items = new JsonArray();
            for (eu.the5zig.mod.modules.items.Item item : module.getItems())
            {
              ModuleMaster.this.activeItems.add(eu.the5zig.mod.modules.items.Item.byItem(item.getClass()).getKey());
              
              JsonObject itemObject = new JsonObject();
              itemObject.addProperty("type", eu.the5zig.mod.modules.items.Item.byItem(item.getClass()).getKey());
              JsonArray settingsArray = new JsonArray();
              for (eu.the5zig.mod.config.items.Item setting : item.getSettings())
              {
                JsonObject settingElement = new JsonObject();
                settingElement.addProperty("name", setting.getKey());
                JsonObject settingValue = new JsonObject();
                setting.serialize(settingValue);
                settingElement.add("value", settingValue);
                settingsArray.add(settingElement);
              }
              if (settingsArray.size() != 0) {
                itemObject.add("settings", settingsArray);
              }
              if (item.getColor() != null)
              {
                JsonObject colorObject = new JsonObject();
                if (item.getColor().prefixFormatting != null) {
                  colorObject.addProperty("prefixFormatting", item.getColor().prefixFormatting.name());
                }
                if (item.getColor().prefixColor != null) {
                  colorObject.addProperty("prefixColor", item.getColor().prefixColor.name());
                }
                if (item.getColor().mainFormatting != null) {
                  colorObject.addProperty("mainFormatting", item.getColor().mainFormatting.name());
                }
                if (item.getColor().mainColor != null) {
                  colorObject.addProperty("mainColor", item.getColor().mainColor.name());
                }
                itemObject.add("color", colorObject);
              }
              items.add(itemObject);
            }
            moduleObject.add("items", items);
            moduleArray.add(moduleObject);
          }
          JsonObject moduleObject;
          root.add("modules", moduleArray);
          JsonObject defaultRoot = ModuleMaster.this.getDefaultRoot();
          root.add("defaultModules", defaultRoot.get("defaultModules"));
          JsonArray availableItems = new JsonArray();
          for (RegisteredItem registeredItem : eu.the5zig.mod.modules.items.Item.getRegisteredItems()) {
            availableItems.add(new JsonPrimitive(registeredItem.getKey()));
          }
          root.add("availableItems", availableItems);
          
          String json = ModuleMaster.this.builder.toJson(root);
          FileWriter writer = new FileWriter(ModuleMaster.this.file);
          writer.write(json);
          writer.close();
        }
        catch (Exception e)
        {
          The5zigMod.logger.warn("Could not save modules!", e);
        }
      }
    });
  }
  
  public List<Module> getModules()
  {
    return this.modules;
  }
  
  public boolean isItemActive(String key)
  {
    return this.activeItems.contains(key);
  }
}
