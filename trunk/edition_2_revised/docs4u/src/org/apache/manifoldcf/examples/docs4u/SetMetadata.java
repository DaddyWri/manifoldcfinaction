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

/** This class sets metadata values for the Docs4U content management system.
*/
public class SetMetadata
{
  private SetMetadata()
  {
  }
  
  public static void main(String[] argv)
  {
    if (argv.length < 1)
    {
      System.err.println("Usage: SetMetadata <directory> <value_1> ... <value_N>");
      System.exit(1);
    }
    
    String directory = argv[0];
    String[] metadataNames = new String[argv.length - 1];
    int i = 0;
    while (i < metadataNames.length)
    {
      metadataNames[i] = argv[i+1];
      i++;
    }
    
    try
    {
      Docs4UAPI api = D4UFactory.makeAPI(directory);
      api.setMetadataNames(metadataNames);
    }
    catch (D4UException e)
    {
      e.printStackTrace(System.err);
      System.exit(2);
    }
  }
  
}
