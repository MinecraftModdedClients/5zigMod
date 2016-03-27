package org.h2.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceWriterAdapter
  implements TraceWriter
{
  private String name;
  private final Logger logger = LoggerFactory.getLogger("h2database");
  
  public void setName(String paramString)
  {
    this.name = paramString;
  }
  
  public boolean isEnabled(int paramInt)
  {
    switch (paramInt)
    {
    case 3: 
      return this.logger.isDebugEnabled();
    case 2: 
      return this.logger.isInfoEnabled();
    case 1: 
      return this.logger.isErrorEnabled();
    }
    return false;
  }
  
  public void write(int paramInt, String paramString1, String paramString2, Throwable paramThrowable)
  {
    if (isEnabled(paramInt))
    {
      if (this.name != null) {
        paramString2 = this.name + ":" + paramString1 + " " + paramString2;
      } else {
        paramString2 = paramString1 + " " + paramString2;
      }
      switch (paramInt)
      {
      case 3: 
        this.logger.debug(paramString2, paramThrowable);
        break;
      case 2: 
        this.logger.info(paramString2, paramThrowable);
        break;
      case 1: 
        this.logger.error(paramString2, paramThrowable);
        break;
      }
    }
  }
}
