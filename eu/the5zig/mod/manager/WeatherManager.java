package eu.the5zig.mod.manager;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.listener.Listener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

public class WeatherManager
  extends Listener
{
  private static final String URL_PATH = "https://weather.yahoo.com";
  private boolean previousWeatherEnabled;
  private long lastChecked;
  private WeatherData weatherData;
  
  public void onTick()
  {
    if (this.previousWeatherEnabled != The5zigMod.getConfig().getBool("renderWeather"))
    {
      this.previousWeatherEnabled = (!this.previousWeatherEnabled);
      if (this.previousWeatherEnabled) {
        this.lastChecked = 0L;
      }
    }
    if ((System.currentTimeMillis() - this.lastChecked > 0L) && (this.previousWeatherEnabled)) {
      checkWeather();
    }
  }
  
  private void checkWeather()
  {
    this.lastChecked = (System.currentTimeMillis() + 1800000L);
    new Thread("Weather-Data")
    {
      public void run()
      {
        WeatherManager.this.getWeather();
      }
    }.start();
  }
  
  private void getWeather()
  {
    The5zigMod.logger.info("Checking for weather...");
    
    HttpsURLConnection connection = null;
    InputStream inputStream = null;
    BufferedReader reader = null;
    try
    {
      URL url = new URL("https://weather.yahoo.com");
      connection = (HttpsURLConnection)url.openConnection();
      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        throw new IOException("Illegal response code received.");
      }
      inputStream = connection.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inputStream));
      
      this.weatherData = new WeatherData();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("<span class=\"name\">")) {
          this.weatherData.setCity(line.split("<span class=\"name\">|</span>")[1]);
        } else if (line.contains("<div class=\"region\">")) {
          this.weatherData.setCountry(line.split("<div class=\"region\">|</div>")[1]);
        } else if (line.contains("<div class=\"cond")) {
          this.weatherData.setCondition(line.split("\">|</div>")[1]);
        } else if (line.contains("<span class=\"f\"><span class=\"num\">")) {
          this.weatherData.setFahrenheit(Integer.parseInt(line.split("<span class=\"num\">|</span>")[1]));
        } else if (line.contains("<span class=\"c\"><span class=\"num\">")) {
          this.weatherData.setCelsius(Integer.parseInt(line.split("<span class=\"num\">|</span>")[1]));
        }
      }
      reader.close();
      inputStream.close();
      The5zigMod.logger.info("Got new weather data!");
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not fetch weather!", e);
      this.lastChecked = (System.currentTimeMillis() + 60000L);
    }
    finally
    {
      if (connection != null) {
        connection.disconnect();
      }
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(inputStream);
    }
  }
  
  public WeatherData getWeatherData()
  {
    return this.weatherData;
  }
}
