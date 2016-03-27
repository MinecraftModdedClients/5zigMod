package eu.the5zig.mod;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.items.StringItem;
import eu.the5zig.mod.util.IOUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

public class I18n
{
  private static final String PRE = "lang/17/";
  private static final String MID = "language";
  private static final String END = ".properties";
  private static final Locale[] defaultLocales = { Locale.US, Locale.GERMANY, new Locale("es", "ES"), new Locale("nl", "NL"), Locale.CHINA, new Locale("pt", "BR") };
  private static List<Locale> languages = Lists.newArrayList();
  private static Locale currentLanguage;
  private static ResourceBundle resourceBundle;
  private static ResourceBundle defaultBundle;
  
  static
  {
    The5zigMod.logger.info("Loading Language files...");
    try
    {
      defaultBundle = ResourceBundle.getBundle("lang/17/language", Locale.US);
    }
    catch (MissingResourceException e)
    {
      The5zigMod.logger.error("Could not load fallback resource bundle!", e);
    }
    extractLocales();
    loadLocales();
    
    Collections.sort(languages, new Comparator()
    {
      public int compare(Locale l1, Locale l2)
      {
        return l1.toString().compareTo(l2.toString());
      }
    });
    currentLanguage = get(The5zigMod.getConfig().getString("language"));
    if (currentLanguage == null) {
      setLanguage(Locale.US);
    } else {
      loadPropertyBundle();
    }
    The5zigMod.logger.info("Loaded {} languages! Using Language {}!", new Object[] { Integer.valueOf(languages.size()), currentLanguage });
    
    checkUpdates();
  }
  
  private static Locale get(String code)
  {
    for (Locale language : languages) {
      if (language.toString().equals(code)) {
        return language;
      }
    }
    return null;
  }
  
  private static void loadResourceBundle()
  {
    The5zigMod.logger.debug("Reloading Resource Bundle {}...", new Object[] { currentLanguage });
    resourceBundle = ResourceBundle.getBundle("lang/17/language", currentLanguage);
  }
  
  private static void loadPropertyBundle()
  {
    The5zigMod.logger.debug("Reloading Property Bundle {}...", new Object[] { currentLanguage });
    FileInputStream stream = null;
    try
    {
      stream = new FileInputStream(new File("the5zigmod/" + serialize(currentLanguage)));
      resourceBundle = new PropertyResourceBundle(stream);
    }
    catch (IOException e)
    {
      The5zigMod.logger.error("Could not Load Property Resource Bundle!", e);
    }
    finally
    {
      IOUtils.closeQuietly(stream);
    }
  }
  
  public static void setLanguage(Locale locale)
  {
    String code = locale.toString();
    setLanguage(code);
  }
  
  public static void setLanguage(String code)
  {
    if (get(code) == null)
    {
      currentLanguage = Locale.US;
      loadResourceBundle();
      return;
    }
    currentLanguage = get(code);
    loadPropertyBundle();
    ((StringItem)The5zigMod.getConfig().get("language", StringItem.class)).set(code);
    The5zigMod.getConfig().save();
  }
  
  public static Locale getCurrentLanguage()
  {
    return currentLanguage;
  }
  
  public static List<Locale> getLanguages()
  {
    return Collections.unmodifiableList(languages);
  }
  
  public static String translate(String key)
  {
    if (resourceBundle != null) {
      try
      {
        return resourceBundle.getString(key);
      }
      catch (MissingResourceException localMissingResourceException) {}
    }
    if (defaultBundle != null) {
      try
      {
        return defaultBundle.getString(key);
      }
      catch (MissingResourceException localMissingResourceException1) {}
    }
    return key;
  }
  
  public static String translate(String key, Object... format)
  {
    try
    {
      return String.format(translate(key), format);
    }
    catch (IllegalFormatException ignored) {}
    return key;
  }
  
  public static boolean has(String key)
  {
    return (resourceBundle != null) && (resourceBundle.containsKey(key)) ? true : defaultBundle != null ? defaultBundle.containsKey(key) : false;
  }
  
  private static Locale deserialize(String file)
  {
    String[] args = file.split("_");
    if (args.length != 3) {
      return null;
    }
    if (!args[0].equals("language")) {
      return null;
    }
    return new Locale(args[1].toLowerCase(), args[2].toUpperCase());
  }
  
  private static String serialize(Locale locale)
  {
    return "lang/17/language_" + locale.toString() + ".properties";
  }
  
