package com.github.kuramastone.cobblemonChallenges.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ByteUtils {

    public static byte[] convertListToByteArray(List<String> stringList) {
        List<Byte> byteList = new ArrayList<>();

        for(String string : stringList) {
            byte[] stringData = string.getBytes(StandardCharsets.UTF_8);
            byteList.add((byte) stringData.length);
            for (byte stringDatum : stringData) { byteList.add(stringDatum); }
        }

        byte[] array = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            array[i] = byteList.get(i);
        }

        return array;
    }

}
