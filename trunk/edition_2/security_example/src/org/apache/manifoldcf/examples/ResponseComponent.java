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
package org.apache.manifoldcf.examples;

import java.util.*;
import java.io.*;

/** This class describes a single component of the authority service response.
*/
public class ResponseComponent
{
  // Response types
  public static final int RESPONSECOMPONENT_TOKEN = 0;
  public static final int RESPONSECOMPONENT_AUTHORIZED = 1;
  public static final int RESPONSECOMPONENT_UNREACHABLEAUTHORITY = 1;
  public static final int RESPONSECOMPONENT_UNAUTHORIZED = 2;
  public static final int RESPONSECOMPONENT_USERNOTFOUND = 3;
  
  /** This the the component type */
  protected int type;
  /** This is the component value */
  protected String value;
  
  /** This is the type map, for quick lookups */
  protected static Map<String,Integer> typeMap;
  
  static
  {
    typeMap = new HashMap<String,Integer>();
    typeMap.put("TOKEN",new Integer(RESPONSECOMPONENT_TOKEN));
    typeMap.put("AUTHORIZED",new Integer(RESPONSECOMPONENT_AUTHORIZED));
    typeMap.put("UNREACHABLEAUTHORITY",new Integer(RESPONSECOMPONENT_UNREACHABLEAUTHORITY));
    typeMap.put("UNAUTHORIZED",new Integer(RESPONSECOMPONENT_UNAUTHORIZED));
    typeMap.put("USERNOTFOUND",new Integer(RESPONSECOMPONENT_USERNOTFOUND));
  }
  
  /** Constructor.
  */
  public ResponseComponent(String componentString)
    throws IOException
  {
    int index = componentString.indexOf(":");
    if (index == -1)
      throw new IOException("Illegal component string: '"+componentString+"'");
    String typeString = componentString.substring(0,index);
    Integer typeInt = typeMap.get(typeString);
    if (typeInt == null)
      throw new IOException("Illegal component string: '"+componentString+"'");
    type = typeInt;
    value = componentString.substring(index+1);
  }
  
  /** Get the component type.
  *@return the type.
  */
  public int getType()
  {
    return type;
  }
  
  /** Get the component value.
  *@return the value.
  */
  public String getValue()
  {
    return value;
  }
  
}
