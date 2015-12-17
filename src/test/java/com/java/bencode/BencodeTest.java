package com.java.bencode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class BencodeTest {

    @Test
    public void decodeTest() throws IOException, URISyntaxException {
        Path path = Paths.get(ClassLoader.getSystemResource("01.torrent").toURI());
        byte[] data = Files.readAllBytes(path);
        Files.write(Paths.get("/Users/mye/decoded01.out"), BencodingUtil.decodeBytes(data));
    }

    @Test
    public void encodeTest() throws IOException, URISyntaxException {
        Path path = Paths.get(ClassLoader.getSystemResource("decoded01.out").toURI());
        byte[] data = Files.readAllBytes(path);
        Files.write(Paths.get("/Users/mye/encoded01.torrent"), BencodingUtil.encodeBytes(data));
    }
    
    @Test
    public void decodeTest2() throws IOException, URISyntaxException {
        Path path = Paths.get(ClassLoader.getSystemResource("encoded01.torrent").toURI());
        byte[] data = Files.readAllBytes(path);
        Files.write(Paths.get("/Users/mye/decoded02.out"), BencodingUtil.decodeBytes(data));
    }
}
