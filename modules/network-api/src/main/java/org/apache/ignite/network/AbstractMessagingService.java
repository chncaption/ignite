/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.apache.ignite.network.annotations.MessageGroup;

/**
 * Base class for {@link MessagingService} implementations.
 */
public abstract class AbstractMessagingService implements MessagingService {
    /** Mapping from group type (array index) to a list of registered message handlers. */
    private final AtomicReferenceArray<List<NetworkMessageHandler>> handlersByGroupType =
        new AtomicReferenceArray<>(Short.MAX_VALUE + 1);

    /** {@inheritDoc} */
    @Override public void addMessageHandler(Class<?> messageGroup, NetworkMessageHandler handler) {
        handlersByGroupType.getAndUpdate(getMessageGroupType(messageGroup), handlers -> {
            if (handlers == null)
                return List.of(handler);

            var result = new ArrayList<NetworkMessageHandler>(handlers.size() + 1);

            result.addAll(handlers);
            result.add(handler);

            return result;
        });
    }

    /**
     * Extracts the message group ID from a class annotated with {@link MessageGroup}.
     */
    private static short getMessageGroupType(Class<?> messageGroup) {
        MessageGroup annotation = messageGroup.getAnnotation(MessageGroup.class);

        assert annotation != null : "No MessageGroup annotation present on " + messageGroup;

        short groupType = annotation.groupType();

        assert groupType >= 0 : "Group type must not be negative";

        return groupType;
    }

    /**
     * @return registered message handlers.
     */
    protected final Collection<NetworkMessageHandler> getMessageHandlers(short groupType) {
        assert groupType >= 0 : "Group type must not be negative";

        List<NetworkMessageHandler> result = handlersByGroupType.get(groupType);

        return result == null ? List.of() : result;
    }
}