/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Http2ServerUpgradeCodecTest {

    @Test
    public void testUpgradeToHttp2ConnectionHandler() {
        testUpgrade(new Http2ConnectionHandlerBuilder().frameListener(new Http2FrameAdapter())
                .sendPreface(false).build());
    }

    @Test
    public void testUpgradeToHttp2FrameCodec() {
        testUpgrade(new Http2FrameCodecBuilder(true).sendPreface(false).build());
    }

    @Test
    public void testUpgradeToHttp2MultiplexCodec() {
        testUpgrade(new Http2MultiplexCodecBuilder(true, new HttpInboundHandler()).sendPreface(false).build());
    }

    private static void testUpgrade(Http2ConnectionHandler handler) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        request.headers().set(HttpHeaderNames.HOST, "netty.io");
        request.headers().set(HttpHeaderNames.CONNECTION, "Upgrade, HTTP2-Settings");
        request.headers().set(HttpHeaderNames.UPGRADE, "h2c");
        request.headers().set("HTTP2-Settings", "AAMAAABkAAQAAP__");

        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        ChannelHandlerContext ctx = channel.pipeline().firstContext();
        Http2ServerUpgradeCodec codec = new Http2ServerUpgradeCodec("connectionHandler", handler);
        assertTrue(codec.prepareUpgradeResponse(ctx, request, new DefaultHttpHeaders()));
        codec.upgradeTo(ctx, request);

        assertSame(handler, channel.pipeline().remove("connectionHandler"));
        assertNull(channel.pipeline().get(handler.getClass()));
        assertFalse(channel.finish());
    }

    @ChannelHandler.Sharable
    private static final class HttpInboundHandler extends ChannelInboundHandlerAdapter { }
}
