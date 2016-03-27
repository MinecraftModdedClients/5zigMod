package org.h2.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;

public class SourceCompiler
{
  static final JavaCompiler JAVA_COMPILER;
  private static final Class<?> JAVAC_SUN;
  private static final String COMPILE_DIR = Utils.getProperty("java.io.tmpdir", ".");
  final HashMap<String, String> sources;
  final HashMap<String, Class<?>> compiled;
  boolean useJavaSystemCompiler;
  
  public SourceCompiler()
  {
    this.sources = New.hashMap();
    
    this.compiled = New.hashMap();
    
    this.useJavaSystemCompiler = SysProperties.JAVA_SYSTEM_COMPILER;
  }
  
  static
  {
    JavaCompiler localJavaCompiler;
    try
    {
      localJavaCompiler = ToolProvider.getSystemJavaCompiler();
    }
    catch (Exception localException1)
    {
      localJavaCompiler = null;
    }
    JAVA_COMPILER = localJavaCompiler;
    Class localClass;
    try
    {
      localClass = Class.forName("com.sun.tools.javac.Main");
    }
    catch (Exception localException2)
    {
      localClass = null;
    }
    JAVAC_SUN = localClass;
  }
  
  public void setSource(String paramString1, String paramString2)
  {
    this.sources.put(paramString1, paramString2);
    this.compiled.clear();
  }
  
  public void setJavaSystemCompiler(boolean paramBoolean)
  {
    this.useJavaSystemCompiler = paramBoolean;
  }
  
  public Class<?> getClass(String paramString)
    throws ClassNotFoundException
  {
    Class localClass = (Class)this.compiled.get(paramString);
    if (localClass != null) {
      return localClass;
    }
    String str = (String)this.sources.get(paramString);
    if (isGroovySource(str))
    {
      localObject = GroovyCompiler.parseClass(str, paramString);
      this.compiled.put(paramString, localObject);
      return (Class<?>)localObject;
    }
    Object localObject = new ClassLoader(getClass().getClassLoader())
    {
      public Class<?> findClass(String paramAnonymousString)
        throws ClassNotFoundException
      {
        Class localClass = (Class)SourceCompiler.this.compiled.get(paramAnonymousString);
        if (localClass == null)
        {
          String str1 = (String)SourceCompiler.this.sources.get(paramAnonymousString);
          String str2 = null;
          int i = paramAnonymousString.lastIndexOf('.');
          String str3;
          if (i >= 0)
          {
            str2 = paramAnonymousString.substring(0, i);
            str3 = paramAnonymousString.substring(i + 1);
          }
          else
          {
            str3 = paramAnonymousString;
          }
          String str4 = SourceCompiler.getCompleteSourceCode(str2, str3, str1);
          if ((SourceCompiler.JAVA_COMPILER != null) && (SourceCompiler.this.useJavaSystemCompiler))
          {
            localClass = SourceCompiler.this.javaxToolsJavac(str2, str3, str4);
          }
          else
          {
            byte[] arrayOfByte = SourceCompiler.this.javacCompile(str2, str3, str4);
            if (arrayOfByte == null) {
              localClass = findSystemClass(paramAnonymousString);
            } else {
              localClass = defineClass(paramAnonymousString, arrayOfByte, 0, arrayOfByte.length);
            }
          }
          SourceCompiler.this.compiled.put(paramAnonymousString, localClass);
        }
        return localClass;
      }
    };
    return ((ClassLoader)localObject).loadClass(paramString);
  }
  
  private static boolean isGroovySource(String paramString)
  {
    return (paramString.startsWith("//groovy")) || (paramString.startsWith("@groovy"));
  }
  
  public Method getMethod(String paramString)
    throws ClassNotFoundException
  {
    Class localClass = getClass(paramString);
    Method[] arrayOfMethod1 = localClass.getDeclaredMethods();
    for (Method localMethod : arrayOfMethod1)
    {
      int k = localMethod.getModifiers();
      if ((Modifier.isPublic(k)) && (Modifier.isStatic(k)))
      {
        String str = localMethod.getName();
        if ((!str.startsWith("_")) && (!localMethod.getName().equals("main"))) {
          return localMethod;
        }
      }
    }
    return null;
  }
  
