package net.heartbeat.util;

public interface IModWithCustomUpdateState {
    boolean heartbeat$hasModBeenUpdated();
    void heartbeat$setModAsUpdated();
}
