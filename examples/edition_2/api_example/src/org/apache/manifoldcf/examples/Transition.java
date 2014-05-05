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

/** The transition interface describes the conditions for a transition from one state
* to another, the actions needed to perform the transition, and the final state of the
* transition.
*/
public interface Transition
{
  /** This method is called by the engine to determine when this
  * transition should be taken.
  *@return true if the transition should be taken.
  */
  public boolean isTransitionReady()
    throws InterruptedException;
  
  /** Call this method to execute the transition.
  *@return the new state, or null if the system should shut down.
  */
  public State executeTransition()
    throws InterruptedException;
}