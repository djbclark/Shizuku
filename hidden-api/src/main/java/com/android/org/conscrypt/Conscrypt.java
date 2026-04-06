package com.android.org.conscrypt;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

public class Conscrypt {
    public static byte[] exportKeyingMaterial(
            SSLSocket socket,
            String label,
            byte[] context,
            int length
    ) throws SSLException {
        throw new RuntimeException();
    }
}
