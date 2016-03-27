package org.h2.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;

public class MathUtils
{
  static SecureRandom cachedSecureRandom;
  static volatile boolean seeded;
  private static final Random RANDOM = new Random();
  
  public static int roundUpInt(int paramInt1, int paramInt2)
  {
    return paramInt1 + paramInt2 - 1 & -paramInt2;
  }
  
  public static long roundUpLong(long paramLong1, long paramLong2)
  {
    return paramLong1 + paramLong2 - 1L & -paramLong2;
  }
  
  private static synchronized SecureRandom getSecureRandom()
  {
    if (cachedSecureRandom != null) {
      return cachedSecureRandom;
    }
    try
    {
      cachedSecureRandom = SecureRandom.getInstance("SHA1PRNG");
      
      Runnable local1 = new Runnable()
      {
        public void run()
        {
          try
          {
            SecureRandom localSecureRandom = SecureRandom.getInstance("SHA1PRNG");
            byte[] arrayOfByte = localSecureRandom.generateSeed(20);
            synchronized (MathUtils.cachedSecureRandom)
            {
              MathUtils.cachedSecureRandom.setSeed(arrayOfByte);
              MathUtils.seeded = true;
            }
          }
          catch (Exception localException)
          {
            MathUtils.warn("SecureRandom", localException);
          }
        }
      };
      try
      {
        Thread localThread = new Thread(local1, "Generate Seed");
        
        localThread.setDaemon(true);
        localThread.start();
        Thread.yield();
        try
        {
          localThread.join(400L);
        }
        catch (InterruptedException localInterruptedException)
        {
          warn("InterruptedException", localInterruptedException);
        }
        if (!seeded)
        {
          byte[] arrayOfByte = generateAlternativeSeed();
          synchronized (cachedSecureRandom)
          {
            cachedSecureRandom.setSeed(arrayOfByte);
          }
        }
      }
      catch (SecurityException localSecurityException)
      {
        local1.run();
        generateAlternativeSeed();
      }
    }
    catch (Exception localException)
    {
      warn("SecureRandom", localException);
      cachedSecureRandom = new SecureRandom();
    }
    return cachedSecureRandom;
  }
  
  public static byte[] generateAlternativeSeed()
  {
    try
    {
      ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
      DataOutputStream localDataOutputStream = new DataOutputStream(localByteArrayOutputStream);
      
      localDataOutputStream.writeLong(System.currentTimeMillis());
      localDataOutputStream.writeLong(System.nanoTime());
      
      localDataOutputStream.writeInt(new Object().hashCode());
      Runtime localRuntime = Runtime.getRuntime();
      localDataOutputStream.writeLong(localRuntime.freeMemory());
      localDataOutputStream.writeLong(localRuntime.maxMemory());
      localDataOutputStream.writeLong(localRuntime.totalMemory());
      try
      {
        String str1 = System.getProperties().toString();
        
        localDataOutputStream.writeInt(str1.length());
        localDataOutputStream.write(str1.getBytes("UTF-8"));
      }
      catch (Exception localException)
      {
        warn("generateAlternativeSeed", localException);
      }
      try
      {
        Class localClass = Class.forName("java.net.InetAddress");
        
        Object localObject1 = localClass.getMethod("getLocalHost", new Class[0]).invoke(null, new Object[0]);
        
        String str2 = localClass.getMethod("getHostName", new Class[0]).invoke(localObject1, new Object[0]).toString();
        
        localDataOutputStream.writeUTF(str2);
        Object[] arrayOfObject1 = (Object[])localClass.getMethod("getAllByName", new Class[] { String.class }).invoke(null, new Object[] { str2 });
        
        Method localMethod = localClass.getMethod("getAddress", new Class[0]);
        for (Object localObject2 : arrayOfObject1) {
          localDataOutputStream.write((byte[])localMethod.invoke(localObject2, new Object[0]));
        }
      }
      catch (Throwable localThrowable) {}
      for (int i = 0; i < 16; i++)
      {
        int j = 0;
        long l = System.currentTimeMillis();
        while (l == System.currentTimeMillis()) {
          j++;
        }
        localDataOutputStream.writeInt(j);
      }
      localDataOutputStream.close();
      return localByteArrayOutputStream.toByteArray();
    }
    catch (IOException localIOException)
    {
      warn("generateAlternativeSeed", localIOException);
    }
    return new byte[1];
  }
  
