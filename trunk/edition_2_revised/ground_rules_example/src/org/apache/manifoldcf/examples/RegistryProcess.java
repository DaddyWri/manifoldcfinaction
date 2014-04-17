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

/** This is the main RMI registry process class. */
public class RegistryProcess
{
  private RegistryProcess()
  {
  }
  
  /** Registry main method */
  public static void main(String[] argv)
  {
    try
    {
      // The registry must listen on a port that's unique to your connector.  I've chosen 8401
      // for this example.
      java.rmi.registry.Registry r = java.rmi.registry.LocateRegistry.createRegistry(8401,
        new RMILocalClientSocketFactory(),new RMILocalSocketFactory());
      // Registry started OK
      System.out.println("Registry started and is awaiting connections.");
      // Sleep forever, until process is externally terminated
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
      System.err.println("Remote exception in registry main: " + er);
      er.printStackTrace(System.err);
    }

  }
  
}