  byte[] javacCompile(String paramString1, String paramString2, String paramString3)
  {
    File localFile1 = new File(COMPILE_DIR);
    if (paramString1 != null)
    {
      localFile1 = new File(localFile1, paramString1.replace('.', '/'));
      FileUtils.createDirectories(localFile1.getAbsolutePath());
    }
    File localFile2 = new File(localFile1, paramString2 + ".java");
    File localFile3 = new File(localFile1, paramString2 + ".class");
    try
    {
      OutputStream localOutputStream = FileUtils.newOutputStream(localFile2.getAbsolutePath(), false);
      Writer localWriter = IOUtils.getBufferedWriter(localOutputStream);
      localFile3.delete();
      localWriter.write(paramString3);
      localWriter.close();
      if (JAVAC_SUN != null) {
        javacSun(localFile2);
      } else {
        javacProcess(localFile2);
      }
      byte[] arrayOfByte1 = new byte[(int)localFile3.length()];
      DataInputStream localDataInputStream = new DataInputStream(new FileInputStream(localFile3));
      localDataInputStream.readFully(arrayOfByte1);
      localDataInputStream.close();
      return arrayOfByte1;
    }
    catch (Exception localException)
    {
      throw DbException.convert(localException);
    }
    finally
    {
      localFile2.delete();
      localFile3.delete();
    }
  }
  
  static String getCompleteSourceCode(String paramString1, String paramString2, String paramString3)
  {
    if (paramString3.startsWith("package ")) {
      return paramString3;
    }
    StringBuilder localStringBuilder = new StringBuilder();
    if (paramString1 != null) {
      localStringBuilder.append("package ").append(paramString1).append(";\n");
    }
    int i = paramString3.indexOf("@CODE");
    String str = "import java.util.*;\nimport java.math.*;\nimport java.sql.*;\n";
    if (i >= 0)
    {
      str = paramString3.substring(0, i);
      paramString3 = paramString3.substring("@CODE".length() + i);
    }
    localStringBuilder.append(str);
    localStringBuilder.append("public class ").append(paramString2).append(" {\n    public static ").append(paramString3).append("\n}\n");
    
    return localStringBuilder.toString();
  }
  
  Class<?> javaxToolsJavac(String paramString1, String paramString2, String paramString3)
  {
    String str1 = paramString1 + "." + paramString2;
    StringWriter localStringWriter = new StringWriter();
    ClassFileManager localClassFileManager = new ClassFileManager(JAVA_COMPILER.getStandardFileManager(null, null, null));
    
    ArrayList localArrayList = new ArrayList();
    localArrayList.add(new StringJavaFileObject(str1, paramString3));
    JAVA_COMPILER.getTask(localStringWriter, localClassFileManager, null, null, null, localArrayList).call();
    
    String str2 = localStringWriter.toString();
    throwSyntaxError(str2);
    try
    {
      return localClassFileManager.getClassLoader(null).loadClass(str1);
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      throw DbException.convert(localClassNotFoundException);
    }
  }
  
  private static void javacProcess(File paramFile)
  {
    exec(new String[] { "javac", "-sourcepath", COMPILE_DIR, "-d", COMPILE_DIR, "-encoding", "UTF-8", paramFile.getAbsolutePath() });
  }
  
  private static int exec(String... paramVarArgs)
  {
    ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
    try
    {
      ProcessBuilder localProcessBuilder = new ProcessBuilder(new String[0]);
      
      localProcessBuilder.environment().remove("JAVA_TOOL_OPTIONS");
      localProcessBuilder.command(paramVarArgs);
      
      Process localProcess = localProcessBuilder.start();
      copyInThread(localProcess.getInputStream(), localByteArrayOutputStream);
      copyInThread(localProcess.getErrorStream(), localByteArrayOutputStream);
      localProcess.waitFor();
      String str = new String(localByteArrayOutputStream.toByteArray(), Constants.UTF8);
      throwSyntaxError(str);
      return localProcess.exitValue();
    }
    catch (Exception localException)
    {
      throw DbException.convert(localException);
    }
  }
  
  private static void copyInThread(InputStream paramInputStream, final OutputStream paramOutputStream)
  {
    new Task()
    {
      public void call()
        throws IOException
      {
        IOUtils.copy(this.val$in, paramOutputStream);
      }
    }.execute();
  }
  
  private static void javacSun(File paramFile)
  {
    PrintStream localPrintStream1 = System.err;
    ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
    PrintStream localPrintStream2 = new PrintStream(localByteArrayOutputStream);
    try
    {
      System.setErr(localPrintStream2);
      
      Method localMethod = JAVAC_SUN.getMethod("compile", new Class[] { String[].class });
      Object localObject1 = JAVAC_SUN.newInstance();
      localMethod.invoke(localObject1, new Object[] { { "-sourcepath", COMPILE_DIR, "-d", COMPILE_DIR, "-encoding", "UTF-8", paramFile.getAbsolutePath() } });
      
      String str = new String(localByteArrayOutputStream.toByteArray(), Constants.UTF8);
      throwSyntaxError(str);
    }
    catch (Exception localException)
    {
      throw DbException.convert(localException);
    }
    finally
    {
      System.setErr(localPrintStream1);
    }
  }
  
