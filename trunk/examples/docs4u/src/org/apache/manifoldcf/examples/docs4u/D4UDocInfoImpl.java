/* $Id$ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.manifoldcf.examples.docs4u;

import java.io.*;
import java.util.*;

/** This class represents a document from the Docs4U content management system.
*/
public class D4UDocInfoImpl implements D4UDocInfo
{
  protected File contents = null;
  
  protected String[] allowed = new String[0];
  protected String[] disallowed = new String[0];
  
  protected Map metadata = new HashMap();
  
  public D4UDocInfoImpl()
  {
  }
  
  /** Write a stream of the content.  It is the caller's responsibility to close
  * the stream when finished.
  *@param dataStream the document content stream.
  */
  public void getData(OutputStream dataStream)
    throws D4UException
  {
    if (contents != null)
    {
      try
      {
        InputStream is = new FileInputStream(contents);
        try
        {
          byte[] buffer = new byte[65536];
          while (true)
          {
            int amt = is.read(buffer);
            if (amt == -1)
              break;
            dataStream.write(buffer,0,amt);
          }
        }
        finally
        {
          is.close();
        }
      }
      catch (IOException e)
      {
        throw new D4UException(e.getMessage(),e);
      }
    }
  }
  
  /** Read a stream of the content.  It is the caller's responsibility to close
  * the stream when finished.
  *@return a document content stream.
  */
  public InputStream readData()
    throws D4UException
  {
    if (contents != null)
    {
      try
      {
        return new FileInputStream(contents);
      }
      catch (IOException e)
      {
        throw new D4UException(e.getMessage(),e);
      }
    }
    return null;
  }

  /** Read the content length.
  *@return the length.
  */
  public Long readDataLength()
    throws D4UException
  {
    if (contents != null)
    {
      return contents.length();
    }
    return null;
  }

  /** Set the content from a stream.  It is the caller's responsibility to close
  * the stream when done.
  *@param dataStream is the data stream.
  */
  public void setData(InputStream dataStream)
    throws D4UException
  {
    // Get rid of old file
    close();
    // Create a new file name.
    try
    {
      contents = File.createTempFile("d4u",null,null);
      // Transfer data to the temp file
      OutputStream os = new FileOutputStream(contents);
      try
      {
        byte[] buffer = new byte[65536];
        while (true)
        {
          int amt = dataStream.read(buffer);
          if (amt == -1)
            break;
          os.write(buffer,0,amt);
        }
        os.flush();
      }
      finally
      {
        os.close();
      }
    }
    catch (IOException e)
    {
      throw new D4UException(e.getMessage(),e);
    }
  }
  
  /** Get allowed users and groups.
  *@return the user and group IDs.
  */
  public String[] getAllowed()
  {
    return allowed;
  }
  
  /** Set allowed users and groups.
  *@param userGroupIDs are the allowed user and group IDs.
  */
  public void setAllowed(String[] userGroupIDs)
  {
    allowed = userGroupIDs;
  }
  
  /** Get disallowed users and groups.
  *@return the disallowed user and group IDs.
  */
  public String[] getDisallowed()
  {
    return disallowed;
  }
  
  /** Set disallowed users and groups.
  *@param userGroupIDs are the disallowed user and group IDs.
  */
  public void setDisallowed(String[] userGroupIDs)
  {
    disallowed = userGroupIDs;
  }
  
  /** Get a list of applicable metadata names.
  *@return the metadata names.
  */
  public String[] getMetadataNames()
  {
    String[] rval = new String[metadata.size()];
    Iterator iter = metadata.keySet().iterator();
    int i = 0;
    while (iter.hasNext())
    {
      rval[i++] = (String)iter.next();
    }
    return rval;
  }
  
  /** Get specified metadata.
  *@param metadataName is the name of the metadata.
  *@return the values, or null for no metadata.
  */
  public String[] getMetadata(String metadataName)
  {
    return (String[])metadata.get(metadataName);
  }
  
  /** Set specified metadata.
  *@param metadataName is the name of the metadata.
  *@param values are the metadata values, or null if none.
  */
  public void setMetadata(String metadataName, String[] values)
  {
    if (values == null)
      metadata.remove(metadataName);
    else
      metadata.put(metadataName,values);
  }

  /** Clear all metadata.
  */
  public void clearMetadata()
  {
    metadata.clear();
  }

  /** Close the document info object. 
  */
  public void close()
    throws D4UException
  {
    if (contents != null)
    {
      if (contents.delete() == false)
        throw new D4UException("Missing content file");
      contents = null;
    }
  }
  
}
