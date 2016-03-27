package eu.the5zig.mod.manager;

public class WeatherData
{
  private String city;
  private String country;
  private String condition;
  private int celsius;
  private int fahrenheit;
  
  public String getCity()
  {
    return this.city;
  }
  
  public void setCity(String city)
  {
    this.city = city;
  }
  
  public String getCountry()
  {
    return this.country;
  }
  
  public void setCountry(String country)
  {
    this.country = country;
  }
  
  public String getCondition()
  {
    return this.condition;
  }
  
  public void setCondition(String condition)
  {
    this.condition = condition;
  }
  
  public int getCelsius()
  {
    return this.celsius;
  }
  
  public void setCelsius(int celsius)
  {
    this.celsius = celsius;
  }
  
  public int getFahrenheit()
  {
    return this.fahrenheit;
  }
  
  public void setFahrenheit(int fahrenheit)
  {
    this.fahrenheit = fahrenheit;
  }
  
  public String toString()
  {
    return "WeatherData{city='" + this.city + '\'' + ", country='" + this.country + '\'' + ", condition='" + this.condition + '\'' + ", celsius=" + this.celsius + ", fahrenheit=" + this.fahrenheit + '}';
  }
}
