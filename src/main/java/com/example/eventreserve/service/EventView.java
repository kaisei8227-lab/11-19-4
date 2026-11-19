package com.example.eventreserve.service;

import com.example.eventreserve.entity.Event;

/** 掲示板に表示する1件分（イベント＋予約数＋自分が予約済みか） */
public class EventView {

    private final Event event;
    private final long reservedCount;
    private final boolean reservedByMe;

    public EventView(Event event, long reservedCount, boolean reservedByMe) {
        this.event = event;
        this.reservedCount = reservedCount;
        this.reservedByMe = reservedByMe;
    }

    public Event getEvent() { return event; }
    public long getReservedCount() { return reservedCount; }
    public boolean isReservedByMe() { return reservedByMe; }

    public long getRemaining() {
        return Math.max(0, event.getCapacity() - reservedCount);
    }

    public boolean isFull() {
        return getRemaining() == 0;
    }

    /** 予約ボタンを押せるか */
    public boolean isReservable() {
        return !isFull() && !reservedByMe && !event.isClosed();
    }
}
