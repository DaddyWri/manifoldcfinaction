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
import java.net.*;

/** This is the main server class, where the session factory object is created
* and registered.
*/
public class ServerProcess
{
  private ServerProcess()
  {
  }
  
  /** Main server process method.  Create the session factory implementation, and register it
  * with a well-known name.
  */
  public static void main(String[] argv)
  {
    try
    {
      // Create the session factory object.
      SessionFactoryImpl factory = new SessionFactoryImpl();
      // Register the object, making sure we use the proper registry port.  This also
      // establishes the object's well-known name.
      Naming.rebind("//127.0.0.1:8401/session_factory", factory);
      System.out.println("Server started and is awaiting connections.");
      while (true)
      {
        Thread.sleep(600000L);
      }
    }
    catch (InterruptedException e)
    {
    }
    catch (RemoteException er)
    {
      System.err.println("Remote exception in server process: " + er);
      er.printStackTrace(System.err);
    }
    catch (MalformedURLException er)
    {
      System.err.println("Exception in server process: " + er);
      er.printStackTrace(System.err);
    }
  }
}
