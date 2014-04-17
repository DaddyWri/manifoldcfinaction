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

/** This class lists the document in the Docs4U content management system.
*/
public class ListDocuments
{
  private ListDocuments()
  {
  }
  
  public static void main(String[] argv)
  {
    if (argv.length < 1)
    {
      System.err.println("Usage: ListDocuments <directory>");
      System.exit(1);
    }
    
    String directory = argv[0];
    
    try
    {
      Docs4UAPI api = D4UFactory.makeAPI(directory);
      D4UDocumentIterator iterator = api.findDocuments(null,null,new HashMap());
      
      while (iterator.hasNext())
      {
        String docID = iterator.getNext();
        D4UDocInfo docInfo = D4UFactory.makeDocInfo();
        try
        {
          api.getDocument(docID,docInfo);
          Long dataLength = docInfo.readDataLength();
          String[] allowed = docInfo.getAllowed();
          String[] disallowed = docInfo.getDisallowed();
          String[] metadataNames = docInfo.getMetadataNames();
          System.out.println(docID+"\t"+dataLength.toString()+"\t"+
            formatACL(allowed,api)+"\t"+formatACL(disallowed,api)+"\t"+
            formatMetadataNames(metadataNames,docInfo));
        }
        finally
        {
          docInfo.close();
        }
      }
    }
    catch (InterruptedException e)
    {
      e.printStackTrace(System.err);
      System.exit(100);
    }
    catch (D4UException e)
    {
      e.printStackTrace(System.err);
      System.exit(2);
    }
  }
  
  protected static String formatACL(String[] acl, Docs4UAPI api)
    throws InterruptedException, D4UException
  {
    StringBuffer sb = new StringBuffer("(");
    int i = 0;
    while (i < acl.length)
    {
      if (i > 0)
        sb.append(",");
      String ugName = api.getUserOrGroupName(acl[i++]);
      sb.append(ugName);
    }
    sb.append(")");
    return sb.toString();
  }
  
  protected static String formatMetadataNames(String[] metadataNames, D4UDocInfo info)
    throws InterruptedException,D4UException
  {
    StringBuffer sb = new StringBuffer("[");
    int i = 0;
    while (i < metadataNames.length)
    {
      if (i > 0)
        sb.append(";");
      String metadataName = metadataNames[i++];
      String[] metadataValues = info.getMetadata(metadataName);
      sb.append(metadataName).append("=");
      int j = 0;
      while (j < metadataValues.length)
      {
        if (j > 0)
          sb.append(",");
        sb.append(metadataValues[j++]);
      }
    }
    sb.append("]");
    return sb.toString();
  }
  
}
