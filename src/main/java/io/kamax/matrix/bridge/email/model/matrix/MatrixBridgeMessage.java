package io.kamax.matrix.bridge.email.model.matrix;

import io.kamax.matrix._MatrixUser;
import io.kamax.matrix.bridge.email.model.ABridgeMessage;
import io.kamax.matrix.bridge.email.model.BridgeMessageTextContent;

import java.util.Collections;

public class MatrixBridgeMessage extends ABridgeMessage<_MatrixUser> implements _MatrixBridgeMessage {

    public MatrixBridgeMessage(String key, _MatrixUser sender, String txtContent) {
        super(key, sender, Collections.singletonList(new BridgeMessageTextContent(txtContent)));
    }

}