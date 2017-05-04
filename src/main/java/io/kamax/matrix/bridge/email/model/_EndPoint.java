package io.kamax.matrix.bridge.email.model;

/**
 * To be used when refactoring
 */
public interface _EndPoint<K, V extends _BridgeMessage> {

    String getId();

    K getIdentity();

    void open();

    void close();

    void sendMessage(V msg);

    void sendNotification(_BridgeEvent ev);

}
