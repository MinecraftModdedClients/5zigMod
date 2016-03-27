package eu.the5zig.mod.server;

import com.google.common.collect.Lists;
import eu.the5zig.util.Callback;
import java.util.List;
import org.apache.commons.lang3.Validate;

public class MultiLineIgnore
{
  private final String startMessage;
  private final String endMessage;
  private final Callback<MultiPatternResult> callback;
  private boolean startedListening = false;
  private final List<String> messages = Lists.newArrayList();
  
  public MultiLineIgnore(String startMessage, String endMessage, Callback<MultiPatternResult> callback)
  {
    this.startMessage = startMessage;
    this.endMessage = endMessage;
    this.callback = callback;
  }
  
  public String getStartMessage()
  {
    return this.startMessage;
  }
  
  public String getEndMessage()
  {
    return this.endMessage;
  }
  
  public Callback<MultiPatternResult> getCallback()
  {
    return this.callback;
  }
  
  public boolean hasStartedListening()
  {
    return this.startedListening;
  }
  
  public void setStartedListening(boolean startedListening)
  {
    this.startedListening = startedListening;
  }
  
  public void add(String message)
  {
    Validate.validState(this.startedListening, "Hadn't started listening yet!", new Object[0]);
    this.messages.add(message);
  }
  
  public void callCallback()
  {
    Validate.validState(this.startedListening, "Hadn't started listening yet!", new Object[0]);
    this.callback.call(new MultiPatternResult(this.messages));
  }
}
