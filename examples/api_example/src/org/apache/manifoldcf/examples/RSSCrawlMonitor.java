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

import java.io.*;
import java.util.*;
import org.apache.manifoldcf.core.interfaces.*;

/** This is the main RSS crawl monitor class.  It creates and monitors an
* RSS feed crawl according to a state/transition diagram, using the
* ManifoldCF API to do the actual work.
*/
public class RSSCrawlMonitor
{
  /** The name of the repository connection */
  protected static final String REPOSITORY_CONNECTION_NAME =
    "RSS Connection";
  /** The name of the job */
  protected static final String JOB_NAME = "RSS Continuous Job";
  
  /** This is the connection to the API that allows us to get things
  * done. */
  protected RSSMCFAPISupport apiAccess;
  /** This is the file where the feeds are kept.  Edits to this file will
  * automatically update the crawl. */
  protected File feedFile;
  /** This is the output connection name */
  protected String outputConnectionName;
  /** Time that we entered the last state. */
  protected long lastStateTime = -1L;
  /** Last feed file modification time. */
  protected long lastFeedModificationTime = -1L;
  
  // The states.
  
  /** The starting state.  This is the only state that can create
  * the connection and the job. */
  protected BasicState startState = new BasicState();
  /** This state means we have a job, but it is not yet running. */
  protected BasicState notYetRunningState = new BasicState();
  /** This state is the main state where the job is running and all
  * is well. */
  protected BasicState runningState = new BasicState();
  /** This state is where we go when the job has aborted. */
  protected BasicState notRunningState = new BasicState();
  /** This state means we've had an abort, and are seeing if we get
  * another one. */
  protected BasicState monitoringState = new BasicState();
  /** The output needs attention! */
  protected BasicState outputNeedsAttentionState = new BasicState();
  /** The job needs attention! */
  protected BasicState jobNeedsAttentionState = new BasicState();

  /** Constructor */
  public RSSCrawlMonitor(String baseURL, String outputConnectionName,
    File feedFile)
  {
    this.apiAccess = new RSSMCFAPISupport(baseURL);
    this.outputConnectionName = outputConnectionName;
    this.feedFile = feedFile;
    
    // The states have already been constructed, so create the
    // transitions and add them.
    
    // Transition to the "output needs attention" state.  All states
    // are given this transition.
    Transition outputCheck = 
      new OutputNotOKCheck(outputNeedsAttentionState);
    startState.addTransition(outputCheck);
    notYetRunningState.addTransition(outputCheck);
    runningState.addTransition(outputCheck);
    notRunningState.addTransition(outputCheck);
    monitoringState.addTransition(outputCheck);
    // Check whether the connection exists; create it if not.
    startState.addTransition(new ConnectionNotExistsCheck(startState));
    // Check whether the job exists; create it if not.
    startState.addTransition(new JobNotExistsCheck(startState));
    // Check whether the job is already running, and transition to
    // the running state if so.
    startState.addTransition(new JobRunningCheck(runningState));
    // Check whether job is not running, and transition to the
    // not-yet-running state if so.
    startState.addTransition(new JobNotRunningCheck(notYetRunningState));
    // Check if the job is in a state to be started, and start it if so.
    notYetRunningState.addTransition(new JobStartCheck(runningState));
    // Check if the job has been in the not-yet-running state for too
    // long, and scream for attention if so.
    notYetRunningState.addTransition(
      new Timeout(300000L,jobNeedsAttentionState));
    // Check if the job is still running, and if not, transition to the
    // not running state.
    runningState.addTransition(new JobNotRunningCheck(notRunningState));
    // If the feed list has changed, update the job.
    runningState.addTransition(new FeedUpdateCheck(runningState));
    // Check if the job can be started, and if so, transition to
    // the monitoring state.
    notRunningState.addTransition(new JobStartCheck(monitoringState));
    // If the job has been unable to be started for long enough, it
    // needs attention.
    notRunningState.addTransition(
      new Timeout(300000L,jobNeedsAttentionState));
    // If the job stops while being monitored, it needs attention.
    monitoringState.addTransition(
      new JobNotRunningCheck(jobNeedsAttentionState));
    // If the job seems OK, we can go back to the 'running' state.
    monitoringState.addTransition(new Timeout(600000L,runningState));
    // If the output is better now, return to the starting state.
    outputNeedsAttentionState.addTransition(new OutputOKCheck(startState));
    // As long as we are in the output-needs-attention state, make sure
    // we signal a problem.
    outputNeedsAttentionState.addTransition(
      new Alarm("Output needs attention!",outputNeedsAttentionState));
    // If the job is running now, return to the running state.
    jobNeedsAttentionState.addTransition(
      new JobRunningCheck(runningState));
    // As long as we are in the job-needs-attention state, make sure
    // we signal a problem.
    jobNeedsAttentionState.addTransition(new Alarm(
      "Job needs attention!",jobNeedsAttentionState));
  }
  
