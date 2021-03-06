/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ljn.netty3.tunnelclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.http.HttpTunnelingClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

/**
 * An HTTP tunneled version of the telnet client example.  Please refer to the
 * API documentation of the <tt>org.jboss.netty.channel.socket.http</tt> package
 * for the detailed instruction on how to deploy the server-side HTTP tunnel in
 * your Servlet container.
 */
public class HttpTunnelingClientExample {

    private final URI uri;

    public HttpTunnelingClientExample(URI uri) {
        this.uri = uri;
    }

    public void run() throws IOException {
        String scheme = uri.getScheme() == null? "http" : uri.getScheme();

        // Configure the client.
        ClientBootstrap b = new ClientBootstrap(
                new HttpTunnelingClientSocketChannelFactory(
                        new OioClientSocketChannelFactory(Executors.newCachedThreadPool())));

        b.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new StringDecoder(),
                        new StringEncoder(),
                        new ClientHandler());
            }
        });

        // Set additional options required by the HTTP tunneling transport.
        b.setOption("serverName", uri.getHost());
        b.setOption("serverPath", uri.getRawPath());

        System.out.println("serverName=" + uri.getHost());
        System.out.println("serverPath=" + uri.getRawPath());
        System.out.println("port=" + uri.getPort());
        
        
        if (!scheme.equals("http")) {
            // Only HTTP are supported.--bylijinnan
            System.err.println("Only HTTP is supported.");
            return;
        }

        // Make the connection attempt.
        ChannelFuture channelFuture = b.connect(
                new InetSocketAddress(uri.getHost(), uri.getPort()));
        channelFuture.awaitUninterruptibly();

        // Read commands from the stdin.
        System.out.println("Enter text ('quit' to exit)");
        ChannelFuture lastWriteFuture = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (; ;) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }

            // Sends the received line to the server.
            //bylijinnan:发送数据是在HttpTunnelingClientSocketPipelineSink
            System.out.println(channelFuture.getChannel().getClass());//HttpTunnelingClientSocketChannel
            lastWriteFuture = channelFuture.getChannel().write(line);
        }

        // Wait until all messages are flushed before closing the channel.
        if (lastWriteFuture != null) {
            lastWriteFuture.awaitUninterruptibly();
        }

        channelFuture.getChannel().close();
        // Wait until the connection is closed or the connection attempt fails.
        channelFuture.getChannel().getCloseFuture().awaitUninterruptibly();

        // Shut down all threads.
        b.releaseExternalResources();
    }

    public static void main(String[] args) throws Exception {
        /*
        if (args.length != 1) {
            System.err.println(
                    "Usage: " + HttpTunnelingClientExample.class.getSimpleName() +
                    " <URL>");
            System.err.println(
                    "Example: " + HttpTunnelingClientExample.class.getSimpleName() +
                    " http://localhost:8080/netty-tunnel");
            return;
        }
        */
        String uri_str = "http://localhost:8088/netty3tunnelserver/netty-tunnel";
        URI uri = new URI(uri_str);
        new HttpTunnelingClientExample(uri).run();
    }
}
