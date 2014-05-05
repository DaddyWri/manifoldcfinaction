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
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.apache.manifoldcf.core.system.Logging;
import java.util.*;
import java.io.*;

/** This class demonstrates threading and global unique identifier generation, written
* as a ManifoldCF core command.
*/
public class UniqueIDGenerator
{
  private UniqueIDGenerator()
  {
  }
  
  public static void main(String[] argv)
  {
    try
    {
      // All processes must have initialization
      ManifoldCF.initializeEnvironment();
      Logging.misc.debug("System successfully initialized");
      
      // Create the worker threads
      IDGeneratorThread thread1 = new IDGeneratorThread("first thread");
      IDGeneratorThread thread2 = new IDGeneratorThread("second thread");
      
      // Prepare to start the threads.  We'll need a second try/catch, because we want to be
      // sure we shut the threads down on exit.
      try
      {
        thread1.start();
        thread2.start();
        
        Logging.misc.debug("Threads have been started");

        // Let the threads run for a while, and then we'll exit and shut them down
        ManifoldCF.sleep(60000L);
      }
      finally
      {
        Logging.misc.debug("Shutting threads down");

        while (true)
        {
          // We continue until the threads have given up on their own.
          if (!thread1.isAlive() && !thread2.isAlive())
            break;
          if (thread1.isAlive())
            thread1.interrupt();
          if (thread2.isAlive())
            thread2.interrupt();
          
          // Yield, so the other threads can do stuff.
          Thread.yield();
        }
        
        Logging.misc.debug("Threads are down");
      }
      
      Logging.misc.debug("Example run complete");
    }
    catch (ManifoldCFException e)
    {
      // Exception during initialization: Print it to standard error, and quit
      e.printStackTrace(System.err);
      System.exit(-1);
    }
    catch (InterruptedException e)
    {
      // If we were interrupted, just exit
    }
  }
  
  protected static class IDGeneratorThread extends Thread
  {
    protected String threadName;
    
    /** Constructor. */
    public IDGeneratorThread(String threadName)
    {
      super(threadName);
      setDaemon(true);
      this.threadName = threadName;
    }
    
    public void run()
    {
      // First, create a thread context
      IThreadContext tc = ThreadContextFactory.make();
      
      // Catch any exceptions
      try
      {
        // Now, generate and log a global identifier every second
        while (true)
        {
          // Make sure we catch the exit signal
          if (Thread.currentThread().isInterrupted())
            break;
          
          System.out.println("Identifier: "+threadName+": "+IDFactory.make(tc));
          
          ManifoldCF.sleep(1000L);
        }
      }
      catch (ManifoldCFException e)
      {
        // If we were interrupted, just exit the thread
        if (e.getErrorCode() != ManifoldCFException.INTERRUPTED)
        {
          // Otherwise, log the error and exit
          Logging.misc.error("Thread got unexpected exception: "+e.getMessage(),e);
        }
      }
      catch (InterruptedException e)
      {
        // Interrupted!  eat the exception and exit
      }
    }
    
  }
  
}
