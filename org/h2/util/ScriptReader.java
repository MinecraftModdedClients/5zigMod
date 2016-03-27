package org.h2.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import org.h2.message.DbException;

public class ScriptReader
  implements Closeable
{
  private final Reader reader;
  private char[] buffer;
  private int bufferPos;
  private int bufferStart = -1;
  private int bufferEnd;
  private boolean endOfFile;
  private boolean insideRemark;
  private boolean blockRemark;
  private boolean skipRemarks;
  private int remarkStart;
  
  public ScriptReader(Reader paramReader)
  {
    this.reader = paramReader;
    this.buffer = new char[' '];
  }
  
  public void close()
  {
    try
    {
      this.reader.close();
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  public String readStatement()
  {
    if (this.endOfFile) {
      return null;
    }
    try
    {
      return readStatementLoop();
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  private String readStatementLoop()
    throws IOException
  {
    this.bufferStart = this.bufferPos;
    int i = read();
    for (;;)
    {
      if (i < 0)
      {
        this.endOfFile = true;
        if (this.bufferPos - 1 != this.bufferStart) {
          break;
        }
        return null;
      }
      if (i == 59) {
        break;
      }
      switch (i)
      {
      case 36: 
        i = read();
        if ((i == 36) && ((this.bufferPos - this.bufferStart < 3) || (this.buffer[(this.bufferPos - 3)] <= ' ')))
        {
          for (;;)
          {
            i = read();
            if (i >= 0) {
              if (i == 36)
              {
                i = read();
                if (i >= 0) {
                  if (i == 36) {
                    break;
                  }
                }
              }
            }
          }
          i = read();
        }
        break;
      case 39: 
        for (;;)
        {
          i = read();
          if (i >= 0) {
            if (i == 39) {
              break;
            }
          }
        }
        i = read();
        break;
      case 34: 
        for (;;)
        {
          i = read();
          if (i >= 0) {
            if (i == 34) {
              break;
            }
          }
        }
        i = read();
        break;
      case 47: 
        i = read();
        if (i == 42)
        {
          startRemark(true);
          do
          {
            do
            {
              i = read();
              if (i < 0) {
                break;
              }
            } while (i != 42);
            i = read();
            if (i < 0)
            {
              clearRemark();
              break;
            }
          } while (i != 47);
          endRemark();
          
          i = read();
        }
        else if (i == 47)
        {
          startRemark(false);
          do
          {
            i = read();
            if (i < 0)
            {
              clearRemark();
              break;
            }
          } while ((i != 13) && (i != 10));
          endRemark();
          
          i = read();
        }
        break;
      case 45: 
        i = read();
        if (i == 45)
        {
          startRemark(false);
          do
          {
            i = read();
            if (i < 0)
            {
              clearRemark();
              break;
            }
          } while ((i != 13) && (i != 10));
          endRemark();
          
          i = read();
        }
        break;
      case 35: 
      case 37: 
      case 38: 
      case 40: 
      case 41: 
      case 42: 
      case 43: 
      case 44: 
      case 46: 
      default: 
        i = read();
      }
    }
    return new String(this.buffer, this.bufferStart, this.bufferPos - 1 - this.bufferStart);
  }
  
  private void startRemark(boolean paramBoolean)
  {
    this.blockRemark = paramBoolean;
    this.remarkStart = (this.bufferPos - 2);
    this.insideRemark = true;
  }
  
  private void endRemark()
  {
    clearRemark();
    this.insideRemark = false;
  }
  
  private void clearRemark()
  {
    if (this.skipRemarks) {
      Arrays.fill(this.buffer, this.remarkStart, this.bufferPos, ' ');
    }
  }
  
  private int read()
    throws IOException
  {
    if (this.bufferPos >= this.bufferEnd) {
      return readBuffer();
    }
    return this.buffer[(this.bufferPos++)];
  }
  
  private int readBuffer()
    throws IOException
  {
    if (this.endOfFile) {
      return -1;
    }
    int i = this.bufferPos - this.bufferStart;
    if (i > 0)
    {
      char[] arrayOfChar = this.buffer;
      if (i + 4096 > arrayOfChar.length)
      {
        if (arrayOfChar.length >= 1073741823) {
          throw new IOException("Error in parsing script, statement size exceeds 1G, first 80 characters of statement looks like: " + new String(this.buffer, this.bufferStart, 80));
        }
        this.buffer = new char[arrayOfChar.length * 2];
      }
      System.arraycopy(arrayOfChar, this.bufferStart, this.buffer, 0, i);
    }
    this.remarkStart -= this.bufferStart;
    this.bufferStart = 0;
    this.bufferPos = i;
    int j = this.reader.read(this.buffer, i, 4096);
    if (j == -1)
    {
      this.bufferEnd = 64512;
      this.endOfFile = true;
      
      this.bufferPos += 1;
      return -1;
    }
    this.bufferEnd = (i + j);
    return this.buffer[(this.bufferPos++)];
  }
  
  public boolean isInsideRemark()
  {
    return this.insideRemark;
  }
  
  public boolean isBlockRemark()
  {
    return this.blockRemark;
  }
  
  public void setSkipRemarks(boolean paramBoolean)
  {
    this.skipRemarks = paramBoolean;
  }
}
