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

/** This class demonstrates lock usage, written
* as a ManifoldCF core command.
*/
public class LockExerciser
{
  /** The lock name we'll be exercising.  Any string should work.  */
  protected static final String LOCK_NAME = "my lock";
  
  /** The number of reader threads. */
  protected static final int NUM_READER_THREADS = 3;
  
  private LockExerciser()
  {
  }
  
  public static void main(String[] argv)
  {
    try
    {
      // All processes must have initialization
      ManifoldCF.initializeEnvironment(ThreadContextFactory.make());
      Logging.misc.debug("System successfully initialized");
      
      // Create the shared resource we're going to protect
      MyResource resource = new MyResource();
      
      int i;
      
      // Create the lock reader threads
      ReaderThread[] readerThreads = new ReaderThread[NUM_READER_THREADS];
      i = 0;
      while (i < readerThreads.length)
      {
        readerThreads[i] = new ReaderThread("Reader#"+Integer.toString(i),resource);
        i++;
      }

      // Create the lock writer threads
      String[][] values = new String[][]{
        new String[]{"the quick brown fox"," jumps over the lazy dogs"},
        new String[]{"oh, be a fine girl",", kiss me"}};
      
      WriterThread[] writerThreads = new WriterThread[values.length];
      i = 0;
      while (i < writerThreads.length)
      {
        writerThreads[i] = new WriterThread("Writer#"+Integer.toString(i),
          resource,values[i][0],values[i][1]);
        i++;
      }
      
      // Prepare to start the threads.  We'll need a second try/catch, because we want to be
      // sure we shut the threads down on exit.
      try
      {
        i = 0;
        while (i < readerThreads.length)
        {
          readerThreads[i++].start();
        }
        i = 0;
        while (i < writerThreads.length)
        {
          writerThreads[i++].start();
        }
        
        Logging.misc.debug("Threads have been started");

        // Let the threads run for a while, and then we'll exit and shut them down
        ManifoldCF.sleep(5000L);
      }
      finally
      {
        Logging.misc.debug("Shutting threads down");

        while (true)
        {
          // We continue until the threads have given up on their own.
          boolean aliveThreadsExist = false;
          i = 0;
          while (i < readerThreads.length)
          {
            if (readerThreads[i].isAlive())
            {
              aliveThreadsExist = true;
              readerThreads[i].interrupt();
            }
            i++;
          }
          i = 0;
          while (i < writerThreads.length)
          {
            if (writerThreads[i].isAlive())
            {
              aliveThreadsExist = true;
              writerThreads[i].interrupt();
            }
            i++;
          }
          if (!aliveThreadsExist)
            break;
          
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
  
  protected static class ReaderThread extends Thread
  {
    protected String threadName;
    protected MyResource resource;
    
    /** Constructor. */
    public ReaderThread(String threadName, MyResource resource)
    {
      super(threadName);
      setDaemon(true);
      this.threadName = threadName;
      this.resource = resource;
    }
    
    public void run()
    {
      // First, create a thread context
      IThreadContext tc = ThreadContextFactory.make();
      
      // Catch any exceptions
      try
      {
        // Make a lock manager
        ILockManager lockManager = LockManagerFactory.make(tc);
        
        // Here's a counter to keep our events straight
        int counter = 0;
        
        // Now, repeatedly grab locks and do things
        while (true)
        {
          // Make sure we catch the exit signal
          if (Thread.currentThread().isInterrupted())
            break;
          
          // This cycle's output string
          String eventName = threadName + ", event "+Integer.toString(counter);
          
          // Enter the read lock
          lockManager.enterReadLock(LOCK_NAME);
          try
          {
            // First half of read lock
            String theString = resource.firstPart;
            System.out.println(eventName + ": "+theString+"...");
            // Yield, to make other thread interruption more likely
            Thread.yield();
            // Second half of read lock
            System.out.println(eventName + ": "+theString + resource.secondPart);
            // Yield, to make other thread interruption more likely
            Thread.yield();
          }
          finally
          {
            lockManager.leaveReadLock(LOCK_NAME);
          }
          counter++;
          // Just yield; we want to spin as quick as we can to demonstrate locking
          Thread.yield();
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
    }
  }

  protected static class WriterThread extends Thread
  {
    protected String threadName;
    protected MyResource resource;
    protected String firstString;
    protected String secondString;
    
    /** Constructor. */
    public WriterThread(String threadName, MyResource resource, String firstString,
      String secondString)
    {
      super(threadName);
      setDaemon(true);
      this.threadName = threadName;
      this.resource = resource;
      this.firstString = firstString;
      this.secondString = secondString;
    }
    
    public void run()
    {
      // First, create a thread context
      IThreadContext tc = ThreadContextFactory.make();
      
      // Catch any exceptions
      try
      {
        // Make a lock manager
        ILockManager lockManager = LockManagerFactory.make(tc);
        
        // Here's a counter to keep our events straight
        int counter = 0;
        
        // Now, repeatedly grab locks and do things
        while (true)
        {
          // Make sure we catch the exit signal
          if (Thread.currentThread().isInterrupted())
            break;
          
          // Enter the write lock
          lockManager.enterWriteLock(LOCK_NAME);
          try
          {
            // First half of write lock
            resource.firstPart = firstString;
            // Yield, to make other thread interruption more likely
            Thread.yield();
            // Second half of write lock
            resource.secondPart = secondString;
            // Yield, to make other thread interruption more likely
            Thread.yield();
          }
          finally
          {
            lockManager.leaveWriteLock(LOCK_NAME);
          }
          counter++;
          // Just yield; we want to spin as quick as we can to demonstrate locking
          Thread.yield();
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
    }
  }

  /** My resource, that we want to keep self-consistent. */
  protected static class MyResource
  {
    public String firstPart = "";
    public String secondPart = "";
  }
  
}
