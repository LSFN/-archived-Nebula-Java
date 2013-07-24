package org.lsfn.nebula;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.lsfn.nebula.STS.STSup.Join.JoinType;
import org.lsfn.nebula.STS.*;

public class JoinManager {
    
    private static final long timeout = 5000;
    
    private Map<UUID, Long> timesConnected;
    private boolean allowJoins;
    
    public JoinManager() {
        this.timesConnected = new HashMap<UUID, Long>();
        this.allowJoins = true;
    }
    
    public void addConnection(UUID id) {
        this.timesConnected.put(id, System.currentTimeMillis());
    }
    
    public void processInput(UUID id, STSup.Join join) {
        if(join.getType() == JoinType.JOIN) {
            if(this.allowJoins) {
                this.timesConnected.remove(id);
                
            }
            
        }
    }
}
