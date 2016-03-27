import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import org.apache.commons.io.IOUtils;

class ResourceManager$2
  implements Callable<String>
{
  ResourceManager$2(ResourceManager this$0, Integer paramInteger) {}
  
  public String call()
    throws Exception
  {
    HttpURLConnection connection = null;
    BufferedReader reader = null;
    try
    {
      connection = (HttpURLConnection)new URL("http://5zig.eu/models/2/" + this.val$modelId).openConnection();
      String str;
      if (connection.getResponseCode() != 200) {
        return null;
      }
      reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      return reader.readLine();
    }
    finally
    {
      IOUtils.closeQuietly(reader);
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