  private static void extract(Locale locale)
  {
    String file = serialize(locale);
    File destination = new File("the5zigmod/" + file);
    try
    {
      if (destination.exists()) {
        return;
      }
      The5zigMod.logger.debug("Extracting {} to {}", new Object[] { file, destination });
      org.apache.commons.io.FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream(file), destination);
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not extract File " + file + "!", e);
    }
  }
  
  private static void extractLocales()
  {
    try
    {
      for (Locale locale : defaultLocales) {
        extract(locale);
      }
    }
    catch (Throwable e)
    {
      The5zigMod.logger.warn("Could not extract Language Files! Using default Language " + Locale.US + "!", e);
      extract(Locale.US);
    }
  }
  
  public static boolean loadLocales()
  {
    File languageDir = new File("the5zigmod/lang/17/");
    File[] languages = languageDir.listFiles();
    if (languages == null) {
      return !languages.isEmpty();
    }
    boolean changed = false;
    List<Locale> folderLocales = Lists.newArrayList();
    for (File file : languages) {
      if (!file.isDirectory())
      {
        String name = file.getName();
        if (name.length() > ".properties".length())
        {
          name = name.substring(0, name.length() - ".properties".length());
          Locale locale = deserialize(name);
          if (locale != null)
          {
            folderLocales.add(locale);
            if (!languages.contains(locale))
            {
              changed = true;
              languages.add(locale);
              if (locale.equals(currentLanguage)) {
                loadPropertyBundle();
              }
            }
          }
        }
      }
    }
    Object currentLanguages = Lists.newArrayList(languages);
    ((List)currentLanguages).removeAll(folderLocales);
    if (!((List)currentLanguages).isEmpty())
    {
      languages.removeAll((Collection)currentLanguages);
      changed = true;
    }
    if ((currentLanguage != null) && (!languages.contains(currentLanguage)))
    {
      currentLanguage = (Locale)languages.get(0);
      loadPropertyBundle();
    }
    return changed;
  }
  
  private static void checkUpdates()
  {
    Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("Update language thread").build()).execute(new Runnable()
    {
      public void run()
      {
        try
        {
          json = IOUtil.download("http://5zig.eu/api/lang?version=17", false);
        }
        catch (IOException e)
        {
          String json;
          throw new RuntimeException("Could not fetch Language File updates!", e);
        }
        String json;
        Gson gson = new Gson();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonElement = (JsonArray)jsonParser.parse(json);
        List<I18n.LanguageJSON> allLanguages = Lists.newArrayList();
        for (JsonElement p : jsonElement)
        {
          I18n.LanguageJSON server = (I18n.LanguageJSON)gson.fromJson(p, I18n.LanguageJSON.class);
          allLanguages.add(server);
        }
        File languageDir = new File("the5zigmod/lang/17");
        File[] languages = languageDir.listFiles();
        if (languages == null)
        {
          The5zigMod.logger.warn("Language Directory is empty! Did something went wrong with extracting all Resource Bundles?");
          return;
        }
        boolean updated = false;
        for (I18n.LanguageJSON languageJSON : allLanguages)
        {
          File lang = null;
          for (File language : languages) {
            if (language.getName().equals(languageJSON.name))
            {
              lang = language;
              break;
            }
          }
          if (lang != null)
          {
            String md5 = null;
            try
            {
              md5 = eu.the5zig.util.io.FileUtils.md5(lang);
            }
            catch (Exception e)
            {
              The5zigMod.logger.error("Could not calculate md5 of " + lang, e);
            }
            The5zigMod.logger.debug("MD5 of Language File {} is {}", new Object[] { lang, md5 });
            if (!languageJSON.md5.equals(md5))
            {
              I18n.downloadUpdate(languageJSON.name);
              updated = true;
            }
          }
          else
          {
            updated = true;
            I18n.downloadUpdate(languageJSON.name);
          }
        }
        if (!updated) {
          The5zigMod.logger.info("All Language Files are up to date!");
        }
        I18n.loadLocales();
      }
    });
  }
  
  private static void downloadUpdate(String name)
  {
    try
    {
      String path = "http://5zig.eu/api/lang?version=17&name=" + name;
      File dest = new File("the5zigmod/lang/17", name);
      The5zigMod.logger.info("Found an Update for Language File {}. Downloading from {} to {}", new Object[] { name, path, dest });
      eu.the5zig.util.io.FileUtils.downloadToFile(path, dest);
      if (currentLanguage.equals(deserialize(name.substring(0, name.length() - ".properties".length())))) {
        loadPropertyBundle();
      }
    }
    catch (IOException e)
    {
      The5zigMod.logger.error("Could not download File " + name + "!", e);
    }
  }
  
  private static class LanguageJSON
  {
    public String name;
    public String md5;
    
    public String toString()
    {
      return "LanguageJSON{name='" + this.name + '\'' + ", md5='" + this.md5 + '\'' + '}';
    }
  }
}
