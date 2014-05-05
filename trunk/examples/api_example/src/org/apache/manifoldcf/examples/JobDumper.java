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

import org.apache.manifoldcf.core.interfaces.*;

import java.io.*;
import java.net.*;

public class JobDumper
{
  private JobDumper()
  {
  }
  
  public static void main(String[] argv)
  {
    if (argv.length < 1 || argv.length > 2)
    {
      System.err.println("Usage: JobDumper <job_id> [<base_url>]");
      System.exit(2);
    }
    String jobID = argv[0];
    String baseURL;
    if (argv.length == 2)
      baseURL = argv[1];
    else
      baseURL = "http://localhost:8345/mcf-api-service";
    
    // Create the connection object
    ManifoldCFAPIConnect apiConnection = new ManifoldCFAPIConnect(baseURL);
    try
    {
      // Perform the GET
      Configuration root = apiConnection.performAPIGetOperation("/jobs/"+jobID);
      // Print out the node hierarchy.
      System.out.println("--NODE HIERARCHY FOLLOWS--");
      ManifoldCFAPIConnect.dumpConfiguration(root);
      // Print out the JSON.
      //System.out.println("--JSON FOLLOWS--");
      //System.out.println(root.toJSON());
      // Write an end marker.
      System.out.println("--END--");
    }
    catch (IOException e)
    {
      System.err.println("IO exception interacting with API: "+e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }
    catch (ManifoldCFException e)
    {
      System.err.println("JSON syntax error interacting with API: "+e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }
  
}