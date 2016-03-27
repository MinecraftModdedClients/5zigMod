package org.h2.jmx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import org.h2.util.Utils;

public class DocumentedMBean
  extends StandardMBean
{
  private final String interfaceName;
  private Properties resources;
  
  public <T> DocumentedMBean(T paramT, Class<T> paramClass)
    throws NotCompliantMBeanException
  {
    super(paramT, paramClass);
    this.interfaceName = (paramT.getClass().getName() + "MBean");
  }
  
  private Properties getResources()
  {
    if (this.resources == null)
    {
      this.resources = new Properties();
      String str = "/org/h2/res/javadoc.properties";
      try
      {
        byte[] arrayOfByte = Utils.getResource(str);
        if (arrayOfByte != null) {
          this.resources.load(new ByteArrayInputStream(arrayOfByte));
        }
      }
      catch (IOException localIOException) {}
    }
    return this.resources;
  }
  
  protected String getDescription(MBeanInfo paramMBeanInfo)
  {
    String str = getResources().getProperty(this.interfaceName);
    return str == null ? super.getDescription(paramMBeanInfo) : str;
  }
  
  protected String getDescription(MBeanOperationInfo paramMBeanOperationInfo)
  {
    String str = getResources().getProperty(this.interfaceName + "." + paramMBeanOperationInfo.getName());
    return str == null ? super.getDescription(paramMBeanOperationInfo) : str;
  }
  
  protected String getDescription(MBeanAttributeInfo paramMBeanAttributeInfo)
  {
    String str1 = paramMBeanAttributeInfo.isIs() ? "is" : "get";
    String str2 = getResources().getProperty(this.interfaceName + "." + str1 + paramMBeanAttributeInfo.getName());
    
    return str2 == null ? super.getDescription(paramMBeanAttributeInfo) : str2;
  }
  
  protected int getImpact(MBeanOperationInfo paramMBeanOperationInfo)
  {
    if (paramMBeanOperationInfo.getName().startsWith("list")) {
      return 0;
    }
    return 1;
  }
}
