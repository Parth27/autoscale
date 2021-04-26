package com.autoscale.core;

public abstract class ApplicationServer {

    public boolean isStarted = false;
    
    public abstract void initialize();

    public abstract void terminate();
}
