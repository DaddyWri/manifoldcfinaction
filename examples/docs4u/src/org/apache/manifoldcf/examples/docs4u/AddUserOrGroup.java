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

import java.util.*;

/** This class adds a user or group to the Docs4U content management system.
*/
public class AddUserOrGroup
{
  private AddUserOrGroup()
  {
  }
  
  public static void main(String[] argv)
  {
    if (argv.length < 3)
    {
      System.err.println("Usage: AddUserOrGroup <directory> <name> <login_id> <group_1> ... <group_N>");
      System.exit(1);
    }
    
    String directory = argv[0];
    String name = argv[1];
    String loginID = argv[2];
    if (loginID.length() == 0)
      loginID = null;
    
    Map groups = new HashMap();
    
    int i = 3;
    while (i < argv.length)
    {
      String item = argv[i++];
      groups.put(item,item);
    }
    
    try
    {
      Docs4UAPI api = D4UFactory.makeAPI(directory);
      String[] groupArray = new String[groups.size()];
      int count = 0;
      Iterator iter = groups.keySet().iterator();
      while (iter.hasNext())
      {
        groupArray[count++] = (String)iter.next();
      }
      String id = api.createUserOrGroup(name,loginID,groupArray);
      System.out.print(id);
    }
    catch (D4UException e)
    {
      e.printStackTrace(System.err);
      System.exit(2);
    }
  }
  
}