  static void warn(String paramString, Throwable paramThrowable)
  {
    System.out.println("Warning: " + paramString);
    if (paramThrowable != null) {
      paramThrowable.printStackTrace();
    }
  }
  
  public static int nextPowerOf2(int paramInt)
  {
    long l = 1L;
    while ((l < paramInt) && (l < 1073741823L)) {
      l += l;
    }
    return (int)l;
  }
  
  public static int convertLongToInt(long paramLong)
  {
    if (paramLong <= -2147483648L) {
      return Integer.MIN_VALUE;
    }
    if (paramLong >= 2147483647L) {
      return Integer.MAX_VALUE;
    }
    return (int)paramLong;
  }
  
  public static int compareInt(int paramInt1, int paramInt2)
  {
    return paramInt1 < paramInt2 ? -1 : paramInt1 == paramInt2 ? 0 : 1;
  }
  
  public static int compareLong(long paramLong1, long paramLong2)
  {
    return paramLong1 < paramLong2 ? -1 : paramLong1 == paramLong2 ? 0 : 1;
  }
  
  /* Error */
  public static long secureRandomLong()
  {
    // Byte code:
    //   0: invokestatic 83	org/h2/util/MathUtils:getSecureRandom	()Ljava/security/SecureRandom;
    //   3: astore_0
    //   4: aload_0
    //   5: dup
    //   6: astore_1
    //   7: monitorenter
    //   8: aload_0
    //   9: invokevirtual 84	java/security/SecureRandom:nextLong	()J
    //   12: aload_1
    //   13: monitorexit
    //   14: lreturn
    //   15: astore_2
    //   16: aload_1
    //   17: monitorexit
    //   18: aload_2
    //   19: athrow
    // Line number table:
    //   Java source line #280	-> byte code offset #0
    //   Java source line #281	-> byte code offset #4
    //   Java source line #282	-> byte code offset #8
    //   Java source line #283	-> byte code offset #15
    // Local variable table:
    //   start	length	slot	name	signature
    //   3	6	0	localSecureRandom	SecureRandom
    //   6	11	1	Ljava/lang/Object;	Object
    //   15	4	2	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   8	14	15	finally
    //   15	18	15	finally
  }
  
  public static void randomBytes(byte[] paramArrayOfByte)
  {
    RANDOM.nextBytes(paramArrayOfByte);
  }
  
  public static byte[] secureRandomBytes(int paramInt)
  {
    if (paramInt <= 0) {
      paramInt = 1;
    }
    byte[] arrayOfByte = new byte[paramInt];
    SecureRandom localSecureRandom = getSecureRandom();
    synchronized (localSecureRandom)
    {
      localSecureRandom.nextBytes(arrayOfByte);
    }
    return arrayOfByte;
  }
  
  public static int randomInt(int paramInt)
  {
    return RANDOM.nextInt(paramInt);
  }
  
  /* Error */
  public static int secureRandomInt(int paramInt)
  {
    // Byte code:
    //   0: invokestatic 83	org/h2/util/MathUtils:getSecureRandom	()Ljava/security/SecureRandom;
    //   3: astore_1
    //   4: aload_1
    //   5: dup
    //   6: astore_2
    //   7: monitorenter
    //   8: aload_1
    //   9: iload_0
    //   10: invokevirtual 89	java/security/SecureRandom:nextInt	(I)I
    //   13: aload_2
    //   14: monitorexit
    //   15: ireturn
    //   16: astore_3
    //   17: aload_2
    //   18: monitorexit
    //   19: aload_3
    //   20: athrow
    // Line number table:
    //   Java source line #332	-> byte code offset #0
    //   Java source line #333	-> byte code offset #4
    //   Java source line #334	-> byte code offset #8
    //   Java source line #335	-> byte code offset #16
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	21	0	paramInt	int
    //   3	6	1	localSecureRandom	SecureRandom
    //   6	12	2	Ljava/lang/Object;	Object
    //   16	4	3	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   8	15	16	finally
    //   16	19	16	finally
  }
}
