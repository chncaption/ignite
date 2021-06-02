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

package org.apache.ignite.network.internal.netty;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ServerChannel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.ignite.lang.IgniteInternalException;
import org.apache.ignite.network.NetworkMessage;
import org.apache.ignite.network.internal.handshake.HandshakeAction;
import org.apache.ignite.network.internal.handshake.HandshakeManager;
import org.apache.ignite.network.serialization.MessageDeserializer;
import org.apache.ignite.network.serialization.MessageMappingException;
import org.apache.ignite.network.serialization.MessageReader;
import org.apache.ignite.network.serialization.MessageSerializationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link NettyServer}.
 */
public class NettyServerTest {
    /** Server. */
    private NettyServer server;

    /** */
    @AfterEach
    final void tearDown() {
        server.stop().join();
    }

    /**
     * Tests a successful server start scenario.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testSuccessfulServerStart() throws Exception {
        var channel = new EmbeddedServerChannel();

        server = getServer(channel.newSucceededFuture(), true);

        assertTrue(server.isRunning());
    }

    /**
     * Tests a graceful server shutdown scenario.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testServerGracefulShutdown() throws Exception {
        var channel = new EmbeddedServerChannel();

        server = getServer(channel.newSucceededFuture(), true);

        server.stop().join();

        assertTrue(server.getBossGroup().isTerminated());
        assertTrue(server.getWorkerGroup().isTerminated());
    }

    /**
     * Tests an unsuccessful server start scenario.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testServerFailedToStart() throws Exception {
        var channel = new EmbeddedServerChannel();

        server = getServer(channel.newFailedFuture(new ClosedChannelException()), false);

        assertTrue(server.getBossGroup().isTerminated());
        assertTrue(server.getWorkerGroup().isTerminated());
    }

    /**
     * Tests a non-graceful server shutdown scenario.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testServerChannelClosedAbruptly() throws Exception {
        var channel = new EmbeddedServerChannel();

        server = getServer(channel.newSucceededFuture(), true);

        channel.close();

        assertTrue(server.getBossGroup().isShuttingDown());
        assertTrue(server.getWorkerGroup().isShuttingDown());
    }

    /**
     * Tests a scenario where a server is stopped before a server socket is successfully bound.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testServerStoppedBeforeStarted() throws Exception {
        var channel = new EmbeddedServerChannel();

        ChannelPromise future = channel.newPromise();

        server = getServer(future, false);

        CompletableFuture<Void> stop = server.stop();

        future.setSuccess(null);

        stop.get(3, TimeUnit.SECONDS);

        assertTrue(server.getBossGroup().isTerminated());
        assertTrue(server.getWorkerGroup().isTerminated());
    }

    /**
     * Tests that a {@link NettyServer#start} method can be called only once.
     *
     * @throws Exception
     */
    @Test
    public void testStartTwice() throws Exception {
        var channel = new EmbeddedServerChannel();

        server = getServer(channel.newSucceededFuture(), true);

        assertThrows(IgniteInternalException.class, server::start);
    }

    /**
     * Tests that handshake manager is invoked upon a client connecting to a server.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testHandshakeManagerInvoked() throws Exception {
        HandshakeManager handshakeManager = Mockito.mock(HandshakeManager.class);

        Mockito.doReturn(CompletableFuture.completedFuture(Mockito.mock(NettySender.class)))
            .when(handshakeManager).handshakeFuture();

        Mockito.doReturn(HandshakeAction.NOOP)
            .when(handshakeManager).init(Mockito.any());

        Mockito.doReturn(HandshakeAction.NOOP)
            .when(handshakeManager).onConnectionOpen(Mockito.any());

        Mockito.doReturn(HandshakeAction.NOOP)
            .when(handshakeManager).onMessage(Mockito.any(), Mockito.any());

        MessageSerializationRegistry registry = new MessageSerializationRegistry() {
            /** {@inheritDoc} */
            @Override public <T extends NetworkMessage> MessageDeserializer<T> createDeserializer(short type) {
                return (MessageDeserializer<T>) new MessageDeserializer<>() {
                    /** {@inheritDoc} */
                    @Override public boolean readMessage(MessageReader reader) throws MessageMappingException {
                        return true;
                    }

                    /** {@inheritDoc} */
                    @Override public Class<NetworkMessage> klass() {
                        return NetworkMessage.class;
                    }

                    /** {@inheritDoc} */
                    @Override public NetworkMessage getMessage() {
                        return new NetworkMessage() {
                            /** {@inheritDoc} */
                            @Override public short directType() {
                                return 0;
                            }
                        };
                    }
                };
            }
        };

        server = new NettyServer(4000, handshakeManager, sender -> {}, (socketAddress, message) -> {}, registry);

        server.start().get(3, TimeUnit.SECONDS);

        CompletableFuture<Channel> connectFut = NettyUtils.toChannelCompletableFuture(
            new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(new NioEventLoopGroup())
                .handler(new ChannelInitializer<>() {
                    /** {@inheritDoc} */
                    @Override protected void initChannel(Channel ch) throws Exception {
                        // No-op.
                    }
                })
                .connect(server.address())
        );

        Channel channel = connectFut.get(3, TimeUnit.SECONDS);

        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();

        // One message only.
        for (int i = 0; i < (NetworkMessage.DIRECT_TYPE_SIZE + 1); i++)
            buffer.writeByte(1);

        channel.writeAndFlush(buffer).get(3, TimeUnit.SECONDS);

        channel.close().get(3, TimeUnit.SECONDS);

        InOrder order = Mockito.inOrder(handshakeManager);

        order.verify(handshakeManager, timeout()).init(Mockito.any());
        order.verify(handshakeManager, timeout()).handshakeFuture();
        order.verify(handshakeManager, timeout()).onConnectionOpen(Mockito.any());
        order.verify(handshakeManager, timeout()).onMessage(Mockito.any(), Mockito.any());
    }

    /**
     * @return Verification mode for a one call with a 3 second timeout.
     */
    private static VerificationMode timeout() {
        return Mockito.timeout(TimeUnit.SECONDS.toMillis(3));
    }

    /**
     * Creates a server from a backing {@link ChannelFuture}.
     *
     * @param future Server channel future.
     * @param shouldStart {@code true} if a server should start successfully
     * @return NettyServer.
     * @throws Exception If failed.
     */
    private static NettyServer getServer(ChannelFuture future, boolean shouldStart) throws Exception {
        ServerBootstrap bootstrap = Mockito.spy(new ServerBootstrap());

        Mockito.doReturn(future).when(bootstrap).bind(Mockito.anyInt());

        var server = new NettyServer(bootstrap, 0, Mockito.mock(HandshakeManager.class), null, null, null);

        try {
            server.start().get(3, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            if (shouldStart)
                fail(e);
        }

        return server;
    }

    /** Server channel on top of the {@link EmbeddedChannel}. */
    private static class EmbeddedServerChannel extends EmbeddedChannel implements ServerChannel {
        // No-op.
    }
}