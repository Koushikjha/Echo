import React, { useState, useEffect, useRef, useCallback } from "react";
import api from "./api.js";
import useWebSocket from "./useWebSocket.js";


const timeNow = () => new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
const formatTime = (dt) => dt ? new Date(dt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "";

const Avatar = ({ initials, color, size = 38, bgColor = "#16161d" }) => (
    <div style={{ position: "relative", flexShrink: 0 }}>
        <div style={{
            width: size, height: size, borderRadius: "50%", background: color,
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: size * 0.35, fontWeight: 600, color: "#fff",
            letterSpacing: "0.5px", fontFamily: "'DM Mono', monospace", userSelect: "none",
        }}>
            {initials}
        </div>
    </div>
);

const stringToColor = (str) => {
    if (!str) return "#6C63FF";
    let hash = 0;
    for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
    const colors = ["#6C63FF", "#10B981", "#F59E0B", "#EC4899", "#3B82F6", "#8B5CF6", "#EF4444", "#14B8A6"];
    return colors[Math.abs(hash) % colors.length];
};

const getInitials = (name) => {
    if (!name) return "?";
    return name.slice(0, 2).toUpperCase();
};

export default function ChatHomePage({ setPage }) {
    const [dark, setDark] = useState(true);
    const [currentUser, setCurrentUser] = useState(null);

    const [presenceMap, setPresenceMap] = useState({});
    const [noMoreMessages, setNoMoreMessages] = useState({});
    const messagesContainerRef = useRef(null);

    const loadingOlderRef = useRef(false);

    // Conversations list from API
    const [conversations, setConversations] = useState([]);
    const [activeConversationId, setActiveConversationId] = useState(null);



    const [input, setInput] = useState("");
    const [sidebarSearch, setSidebarSearch] = useState("");

    // Dialog states
    const [showLogout, setShowLogout] = useState(false);
    const [showNewChat, setShowNewChat] = useState(false);
    const [allUsers, setAllUsers] = useState([]);
    const [contactSearch, setContactSearch] = useState("");
    const [loadingUsers, setLoadingUsers] = useState(false);

    const messagesEndRef = useRef(null);
    const inputRef = useRef(null);
    const contactInputRef = useRef(null);
    const pendingConvoRef = useRef(null);

    const activeConversationIdRef = useRef(null);

    const currentUserRef = useRef(null);

    const [messageMenu, setMessageMenu] = useState(null);

    const [deleteChoiceDialog, setDeleteChoiceDialog] =
        useState(null);

    const [confirmDeleteDialog, setConfirmDeleteDialog] =
        useState(null);

    const [chatMenu, setChatMenu] = useState(null);

    const [deleteConversationDialog,
        setDeleteConversationDialog] = useState(null);

    const [showRestoreButton, setShowRestoreButton] = useState(false);

    const [restoreDialogOpen, setRestoreDialogOpen] = useState(false);

    const [historyMode, setHistoryMode] = useState(false);

    const [
        conversationLifecycles,
        setConversationLifecycles
    ] = useState([]);

    const [
        selectedHistoryConversationId,
        setSelectedHistoryConversationId
    ] = useState(null);

    const [participantLifecycles, setParticipantLifecycles] = useState([]);
    const [selectedConversationLifecycle, setSelectedConversationLifecycle] = useState(null);
    const [historyView, setHistoryView] = useState("conversation");

    const [historyMessages, setHistoryMessages] = useState([]);
    const [selectedParticipantLifecycle, setSelectedParticipantLifecycle] = useState(null);

    const historyMessagesContainerRef = useRef(null);
    const historyMessagesEndRef = useRef(null);

    const loadingOlderHistoryRef = useRef(false);

    const [noMoreHistoryMessages, setNoMoreHistoryMessages] = useState(false);

    const [messagesCache, setMessagesCache] = useState({});


    async function deleteMessageForMe(
        conversationId,
        messageId
    ) {

        return api.delete(
            `/api/v1/chat/conversations/${conversationId}/messages/${messageId}/me`
        );
    }

    const closeAllDialogs = () => {

        setMessageMenu(null);

        setDeleteChoiceDialog(null);

        setConfirmDeleteDialog(null);
    };



    // ── Theme ─────────────────────────────────────────────────────────────────
    const t = {
        bg:           dark ? "#0f0f13" : "#f2f2f7",
        bgSidebar:    dark ? "#16161d" : "#ffffff",
        bgChat:       dark ? "#0f0f13" : "#f2f2f7",
        bgBubbleMe:   "#6C63FF",
        bgBubbleThem: dark ? "#1e1e2a" : "#e4e4ee",
        bgInput:      dark ? "#1e1e2a" : "#ffffff",
        bgHover:      dark ? "#1e1e2a" : "#f0f0f8",
        bgActive:     dark ? "#252538" : "#eaeaf8",
        bgModal:      dark ? "#1a1a26" : "#ffffff",
        border:       dark ? "#2a2a3a" : "#e0e0ea",
        text:         dark ? "#e8e8f2" : "#1a1a2e",
        textSec:      dark ? "#8888aa" : "#6868a0",
        textMuted:    dark ? "#55557a" : "#a0a0c0",
        searchBg:     dark ? "#1e1e2a" : "#f0f0f8",
        accent:       "#6C63FF",
        accentSoft:   dark ? "#2a2845" : "#ebebff",
    };



    useEffect(() => {
        if (!activeConversationId) return;

        setNoMoreMessages(prev => ({
            ...prev,
            [activeConversationId]: false
        }));

        setTimeout(() => {
            messagesEndRef.current?.scrollIntoView({ behavior: "instant" });
        }, 0);
    }, [activeConversationId]);



    // ── Load current user ─────────────────────────────────────────────────────
    useEffect(() => {
        api.get("/api/v1/user/me")
            .then(res => {
                setCurrentUser(res.data);
                currentUserRef.current = res.data;
            })
            .catch(err => console.error("Failed to load user", err));
    }, []);

    // ── Load conversations ────────────────────────────────────────────────────
    const loadConversations = useCallback(() => {
        api.get("/api/v1/chat/conversations")
            .then(res => {
                const conversations =
                    Array.isArray(res.data)
                        ? res.data
                        : [];

                setConversations(conversations);

                const map = {};

                conversations.forEach(convo => {
                    map[convo.otherUserId] = {
                        online: convo.online,
                        lastSeen: convo.lastSeen
                    };
                });

                setPresenceMap(map);

                return api.get("/api/v1/presence/online-users")
                    .then(onlineRes => {
                        const onlineUsers =
                            Array.isArray(onlineRes.data)
                                ? onlineRes.data
                                : [];

                        const onlineSet =
                            new Set(onlineUsers.map(Number));

                        setPresenceMap(prev => {
                            const updated = { ...prev };

                            conversations.forEach(convo => {
                                updated[convo.otherUserId] = {
                                    ...(updated[convo.otherUserId] || {}),
                                    online: onlineSet.has(
                                        Number(convo.otherUserId)
                                    ),
                                    lastSeen: convo.lastSeen
                                };
                            });

                            return updated;
                        });
                    })
                    .catch(err =>
                        console.error(
                            "Failed to load online users",
                            err
                        )
                    );
            })
            .catch(err =>
                console.error(
                    "Failed to load conversations",
                    err
                )
            );
    }, []);

    const handleDeleteConversationForEveryone = async (conversationId) => {
        try {
            await api.delete(
                `/api/v1/chat/conversations/${conversationId}/everyone`
            );

            setConversations(prev =>
                prev.filter(
                    c => String(c.conversationId) !== String(conversationId)
                )
            );

            setMessagesCache(prev => {
                const updated = { ...prev };
                delete updated[String(conversationId)];
                delete updated[conversationId];
                return updated;
            });

            if (String(activeConversationId) === String(conversationId)) {
                setActiveConversationId(null);
                activeConversationIdRef.current = null;
            }

        } catch (err) {
            console.error("Delete conversation for everyone failed", err);
        }
    };


    const seenTimerRef = useRef({});

    const markConversationSeen = useCallback((conversationId) => {
        if (!conversationId) return;

        const key = String(conversationId);

        if (seenTimerRef.current[key]) {
            clearTimeout(seenTimerRef.current[key]);
        }

        seenTimerRef.current[key] = setTimeout(() => {
            api.post(`/api/v1/chat/conversations/${conversationId}/seen`)
                .catch(err =>
                    console.error("Failed to mark seen", err)
                )
                .finally(() => {
                    delete seenTimerRef.current[key];
                });
        }, 500);
    }, []);

    useEffect(() => {
        loadConversations();
    }, [loadConversations]);
    // ── Load messages when switching conversation ─────────────────────────────
    useEffect(() => {
        if (!activeConversationId) return;

        if (String(activeConversationId).startsWith("pending-")) {
            return;
        }

        const cache =
            messagesCache[activeConversationId];

        const scrollToBottom = () => {
            requestAnimationFrame(() => {
                const container =
                    messagesContainerRef.current;

                if (container) {
                    container.scrollTop =
                        container.scrollHeight;
                }
            });
        };

        if (cache?.loaded) {
            scrollToBottom();

            const convo = conversations.find(
                c => String(c.conversationId) === String(activeConversationId)
            );

            if ((convo?.unreadCount || 0) > 0) {
                markConversationSeen(activeConversationId);
            }

            return;
        }



        api.get(`/api/v1/chat/conversations/${activeConversationId}/messages`)
            .then(res => {
                setMessagesCache(prev => ({
                    ...prev,
                    [activeConversationId]: {
                        messages: Array.isArray(res.data)
                            ? res.data
                            : [],
                        loaded: true,
                        noMore: false,
                        lastFetchedAt: Date.now()
                    }
                }));

                scrollToBottom();

                const convo = conversations.find(
                    c => String(c.conversationId) === String(activeConversationId)
                );

                if ((convo?.unreadCount || 0) > 0) {
                    markConversationSeen(activeConversationId);
                }
            })
            .catch(err =>
                console.error("Failed to load messages", err)
            );

    }, [
        activeConversationId,
        messagesCache,
        conversations,
        markConversationSeen
    ]);



    // ── WebSocket incoming messages ───────────────────────────────────────────
    const { sendMessage: wsSend } = useWebSocket({
        onMessage: (msg) => {
            if (!msg) return;

            const realConvoId = msg.conversationId;
            const messageId = msg.messageId;

            if (!realConvoId) return;

            // Message deleted event
            if (msg.type === "MESSAGE_DELETED") {
                const realConvoId = String(msg.conversationId);
                const messageId = Number(msg.messageId);

                setMessagesCache(prev => {
                    const existing = prev[realConvoId]?.messages || [];

                    return {
                        ...prev,
                        [realConvoId]: {
                            ...(prev[realConvoId] || {}),
                            messages: existing.map(m =>
                                Number(m.messageId) === messageId
                                    ? { ...m, deletedForEveryone: true }
                                    : m
                            ),
                            loaded: true,
                            lastFetchedAt: Date.now()
                        }
                    };
                });

                if (msg.isLastMessage) {
                    setConversations(prev =>
                        prev.map(convo =>
                            String(convo.conversationId) === realConvoId
                                ? {
                                    ...convo,
                                    lastMessageDeleted: true,
                                    lastMessage: "This message was deleted"
                                }
                                : convo
                        )
                    );
                }

                return;
            }

            if (msg.type === "MESSAGE_DELIVERED") {
                const realConvoId = msg.conversationId;
                const messageId = msg.messageId;

                if (!realConvoId || !messageId) return;

                setMessagesCache(prev => {
                    const existing = prev[realConvoId]?.messages;

                    if (!existing) return prev;

                    return {
                        ...prev,
                        [realConvoId]: {
                            ...prev[realConvoId],
                            messages: existing.map(m =>
                                m.messageId === messageId
                                    ? {
                                        ...m,
                                        delivered: true
                                    }
                                    : m
                            ),
                            lastFetchedAt: Date.now()
                        }
                    };
                });

                return;
            }

            if (msg.type === "CONVERSATION_DELETED") {
                const conversationId = String(msg.conversationId);

                setConversations(prev =>
                    prev.filter(
                        c => String(c.conversationId) !== conversationId
                    )
                );

                setMessagesCache(prev => {
                    const updated = { ...prev };
                    delete updated[conversationId];
                    delete updated[msg.conversationId];
                    return updated;
                });

                if (String(activeConversationIdRef.current) === conversationId) {
                    setActiveConversationId(null);
                    activeConversationIdRef.current = null;
                }

                return;
            }

            const isActive =
                String(realConvoId) === String(activeConversationIdRef.current);

            const isMine =
                Number(msg.senderId) === Number(currentUserRef.current?.id);

            if (isActive && !isMine) {
                markConversationSeen(realConvoId);
            }

            const pending = pendingConvoRef.current;

            // Pending temporary conversation converted to real conversation
            if (
                pending?.isPending &&
                msg.senderId === currentUserRef.current?.id
            ) {
                setConversations(prev =>
                    prev.map(c =>
                        c.conversationId === pending.tempId
                            ? {
                                ...c,
                                conversationId: realConvoId,
                                pending: false,
                                lastMessageAt: msg.createdAt,
                                lastMessage: msg.deletedForEveryone
                                    ? "Message deleted"
                                    : msg.content,
                                lastMessageDeleted: !!msg.deletedForEveryone
                            }
                            : c
                    )
                );

                setMessagesCache(prev => {
                    const oldMessages =
                        prev[pending.tempId]?.messages || [];

                    const updated = { ...prev };

                    delete updated[pending.tempId];

                    updated[realConvoId] = {
                        messages: [...oldMessages, msg],
                        loaded: true,
                        noMore: false,
                        lastFetchedAt: Date.now()
                    };

                    return updated;
                });

                setActiveConversationId(realConvoId);
                activeConversationIdRef.current = realConvoId;
                pendingConvoRef.current = null;

                return;
            }

            if (msg.type === "MESSAGE_SEEN") {
                const realConvoId = String(msg.conversationId);
                const messageId = Number(msg.messageId);

                setMessagesCache(prev => {
                    const existing =
                        prev[realConvoId]?.messages || [];

                    if (existing.length === 0) return prev;

                    return {
                        ...prev,
                        [realConvoId]: {
                            ...(prev[realConvoId] || {}),
                            messages: existing.map(m =>
                                Number(m.messageId) === messageId
                                    ? {
                                        ...m,
                                        delivered: true,
                                        seen: true
                                    }
                                    : m
                            ),
                            loaded: true,
                            lastFetchedAt: Date.now()
                        }
                    };
                });

                return;
            }

            // Normal incoming message / send confirmation
            setMessagesCache(prev => {
                const existing =
                    prev[realConvoId]?.messages || [];

                const index = existing.findIndex(
                    m => m.messageId === messageId
                );

                let updatedMessages;

                if (index !== -1) {
                    updatedMessages = existing.map(m =>
                        m.messageId === messageId
                            ? {
                                ...m,
                                ...msg,
                                delivered: msg.delivered ?? m.delivered,
                                seen: msg.seen ?? m.seen
                            }
                            : m
                    );
                } else {
                    updatedMessages = [...existing, msg];
                }

                return {
                    ...prev,
                    [realConvoId]: {
                        ...(prev[realConvoId] || {}),
                        messages: updatedMessages,
                        loaded: true,
                        noMore: prev[realConvoId]?.noMore || false,
                        lastFetchedAt: Date.now()
                    }
                };
            });
            // Move conversation to top
            setConversations(prev => {
                const current = prev.find(
                    c => c.conversationId === realConvoId
                );

                if (!current) {
                    loadConversations();
                    return prev;
                }

                const remaining = prev.filter(
                    c => c.conversationId !== realConvoId
                );

                const isActive =
                    realConvoId === activeConversationIdRef.current;

                const isMine =
                    msg.senderId === currentUserRef.current?.id;

                return [
                    {
                        ...current,
                        lastMessageAt: msg.createdAt,
                        lastMessage: msg.deletedForEveryone
                            ? "Message deleted"
                            : msg.content,
                        lastMessageDeleted: !!msg.deletedForEveryone,
                        unreadCount: (!isActive && !isMine)
                            ? (current.unreadCount || 0) + 1
                            : current.unreadCount
                    },
                    ...remaining
                ];
            });
        },

        onPresence: (presence) => {
            if (!presence?.userId) return;

            setPresenceMap(prev => ({
                ...prev,
                [presence.userId]: {
                    online: presence.online,
                    lastSeen: presence.lastSeen
                }
            }));
        },

        onConnect: () => {
            api.post("/api/v1/chat/markBulkDelivery")
                .catch(err =>
                    console.error("Failed to sync delivered receipts", err)
                );
        }
    });



    const handleDeleteConversationForMe =
        async (conversationId) => {

            try {

                await api.delete(
                    `/api/v1/chat/conversations/${conversationId}/me`
                );

                setConversations(prev =>
                    prev.filter(
                        c => c.conversationId !== conversationId
                    )
                );

                if (
                    activeConversationId === conversationId
                ) {

                    setActiveConversationId(null);

                }

                setMessagesCache(prev => {

                    const updated = { ...prev };

                    delete updated[conversationId];

                    return updated;
                });

            } catch (err) {

                console.error(err);

            }

        };

    function getPresenceText(userId) {

        const presence =
            presenceMap[userId];

        if (!presence)
            return "";

        if (presence.online)
            return "online";

        return formatLastSeen(
            presence.lastSeen
        );
    }




    // ── Active conversation data ───────────────────────────────────────────────
    const activeConversation = Array.isArray(conversations)
        ? conversations.find(
            c => c.conversationId === activeConversationId
        )
        : null;
    const activeMessages =
        messagesCache[activeConversationId]?.messages || [];
    useEffect(() => {

        if (activeMessages.length > 0) {
            setShowRestoreButton(false);
        }

    }, [activeMessages]);

    // ── Sidebar search ────────────────────────────────────────────────────────
    const filteredConversations =
        Array.isArray(conversations)
            ? (
                sidebarSearch.trim()
                    ? conversations.filter(c =>
                        !c.pending &&
                        c.otherHandleName?.toLowerCase()
                            .includes(sidebarSearch.toLowerCase())
                    )
                    : conversations.filter(c => !c.pending)
            )
            : [];

    // ── Open new chat dialog — fetch all users ────────────────────────────────
    const openNewChat = async () => {
        setShowNewChat(true);
        setLoadingUsers(true);
        try {
            const res = await api.get("/api/v1/user/all");
            setAllUsers(res.data);
        } catch (err) {
            console.error("Failed to load users", err);
        } finally {
            setLoadingUsers(false);
            setTimeout(() => contactInputRef.current?.focus(), 80);
        }
    };

    const closeNewChat = () => {
        setShowNewChat(false);
        setContactSearch("");
    };

    // ── Start or open conversation ────────────────────────────────────────────
    const handleStartChat = async (user) => {
        // Check if conversation already exists
        const existing = conversations.find(c => c.otherUserId === user.id);
        if (existing) {
            setActiveConversationId(existing.conversationId);
            activeConversationIdRef.current = existing.conversationId;
            closeNewChat();
            return;
        }
        closeNewChat();

        try {

            const res = await api.get(
                `/api/v1/chat/restore-eligible/${user.id}`
            );

            setShowRestoreButton(res.data);

        } catch (err) {

            console.error(err);

            setShowRestoreButton(false);

        }

        const tempId = `pending-${user.id}`;
        // First message will create the conversation via REST
        // For now just set a temporary pending conversation
        setActiveConversationId(tempId);
        activeConversationIdRef.current = tempId;
        setMessagesCache(prev => ({ ...prev, [`pending-${user.id}`]: [] }));
        setConversations(prev => [{
            conversationId: `pending-${user.id}`,
            otherUserId: user.id,
            otherHandleName: user.handleName,
            lastMessageAt: null,
            pending: true,
        }, ...prev]);
    };


    // ── Send message ──────────────────────────────────────────────────────────
    const handleSend = () => {
        if (!input.trim() || !activeConversationId) return;

        const text = input.trim();
        setInput("");

        const isPending = String(activeConversationId).startsWith("pending-");
        const receiverId = isPending
            ? activeConversation?.otherUserId
            : activeConversation?.otherUserId;

        // Store pending state before sending — needed in onMessage for swap
        pendingConvoRef.current = {
            tempId: activeConversationId,
            isPending,
            receiverId,
        };

        wsSend("/app/chat.send", {
            receiverId,
            content: text,
            conversationId: isPending ? null : activeConversationId,
        });

        setTimeout(() => inputRef.current?.focus(), 0);
    };

    const handleKeyDown = e => {
        if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSend(); }
    };

    const handleMessagesScroll = useCallback(() => {
        const container = messagesContainerRef.current;
        if (!container) return;
        if (loadingOlderRef.current) return;
        if (container.scrollTop > 50) return;
        if (noMoreMessages[activeConversationId]) return;

        const messages =
            messagesCache[activeConversationId]?.messages || [];

        if (messages.length === 0) return;

// topmost message is first in array since ascending order
        const offsetId = messages[0].messageId;

        loadingOlderRef.current = true;
        api.get(`/api/v1/chat/conversations/${activeConversationId}/messages`, {
            params: { offsetId }
        }).then(res => {
            if (res.data.length === 0) {
                setNoMoreMessages(prev => ({ ...prev, [activeConversationId]: true }));
                return;
            }
            const container = messagesContainerRef.current;
            const scrollHeightBefore = container?.scrollHeight || 0;

            // Prepend older messages to top
            setMessagesCache(prev => ({
                ...prev,
                [activeConversationId]: {
                    ...(prev[activeConversationId] || {}),
                    messages: [
                        ...res.data,
                        ...(prev[activeConversationId]?.messages || [])
                    ],
                    loaded: true,
                    noMore: false,
                    lastFetchedAt: Date.now()
                }
            }));

            // Restore scroll position after prepend
            requestAnimationFrame(() => {
                if (container) {
                    container.scrollTop = container.scrollHeight - scrollHeightBefore;
                }
            });
        })
            .catch(err => {
                console.error("Failed to load older messages", err);
            })
            .finally(() => {
                loadingOlderRef.current = false;
            });
    }, [activeConversationId, messagesCache, noMoreMessages]);

    useEffect(() => {
        const container = messagesContainerRef.current;
        if (!container) return;
        container.addEventListener("scroll", handleMessagesScroll);
        return () => container.removeEventListener("scroll", handleMessagesScroll);
    }, [handleMessagesScroll]);

    // ── Logout ────────────────────────────────────────────────────────────────
    const handleLogout = async () => {
        try {
            await api.post("/api/v1/auth/logout");
            setShowLogout(false);
            setPage("login");
        } catch (err) {
            console.error(err);
            alert("Logout failed");
        }
    };



    const handleDeleteForMe =
        async (messageId) => {

            try {

                await api.delete(
                    `/api/v1/chat/conversations/${activeConversationId}/messages/${messageId}/me`
                );

                setMessagesCache(prev => {
                    const existing =
                        prev[activeConversationId]?.messages || [];

                    return {
                        ...prev,
                        [activeConversationId]: {
                            ...(prev[activeConversationId] || {}),
                            messages: existing.filter(
                                m => m.messageId !== messageId
                            ),
                            loaded: true,
                            lastFetchedAt: Date.now()
                        }
                    };
                });

            } catch (err) {

                console.error(
                    "Delete failed",
                    err
                );

            } finally {

                setMessageMenu(null);
                closeAllDialogs();
            }
        };

    const handleDeleteForEveryone = async (messageId) => {
        try {
            await api.delete(
                `/api/v1/chat/conversations/${activeConversationId}/messages/${messageId}/everyone`
            );

            const existing =
                messagesCache[activeConversationId]?.messages || [];

            const isLastMessage =
                existing.length > 0 &&
                Number(existing[existing.length - 1].messageId) === Number(messageId);

            setMessagesCache(prev => {
                const current =
                    prev[activeConversationId]?.messages || [];

                return {
                    ...prev,
                    [activeConversationId]: {
                        ...(prev[activeConversationId] || {}),
                        messages: current.map(msg =>
                            Number(msg.messageId) === Number(messageId)
                                ? {
                                    ...msg,
                                    deletedForEveryone: true
                                }
                                : msg
                        ),
                        loaded: true,
                        lastFetchedAt: Date.now()
                    }
                };
            });

            if (isLastMessage) {
                setConversations(prev =>
                    prev.map(convo =>
                        String(convo.conversationId) === String(activeConversationId)
                            ? {
                                ...convo,
                                lastMessageDeleted: true,
                                lastMessage: "This message was deleted"
                            }
                            : convo
                    )
                );
            }

        } catch (err) {
            console.error(err);
        } finally {
            closeAllDialogs();
        }
    };

    function formatDateDivider(dateString) {

        const date = new Date(dateString);

        const today = new Date();

        const yesterday = new Date();
        yesterday.setDate(today.getDate() - 1);

        if (date.toDateString() === today.toDateString()) {
            return "Today";
        }

        if (date.toDateString() === yesterday.toDateString()) {
            return "Yesterday";
        }

        return date.toLocaleDateString([], {
            day: "numeric",
            month: "short",
            year: "numeric"
        });
    }

    function buildMessageBlocks(messages) {

        const result = [];

        let previous = null;

        messages.forEach(msg => {

            const currentTime =
                new Date(msg.createdAt);

            let showDateDivider = false;
            let showTimeDivider = false;

            if (!previous) {

                showDateDivider = true;

            } else {

                const previousTime =
                    new Date(previous.createdAt);

                const dayChanged =
                    currentTime.toDateString() !==
                    previousTime.toDateString();

                if (dayChanged) {

                    showDateDivider = true;

                } else {

                    const diffMinutes =
                        (currentTime - previousTime) /
                        (1000 * 60);

                    if (diffMinutes >= 30) {
                        showTimeDivider = true;
                    }
                }
            }

            result.push({
                msg,
                showDateDivider,
                showTimeDivider
            });

            previous = msg;
        });

        return result;
    }

    function formatLastSeen(lastSeen) {

        if (!lastSeen) return "";

        const date = new Date(lastSeen);
        const now = new Date();

        const isToday =
            date.toDateString() === now.toDateString();

        const yesterday = new Date();
        yesterday.setDate(now.getDate() - 1);

        const isYesterday =
            date.toDateString() === yesterday.toDateString();

        const time =
            date.toLocaleTimeString([], {
                hour: "numeric",
                minute: "2-digit"
            });

        if (isToday)
            return `last seen today at ${time}`;

        if (isYesterday)
            return `last seen yesterday at ${time}`;

        return `last seen on ${date.toLocaleDateString([], {
            day: "numeric",
            month: "short"
        })} at ${time}`;
    }

    // ── Filtered users in new chat dialog ─────────────────────────────────────
    const filteredUsers = contactSearch.trim()
        ? allUsers.filter(u =>
            u.handleName?.toLowerCase().includes(contactSearch.toLowerCase())
        )
        : allUsers;

    const messageBlocks =
        buildMessageBlocks(activeMessages);

    const handleRestoreChat = async () => {

        try {

            await api.post(
                "/api/v1/chat/restore",
                null,
                {
                    params: {
                        otherUserId:
                        activeConversation.otherUserId
                    }
                }
            );

            const convRes =
                await api.get("/api/v1/chat/conversations");

            setConversations(convRes.data);

            const restoredConversation =
                convRes.data.find(
                    c =>
                        c.otherUserId ===
                        activeConversation.otherUserId
                );

            if (!restoredConversation) {
                return;
            }

            setActiveConversationId(
                restoredConversation.conversationId
            );

            activeConversationIdRef.current =
                restoredConversation.conversationId;

            const msgRes = await api.get(
                `/api/v1/chat/conversations/${restoredConversation.conversationId}/messages`
            );

            setMessagesCache(prev => ({
                ...prev,
                [restoredConversation.conversationId]:
                msgRes.data
            }));

            setShowRestoreButton(false);
            setRestoreDialogOpen(false);

        } catch (err) {

            console.error(
                "[RESTORE_CHAT_FAILED]",
                err
            );

        }

    };

    const loadConversationLifecycles = async (
        conversationId
    ) => {

        try {

            const res = await api.get(
                `/api/v1/chat/conversations/${conversationId}/history`
            );

            setConversationLifecycles(res.data);

        } catch (err) {

            console.error(
                "[LOAD_LIFECYCLES_FAILED]",
                err
            );

        }

    };

    const loadParticipantLifecycles = async (
        conversationId,
        conversationLifecycleId
    ) => {

        try {

            const res = await api.get(
                `api/v1/chat/conversations/${conversationId}/history/${conversationLifecycleId}/participants`
            );

            setParticipantLifecycles(
                [...res.data].reverse()
            );

            setSelectedConversationLifecycle(
                conversationLifecycleId
            );

            setHistoryView("participants");

        } catch (err) {

            console.error(
                "[LOAD_PARTICIPANT_LIFECYCLES_FAILED]",
                err
            );

        }

    };

    const loadLifecycleMessages = async (participantLifecycle) => {
        try {
            const res = await api.get(
                `/api/v1/chat/lifecycles/${participantLifecycle.lifecycleId}/messages`
            );

            setSelectedParticipantLifecycle(participantLifecycle);
            setHistoryMessages(res.data);
            setNoMoreHistoryMessages(false);
            setHistoryView("messages");

        } catch (err) {
            console.error("[LOAD_HISTORY_MESSAGES_FAILED]", err);
        }
    };

    useEffect(() => {
        if (historyView !== "messages") return;

        requestAnimationFrame(() => {
            historyMessagesEndRef.current?.scrollIntoView({
                behavior: "instant"
            });
        });
    }, [historyView, historyMessages]);

    useEffect(() => {
        if (historyView !== "messages") return;

        requestAnimationFrame(() => {
            const el = historyMessagesEndRef.current;
            if (!el) return;

            el.scrollIntoView({
                behavior: "instant",
                block: "end"
            });
        });
    }, [historyView, historyMessages.length]);

    const handleHistoryMessagesScroll = useCallback(() => {
        const container = historyMessagesContainerRef.current;

        if (!container) return;
        if (loadingOlderHistoryRef.current) return;
        if (noMoreHistoryMessages) return;
        if (!selectedParticipantLifecycle) return;
        if (historyMessages.length === 0) return;

        if (container.scrollTop > 50) return;

        const offsetId = historyMessages[0].messageId;

        loadingOlderHistoryRef.current = true;

        const scrollHeightBefore = container.scrollHeight;

        api.get(
            `/api/v1/chat/lifecycles/${selectedParticipantLifecycle.lifecycleId}/messages/older`,
            {
                params: { offsetId }
            }
        )
            .then(res => {
                if (!res.data || res.data.length === 0) {
                    setNoMoreHistoryMessages(true);
                    return;
                }

                setHistoryMessages(prev => [
                    ...res.data,
                    ...prev
                ]);

                requestAnimationFrame(() => {
                    const c = historyMessagesContainerRef.current;
                    if (c) {
                        c.scrollTop = c.scrollHeight - scrollHeightBefore;
                    }
                });
            })
            .catch(err => {
                console.error("[LOAD_OLDER_HISTORY_MESSAGES_FAILED]", err);
            })
            .finally(() => {
                loadingOlderHistoryRef.current = false;
            });

    }, [
        historyMessages,
        selectedParticipantLifecycle,
        noMoreHistoryMessages
    ]);

    // requestAnimationFrame(() => {
    //     const container = historyMessagesContainerRef.current;
    //
    //     if (container) {
    //         container.scrollTop = container.scrollHeight;
    //     }
    // });

    useEffect(() => {

        if (
            historyView !== "messages" ||
            historyMessages.length === 0
        ) {
            return;
        }

        const container = historyMessagesContainerRef.current;

        if (container) {
            container.scrollTop = container.scrollHeight;
        }

    }, [historyView]);



    // ── Render ────────────────────────────────────────────────────────────────
    return (
        <div style={{ display: "flex", height: "100vh", background: t.bg, fontFamily: "'DM Sans','Segoe UI',sans-serif", overflow: "hidden", color: t.text, transition: "background 0.2s" }}>
            <style>{`
                @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@300;400;500;600&family=DM+Mono:wght@400;500&display=swap');
                *{box-sizing:border-box;margin:0;padding:0}
                ::-webkit-scrollbar{width:4px}::-webkit-scrollbar-track{background:transparent}
                ::-webkit-scrollbar-thumb{background:${t.border};border-radius:4px}
                input:focus,textarea:focus{outline:none}
                button{cursor:pointer;border:none;background:none;font-family:inherit}
                @keyframes slideDown {
        from { opacity: 0; transform: translateY(-12px); }
        to   { opacity: 1; transform: translateY(0); }
    }

            `}</style>

            {/* ═══════════════════ SIDEBAR ═══════════════════ */}
            <div
                style={{
                    width: 300,
                    minWidth: 300,
                    maxWidth: 300,
                    flexShrink: 0,
                    background: t.bgSidebar,
                    borderRight: `1px solid ${t.border}`,
                    display: "flex",
                    flexDirection: "column",
                    height: "100vh",
                    overflow: "hidden"
                }}
            >

                {/* Top bar */}
                <div style={{ padding: "14px 14px 12px", display: "flex", alignItems: "center", justifyContent: "space-between", borderBottom: `1px solid ${t.border}` }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
                        <div style={{ width: 34, height: 34, borderRadius: 10, background: "linear-gradient(135deg,#6C63FF,#a78bfa)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" fill="white" opacity="0.9"/></svg>
                        </div>
                        <span style={{ fontSize: 17, fontWeight: 600, letterSpacing: "-0.3px" }}>ECHO</span>
                    </div>
                    <div style={{ display: "flex", gap: 6 }}>
                        <button onClick={() => setDark(d => !d)}
                                style={{ width: 32, height: 32, borderRadius: 8, background: t.searchBg, display: "flex", alignItems: "center", justifyContent: "center", color: t.textSec }}>
                            {dark
                                ? <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
                                : <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
                            }
                        </button>
                        <button onClick={openNewChat}
                                style={{ width: 32, height: 32, borderRadius: 8, background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 22, fontWeight: 300, lineHeight: 1 }}>
                            +
                        </button>
                    </div>
                </div>

                {/* Search */}
                <div style={{ padding: "10px 12px 6px" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8, background: t.searchBg, borderRadius: 10, padding: "8px 12px", border: `1px solid ${t.border}` }}>
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke={t.textMuted} strokeWidth="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                        <input value={sidebarSearch} onChange={e => setSidebarSearch(e.target.value)} placeholder="Search chats…"
                               style={{ flex: 1, background: "none", border: "none", fontSize: 13, color: t.text, fontFamily: "inherit" }} />
                    </div>
                </div>

                {/* Conversation list */}
                <div style={{ flex: 1, minHeight: 0, overflowY: "auto", padding: "4px 8px" }}>
                    {filteredConversations.length === 0 ? (
                        <div style={{ textAlign: "center", padding: "44px 20px", color: t.textMuted, fontSize: 13 }}>
                            <div style={{ fontSize: 34, marginBottom: 10 }}>💬</div>
                            <div style={{ fontWeight: 500, color: t.textSec, marginBottom: 5 }}>No chats yet</div>
                            <div style={{ lineHeight: 1.6 }}>Tap <strong style={{ color: t.accent }}>+</strong> to start a conversation</div>
                        </div>
                    ) : filteredConversations.map(convo => (
                        <div
                            key={convo.conversationId}

                            onContextMenu={(e) => {
                                e.preventDefault();

                                setChatMenu({
                                    x: e.clientX,
                                    y: e.clientY,
                                    conversationId: convo.conversationId,
                                    otherUserId: convo.otherUserId
                                });
                            }}

                            onClick={() => {

                                setHistoryMode(false);

                                setActiveConversationId(
                                    convo.conversationId
                                );

                                activeConversationIdRef.current =
                                    convo.conversationId;

                                const unreadCount =
                                    convo.unreadCount || 0;

                                if (unreadCount > 0) {

                                    markConversationSeen(
                                        convo.conversationId
                                    );

                                    setConversations(prev =>
                                        prev.map(c =>
                                            String(c.conversationId) ===
                                            String(convo.conversationId)
                                                ? {
                                                    ...c,
                                                    unreadCount: 0
                                                }
                                                : c
                                        )
                                    );
                                }

                            }}
                             style={{ display: "flex", alignItems: "center", gap: 11, padding: "10px 10px", borderRadius: 12, cursor: "pointer", marginBottom: 2, transition: "background 0.12s", background: activeConversationId === convo.conversationId ? t.bgActive : "transparent", animation: convo.lastMessageAt && new Date(convo.lastMessageAt) > new Date(Date.now() - 2000) ? "slideDown 0.25s ease" : "none" }}
                             onMouseEnter={e => { if (activeConversationId !== convo.conversationId) e.currentTarget.style.background = t.bgHover; }}
                             onMouseLeave={e => { if (activeConversationId !== convo.conversationId) e.currentTarget.style.background = "transparent"; }}>
                            <div style={{ position: "relative" }}>
                                <Avatar
                                    initials={getInitials(convo.otherHandleName)}
                                    color={stringToColor(convo.otherHandleName)}
                                    size={42}
                                    bgColor={t.bgSidebar}
                                />


                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                {/* Row 1 — name + time */}
                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 2 }}>
                                    <div
                                        style={{
                                            display: "flex",
                                            alignItems: "center",
                                            gap: 6,
                                            minWidth: 0
                                        }}
                                    >
    <span
        style={{
            fontSize: 14,
            fontWeight: 500,
            whiteSpace: "nowrap",
            overflow: "hidden",
            textOverflow: "ellipsis",
            maxWidth: 130
        }}
    >
        {convo.otherHandleName}
    </span>

                                        {presenceMap[convo.otherUserId]?.online && (
                                            <div
                                                style={{
                                                    width: 8,
                                                    height: 8,
                                                    borderRadius: "50%",
                                                    background: "#25D366",
                                                    flexShrink: 0
                                                }}
                                            />
                                        )}
                                    </div>

                                    <span
                                        style={{
                                            fontSize: 11,
                                            color: t.textMuted,
                                            flexShrink: 0,
                                            marginLeft: 6
                                        }}
                                    >
        {formatTime(convo.lastMessageAt)}
    </span>
                                </div>


                                {/* Row 2 — last message + unread bubble */}
                                <div
                                    style={{
                                        display: "flex",
                                        justifyContent: "space-between",
                                        alignItems: "center"
                                    }}
                                >
                    <span style={{
                        fontSize: 12,
                        color: convo.unreadCount > 0 ? t.textSec : t.textMuted,
                        fontWeight: convo.unreadCount > 0 ? 500 : 400,
                        whiteSpace: "nowrap",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        maxWidth: 160,
                        fontStyle: convo.lastMessageDeleted
                            ? "italic"
                            : convo.lastMessage
                                ? "normal"
                                : "italic"
                    }}>
                        {
                            convo.lastMessageDeleted
                                ? "This message was deleted"
                                : (convo.lastMessage || "No messages yet")
                        }
                    </span>
                                    {convo.unreadCount > 0 && (
                                        <div style={{
                                            minWidth: 18,
                                            height: 18,
                                            borderRadius: 9,
                                            background: t.accent,
                                            color: "#fff",
                                            fontSize: 11,
                                            fontWeight: 600,
                                            display: "flex",
                                            alignItems: "center",
                                            justifyContent: "center",
                                            padding: "0 5px",
                                            flexShrink: 0,
                                            marginLeft: 6,
                                            fontFamily: "'DM Mono', monospace"
                                        }}>
                                            {convo.unreadCount > 99 ? "99+" : convo.unreadCount}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Profile + logout */}
                <div style={{ padding: "12px", borderTop: `1px solid ${t.border}`, display: "flex", alignItems: "center", gap: 10, background: t.bgSidebar }}>
                    <Avatar initials={getInitials(currentUser?.handleName)} color={stringToColor(currentUser?.handleName)} size={36} bgColor={t.bgSidebar} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 13, fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                            {currentUser?.handleName || "..."}
                        </div>
                    </div>
                    <button onClick={() => setShowLogout(true)}
                            style={{ width: 32, height: 32, borderRadius: 8, background: dark ? "#2a1a1a" : "#fdecea", display: "flex", alignItems: "center", justifyContent: "center", color: "#ef4444" }}>
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                    </button>
                </div>
            </div>

            {/* ═══════════════════ CHAT AREA ═══════════════════ */}
            <div
                style={{
                    flex: "1 1 0",
                    minWidth: 0,
                    maxWidth: "calc(100vw - 300px)",
                    height: "100vh",
                    overflow: "hidden",
                    display: "flex",
                    flexDirection: "column",
                    background: t.bgChat
                }}
            >
                {activeConversation ? (
                    historyMode ? (
                        <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>

                            {/* History Header */}
                            <div
                                style={{
                                    height: 64,
                                    padding: "0 20px",
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "space-between",
                                    borderBottom: `1px solid ${t.border}`,
                                    background: t.bgSidebar
                                }}
                            >
                                <div>
                                    <div style={{ fontSize: 16, fontWeight: 600 }}>
                                        {historyView === "conversation"
                                            ? "Chat History"
                                            : historyView === "participants"
                                                ? "Participant Lifecycles"
                                                : "Archived Messages"}
                                    </div>
                                    <div style={{ fontSize: 12, color: t.textMuted }}>
                                        {historyView === "conversation"
                                            ? "Browse previous versions of this conversation"
                                            : historyView === "participants"
                                                ? "Select a participant lifecycle to view messages"
                                                : "Read-only view of this lifecycle's messages"}
                                    </div>
                                </div>

                                <div style={{ display: "flex", alignItems: "center", gap: 20 }}>

                                    {historyView === "participants" && (
                                        <div
                                            onClick={() => {
                                                setHistoryView("conversation");
                                                setParticipantLifecycles([]);
                                            }}
                                            style={{ cursor: "pointer", fontSize: 14, fontWeight: 600, color: t.accent }}
                                        >
                                            ← Back
                                        </div>
                                    )}

                                    {historyView === "messages" && (
                                        <div
                                            onClick={() => {
                                                setHistoryView("participants");
                                                setHistoryMessages([]);
                                            }}
                                            style={{ cursor: "pointer", fontSize: 14, fontWeight: 600, color: t.accent }}
                                        >
                                            ← Back
                                        </div>
                                    )}

                                    <div
                                        onClick={() => {
                                            setHistoryMode(false);
                                            setHistoryView("conversation");
                                            setConversationLifecycles([]);
                                            setParticipantLifecycles([]);
                                            setHistoryMessages([]);
                                        }}
                                        style={{ cursor: "pointer", fontSize: 20 }}
                                    >
                                        ✕
                                    </div>

                                </div>
                            </div>

                            {/* History Content */}
                            <div
                                style={{
                                    height: "calc(100vh - 64px)",
                                    padding: 24,
                                    overflowY: "auto"
                                }}
                            >
                                <div
                                    style={{
                                        height: "100%",
                                        minHeight: 0,
                                        display: "flex",
                                        flexDirection: "column"
                                    }}
                                >

                                    {historyView === "conversation" && (
                                        conversationLifecycles.map(lifecycle => (
                                            <div
                                                key={lifecycle.lifecycleId}
                                                onClick={() => loadParticipantLifecycles(selectedHistoryConversationId, lifecycle.lifecycleId)}
                                                style={{
                                                    padding: 16,
                                                    borderRadius: 12,
                                                    background: t.bgSidebar,
                                                    border: `1px solid ${t.border}`,
                                                    cursor: "pointer"
                                                }}
                                            >
                                                <div style={{ fontWeight: 600, marginBottom: 6 }}>Conversation Lifecycle</div>
                                                <div style={{ fontSize: 12, color: t.textMuted }}>
                                                    Started: {new Date(lifecycle.startAt).toLocaleString()}
                                                </div>
                                                <div style={{ fontSize: 12, color: t.textMuted }}>
                                                    Ended:{" "}
                                                    {lifecycle.endAt
                                                        ? new Date(lifecycle.endAt).toLocaleString()
                                                        : <span style={{ color: "#25D366", fontWeight: 600 }}>Active</span>
                                                    }
                                                </div>
                                            </div>
                                        ))
                                    )}

                                    {historyView === "participants" && (
                                        participantLifecycles.map(pl => (
                                            <div
                                                key={pl.lifecycleId}
                                                onClick={() => loadLifecycleMessages(pl)}
                                                style={{
                                                    padding: 16,
                                                    borderRadius: 12,
                                                    background: t.bgSidebar,
                                                    border: `1px solid ${t.border}`,
                                                    cursor: "pointer"
                                                }}
                                            >
                                                <div style={{ fontWeight: 600, marginBottom: 8 }}>{currentUser.handleName}</div>
                                                <div style={{ fontSize: 12, color: t.textMuted }}>
                                                    Joined: {new Date(pl.joinedAt).toLocaleString()}
                                                </div>
                                                <div style={{ fontSize: 12, color: pl.active ? "#25D366" : t.textMuted }}>
                                                    {pl.active ? "Active" : `Left: ${new Date(pl.leftAt).toLocaleString()}`}
                                                </div>
                                            </div>
                                        ))
                                    )}

                                    {historyView === "messages" && (
                                        <div
                                            ref={historyMessagesContainerRef}
                                            onScroll={handleHistoryMessagesScroll}
                                            style={{
                                                height: "calc(100vh - 64px - 48px)",
                                                overflowY: "auto",
                                                display: "flex",
                                                flexDirection: "column",
                                                gap: 8
                                            }}
                                        >
                                            <div
                                                style={{
                                                    flexShrink: 0,
                                                    padding: "10px 14px",
                                                    borderRadius: 10,
                                                    background: t.bgSidebar,
                                                    border: `1px solid ${t.border}`,
                                                    color: t.textMuted,
                                                    fontSize: 13,
                                                    marginBottom: 12
                                                }}
                                            >
                                                You are viewing archived messages. This chat is read-only.
                                            </div>

                                            {historyMessages.length === 0 ? (
                                                <div
                                                    style={{
                                                        textAlign: "center",
                                                        color: t.textMuted,
                                                        padding: 40,
                                                        fontSize: 13
                                                    }}
                                                >
                                                    No messages found in this lifecycle.
                                                </div>
                                            ) : (
                                                buildMessageBlocks(historyMessages).map((item) => {

                                                    const msg = item.msg;

                                                    const isMe =
                                                        msg.senderId === currentUser?.id;

                                                    const deletedMessage =
                                                        msg.deletedForEveryone;

                                                    return (
                                                        <React.Fragment
                                                            key={msg.messageId}
                                                        >

                                                            {item.showDateDivider && (
                                                                <div
                                                                    style={{
                                                                        display: "flex",
                                                                        justifyContent: "center",
                                                                        margin: "12px 0"
                                                                    }}
                                                                >
                                                                    <div
                                                                        style={{
                                                                            background: dark
                                                                                ? "#1f2c34"
                                                                                : "#d9fdd3",
                                                                            color: t.textSec,
                                                                            padding: "5px 12px",
                                                                            borderRadius: 8,
                                                                            fontSize: 12,
                                                                            fontWeight: 500
                                                                        }}
                                                                    >
                                                                        {formatDateDivider(
                                                                            msg.createdAt
                                                                        )}
                                                                    </div>
                                                                </div>
                                                            )}

                                                            {item.showTimeDivider && (
                                                                <div
                                                                    style={{
                                                                        display: "flex",
                                                                        justifyContent: "center",
                                                                        margin: "8px 0"
                                                                    }}
                                                                >
                                                                    <div
                                                                        style={{
                                                                            fontSize: 11,
                                                                            color: t.textMuted
                                                                        }}
                                                                    >
                                                                        {formatTime(
                                                                            msg.createdAt
                                                                        )}
                                                                    </div>
                                                                </div>
                                                            )}

                                                            <div
                                                                style={{
                                                                    display: "flex",
                                                                    flexDirection: isMe
                                                                        ? "row-reverse"
                                                                        : "row",
                                                                    alignItems: "flex-end",
                                                                    gap: 8
                                                                }}
                                                            >
                                                                <div
                                                                    style={{
                                                                        maxWidth: "65%"
                                                                    }}
                                                                >
                                                                    <div
                                                                        style={{
                                                                            padding: "9px 14px",
                                                                            borderRadius: isMe
                                                                                ? "16px 16px 4px 16px"
                                                                                : "16px 16px 16px 4px",
                                                                            background: deletedMessage
                                                                                ? (
                                                                                    dark
                                                                                        ? "#202c33"
                                                                                        : "#f0f2f5"
                                                                                )
                                                                                : (
                                                                                    isMe
                                                                                        ? t.bgBubbleMe
                                                                                        : t.bgBubbleThem
                                                                                ),
                                                                            color: deletedMessage
                                                                                ? "#8696a0"
                                                                                : (
                                                                                    isMe
                                                                                        ? "#fff"
                                                                                        : t.text
                                                                                ),
                                                                            fontStyle: deletedMessage
                                                                                ? "italic"
                                                                                : "normal",
                                                                            fontSize: 14,
                                                                            lineHeight: 1.42,
                                                                            wordBreak: "break-word"
                                                                        }}
                                                                    >
                                                                        {deletedMessage
                                                                            ? "This message was deleted"
                                                                            : msg.content}
                                                                    </div>

                                                                    <div
                                                                        style={{
                                                                            fontSize: 11,
                                                                            color: t.textMuted,
                                                                            marginTop: 3,
                                                                            textAlign: isMe
                                                                                ? "right"
                                                                                : "left"
                                                                        }}
                                                                    >
                                                                        {formatTime(
                                                                            msg.createdAt
                                                                        )}
                                                                    </div>
                                                                </div>
                                                            </div>

                                                        </React.Fragment>
                                                    );
                                                })
                                            )}

                                            <div ref={historyMessagesEndRef} />
                                        </div>
                                    )}
                                </div>
                            </div>

                        </div>
                    ) : (
                        <>
                            {/* Header */}
                            <div
                                style={{
                                    height: 64,
                                    padding: "0 20px",
                                    display: "flex",
                                    alignItems: "center",
                                    borderBottom: `1px solid ${t.border}`,
                                    background: t.bgSidebar,
                                    flexShrink: 0
                                }}
                            >
                                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                                    <Avatar initials={getInitials(activeConversation.otherHandleName)} color={stringToColor(activeConversation.otherHandleName)} size={40} bgColor={t.bgSidebar} />
                                    <div>
                                        <div style={{ fontSize: 15, fontWeight: 600 }}>
                                            {activeConversation.otherHandleName}
                                        </div>
                                        <div
                                            style={{
                                                fontSize: 12,
                                                color: presenceMap[activeConversation.otherUserId]?.online ? "#25D366" : "#8696A0"
                                            }}
                                        >
                                            {getPresenceText(activeConversation.otherUserId)}
                                        </div>
                                    </div>
                                </div>

                                {showRestoreButton && (
                                    <button
                                        onClick={() => setRestoreDialogOpen(true)}
                                        style={{
                                            marginLeft: "auto",
                                            padding: "7px 16px",
                                            borderRadius: 10,
                                            border: "none",
                                            cursor: "pointer",
                                            background: t.accent,
                                            color: "#fff",
                                            fontSize: 13,
                                            fontWeight: 600
                                        }}
                                    >
                                        Restore
                                    </button>
                                )}
                            </div>

                            {/* Messages */}
                            <div ref={messagesContainerRef} style={{ flex: 1, overflowY: "auto", padding: "20px 24px", display: "flex", flexDirection: "column", gap: 8 }}>
                                {activeMessages.length === 0 ? (
                                    <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 10, color: t.textMuted }}>
                                        <Avatar initials={getInitials(activeConversation.otherHandleName)} color={stringToColor(activeConversation.otherHandleName)} size={62} bgColor={t.bgChat} />
                                        <div style={{ fontSize: 15, fontWeight: 500, color: t.textSec, marginTop: 4 }}>{activeConversation.otherHandleName}</div>
                                        <div style={{ fontSize: 13 }}>Say hi to start the conversation!</div>
                                    </div>
                                ) : messageBlocks.map((item) => {
                                    const msg = item.msg;
                                    const dividerElements = (
                                        <React.Fragment>
                                            {item.showDateDivider && (
                                                <div style={{ display: "flex", justifyContent: "center", margin: "12px 0" }}>
                                                    <div
                                                        style={{
                                                            background: dark ? "#1f2c34" : "#d9fdd3",
                                                            color: t.textSec,
                                                            padding: "5px 12px",
                                                            borderRadius: 8,
                                                            fontSize: 12,
                                                            fontWeight: 500
                                                        }}
                                                    >
                                                        {formatDateDivider(msg.createdAt)}
                                                    </div>
                                                </div>
                                            )}
                                            {item.showTimeDivider && (
                                                <div style={{ display: "flex", justifyContent: "center", margin: "8px 0" }}>
                                                    <div style={{ fontSize: 11, color: t.textMuted }}>
                                                        {formatTime(msg.createdAt)}
                                                    </div>
                                                </div>
                                            )}
                                        </React.Fragment>
                                    );

                                    const isMe = msg.senderId === currentUser?.id;
                                    const deletedMessage = msg.deletedForEveryone;
                                    return (
                                        <React.Fragment key={msg.messageId}>
                                            {dividerElements}
                                            <div
                                                style={{
                                                    display: "flex",
                                                    flexDirection: isMe ? "row-reverse" : "row",
                                                    alignItems: "flex-end",
                                                    gap: 8
                                                }}
                                            >
                                                {!isMe && <Avatar initials={getInitials(msg.handleName)} color={stringToColor(msg.handleName)} size={28} bgColor={t.bgChat} />}
                                                <div style={{ maxWidth: "65%" }}>
                                                    <div
                                                        onContextMenu={(e) => {
                                                            if (msg.deletedForEveryone) {
                                                                return;
                                                            }

                                                            e.preventDefault();
                                                            const isMe = msg.senderId === currentUser?.id;
                                                            setMessageMenu({
                                                                x: isMe ? e.clientX - 180 : e.clientX,
                                                                y: e.clientY,
                                                                messageId: msg.messageId,
                                                                isMe
                                                            });
                                                        }}
                                                        style={{
                                                            padding: "9px 14px",
                                                            borderRadius: isMe ? "16px 16px 4px 16px" : "16px 16px 16px 4px",
                                                            background: deletedMessage
                                                                ? (dark ? "#202c33" : "#f0f2f5")
                                                                : isMe ? t.bgBubbleMe : t.bgBubbleThem,
                                                            color: deletedMessage ? "#8696a0" : isMe ? "#fff" : t.text,
                                                            fontStyle: deletedMessage ? "italic" : "normal",
                                                            fontSize: 14,
                                                            lineHeight: 1.42,
                                                            fontWeight: 400,
                                                            letterSpacing: "0.1px",
                                                            wordBreak: "break-word"
                                                        }}
                                                    >
                                                        {deletedMessage ? "This message was deleted" : msg.content}
                                                    </div>
                                                    <div style={{ fontSize: 11, color: t.textMuted, marginTop: 3, textAlign: isMe ? "right" : "left" }}>
                                                        {formatTime(msg.createdAt)}
                                                        {isMe && (
                                                            <span
                                                                style={{
                                                                    marginLeft: 3,
                                                                    fontSize: "11px",
                                                                    color: msg.seen ? "#53bdeb" : "#8696a0",
                                                                    fontWeight: 500
                                                                }}
                                                            >
                                                    {msg.delivered ? "✓✓" : "✓"}
                                                </span>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        </React.Fragment>
                                    );
                                })}

                                {messageMenu && (
                                    <div
                                        style={{
                                            position: "fixed",
                                            top: messageMenu.y,
                                            left: messageMenu.x,
                                            background: t.bgSidebar,
                                            border: `1px solid ${t.border}`,
                                            borderRadius: 8,
                                            zIndex: 9999,
                                            minWidth: 120,
                                            boxShadow: "0 4px 12px rgba(0,0,0,0.2)"
                                        }}
                                    >
                                        <div
                                            onClick={() => handleDeleteForMe(confirmDeleteDialog.messageId)}
                                            style={{ padding: "10px 14px", cursor: "pointer", color: "#ff4d4f" }}
                                        >
                                            Delete
                                        </div>
                                    </div>
                                )}
                                <div ref={messagesEndRef} />
                            </div>

                            {/* Input */}
                            <div style={{ padding: "14px 20px", borderTop: `1px solid ${t.border}`, background: t.bgSidebar, flexShrink: 0 }}>
                                <div style={{ display: "flex", alignItems: "center", gap: 10, background: t.bgInput, border: `1px solid ${t.border}`, borderRadius: 14, padding: "8px 8px 8px 14px" }}>
                                    <input
                                        ref={inputRef}
                                        value={input}
                                        onChange={e => setInput(e.target.value)}
                                        onKeyDown={handleKeyDown}
                                        placeholder={`Message ${activeConversation.otherHandleName}…`}
                                        style={{ flex: 1, background: "none", border: "none", fontSize: 14, color: t.text, fontFamily: "inherit" }}
                                    />
                                    <button
                                        onClick={handleSend}
                                        disabled={!input.trim()}
                                        style={{
                                            width: 38, height: 38, borderRadius: 10,
                                            display: "flex", alignItems: "center", justifyContent: "center",
                                            flexShrink: 0,
                                            background: input.trim() ? t.accent : (dark ? "#252535" : "#e0e0ee"),
                                            color: input.trim() ? "#fff" : t.textMuted
                                        }}
                                    >
                                        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                            <line x1="22" y1="2" x2="11" y2="13"/>
                                            <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                                        </svg>
                                    </button>
                                </div>
                            </div>
                        </>
                    )
                ) : (
                    <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 14, color: t.textMuted }}>
                        <div style={{ width: 80, height: 80, borderRadius: 24, background: dark ? "#1e1e2a" : "#e6e6f0", display: "flex", alignItems: "center", justifyContent: "center" }}>
                            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke={t.accent} strokeWidth="1.5" opacity="0.8">
                                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                            </svg>
                        </div>
                        <div style={{ textAlign: "center" }}>
                            <div style={{ fontSize: 19, fontWeight: 600, color: t.textSec, marginBottom: 6 }}>Your messages</div>
                            <div style={{ fontSize: 14, maxWidth: 260, lineHeight: 1.6 }}>
                                Select a chat or tap <strong style={{ color: t.accent }}>+</strong> to start a new conversation
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* ═══════════════════ NEW CHAT DIALOG ═══════════════════ */}
            {showNewChat && (
                <div onClick={closeNewChat}
                     style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.62)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 200 }}>
                    <div onClick={e => e.stopPropagation()}
                         style={{ background: t.bgModal, borderRadius: 20, width: 430, maxHeight: "72vh", display: "flex", flexDirection: "column", border: `1px solid ${t.border}`, boxShadow: "0 28px 80px rgba(0,0,0,0.5)", overflow: "hidden" }}>

                        <div style={{ padding: "20px 20px 14px", borderBottom: `1px solid ${t.border}`, flexShrink: 0 }}>
                            <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 14 }}>
                                <div>
                                    <div style={{ fontSize: 16, fontWeight: 600, color: t.text, marginBottom: 3 }}>New Conversation</div>
                                    <div style={{ fontSize: 12, color: t.textSec }}>Search registered users by username</div>
                                </div>
                                <button onClick={closeNewChat}
                                        style={{ width: 30, height: 30, borderRadius: 8, background: t.searchBg, display: "flex", alignItems: "center", justifyContent: "center", color: t.textSec, fontSize: 15 }}>
                                    ✕
                                </button>
                            </div>
                            <div style={{ display: "flex", alignItems: "center", gap: 8, background: t.searchBg, borderRadius: 11, padding: "9px 13px", border: `1px solid ${t.border}` }}>
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={t.textMuted} strokeWidth="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                                <input ref={contactInputRef} value={contactSearch} onChange={e => setContactSearch(e.target.value)}
                                       placeholder="Search handle name…"
                                       style={{ flex: 1, background: "none", border: "none", fontSize: 14, color: t.text, fontFamily: "inherit" }} />
                                {contactSearch && <button onClick={() => setContactSearch("")} style={{ color: t.textMuted, fontSize: 14 }}>✕</button>}
                            </div>
                        </div>

                        <div style={{ overflowY: "auto", padding: "10px 10px" }}>
                            {loadingUsers ? (
                                <div style={{ textAlign: "center", padding: "36px", color: t.textMuted, fontSize: 13 }}>Loading users...</div>
                            ) : filteredUsers.length === 0 ? (
                                <div style={{ textAlign: "center", padding: "36px 20px", color: t.textMuted, fontSize: 13 }}>
                                    <div style={{ fontSize: 28, marginBottom: 8 }}>🔍</div>
                                    <div style={{ fontWeight: 500, color: t.textSec }}>No users found</div>
                                </div>
                            ) : filteredUsers.filter(u => u.id !== currentUser?.id).map(user => (
                                <div key={user.id} onClick={() => handleStartChat(user)}
                                     style={{ display: "flex", alignItems: "center", gap: 12, padding: "11px 12px", borderRadius: 12, cursor: "pointer", marginBottom: 2, transition: "background 0.12s" }}
                                     onMouseEnter={e => e.currentTarget.style.background = t.bgHover}
                                     onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
                                    <Avatar initials={getInitials(user.handleName)} color={stringToColor(user.handleName)} size={44} bgColor={t.bgModal} />
                                    <div style={{ flex: 1, minWidth: 0 }}>
                                        <div style={{ fontSize: 14, fontWeight: 500, color: t.text }}>{user.handleName}</div>
                                        {user.fullName && <div style={{ fontSize: 12, color: t.textSec }}>{user.fullName}</div>}
                                    </div>
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={t.textMuted} strokeWidth="2"><polyline points="9 18 15 12 9 6"/></svg>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* ═══════════════════ LOGOUT DIALOG ═══════════════════ */}
            {showLogout && (
                <div onClick={() => setShowLogout(false)}
                     style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.62)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 200 }}>
                    <div onClick={e => e.stopPropagation()}
                         style={{ background: t.bgModal, borderRadius: 20, padding: "28px 32px", width: 320, border: `1px solid ${t.border}`, boxShadow: "0 28px 80px rgba(0,0,0,0.5)" }}>
                        <div style={{ width: 50, height: 50, borderRadius: 14, background: dark ? "#2a1a1a" : "#fdecea", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 16px" }}>
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#ef4444" strokeWidth="2.2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                        </div>
                        <div style={{ textAlign: "center", marginBottom: 22 }}>
                            <div style={{ fontSize: 17, fontWeight: 600, marginBottom: 6 }}>Log out?</div>
                            <div style={{ fontSize: 13, color: t.textSec, lineHeight: 1.6 }}>You'll need to sign in again to access your chats.</div>
                        </div>
                        <div style={{ display: "flex", gap: 10 }}>
                            <button onClick={() => setShowLogout(false)}
                                    style={{ flex: 1, padding: "11px", borderRadius: 11, background: t.searchBg, color: t.text, fontSize: 14, fontWeight: 500, border: `1px solid ${t.border}` }}>
                                Cancel
                            </button>
                            <button onClick={handleLogout}
                                    style={{ flex: 1, padding: "11px", borderRadius: 11, background: "#ef4444", color: "#fff", fontSize: 14, fontWeight: 500 }}>
                                Log out
                            </button>
                        </div>
                    </div>
                </div>
            )}
            {(deleteChoiceDialog ||
                confirmDeleteDialog) && (

                <div
                    onClick={closeAllDialogs}
                    style={{
                        position: "fixed",
                        inset: 0,
                        background:
                            "rgba(0,0,0,.35)",
                        backdropFilter:
                            "blur(3px)",
                        zIndex: 9998
                    }}
                />

            )}
            {deleteConversationDialog && (

                <div
                    style={{
                        position: "fixed",
                        top: "50%",
                        left: "50%",
                        transform: "translate(-50%, -50%)",
                        width: 340,
                        background: t.bgSidebar,
                        borderRadius: 16,
                        zIndex: 10001
                    }}
                >

                    <div
                        onClick={() =>
                            setDeleteConversationDialog(null)
                        }
                        style={{
                            position: "absolute",
                            top: 12,
                            right: 12,
                            cursor: "pointer",
                            fontSize: 18
                        }}
                    >
                        ✕
                    </div>

                    <div
                        style={{
                            padding: 20
                        }}
                    >
                        <div
                            style={{
                                fontSize: 18,
                                fontWeight: 600,
                                marginBottom: 10
                            }}
                        >
                            Delete chat?
                        </div>

                        <div
                            style={{
                                color: t.textMuted
                            }}
                        >
                            This chat will disappear from your chat list.
                        </div>
                    </div>

                    <div
                        style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            padding: 16
                        }}
                    >
                        <button
                            onClick={() => {

                                if (
                                    deleteConversationDialog.type === "EVERYONE"
                                ) {

                                    handleDeleteConversationForEveryone(
                                        deleteConversationDialog.conversationId
                                    );

                                } else {

                                    handleDeleteConversationForMe(
                                        deleteConversationDialog.conversationId
                                    );

                                }

                                setDeleteConversationDialog(null);

                            }}
                            style={{
                                color: "#ff4d4f"
                            }}
                        >
                            Delete
                        </button>
                    </div>

                </div>

            )}
            {restoreDialogOpen && (

                <div
                    onClick={() => setRestoreDialogOpen(false)}
                    style={{
                        position: "fixed",
                        inset: 0,
                        background: "rgba(0,0,0,0.45)",
                        zIndex: 9998
                    }}
                >

                    <div
                        onClick={(e) => e.stopPropagation()}
                        style={{
                            position: "fixed",
                            top: "50%",
                            left: "50%",
                            transform: "translate(-50%, -50%)",
                            width: 360,
                            background: t.bgSidebar,
                            borderRadius: 16,
                            zIndex: 9999
                        }}
                    >

                        <div
                            onClick={() => setRestoreDialogOpen(false)}
                            style={{
                                position: "absolute",
                                top: 12,
                                right: 12,
                                cursor: "pointer",
                                fontSize: 18
                            }}
                        >
                            ✕
                        </div>

                        <div
                            style={{
                                padding: 22
                            }}
                        >

                            <div
                                style={{
                                    fontSize: 18,
                                    fontWeight: 600,
                                    marginBottom: 10
                                }}
                            >
                                Restore chat?
                            </div>

                            <div
                                style={{
                                    color: t.textMuted,
                                    lineHeight: 1.5
                                }}
                            >
                                Do you want to restore your previous chat history?
                            </div>

                        </div>

                        <div
                            style={{
                                display: "flex",
                                justifyContent: "flex-end",
                                padding: 16
                            }}
                        >

                            <button
                                onClick={handleRestoreChat}
                                style={{
                                    background: t.accent,
                                    color: "#fff",
                                    border: "none",
                                    borderRadius: 8,
                                    padding: "8px 16px",
                                    cursor: "pointer"
                                }}
                            >
                                Restore
                            </button>

                        </div>

                    </div>

                </div>

            )}
            {deleteChoiceDialog && (

                <div
                    style={{
                        position: "fixed",
                        top: "50%",
                        left: "50%",
                        transform: "translate(-50%, -50%)",
                        width: 320,
                        background: t.bgSidebar,
                        borderRadius: 16,
                        zIndex: 9999,
                        overflow: "hidden"
                    }}
                >

                    {/* Cross */}
                    <div
                        onClick={closeAllDialogs}
                        style={{
                            position: "absolute",
                            top: 12,
                            right: 12,
                            cursor: "pointer",
                            fontSize: 18
                        }}
                    >
                        ✕
                    </div>

                    <div
                        style={{
                            padding: 20,
                            fontSize: 18,
                            fontWeight: 600
                        }}
                    >
                        Delete message?
                    </div>

                    <div
                        onClick={() => {

                            const messageId =
                                deleteChoiceDialog.messageId;

                            setDeleteChoiceDialog(null);

                            setConfirmDeleteDialog({
                                type: "ME",
                                messageId
                            });

                        }}
                        style={{
                            padding: 16,
                            cursor: "pointer"
                        }}
                    >
                        Delete for me
                    </div>

                    {deleteChoiceDialog?.isMe && (
                        <div
                            onClick={() => {

                                const messageId =
                                    deleteChoiceDialog.messageId;

                                setDeleteChoiceDialog(null);

                                setConfirmDeleteDialog({
                                    type: "EVERYONE",
                                    messageId
                                });

                            }}
                            style={{
                                padding: 16,
                                cursor: "pointer"
                            }}
                        >
                            Delete for everyone
                        </div>
                    )}

                </div>

            )}
            {chatMenu && (

                <React.Fragment key="chat-menu">

                    {/* Backdrop */}
                    <div
                        onClick={() => setChatMenu(null)}
                        style={{
                            position: "fixed",
                            inset: 0,
                            zIndex: 9998
                        }}
                    />

                    {/* Menu */}
                    <div
                        style={{
                            position: "fixed",
                            top: chatMenu.y,
                            left: chatMenu.x,
                            background: t.bgSidebar,
                            borderRadius: 12,
                            minWidth: 220,
                            overflow: "hidden",
                            boxShadow: "0 8px 24px rgba(0,0,0,0.25)",
                            zIndex: 9999
                        }}
                    >

                        {/* History */}
                        <div
                            onClick={() => {

                                setHistoryMode(true);

                                setSelectedHistoryConversationId(
                                    chatMenu.conversationId
                                );

                                loadConversationLifecycles(
                                    chatMenu.conversationId
                                );

                                setChatMenu(null);

                            }}
                            onMouseEnter={(e) =>
                                e.currentTarget.style.background =
                                    dark ? "#2a3942" : "#f5f6f6"
                            }
                            onMouseLeave={(e) =>
                                e.currentTarget.style.background =
                                    "transparent"
                            }
                            style={{
                                padding: "14px 16px",
                                cursor: "pointer",
                                fontSize: 14,
                                display: "flex",
                                alignItems: "center",
                                gap: 10
                            }}
                        >
                            <span>🕒</span>
                            <span>History</span>
                        </div>

                        {/* Delete Chat For Me */}
                        <div
                            onClick={() => {

                                setDeleteConversationDialog({
                                    conversationId: chatMenu.conversationId,
                                    handleName: chatMenu.handleName,
                                    type: "ME"
                                });

                                setChatMenu(null);

                            }}
                            onMouseEnter={(e) =>
                                e.currentTarget.style.background =
                                    dark ? "#2a3942" : "#f5f6f6"
                            }
                            onMouseLeave={(e) =>
                                e.currentTarget.style.background =
                                    "transparent"
                            }
                            style={{
                                padding: "14px 16px",
                                cursor: "pointer",
                                fontSize: 14,
                                display: "flex",
                                alignItems: "center",
                                gap: 10
                            }}
                        >
                            <span>🗑</span>
                            <span>Delete Chat</span>
                        </div>

                        {/* Close Conversation For Everyone */}
                        <div
                            onClick={() => {

                                setDeleteConversationDialog({
                                    conversationId: chatMenu.conversationId,
                                    handleName: chatMenu.handleName,
                                    type: "EVERYONE"
                                });

                                setChatMenu(null);

                            }}
                            onMouseEnter={(e) =>
                                e.currentTarget.style.background =
                                    dark ? "#2a3942" : "#f5f6f6"
                            }
                            onMouseLeave={(e) =>
                                e.currentTarget.style.background =
                                    "transparent"
                            }
                            style={{
                                padding: "14px 16px",
                                cursor: "pointer",
                                fontSize: 14,
                                display: "flex",
                                alignItems: "center",
                                gap: 10,
                                color: "#ff4d4f"
                            }}
                        >
                            <span>🚫</span>
                            <span>Close Conversation</span>
                        </div>

                    </div>

                </React.Fragment>

            )}
            {confirmDeleteDialog && (

                <div
                    style={{
                        position: "fixed",
                        top: "50%",
                        left: "50%",
                        transform:
                            "translate(-50%, -50%)",
                        width: 340,
                        background: t.bgSidebar,
                        borderRadius: 16,
                        zIndex: 10001
                    }}
                >

                    <div
                        onClick={closeAllDialogs}
                        style={{
                            position: "absolute",
                            top: 12,
                            right: 12,
                            cursor: "pointer",
                            fontSize: 18
                        }}
                    >
                        ✕
                    </div>

                    <div
                        style={{
                            padding: 20
                        }}
                    >
                        <div
                            style={{
                                fontSize: 18,
                                fontWeight: 600,
                                marginBottom: 10
                            }}
                        >
                            {
                                confirmDeleteDialog.type
                                === "ME"
                                    ? "Delete for me?"
                                    : "Delete for everyone?"
                            }
                        </div>

                        <div
                            style={{
                                color: t.textMuted
                            }}
                        >
                            {
                                confirmDeleteDialog.type
                                === "ME"
                                    ? "This message will only disappear from your chat."
                                    : "This message will be removed for everyone."
                            }
                        </div>
                    </div>

                    <div
                        style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            padding: 16
                        }}
                    >
                        <button
                            onClick={() => {

                                if (
                                    confirmDeleteDialog.type
                                    === "ME"
                                ) {

                                    handleDeleteForMe(
                                        confirmDeleteDialog.messageId
                                    );

                                } else {

                                    handleDeleteForEveryone(
                                        confirmDeleteDialog.messageId
                                    );

                                }

                            }}
                            style={{
                                color: "#ff4d4f"
                            }}
                        >
                            Delete
                        </button>
                    </div>

                </div>

            )}
            {messageMenu && (

                <div
                    onClick={() =>
                        setMessageMenu(null)
                    }
                    style={{
                        position: "fixed",
                        inset: 0,
                        zIndex: 9999
                    }}
                />

            )}
            {messageMenu && (



                <div
                    style={{
                        position: "fixed",
                        top: messageMenu.y,
                        left: messageMenu.x,
                        background: t.bgSidebar,
                        border: `1px solid ${t.border}`,
                        borderRadius: 12,
                        minWidth: 180,
                        overflow: "hidden",
                        zIndex: 10000,
                        boxShadow:
                            "0 8px 24px rgba(0,0,0,.25)"
                    }}
                >

                    <div
                        onClick={() => {
                            if (!messageMenu) return;

                            setDeleteChoiceDialog({
                                messageId: messageMenu.messageId,
                                isMe: messageMenu.isMe
                            });
                            setMessageMenu(null);

                        }}
                        style={{
                            padding: "14px 16px",
                            cursor: "pointer",
                            fontSize: 14
                        }}
                    >
                        🗑 Delete Message
                    </div>

                </div>

            )}
        </div>
    );
}