package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;
import java.util.Set;

public interface PortAllocator {

    int allocatePort(String groupId);

    int allocateTcpPort(String groupId, boolean tcps);

    void freeAll();

    void freeAllExcept(String groupId, Set<Integer> ports);

    Map<String, Set<Integer>> getAllocatedPorts();

    void setAllocatedPorts(Map<String, Set<Integer>> allocatedPorts);

}
