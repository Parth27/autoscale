package com.autoscale.core;

import java.util.List;

public interface MetaServer {
    
    public void start(List<String> serverList);
    
    public void rebalance(List<String> serverList);
}
