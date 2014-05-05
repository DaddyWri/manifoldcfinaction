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

import java.rmi.*;

/** This class represents your connector, and is the RMI client.
*/
public class ClientProcess
{
  private ClientProcess()
  {
  }
  
  /** Main method.  Set up RMI and do a few operations.
  */
  public static void main(String[] argv)
  {
    try
    {
      // Get the factory
      System.out.println("Obtaining a factory handle...");
      // Use the proper registry port and the well-known name
      ISessionFactory factory = (ISessionFactory)Naming.lookup("rmi://127.0.0.1:8401/session_factory");
      // Get the session handle
      System.out.println("Getting a session handle...");
      ISession newSession = factory.make();
      // Now, let's call the one method we can!
      System.out.println("Calling remote method...");
      MyReturnObject result = newSession.doStuff(new MyArgumentObject("hello",123));
      System.out.println("Result: "+result.getValue());
      System.out.println("Done!");
    }
    catch (Exception e)
    {
      e.printStackTrace(System.err);
      System.exit(2);
    }
  }
}