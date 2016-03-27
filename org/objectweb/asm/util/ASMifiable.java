package org.objectweb.asm.util;

import java.util.Map;

public abstract interface ASMifiable
{
  public abstract void asmify(StringBuffer paramStringBuffer, String paramString, Map paramMap);
}
