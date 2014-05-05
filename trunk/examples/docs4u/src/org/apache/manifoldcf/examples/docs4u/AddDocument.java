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

/** This class adds a document to the Docs4U content management system.
*/
public class AddDocument
{
  private AddDocument()
  {
  }
  
  public static void main(String[] argv)
  {
    if (argv.length < 2)
    {
      System.err.println("Usage: AddDocument <directory> <filename> <item_1> ... <item_N>");
      System.err.println("Each item can have the following formats:");
      System.err.println("name=value (for metadata)");
      System.err.println("+user/group_id (for 'allow' security)");
      System.err.println("-user/group_id (for 'disallow' security)");
      System.exit(1);
    }
    
    String directory = argv[0];
    File file = new File(argv[1]);
    
    Map metadata = new HashMap();
    Map allowed = new HashMap();
    Map disallowed = new HashMap();
    
    int i = 2;
    while (i < argv.length)
    {
      String item = argv[i++];
      if (item.startsWith("+"))
      {
        String token = item.substring(1);
        allowed.put(token,token);
      }
      else if (item.startsWith("-"))
      {
        String token = item.substring(1);
        disallowed.put(token,token);
      }
      else
      {
        int index = item.indexOf("=");
        if (index == -1)
        {
          System.err.println("Item '"+item+"' unrecognized");
          System.exit(1);
        }
        String name = item.substring(0,index);
        String value = item.substring(index+1);
        List list = (List)metadata.get(name);
        if (list == null)
        {
          list = new ArrayList();
          metadata.put(name,list);
        }
        list.add(value);
      }
    }
    
    // Almost ready.  Try to open the file.
    try
    {
      InputStream is = new FileInputStream(file);
      try
      {
        Docs4UAPI api = D4UFactory.makeAPI(directory);
        // Create a DocInfo object
        D4UDocInfo info = D4UFactory.makeDocInfo();
        try
        {
          // Set the data
          info.setData(is);
          // Set the metadata
          Iterator iter = metadata.keySet().iterator();
          while (iter.hasNext())
          {
            String dataName = (String)iter.next();
            List dataValue = (List)metadata.get(dataName);
            String[] dataValues = (String[])dataValue.toArray(new String[0]);
            info.setMetadata(dataName,dataValues);
          }

          String[] permissions;
          int count;
          
          // Set the 'allowed' permissions
          permissions = new String[allowed.size()];
          count = 0;
          iter = allowed.keySet().iterator();
          while (iter.hasNext())
          {
            String allowedPermission = (String)iter.next();
            permissions[count++] = allowedPermission;
          }
          info.setAllowed(permissions);
          
          // Set the 'disallowed' permissions
          permissions = new String[disallowed.size()];
          count = 0;
          iter = disallowed.keySet().iterator();
          while (iter.hasNext())
          {
            String disallowedPermission = (String)iter.next();
            permissions[count++] = disallowedPermission;
          }
          info.setDisallowed(permissions);

          // Create the document
          String id = api.createDocument(info);
          System.out.print(id);
        }
        finally
        {
          info.close();
        }
      }
      catch (D4UException e)
      {
        e.printStackTrace(System.err);
        System.exit(2);
      }
      finally
      {
        is.close();
      }
    }
    catch (IOException e)
    {
      e.printStackTrace(System.err);
      System.exit(3);
    }
  }
  
}
