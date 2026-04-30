/*
 * Copyright (c) 2026 CSC- IT Center for Science, www.csc.fi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.csc.shibboleth.plugin.library.inform.impl;

import java.util.concurrent.LinkedBlockingDeque;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class EventStore {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(EventStore.class);

    /** Max items in buffer. */
    @Value("%{csclib.eventStore.maxBufferItems:1000}")
    private int maxSize;

    /** Buffer. */
    private final LinkedBlockingDeque<String> buffer = new LinkedBlockingDeque<>();

    /**
     * Add new Item to buffer. If maximum number is reached oldest one is removed.
     * 
     * @param value item to add
     */
    public synchronized void addItemToBuffer(String value) {
        if (buffer.size() >= maxSize) {
            String removed = buffer.pollFirst();
            log.warn("Buffer is full, removing oldest item '{}'", removed);
        }
        buffer.offerLast(value);
    }

    /**
     * Get and remove oldest item from buffer.
     * 
     * @return oldest item.
     * @throws InterruptedException
     */
    public String takeItemFromBuffer() throws InterruptedException {
        return buffer.takeFirst();
    }

    /**
     * Returns back the item as oldest. So it will be tried next.
     * 
     * @param value item.
     */
    public synchronized void returnItemToBuffer(String value) {
        log.warn("Item returned to buffer, item is '{}'", value);
        if (buffer.size() >= maxSize) {
            buffer.pollLast();
        }
        buffer.offerFirst(value);
    }
}