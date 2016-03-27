package org.h2.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.StringUtils;

public class CipherFactory
{
  public static final String KEYSTORE_PASSWORD = "h2pass";
  private static final String KEYSTORE = "~/.h2.keystore";
  private static final String KEYSTORE_KEY = "javax.net.ssl.keyStore";
  private static final String KEYSTORE_PASSWORD_KEY = "javax.net.ssl.keyStorePassword";
  
  public static BlockCipher getBlockCipher(String paramString)
  {
    if ("XTEA".equalsIgnoreCase(paramString)) {
      return new XTEA();
    }
    if ("AES".equalsIgnoreCase(paramString)) {
      return new AES();
    }
    if ("FOG".equalsIgnoreCase(paramString)) {
      return new Fog();
    }
    throw DbException.get(90055, paramString);
  }
  
  public static Socket createSocket(InetAddress paramInetAddress, int paramInt)
    throws IOException
  {
    Object localObject = null;
    setKeystore();
    SSLSocketFactory localSSLSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
    SSLSocket localSSLSocket = (SSLSocket)localSSLSocketFactory.createSocket();
    localSSLSocket.connect(new InetSocketAddress(paramInetAddress, paramInt), SysProperties.SOCKET_CONNECT_TIMEOUT);
    
    localSSLSocket.setEnabledProtocols(disableSSL(localSSLSocket.getEnabledProtocols()));
    if (SysProperties.ENABLE_ANONYMOUS_TLS)
    {
      String[] arrayOfString = enableAnonymous(localSSLSocket.getEnabledCipherSuites(), localSSLSocket.getSupportedCipherSuites());
      
      localSSLSocket.setEnabledCipherSuites(arrayOfString);
    }
    localObject = localSSLSocket;
    return (Socket)localObject;
  }
  
  public static ServerSocket createServerSocket(int paramInt, InetAddress paramInetAddress)
    throws IOException
  {
    Object localObject = null;
    setKeystore();
    ServerSocketFactory localServerSocketFactory = SSLServerSocketFactory.getDefault();
    SSLServerSocket localSSLServerSocket;
    if (paramInetAddress == null) {
      localSSLServerSocket = (SSLServerSocket)localServerSocketFactory.createServerSocket(paramInt);
    } else {
      localSSLServerSocket = (SSLServerSocket)localServerSocketFactory.createServerSocket(paramInt, 0, paramInetAddress);
    }
    localSSLServerSocket.setEnabledProtocols(disableSSL(localSSLServerSocket.getEnabledProtocols()));
    if (SysProperties.ENABLE_ANONYMOUS_TLS)
    {
      String[] arrayOfString = enableAnonymous(localSSLServerSocket.getEnabledCipherSuites(), localSSLServerSocket.getSupportedCipherSuites());
      
      localSSLServerSocket.setEnabledCipherSuites(arrayOfString);
    }
    localObject = localSSLServerSocket;
    return (ServerSocket)localObject;
  }
  
