package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.CollectionUtils;

public class PortAllocatorMock implements PortAllocator {

    private Map<String, Set<Integer>> allocatedPorts = new HashMap<String, Set<Integer>>();
    private int minPort;

    public PortAllocatorMock(int minPort) {
        this.minPort = minPort;
    }

    @Override
    public void freeAllExcept(String groupId, Set<Integer> ports) {
        Set<Integer> allocatedPortsForGroup = getAllocatedPortsForGroup(groupId);
        allocatedPortsForGroup.retainAll(ports);
        allocatedPorts.put(groupId, allocatedPortsForGroup);
    }

    @Override
    public void freeAll() {
        allocatedPorts.clear();
    }

    @Override
    public int allocatePort(String groupId) {
        int allocatedPort = minPort++;
        addPortInGroup(groupId, allocatedPort);
        return allocatedPort;
    }

    @Override
    public void setAllocatedPorts(Map<String, Set<Integer>> allocatedPorts) {
        this.allocatedPorts = allocatedPorts;
    }

    @Override
    public Map<String, Set<Integer>> getAllocatedPorts() {
        return allocatedPorts;
    }

    @Override
    public int allocateTcpPort(String groupId, boolean tcps) {
        return allocatePort(groupId);
    }

    private Set<Integer> getAllocatedPortsForGroup(String groupId) {
        if (CollectionUtils.isEmpty(allocatedPorts.get(groupId))) {
            return new TreeSet<Integer>();
        }
        return allocatedPorts.get(groupId);
    }
    
    private void addPortInGroup(String groupId, int port) {
        Set<Integer> allocatedPortsForGroup = getAllocatedPortsForGroup(groupId);
        allocatedPortsForGroup.add(port);
        allocatedPorts.put(groupId, allocatedPortsForGroup);
    }

}
