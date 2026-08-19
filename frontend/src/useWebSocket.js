import { useEffect, useRef, useCallback } from "react";
import { Client } from "@stomp/stompjs";

export default function useWebSocket({ onMessage, onPresence,onConnect }) {
    const clientRef = useRef(null);
    const heartbeatRef = useRef(null);

    const onMessageRef = useRef(onMessage);
    const onPresenceRef = useRef(onPresence);

    useEffect(() => {
        onMessageRef.current = onMessage;
    }, [onMessage]);

    useEffect(() => {
        onPresenceRef.current = onPresence;
    }, [onPresence]);

    const stopHeartbeat = () => {
        if (heartbeatRef.current) {
            clearInterval(heartbeatRef.current);
            heartbeatRef.current = null;
        }
    };

    useEffect(() => {
        console.log("[WS] Attempting connection...");

        const client = new Client({
            brokerURL: "ws://localhost:8080/ws/websocket",
            reconnectDelay: 5000,

            onConnect: () => {
                console.log("[WS] Connected");

                client.subscribe("/user/queue/messages", frame => {
                    const msg = JSON.parse(frame.body);
                    onMessageRef.current?.(msg);
                });

                client.subscribe("/topic/presence", frame => {
                    const presence = JSON.parse(frame.body);
                    onPresenceRef.current?.(presence);
                });

                onConnect?.();

                stopHeartbeat();

                heartbeatRef.current = setInterval(() => {
                    if (client.connected) {
                        client.publish({
                            destination: "/app/presence.heartbeat",
                            body: JSON.stringify({})
                        });
                    }
                }, 10000);
            },

            onDisconnect: () => {
                console.log("[WS] Disconnected");
                stopHeartbeat();
            },

            onStompError: frame => {
                console.error("[WS] STOMP error", frame);
            },

            onWebSocketError: error => {
                console.error("[WS] WebSocket error", error);
            },

            onWebSocketClose: event => {
                console.log("[WS] WebSocket closed", event.code, event.reason);
                stopHeartbeat();
            }
        });

        client.activate();
        clientRef.current = client;

        return () => {
            stopHeartbeat();
            void client.deactivate();
            clientRef.current = null;
        };
    }, []);

    const sendMessage = useCallback((destination, payload = {}) => {
        const client = clientRef.current;

        if (!client?.connected) {
            console.warn("[WS] Cannot send, socket not connected");
            return;
        }

        client.publish({
            destination,
            body: JSON.stringify(payload)
        });
    }, []);

    return { sendMessage };
}