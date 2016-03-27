package org.h2.command.dml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.h2.command.Prepared;
import org.h2.engine.Constants;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.util.ScriptReader;

public class RunScriptCommand
  extends ScriptBase
{
  private static final char UTF8_BOM = '﻿';
  private Charset charset = Constants.UTF8;
  
  public RunScriptCommand(Session paramSession)
  {
    super(paramSession);
  }
  
  public int update()
  {
    this.session.getUser().checkAdmin();
    int i = 0;
    try
    {
      openInput();
      BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(this.in, this.charset));
      
      localBufferedReader.mark(1);
      if (localBufferedReader.read() != 65279) {
        localBufferedReader.reset();
      }
      ScriptReader localScriptReader = new ScriptReader(localBufferedReader);
      for (;;)
      {
        String str = localScriptReader.readStatement();
        if (str == null) {
          break;
        }
        execute(str);
        i++;
        if ((i & 0x7F) == 0) {
          checkCanceled();
        }
      }
      localBufferedReader.close();
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
    finally
    {
      closeIO();
    }
    return i;
  }
  
  private void execute(String paramString)
  {
    try
    {
      Prepared localPrepared = this.session.prepare(paramString);
      if (localPrepared.isQuery()) {
        localPrepared.query(0);
      } else {
        localPrepared.update();
      }
      if (this.session.getAutoCommit()) {
        this.session.commit(false);
      }
    }
    catch (DbException localDbException)
    {
      throw localDbException.addSQL(paramString);
    }
  }
  
  public void setCharset(Charset paramCharset)
  {
    this.charset = paramCharset;
  }
  
  public ResultInterface queryMeta()
  {
    return null;
  }
  
  public int getType()
  {
    return 64;
  }
}