  /** Run the monitor. */
  public void run()
    throws InterruptedException
  {
    // Create the engine, and run it.
    Engine engine = new Engine(startState);
    engine.execute();
  }
  
  /** Method for sounding the alarm.
  *@param message is the message to display.
  */
  protected void soundTheAlarm(String message)
  {
    System.err.println("ALARM: "+message);
  }
  
  /** Method to read feeds from a file.
  *@param feedFile is the file to read.
  *@return the list of feeds from that file.
  */
  protected static List<String> readFeeds(File feedFile)
    throws IOException
  {
    BufferedReader br = new BufferedReader(new FileReader(feedFile));
    try
    {
      ArrayList<String> rval = new ArrayList<String>();
      while (true)
      {
        String s = br.readLine();
        if (s == null)
          break;
        rval.add(s);
      }
      return rval;
    }
    finally
    {
      br.close();
    }
  }
  
  // State and transition classes
  
  /** Basic state class, allowing any number of transitions to be added.
  */
  protected static class BasicState implements State
  {
    protected ArrayList<Transition> transitions =
      new ArrayList<Transition>();
    
    public Transition[] getTransitions()
    {
      Transition[] rval = new Transition[transitions.size()];
      transitions.toArray(rval);
      return rval;
    }
    
    public void addTransition(Transition t)
    {
      transitions.add(t);
    }
  }

  /** Transition base class, which handles API exceptions in a
  * consistent way.
  */
  protected abstract class BaseTransition implements Transition
  {
    protected State targetState;
    protected Exception theException = null;
    
    public BaseTransition(State targetState)
    {
      this.targetState = targetState;
    }

    public boolean isTransitionReady()
      throws InterruptedException
    {
      try
      {
        return checkTransition();
      }
      catch (InterruptedIOException e)
      {
        throw new InterruptedException(e.getMessage());
      }
      catch (Exception e)
      {
        theException = e;
        return true;
      }
    }

    public State executeTransition()
      throws InterruptedException
    {
      if (theException != null)
      {
        theException.printStackTrace(System.err);
        soundTheAlarm("Lost communication with ManifoldCF");
        return null;
      }
      try
      {
        doTransition();
      }
      catch (InterruptedIOException e)
      {
        throw new InterruptedException(e.getMessage());
      }
      catch (Exception e)
      {
        e.printStackTrace(System.err);
        soundTheAlarm("Lost communication with ManifoldCF");
        return null;
      }
      return targetState;
    }
    
    protected boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      return true;
    }
    