  private static byte[] getKeyStoreBytes(KeyStore paramKeyStore, String paramString)
    throws IOException
  {
    ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
    try
    {
      paramKeyStore.store(localByteArrayOutputStream, paramString.toCharArray());
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
    return localByteArrayOutputStream.toByteArray();
  }
  
  public static KeyStore getKeyStore(String paramString)
    throws IOException
  {
    try
    {
      KeyStore localKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      
      localKeyStore.load(null, paramString.toCharArray());
      KeyFactory localKeyFactory = KeyFactory.getInstance("RSA");
      localKeyStore.load(null, paramString.toCharArray());
      PKCS8EncodedKeySpec localPKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(StringUtils.convertHexToBytes("30820277020100300d06092a864886f70d0101010500048202613082025d02010002818100dc0a13c602b7141110eade2f051b54777b060d0f74e6a110f9cce81159f271ebc88d8e8aa1f743b505fc2e7dfe38d33b8d3f64d1b363d1af4d877833897954cbaec2fa384c22a415498cf306bb07ac09b76b001cd68bf77ea0a628f5101959cf2993a9c23dbee79b19305977f8715ae78d023471194cc900b231eecb0aaea98d02030100010281810099aa4ff4d0a09a5af0bd953cb10c4d08c3d98df565664ac5582e494314d5c3c92dddedd5d316a32a206be4ec084616fe57be15e27cad111aa3c21fa79e32258c6ca8430afc69eddd52d3b751b37da6b6860910b94653192c0db1d02abcfd6ce14c01f238eec7c20bd3bb750940004bacba2880349a9494d10e139ecb2355d101024100ffdc3defd9c05a2d377ef6019fa62b3fbd5b0020a04cc8533bca730e1f6fcf5dfceea1b044fbe17d9eababfbc7d955edad6bc60f9be826ad2c22ba77d19a9f65024100dc28d43fdbbc93852cc3567093157702bc16f156f709fb7db0d9eec028f41fd0edcd17224c866e66be1744141fb724a10fd741c8a96afdd9141b36d67fff6309024077b1cddbde0f69604bdcfe33263fb36ddf24aa3b9922327915b890f8a36648295d0139ecdf68c245652c4489c6257b58744fbdd961834a4cab201801a3b1e52d024100b17142e8991d1b350a0802624759d48ae2b8071a158ff91fabeb6a8f7c328e762143dc726b8529f42b1fab6220d1c676fdc27ba5d44e847c72c52064afd351a902407c6e23fe35bcfcd1a662aa82a2aa725fcece311644d5b6e3894853fd4ce9fe78218c957b1ff03fc9e5ef8ffeb6bd58235f6a215c97d354fdace7e781e4a63e8b"));
      
      PrivateKey localPrivateKey = localKeyFactory.generatePrivate(localPKCS8EncodedKeySpec);
      Certificate[] arrayOfCertificate = { CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(StringUtils.convertHexToBytes("3082018b3081f502044295ce6b300d06092a864886f70d0101040500300d310b3009060355040313024832301e170d3035303532363133323630335a170d3337303933303036353734375a300d310b300906035504031302483230819f300d06092a864886f70d010101050003818d0030818902818100dc0a13c602b7141110eade2f051b54777b060d0f74e6a110f9cce81159f271ebc88d8e8aa1f743b505fc2e7dfe38d33b8d3f64d1b363d1af4d877833897954cbaec2fa384c22a415498cf306bb07ac09b76b001cd68bf77ea0a628f5101959cf2993a9c23dbee79b19305977f8715ae78d023471194cc900b231eecb0aaea98d0203010001300d06092a864886f70d01010405000381810083f4401a279453701bef9a7681a5b8b24f153f7d18c7c892133d97bd5f13736be7505290a445a7d5ceb75522403e5097515cd966ded6351ff60d5193de34cd36e5cb04d380398e66286f99923fd92296645fd4ada45844d194dfd815e6cd57f385c117be982809028bba1116c85740b3d27a55b1a0948bf291ddba44bed337b9"))) };
      
      localKeyStore.setKeyEntry("h2", localPrivateKey, paramString.toCharArray(), arrayOfCertificate);
      
      return localKeyStore;
    }
    catch (Exception localException)
    {
      throw DbException.convertToIOException(localException);
    }
  }
  
  private static void setKeystore()
    throws IOException
  {
    Properties localProperties = System.getProperties();
    if (localProperties.getProperty("javax.net.ssl.keyStore") == null)
    {
      String str1 = "~/.h2.keystore";
      byte[] arrayOfByte1 = getKeyStoreBytes(getKeyStore("h2pass"), "h2pass");
      
      int i = 1;
      Object localObject;
      if ((FileUtils.exists(str1)) && (FileUtils.size(str1) == arrayOfByte1.length))
      {
        localObject = FileUtils.newInputStream(str1);
        byte[] arrayOfByte2 = IOUtils.readBytesAndClose((InputStream)localObject, 0);
        if ((arrayOfByte2 != null) && (Arrays.equals(arrayOfByte1, arrayOfByte2))) {
          i = 0;
        }
      }
      if (i != 0) {
        try
        {
          localObject = FileUtils.newOutputStream(str1, false);
          ((OutputStream)localObject).write(arrayOfByte1);
          ((OutputStream)localObject).close();
        }
        catch (Exception localException)
        {
          throw DbException.convertToIOException(localException);
        }
      }
      String str2 = FileUtils.toRealPath(str1);
      System.setProperty("javax.net.ssl.keyStore", str2);
    }
    if (localProperties.getProperty("javax.net.ssl.keyStorePassword") == null) {
      System.setProperty("javax.net.ssl.keyStorePassword", "h2pass");
    }
  }
  
  private static String[] enableAnonymous(String[] paramArrayOfString1, String[] paramArrayOfString2)
  {
    HashSet localHashSet = new HashSet();
    Collections.addAll(localHashSet, paramArrayOfString1);
    for (String str : paramArrayOfString2) {
      if ((!str.startsWith("SSL")) && (str.indexOf("_anon_") >= 0) && (str.indexOf("_AES_") >= 0) && (str.indexOf("_SHA") >= 0)) {
        localHashSet.add(str);
      }
    }
    return (String[])localHashSet.toArray(new String[0]);
  }
  
  private static String[] disableSSL(String[] paramArrayOfString)
  {
    HashSet localHashSet = new HashSet();
    for (String str : paramArrayOfString) {
      if (!str.startsWith("SSL")) {
        localHashSet.add(str);
      }
    }
    return (String[])localHashSet.toArray(new String[0]);
  }
}
