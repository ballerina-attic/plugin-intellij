/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ballerinalang.plugins.idea.debugger;

import com.intellij.openapi.diagnostic.Logger;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import org.ballerinalang.plugins.idea.debugger.protocol.Command;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BallerinaWebSocketConnector {

    private static final Logger LOGGER = Logger.getInstance(BallerinaWebSocketConnector.class);

    private WebSocket myWebSocket;
    private String myAddress;
    private ConnectionState myConnectionState;
    private static WebSocketFactory webSocketFactory = new WebSocketFactory();

    private static final int TIMEOUT = 10000;
    private static final String DEBUG_PROTOCOL = "ws://";
    private static final String DEBUG_WEB_SOCKET_PATH = "/debug";

    public BallerinaWebSocketConnector(@NotNull String address) {
        myAddress = address;
        myConnectionState = ConnectionState.NOT_CONNECTED;
        webSocketFactory.setConnectionTimeout(TIMEOUT);
        createConnection();
    }

    void createConnection() {
        try {
            myWebSocket = webSocketFactory.createSocket(getUri());
            myWebSocket.addListener(new BallerinaWebSocketListenerAdapter());
            myWebSocket.connect();
        } catch (IOException | WebSocketException e) {
            myConnectionState = ConnectionState.ERROR;
            LOGGER.debug(e);
        }
    }

    @NotNull
    private String getUri() {
        return DEBUG_PROTOCOL + myAddress + DEBUG_WEB_SOCKET_PATH;
    }

    void sendCommand(Command command) {
        if (isConnected()) {
            myWebSocket.sendText(generateRequest(command));
        }
    }

    private String generateRequest(Command command) {
        return "{\"command\":\"" + command + "\"}";
    }

    void send(String json) {
        if (isConnected()) {
            myWebSocket.sendText(json);
        }
    }

    boolean isConnected() {
        return myWebSocket.isOpen();
    }

    void addListener(WebSocketAdapter adapter) {
        myWebSocket.addListener(adapter);
    }

    String getState() {
        if (myConnectionState == ConnectionState.NOT_CONNECTED) {
            return "Not connected.";
        } else if (myConnectionState == ConnectionState.CONNECTED) {
            return "Connected to " + myWebSocket.getURI() + ".";
        } else if (myConnectionState == ConnectionState.DISCONNECTED) {
            return "Disconnected.";
        } else if (myConnectionState == ConnectionState.ERROR) {
            return "Error occurred.";
        }
        return "Unknown";
    }

    private enum ConnectionState {
        NOT_CONNECTED, CONNECTED, DISCONNECTED, ERROR
    }

    private class BallerinaWebSocketListenerAdapter extends WebSocketAdapter {

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            myConnectionState = ConnectionState.CONNECTED;
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            myConnectionState = ConnectionState.ERROR;
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame
                clientCloseFrame, boolean closedByServer) throws Exception {
            myConnectionState = ConnectionState.DISCONNECTED;
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            myConnectionState = ConnectionState.ERROR;
        }

        @Override
        public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame)
                throws Exception {
            myConnectionState = ConnectionState.ERROR;
        }

        @Override
        public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames)
                throws Exception {
            myConnectionState = ConnectionState.ERROR;
        }

        @Override
        public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed)
                throws Exception {
            myConnectionState = ConnectionState.ERROR;
        }

        @Override
        public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws
                Exception {
            myConnectionState = ConnectionState.ERROR;
        }

        @Override
        public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws
                Exception {
            myConnectionState = ConnectionState.ERROR;
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            myConnectionState = ConnectionState.ERROR;
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
            myConnectionState = ConnectionState.ERROR;
        }
    }
}