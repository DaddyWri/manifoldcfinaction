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

import java.rmi.server.*;
import java.net.*;
import java.io.IOException;

/** This factory mints client-side sockets.  I've created one so that RMI doesn't attempt
* to connect to anything other than localhost (127.0.0.1).  This is a security measure.
*/
public class RMILocalClientSocketFactory implements RMIClientSocketFactory, java.io.Serializable
{
  protected static InetAddress loopbackAddress;

  static
  {
    try
    {
      loopbackAddress = InetAddress.getByAddress(new byte[]{127,0,0,1});
    }
    catch (UnknownHostException e)
    {
      e.printStackTrace();
    }
  }

  /** Constructor */
  public RMILocalClientSocketFactory()
  {
  }

  /** The method that mints a socket of the right kind.
  */
  public Socket createSocket(String host, int port)
    throws IOException
  {
    return new LocalClientSocket(port);
  }

  /** The contract makes us implement equals and hashcode */
  public boolean equals(Object o)
  {
    return (o instanceof RMILocalClientSocketFactory);
  }

  /** Hashcode consistent with equals() */
  public int hashCode()
  {
    // All classes of this kind have the same number (randomly picked)
    return 259475;
  }

  /** This class wraps Socket and does not permit it to be directed to connect anywhere other than to localhost.
  */
  protected static class LocalClientSocket extends Socket
  {
    protected int currentPort;

    /** Constructor */
    public LocalClientSocket(int port)
      throws IOException
    {
      super(loopbackAddress,port);
      currentPort = port;
    }

    public void connect(SocketAddress endpoint)
      throws IOException
    {
      int thisPort = currentPort;
      if (endpoint instanceof InetSocketAddress)
        thisPort = ((InetSocketAddress)endpoint).getPort();
      endpoint = new InetSocketAddress(loopbackAddress,thisPort);
      super.connect(endpoint);
    }

    public void connect(SocketAddress endpoint, int timeout)
      throws IOException
    {
      int thisPort = currentPort;
      if (endpoint instanceof InetSocketAddress)
        thisPort = ((InetSocketAddress)endpoint).getPort();
      endpoint = new InetSocketAddress(loopbackAddress,thisPort);
      super.connect(endpoint,timeout);
    }
  }
}
