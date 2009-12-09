package org.drools.runtime;

import org.drools.KnowledgeBase;
import org.drools.event.KnowledgeRuntimeEventManager;
import org.drools.runtime.process.ProcessRuntime;
import org.drools.runtime.rule.WorkingMemory;
import org.drools.time.SessionClock;

public interface KnowledgeRuntime
    extends
    WorkingMemory,
    ProcessRuntime,
    KnowledgeRuntimeEventManager {

    /**
     * Returns the session clock instance assigned to this session
     * @return
     */
    public <T extends SessionClock> T getSessionClock();
    
    /**
     * Sets a global value on the internal collection
     * @param identifer the global identifier
     * @param value the value assigned to the global identifier
     */
    void setGlobal(String identifier,
                   Object object);

    Object getGlobal(String identifier);

    Globals getGlobals();
    
    Calendars getCalendars();

    Environment getEnvironment();

    /**
     * Returns the KnowledgeBase reference from which this stateful session was created.
     * 
     * @return
     */
    KnowledgeBase getKnowledgeBase();

    void registerExitPoint(String name,
                           ExitPoint exitPoint);

    void unregisterExitPoint(String name);

}
