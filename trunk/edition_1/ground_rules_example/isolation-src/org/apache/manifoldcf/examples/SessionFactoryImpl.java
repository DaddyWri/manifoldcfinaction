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

/** Session factory implementation.  This class exists only on the server.
*/
public class SessionFactoryImpl extends UnicastRemoteObject
  implements ISessionFactory
{
  /** Constructor */
  public SessionFactoryImpl()
    throws RemoteException
  {
    // Use the correct socket factories to limit exposure to off-machine
    // requests!
    super(0,new RMILocalClientSocketFactory(),new RMILocalSocketFactory());
  }
  
  /** Return an actual session object. */
  public ISession make()
    throws RemoteException
  {
    // Construct a session implementation object.  Because it's an
    // RMI object, this object will NOT be returned to the remote process!
    // Instead, a stub representing it will be created.
    return new SessionImpl();
  }
  
}