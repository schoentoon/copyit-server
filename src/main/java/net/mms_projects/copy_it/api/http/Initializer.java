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

package net.mms_projects.copy_it.api.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class Initializer extends ChannelInitializer<SocketChannel> {
    private static final String DECODER = "decoder";
    private static final String ENCODER = "encoder";
    private static final String HANDLER = "handler";

    public void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline p = ch.pipeline();
        p.addLast(DECODER, new HttpRequestDecoder());
        p.addLast(ENCODER, new HttpResponseEncoder());
        p.addLast(HANDLER, new Handler());
    }
}