  private static void throwSyntaxError(String paramString)
  {
    if (!paramString.startsWith("Note:")) {
      if (paramString.length() > 0)
      {
        paramString = StringUtils.replaceAll(paramString, COMPILE_DIR, "");
        throw DbException.get(42000, paramString);
      }
    }
  }
  
  private static final class GroovyCompiler
  {
    private static final Object LOADER;
    private static final Throwable INIT_FAIL_EXCEPTION;
    
    static
    {
      Object localObject1 = null;
      Object localObject2 = null;
      try
      {
        Class localClass = Class.forName("org.codehaus.groovy.control.customizers.ImportCustomizer");
        
        Object localObject3 = Utils.newInstance("org.codehaus.groovy.control.customizers.ImportCustomizer", new Object[0]);
        
        String[] arrayOfString = { "java.sql.Connection", "java.sql.Types", "java.sql.ResultSet", "groovy.sql.Sql", "org.h2.tools.SimpleResultSet" };
        
        Utils.callMethod(localObject3, "addImports", new Object[] { arrayOfString });
        
        Object localObject4 = Array.newInstance(localClass, 1);
        Array.set(localObject4, 0, localObject3);
        Object localObject5 = Utils.newInstance("org.codehaus.groovy.control.CompilerConfiguration", new Object[0]);
        
        Utils.callMethod(localObject5, "addCompilationCustomizers", new Object[] { localObject4 });
        
        ClassLoader localClassLoader = GroovyCompiler.class.getClassLoader();
        localObject1 = Utils.newInstance("groovy.lang.GroovyClassLoader", new Object[] { localClassLoader, localObject5 });
      }
      catch (Exception localException)
      {
        localObject2 = localException;
      }
      LOADER = localObject1;
      INIT_FAIL_EXCEPTION = (Throwable)localObject2;
    }
    
    public static Class<?> parseClass(String paramString1, String paramString2)
    {
      if (LOADER == null) {
        throw new RuntimeException("Compile fail: no Groovy jar in the classpath", INIT_FAIL_EXCEPTION);
      }
      try
      {
        Object localObject = Utils.newInstance("groovy.lang.GroovyCodeSource", new Object[] { paramString1, paramString2 + ".groovy", "UTF-8" });
        
        Utils.callMethod(localObject, "setCachable", new Object[] { Boolean.valueOf(false) });
        return (Class)Utils.callMethod(LOADER, "parseClass", new Object[] { localObject });
      }
      catch (Exception localException)
      {
        throw new RuntimeException(localException);
      }
    }
  }
  
  static class StringJavaFileObject
    extends SimpleJavaFileObject
  {
    private final String sourceCode;
    
    public StringJavaFileObject(String paramString1, String paramString2)
    {
      super(JavaFileObject.Kind.SOURCE);
      
      this.sourceCode = paramString2;
    }
    
    public CharSequence getCharContent(boolean paramBoolean)
    {
      return this.sourceCode;
    }
  }
  
  static class JavaClassObject
    extends SimpleJavaFileObject
  {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    public JavaClassObject(String paramString, JavaFileObject.Kind paramKind)
    {
      super(paramKind);
    }
    
    public byte[] getBytes()
    {
      return this.out.toByteArray();
    }
    
    public OutputStream openOutputStream()
      throws IOException
    {
      return this.out;
    }
  }
  
  static class ClassFileManager
    extends ForwardingJavaFileManager<StandardJavaFileManager>
  {
    SourceCompiler.JavaClassObject classObject;
    
    public ClassFileManager(StandardJavaFileManager paramStandardJavaFileManager)
    {
      super();
    }
    
    public ClassLoader getClassLoader(JavaFileManager.Location paramLocation)
    {
      new SecureClassLoader()
      {
        protected Class<?> findClass(String paramAnonymousString)
          throws ClassNotFoundException
        {
          byte[] arrayOfByte = SourceCompiler.ClassFileManager.this.classObject.getBytes();
          return super.defineClass(paramAnonymousString, arrayOfByte, 0, arrayOfByte.length);
        }
      };
    }
    
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location paramLocation, String paramString, JavaFileObject.Kind paramKind, FileObject paramFileObject)
      throws IOException
    {
      this.classObject = new SourceCompiler.JavaClassObject(paramString, paramKind);
      return this.classObject;
    }
  }
}
