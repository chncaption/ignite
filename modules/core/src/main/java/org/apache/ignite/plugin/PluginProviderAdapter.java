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

package org.apache.ignite.plugin;

import org.apache.ignite.*;
import org.apache.ignite.cluster.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Adapter for {@link PluginProvider}.
 */
public class PluginProviderAdapter<C extends PluginConfiguration> implements PluginProvider<C> {
    /** {@inheritDoc} */
    @Override public String name() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public String version() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public String copyright() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public <T extends IgnitePlugin> T plugin() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void initExtensions(PluginContext ctx, ExtensionRegistry registry) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Nullable @Override public <T> T createComponent(PluginContext ctx, Class<T> cls) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void onBeforeStart() throws IgniteCheckedException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void start(PluginContext ctx) throws IgniteCheckedException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onAfterStart() throws IgniteCheckedException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onAfterStop(boolean cancel) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) throws IgniteCheckedException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onBeforeStop(boolean cancel) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Nullable @Override public Serializable provideDiscoveryData(UUID nodeId) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void receiveDiscoveryData(UUID nodeId, Serializable data) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void validateNewNode(ClusterNode node) throws PluginValidationException {
        // No-op.
    }
}
