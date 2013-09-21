/*  copyit-server
 *  Copyright (C) 2013  Toon Schoenmakers
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.mms_projects.copy_it.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.mms_projects.copy_it.api.http.Initializer;
import net.mms_projects.copy_it.server.config.Config;
import net.mms_projects.copy_it.server.database.DatabasePool;
import net.mms_projects.copy_it.server.database.MySQL;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0)
            new Config(new File(args[0]));
        else
            new Config(new File("copyit.config"));
        new DatabasePool(MySQL.class, Config.getMaxConnectionsDatabasePool());
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Signal.handle(new Signal("USR2"), new SignalHandler() {
                public void handle(Signal signal) {
                    FileCache.clear();
                    System.err.println("[OK] Cleared file cache.");
                }
            });
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                   .channel(NioServerSocketChannel.class)
                   .childHandler(new Initializer());
            ChannelFuture channelFuture = b.bind(Config.getHTTPAPIPort());
            if (channelFuture.await().isSuccess()) {
                System.err.println("[OK] Bound to port " + Config.getHTTPAPIPort());
                Channel channel = channelFuture.sync().channel();
                channel.closeFuture().sync();
            } else
                System.err.println("[!!] Failed to listen on " + Config.getHTTPAPIPort());
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
