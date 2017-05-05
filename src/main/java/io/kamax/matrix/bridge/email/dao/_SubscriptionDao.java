package io.kamax.matrix.bridge.email.dao;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface _SubscriptionDao {

    void store(BridgeSubscriptionDao dao);

    void delete(String id);

    List<BridgeSubscriptionDao> list();

}
