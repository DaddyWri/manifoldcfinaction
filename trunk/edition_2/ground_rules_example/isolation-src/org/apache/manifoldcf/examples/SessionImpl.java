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
import java.rmi.server.UnicastRemoteObject;

/** Session implementation.  This class exists only on the server.
*/
public class SessionImpl extends UnicastRemoteObject
  implements ISession
{
  /** Constructor */
  public SessionImpl()
    throws RemoteException
  {
    // Use the correct socket factories to limit exposure to off-machine
    // requests!
    super(0,new RMILocalClientSocketFactory(),new RMILocalSocketFactory());
  }

  /** Our method which demonstrates something actually happening. */
  public MyReturnObject doStuff(MyArgumentObject inputObject)
    throws MyException, RemoteException
  {
    if (inputObject.getField2() == 0)
      throw new MyException("Hey, you sent in a zero!");
    
    System.out.println("The server process got a string of '"+inputObject.getField1()+"'");
    
    // Respond with a string of our own.
    return new MyReturnObject("This is my response");
  }

}
