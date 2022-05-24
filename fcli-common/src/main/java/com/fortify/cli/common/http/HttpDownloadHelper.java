package com.fortify.cli.common.http;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class HttpDownloadHelper {
    public static void Download(URL url, String downloadDest){
        try {
            ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());

            FileOutputStream fileOutputStream = new FileOutputStream(downloadDest);
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            //fileOutputStream.getChannel()
            //        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
