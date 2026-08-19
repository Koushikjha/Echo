# Temporal Chat Architecture — Lifecycle-Driven Messaging System

> A lifecycle-driven real-time messaging system built around temporal participation windows rather than traditional membership checks.
>
> The platform combines event-oriented design, lifecycle versioning, real-time communication, historical reconstruction, and per-user message state management to provide accurate visibility control, conversation recovery, and time-travel capabilities.

---

# Table of Contents

* Overview
* Core Design Philosophy
* Key Features
* Architecture
* Domain Model
* Lifecycle Architecture
* Real-Time Communication
* Presence System
* Time-Travel Reconstruction
* Message Processing Flow
* Performance Optimizations
* Technical Decisions
* Challenges Solved
* Future Enhancements

---

# Overview

Temporal Chat Architecture is a full-stack real-time messaging platform centered around a simple question:

> Was this user participating when this message existed?

Instead of relying on static membership checks, the system models participation as a sequence of lifecycle windows.

Every major capability is derived from these windows:

* Message visibility
* Conversation restoration
* Delete-for-me
* Rejoin semantics
* Delivered tracking
* Seen tracking
* Time-travel reconstruction
* Historical auditing

The platform currently supports:

* Private messaging
* Real-time WebSocket communication
* Online presence
* Last-seen tracking
* Lifecycle-driven visibility
* Per-user message receipts
* Conversation restoration
* Historical conversation reconstruction
* Pagination and lazy loading
* Lifecycle history browsing

---

# Core Design Philosophy

Traditional messaging systems typically answer:

> Is this user a member of this conversation?

This platform answers:

> Was this user a participant when this message was sent?

This distinction allows the system to maintain complete participation history and reconstruct conversation state across different points in time.

Participation becomes a temporal record rather than a boolean state.

---

# Key Features

## Lifecycle-Driven Visibility

Messages become visible only when:

```sql
message.created_at >= lifecycle.joined_at
AND
(
    lifecycle.left_at IS NULL
    OR
    message.created_at <= lifecycle.left_at
)
```

This guarantees accurate visibility during:

* Join
* Leave
* Restore
* Rejoin
* Time-travel operations

---

## Independent User Lifecycles

Each participant owns an independent lifecycle timeline.

Actions performed by one user never modify the visibility history of another user.

Examples:

* Delete chat for me
* Restore chat
* Rejoin conversation

All operate independently.

---

## Real-Time Messaging

The platform uses WebSockets for:

* Message delivery
* Receipt synchronization
* Online presence updates
* Conversation refreshes
* Reconnect synchronization

Client reconnection automatically synchronizes missed state transitions.

---

## Presence and Last Seen

Presence is maintained independently from messaging state.

Capabilities include:

* Online status
* Offline status
* Last seen timestamp
* Automatic state updates during reconnects

---

## Per-User Receipt Tracking

Every message receives dedicated receipt rows.

Tracked states:

* Delivered
* Seen
* Deleted For Me

Receipts are created eagerly during message creation.

This eliminates:

* Insert races
* Upsert complexity
* Missing receipt scenarios

---

## Conversation Restoration

Deleting a conversation does not destroy history.

Instead:

* Participant lifecycle closes.
* Messages remain intact.
* Conversation becomes invisible.

Restoration reopens participation without mutating historical records.

---

## Historical Reconstruction

The system can reconstruct:

* Previous conversation versions
* Previous participation windows
* Previous visibility states

Historical data remains immutable.

---

# Architecture

```text
Frontend (React)

        │

        ▼

Spring Cloud Gateway

        │

        ▼

Messaging Service

        │

 ┌──────┼──────┐

 ▼      ▼      ▼

Lifecycle  Message  Receipt
Service    Service  Service

        │

        ▼

MySQL
```

Future deployment architecture:

```text
API Gateway

    │

    ├──────── User Service

    ├──────── Messaging Service

    ├──────── Notification Service

    └──────── Event Service

            │

            ▼

        Eureka Registry

            │

            ▼

         OpenFeign
```

---

# Domain Model

## Conversation

Stores:

* Conversation metadata
* Pair uniqueness
* Last message ordering

---

## ConversationLifecycle

Represents:

* Active lifetime of conversation
* Historical versions
* Restore boundaries

---

## ParticipantLifecycle

Represents:

* User participation windows
* Visibility boundaries
* Rejoin history

This entity forms the foundation of the platform.

---

## ChatMessage

Stores:

* Immutable message history
* Edit timestamps
* Soft delete metadata

Messages are never physically removed.

---

## MessageReceipt

Stores:

* Delivered state
* Seen state
* Deleted-for-me state

Receipts are maintained independently per user.

---

# Lifecycle Architecture

The lifecycle model separates:

## Conversation Lifecycle

Tracks:

* When conversations exist

## Participant Lifecycle

Tracks:

* When users participate

Combining both allows:

* Visibility reconstruction
* Accurate rejoin semantics
* Time-travel queries

---

# Real-Time Communication

WebSocket infrastructure supports:

* Instant messaging
* Receipt propagation
* Presence synchronization
* Connection recovery

Reconnect logic guarantees consistency after temporary disconnects.

---

# Presence System

The platform supports:

* Online indicators
* Last-seen timestamps
* Reconnect recovery
* State synchronization

Presence changes propagate in real time to active participants.

---

# Time-Travel Reconstruction

One of the platform's defining capabilities.

Users can browse:

* Previous conversation versions
* Historical participation windows
* Read-only message timelines

The system reconstructs conversation state using:

* ConversationLifecycle
* ParticipantLifecycle
* ChatMessage timestamps

without mutating historical records.

---

# Message Processing Flow

```text
Send Message

    │

    ▼

Validate Lifecycle

    │

    ▼

Create Message

    │

    ▼

Create Receipts

    │

    ▼

Update Conversation

    │

    ▼

Broadcast via WebSocket
```

---

# Performance Optimizations

Implemented optimizations include:

* Lifecycle-window bounded queries
* N+1 elimination in chat retrieval
* Joined fetch strategies
* Bulk delivered updates
* Bulk seen updates
* Offset-based pagination
* Timestamp indexing
* Conversation ordering by last activity

These allow large portions of irrelevant history to be skipped during retrieval.

---

# Technical Decisions

## Append-Only Historical Records

Historical data is preserved.

No lifecycle history is overwritten.

---

## Eager Receipt Creation

Receipts are always available.

Updates become simple UPDATE operations.

---

## Independent Lifecycle Ownership

Participants control their own visibility timelines.

No shared state mutation.

---

## Temporal Authorization

Authorization is determined by participation windows.

Not by static membership.

---

# Challenges Solved

## Rejoin Visibility Problem

Users should not automatically regain access to messages from periods in which they were absent.

Solved through ParticipantLifecycle windows.

---

## Conversation Versioning

Conversations require historical versions.

Solved through ConversationLifecycle records.

---

## Receipt Correctness

Delivery and seen states must remain user-specific.

Solved through dedicated MessageReceipt entities.

---

## Historical Reconstruction

Past conversation states must remain accessible.

Solved through immutable lifecycle records.

---

# Future Enhancements

* Group conversations
* Typing indicators
* Notification service
* Event log service
* Kafka event publishing
* Elasticsearch message search
* Media and attachment support
* Distributed event sourcing infrastructure

---

## Outcome

Temporal Chat Architecture evolved beyond a conventional chat application into a lifecycle-driven messaging platform capable of real-time communication, historical reconstruction, and user-specific temporal visibility.

Its primary innovation is treating participation as a time-based record rather than a membership flag, enabling capabilities that are difficult to achieve using traditional chat architectures.
