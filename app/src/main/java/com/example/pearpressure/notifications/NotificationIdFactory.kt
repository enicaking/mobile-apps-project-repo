package com.example.pearpressure.notifications

import java.util.concurrent.atomic.AtomicInteger

object NotificationIdFactory {
    private val counter = AtomicInteger((System.currentTimeMillis() % Int.MAX_VALUE).toInt())

    fun nextId(): Int {
        return counter.incrementAndGet()
    }
}