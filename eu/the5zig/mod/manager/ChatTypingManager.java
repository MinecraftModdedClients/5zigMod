package eu.the5zig.mod.manager;

import com.google.common.collect.Lists;
import eu.the5zig.mod.listener.Listener;
import java.util.List;
import java.util.UUID;

public class ChatTypingManager
  extends Listener
{
  private UUID typingTo = null;
  private List<UUID> typingFrom = Lists.newArrayList();
  
  public UUID getTypingTo()
  {
    return this.typingTo;
  }
  
  public void setTypingTo(UUID typingTo)
  {
    this.typingTo = typingTo;
  }
  
  public boolean addToTyping(UUID friend)
  {
    return (!this.typingFrom.contains(friend)) && (this.typingFrom.add(friend));
  }
  
  public boolean removeFromTyping(UUID friend)
  {
    return this.typingFrom.remove(friend);
  }
  
  public boolean isTyping(UUID friend)
  {
    return this.typingFrom.contains(friend);
  }
}