    protected void doTransition()
      throws IOException, ManifoldCFException
    {
    }
  }

  /** Transition class for checking if the output is OK, and if not,
  * make the transition.
  */
  protected class OutputNotOKCheck extends BaseTransition
  {
    protected String statusString;
    
    public OutputNotOKCheck(State targetState)
    {
      super(targetState);
    }
    
    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      statusString = apiAccess.checkOutputConnection(outputConnectionName);
      return statusString != null;
    }

    public void doTransition()
      throws IOException, ManifoldCFException
    {
      System.out.println("Status from output was '"+statusString+"'");
    }
  }
  
  /** Transition class which checks if a connection doesn’t exist,
  * and creates it if not.
  */
  protected class ConnectionNotExistsCheck extends BaseTransition
  {
    public ConnectionNotExistsCheck(State targetState)
    {
      super(targetState);
    }
    
    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      return !apiAccess.doesRepositoryConnectionExist(
        REPOSITORY_CONNECTION_NAME);
    }
    
    public void doTransition()
      throws IOException, ManifoldCFException
    {
      // Create the repository connection
      apiAccess.createRSSRepositoryConnection(REPOSITORY_CONNECTION_NAME,
        "Continuous RSS repository connection",
        new Double(1.6666666E-4),"someone@somewhere.com",
        new Integer(64), new Integer(2), new Integer(12));
      System.out.println("Created repository connection '"+
        REPOSITORY_CONNECTION_NAME+"'");
    }
  }
  
  protected class JobNotExistsCheck extends BaseTransition
  {
    public JobNotExistsCheck(State targetState)
    {
      super(targetState);
    }
    
    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      return apiAccess.findJobID(JOB_NAME) == null;
    }
    
    public void doTransition()
      throws IOException, ManifoldCFException
    {
      lastFeedModificationTime = feedFile.lastModified();
      // Create the job
      String jobID = apiAccess.createRSSJob(JOB_NAME,
        REPOSITORY_CONNECTION_NAME,
        outputConnectionName,5,new Long(2592000000L),null,
        new Long(3600000L),readFeeds(feedFile),new Integer(60),
        new Integer(15),new Integer(1440),new Configuration());
      System.out.println("Created job "+jobID);
    }
  }
  
  protected class JobRunningCheck extends BaseTransition
  {
    protected String jobID;
    
    public JobRunningCheck(State targetState)
    {
      super(targetState);
    }

    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      jobID = apiAccess.findJobID(JOB_NAME);
      if (jobID == null)
        return false;
      String jobStatus = apiAccess.getJobStatus(jobID);
      if (jobStatus == null)
        return false;
      return jobStatus.equals("running") ||
        jobStatus.equals("starting up");
    }
  }
  
  protected class JobNotRunningCheck extends BaseTransition
  {
    protected String jobID;
    
    public JobNotRunningCheck(State targetState)
    {
      super(targetState);
    }

    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      jobID = apiAccess.findJobID(JOB_NAME);
      if (jobID == null)
        return false;
      String jobStatus = apiAccess.getJobStatus(jobID);
      if (jobStatus == null)
        return false;
      return !jobStatus.equals("running") &&
        !jobStatus.equals("starting up");
    }
    
    public void doTransition()
      throws IOException, ManifoldCFException
    {
      lastStateTime = System.currentTimeMillis();
    }
  }

  protected class JobStartCheck extends BaseTransition
  {
    protected String jobID;
    
    public JobStartCheck(State targetState)
    {
      super(targetState);
    }

    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      jobID = apiAccess.findJobID(JOB_NAME);
      if (jobID == null)
        return false;
      String jobStatus = apiAccess.getJobStatus(jobID);
      if (jobStatus == null)
        return false;
      return jobStatus.equals("not yet run") || jobStatus.equals("done") ||
        jobStatus.equals("error");
    }

    public void doTransition()
      throws IOException, ManifoldCFException
    {
      // Start the job
      System.out.println("Starting job "+jobID);
      apiAccess.startJob(jobID);
      lastStateTime = System.currentTimeMillis();
    }
  }
  
  protected class Timeout extends BaseTransition
  {
    long timeout;
    
    public Timeout(long timeout, State targetState)
    {
      super(targetState);
      this.timeout = timeout;
    }
    
    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      return System.currentTimeMillis() - lastStateTime >= timeout;
    }
  }
  
  protected class FeedUpdateCheck extends BaseTransition
  {
    String jobID;
    long currentModificationTime;
    
    public FeedUpdateCheck(State targetState)
    {
      super(targetState);
    }
    
    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      jobID = apiAccess.findJobID(JOB_NAME);
      if (jobID == null)
        return false;
      currentModificationTime = feedFile.lastModified();
      return currentModificationTime != lastFeedModificationTime;
    }
    
    public void doTransition()
      throws IOException, ManifoldCFException
    {
      // Update feeds
      System.out.println("Updating feeds");
      apiAccess.setRSSJobFeeds(jobID,readFeeds(feedFile));
      lastFeedModificationTime = currentModificationTime;
    }
  }
  
  protected class OutputOKCheck extends BaseTransition
  {
    public OutputOKCheck(State targetState)
    {
      super(targetState);
    }
    
    public boolean checkTransition()
      throws IOException, ManifoldCFException
    {
      return apiAccess.checkOutputConnection(outputConnectionName) == null;
    }
  }

  protected class Alarm extends BaseTransition
  {
    protected String alarmString;
    
    public Alarm(String alarmString, State targetState)
    {
      super(targetState);
      this.alarmString = alarmString;
    }
    
    public void doTransition()
      throws IOException, ManifoldCFException
    {
      // Sound the alarm!
      soundTheAlarm(alarmString);
    }
  }
  
  /** Main method. */
  public static void main(String[] argv)
  {
    if (argv.length != 3)
    {
      System.err.println("Usage: RSSCrawlMonitor <base_URL> "+
        "<output_connection_name> <feed_file>");
      System.exit(1);
    }
    RSSCrawlMonitor cm = new RSSCrawlMonitor(argv[0],argv[1],
      new File(argv[2]));
    try
    {
      cm.run();
    }
    catch (InterruptedException e)
    {
    }
  }
}
