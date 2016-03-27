package eu.the5zig.util;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.UIManager;

public class Utils
{
  private static final Pattern INCREMENTAL_PATTERN = Pattern.compile("(?:(https?://[^ ][^ ]*?)(?=[\\.\\?!,;:]?(?:[ \\n]|$)))", 2);
  
  public static void appendToFile(String text, File file)
  {
    try
    {
      if ((!file.exists()) && (!file.createNewFile())) {
        throw new IOException("Could not create File at path: " + file.getPath());
      }
      Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getPath()), "UTF-8"));
      out.append(text).append('\n');
      out.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  public static String downloadFile(String path)
  {
    return downloadFile(path, 5000);
  }
  
  public static String downloadFile(String path, int timeout)
  {
    BufferedReader br = null;
    InputStreamReader isr = null;
    try
    {
      StringBuilder buffer = new StringBuilder();
      URL url = new URL(path);
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setConnectTimeout(timeout);
      connection.setReadTimeout(timeout);
      connection.connect();
      
      int code = connection.getResponseCode();
      if (code == 200) {
        isr = new InputStreamReader(connection.getInputStream(), "UTF-8");
      } else {
        isr = new InputStreamReader(connection.getErrorStream());
      }
      br = new BufferedReader(isr);
      String line;
      while ((line = br.readLine()) != null) {
        buffer.append(line);
      }
      String str1;
      if (code == 200) {
        return buffer.toString();
      }
      System.err.println("Could not download string! Error code " + code + "!");
      return buffer.toString();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.err.println("Could not download string!");
    }
    finally
    {
      if (br != null) {
        try
        {
          br.close();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
      }
      if (isr != null) {
        try
        {
          isr.close();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
      }
    }
    return null;
  }
  
  public static Integer versionCompare(String str1, String str2)
  {
    String[] vals1 = str1.split("\\.");
    String[] vals2 = str2.split("\\.");
    int i = 0;
    while ((i < vals1.length) && (i < vals2.length) && (vals1[i].equals(vals2[i]))) {
      i++;
    }
    if ((i < vals1.length) && (i < vals2.length))
    {
      int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
      return Integer.valueOf(Integer.signum(diff));
    }
    return Integer.valueOf(Integer.signum(vals1.length - vals2.length));
  }
  
  public static void setUI(String className)
  {
    try
    {
      UIManager.setLookAndFeel(className);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public static File getRunningJar()
  {
    try
    {
      return new File(Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
    }
    catch (Exception e) {}
    return null;
  }
  
  public static String convertStreamToString(InputStream is)
    throws IOException
  {
    return new BufferedReader(new InputStreamReader(is)).readLine();
  }
  
  public static String getShortenedDouble(double value, int decimals)
  {
    if (decimals == 0) {
      return String.valueOf((int)Math.round(value));
    }
    double l = 1.0D;
    for (int i = 0; i < decimals; i++) {
      l *= 10.0D;
    }
    return String.valueOf((value * l) / l);
  }
  
  public static String getShortenedDouble(double value)
  {
    return getShortenedDouble(value, 2);
  }
  
  public static String getShortenedFloat(float value, int decimals)
  {
    if (decimals == 0) {
      return String.valueOf(Math.round(value));
    }
    float l = 1.0F;
    for (int i = 0; i < decimals; i++) {
      l = (float)(l * 10.0D);
    }
    return String.valueOf(Math.round(value * l) / l);
  }
  
  public static String getShortenedFloat(float value)
  {
    return getShortenedFloat(value, 2);
  }
  
  public static UUID getUUID(String uuid)
  {
    return UUID.fromString(uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32));
  }
  
  public static String getUUIDWithoutDashes(UUID uuid)
  {
    return uuid.toString().replace("-", "");
  }
  
  public static String upperToDash(String input)
  {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++)
    {
      char c = input.charAt(i);
      if (Character.isUpperCase(c))
      {
        if ((i == 0) || (input.charAt(i - 1) == '_')) {
          result.append(Character.toLowerCase(c));
        } else {
          result.append("_").append(Character.toLowerCase(c));
        }
      }
      else {
        result.append(c);
      }
    }
    return result.toString();
  }
  
  public static int closest(int of, List<Integer> in)
  {
    int min = Integer.MAX_VALUE;
    int closest = of;
    for (Iterator localIterator = in.iterator(); localIterator.hasNext();)
    {
      int v = ((Integer)localIterator.next()).intValue();
      int diff = Math.abs(v - of);
      if (diff < min)
      {
        min = diff;
        closest = v;
      }
    }
    return closest;
  }
  
  public static float clamp(float f1, float f2, float f3)
  {
    return f1 > f3 ? f3 : f1 < f2 ? f2 : f1;
  }
  
  public static String convertToDate(long millis)
  {
    Date dateTime = new Date(millis);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateTime);
    Calendar today = Calendar.getInstance();
    Calendar yesterday = Calendar.getInstance();
    yesterday.add(5, -1);
    DateFormat timeFormatter = DateFormat.getTimeInstance(3);
    if ((calendar.get(1) == today.get(1)) && (calendar.get(6) == today.get(6))) {
      return "Today " + timeFormatter.format(dateTime);
    }
    if ((calendar.get(1) == yesterday.get(1)) && (calendar.get(6) == yesterday.get(6))) {
      return "Yesterday " + timeFormatter.format(dateTime);
    }
    timeFormatter = DateFormat.getDateTimeInstance(3, 3);
    return timeFormatter.format(dateTime);
  }
  
  public static String convertToDateWithoutTime(long millis)
  {
    Date dateTime = new Date(millis);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateTime);
    Calendar today = Calendar.getInstance();
    Calendar yesterday = Calendar.getInstance();
    yesterday.add(5, -1);
    if ((calendar.get(1) == today.get(1)) && (calendar.get(6) == today.get(6))) {
      return "Today";
    }
    if ((calendar.get(1) == yesterday.get(1)) && (calendar.get(6) == yesterday.get(6))) {
      return "Yesterday";
    }
    DateFormat timeFormatter = DateFormat.getDateInstance(3);
    return timeFormatter.format(dateTime);
  }
  
  public static boolean isSameDay(long millis1, long millis2)
  {
    Date dateTime1 = new Date(millis1);
    Date dateTime2 = new Date(millis2);
    Calendar calendar1 = Calendar.getInstance();
    calendar1.setTime(dateTime1);
    Calendar calendar2 = Calendar.getInstance();
    calendar2.setTime(dateTime2);
    return (calendar1.get(1) == calendar2.get(1)) && (calendar1.get(6) == calendar2.get(6));
  }
  
  public static String convertToTime(long millis)
  {
    return String.format("%02d:%02d:%02d (HH:MM:SS)", new Object[] { Long.valueOf(TimeUnit.MILLISECONDS.toHours(millis)), 
      Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))), 
      Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))) });
  }
  
  public static String convertToClock(long millis)
  {
    return String.format("%02d:%02d", new Object[] { Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(millis)), 
      Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))) });
  }
  
  public static String convertToTimeWithMinutes(long millis)
  {
    Date dateTime = new Date(millis);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateTime);
    DateFormat timeFormatter = DateFormat.getTimeInstance(3);
    return timeFormatter.format(dateTime);
  }
  
  public static String convertToTimeWithDays(long millis)
  {
    return String.format("%d:%02d:%02d:%02d (DD:HH:MM:SS)", new Object[] { Long.valueOf(TimeUnit.MILLISECONDS.toDays(millis)), 
      Long.valueOf(TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis))), 
      Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))), 
      Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))) });
  }
  
  public static long parseTimeFormatToMillis(String time)
  {
    return parseTimeFormatToMillis(time, "HH:mm:ss");
  }
  
  public static long parseTimeFormatToMillis(String time, String format)
  {
    try
    {
      SimpleDateFormat sdf = new SimpleDateFormat(format);
      Date date = sdf.parse(time);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      int hour = calendar.get(10);
      int minute = calendar.get(12);
      int second = calendar.get(13);
      return 1000 * second + 60000 * minute + 3600000 * hour;
    }
    catch (ParseException e)
    {
      e.printStackTrace();
    }
    return -1L;
  }
  
  public static void openURL(String path)
  {
    try
    {
      Desktop.getDesktop().browse(new URI(path));
    }
    catch (Throwable e)
    {
      System.err.println("Couldn't open url: " + path);
      e.printStackTrace();
    }
  }
  
  public static void openURL(URI uri)
  {
    try
    {
      Desktop.getDesktop().browse(uri);
    }
    catch (Throwable e)
    {
      System.err.println("Couldn't open url: " + uri.toString());
      e.printStackTrace();
    }
  }
  
  public static void openURLIfFound(String input)
  {
    List<String> matchedURLs = matchURL(input);
    if (matchedURLs.isEmpty()) {
      return;
    }
    for (String matchedURL : matchedURLs) {
      openURL(matchedURL);
    }
  }
  
  public static List<String> matchURL(String input)
  {
    List<String> result = new ArrayList();
    Matcher matcher = INCREMENTAL_PATTERN.matcher(input);
    while (matcher.find()) {
      result.add(matcher.group());
    }
    return result;
  }
  
  public static boolean matches(String message, String match)
  {
    message = message.toLowerCase();
    Pattern pattern = compileMatchPattern(match);
    Matcher matcher = pattern.matcher(message);
    
    return matcher.matches();
  }
  
  public static boolean contains(String message, String find)
  {
    message = message.toLowerCase();
    Pattern pattern = compileMatchPattern(find);
    Matcher matcher = pattern.matcher(message);
    
    return matcher.find();
  }
  
  public static Pattern compileMatchPattern(String match)
  {
    match = match.toLowerCase();
    
    match = match.replaceAll("(\\*)\\1+", "*");
    
    match = match.replaceAll("(?<!\\\\)(\\\\\\\\)+(?!\\\\)", "*");
    
    match = match.replaceAll("(?<!\\\\)[?]*[*][*?]+", "*");
    
    match = match.replaceAll("(?<!\\\\)([|\\[\\]{}(),.^$+-])", "\\\\$1");
    
    match = match.replaceAll("(?<!\\\\)[?]", ".");
    
    match = match.replaceAll("(?<!\\\\)[*]", ".*");
    return Pattern.compile(match);
  }
  
  public static String escapeStringForRegex(String string)
  {
    string = string.replaceAll("(?<!\\\\)([|\\[\\]{}(),.^$+-]\\?*)", "\\\\$1");
    
    return string;
  }
  
  public static String substringFrom(String input, String from)
  {
    if (!input.contains(from)) {
      return input;
    }
    int substring = input.indexOf(from);
    return input.substring(substring);
  }
  
  public static String bytesToReadable(long bytes)
  {
    int unit = 1024;
    if (bytes < 1024L) {
      return bytes + " B";
    }
    int exp = (int)(Math.log(bytes) / Math.log(1024.0D));
    char pre = "KMGTPE".charAt(exp - 1);
    return String.format("%.1f %sB", new Object[] { Double.valueOf(bytes / Math.pow(1024.0D, exp)), Character.valueOf(pre) });
  }
  
  public static boolean isInt(CharSequence cs)
  {
    if ((cs == null) || (cs.length() == 0)) {
      return false;
    }
    int sz = cs.length();
    for (int i = 0; i < sz; i++)
    {
      char currentChar = cs.charAt(i);
      if ((currentChar != '-') || (i != 0) || (sz <= 1)) {
        if (!Character.isDigit(currentChar)) {
          return false;
        }
      }
    }
    return true;
  }
  
  public static String loadJson(File fileJson)
  {
    try
    {
      BufferedReader reader = new BufferedReader(new FileReader(fileJson));
      
      StringBuilder sb = new StringBuilder();
      String str;
      while ((str = reader.readLine()) != null) {
        sb.append(str).append("\n");
      }
      reader.close();
      return sb.toString();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  public static int getARBGInt(int a, int r, int g, int b)
  {
    return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
  }
  
  public static String getOSName()
  {
    return System.getProperty("os.name");
  }
  
  public static String getJava()
  {
    return System.getProperty("java.version");
  }
  
  public static String lineSeparator()
  {
    return System.getProperty("line.separator");
  }
  
  public static String lineSeparator(int count)
  {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < count; i++) {
      result.append(lineSeparator());
    }
    return result.toString();
  }
  
  public static Platform getPlatform()
  {
    String osName = getOSName().toLowerCase();
    if (osName.contains("win")) {
      return Platform.WINDOWS;
    }
    if (osName.contains("mac")) {
      return Platform.MAC;
    }
    if ((osName.contains("linux")) || (osName.contains("sunos")) || (osName.contains("unix"))) {
      return Platform.LINUX;
    }
    if (osName.contains("solaris")) {
      return Platform.SOLARIS;
    }
    return Platform.UNKNOWN;
  }
  
  public static <T> T[] concat(T[] first, T[] second)
  {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
  
  public static <T> T[] asArray(T... array)
  {
    return array;
  }
  
  public static enum Platform
  {
    WINDOWS,  MAC,  LINUX,  SOLARIS,  UNKNOWN;
    
    private Platform() {}
  }
}
