// 
// Decompiled by Procyon v0.5.36
// 

package com.mulesoft.tool.network;

import java.io.InputStream;
import java.util.Scanner;
import java.io.SequenceInputStream;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.InetAddress;

public class NetworkUtils
{
    public static String ping(final String host) throws Exception {
        return execute(new ProcessBuilder(new String[] { "ping", "-c", "4", host }));
    }
    
    public static String resolveIPs(final String host, String dnsServer) throws UnknownHostException {
        if (dnsServer.equals("default")) {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < addresses.length; ++i) {
                if (i != 0) {
                    sb.append("\n");
                }
                sb.append(addresses[i].getHostAddress());
            }
            return sb.toString();
        }
        dnsServer = "@" + dnsServer;
        try {
            return execute(new ProcessBuilder(new String[] { "dig", "+short", dnsServer, host }));
        }
        catch (IOException e) {
            return e.getMessage();
        }
    }
    
    public static String curl(final String url) throws IOException {
        return execute(new ProcessBuilder(new String[] { "curl", "-k", "-i", "-L", url }));
    }
    
    public static String testConnect(final String host, final String port) {
        long startTime = System.nanoTime();
        long totalTime = System.nanoTime();
        String result = "";
        for (int x = 1; x <= 5; ++x) {
            try {
                final Socket socket = new Socket();
                startTime = System.nanoTime();
                socket.connect(new InetSocketAddress(host, Integer.parseInt(port)), 10000);
                socket.setSoTimeout(10000);
                if (socket.isConnected()) {
                    totalTime = System.nanoTime() - startTime;
                    socket.getInputStream();
                }
                socket.close();
            }
            catch (UnknownHostException e3) {
                return "Could not resolve host " + host;
            }
            catch (SocketTimeoutException e4) {
                return "Timeout while trying to connect to " + host;
            }
            catch (IllegalArgumentException e) {
                return e.getMessage();
            }
            catch (Exception e2) {
                final ByteArrayOutputStream b = new ByteArrayOutputStream();
                e2.printStackTrace(new PrintStream(b));
                return b.toString();
            }
            result = result + "Probe " + x + ": Connection successful, RTT=" + Long.toString(totalTime / 1000000L) + "ms\n";
        }
        return result + "socket test completed";
    }
    
    public static String traceRoute(final String host) throws Exception {
        return execute(new ProcessBuilder(new String[] { "traceroute", "-w", "3", "-q", "1", "-m", "18", "-n", host }));
    }
    
    public static String certest(final String host, final String port) throws Exception {
        return execute(new ProcessBuilder(new String[] { "openssl", "s_client", "-showcerts", "-servername", host, "-connect", host + ":" + port }));
    }
    
    public static String cipherTest(final String host, final String port) throws Exception {
        String remoteEndpointSupportedCiphers = "List of supported ciphers:\n\n";
        final String[] split;
        final String[] openSslAvailableCiphers = split = execute(new ProcessBuilder(new String[] { "openssl", "ciphers", "ALL:!eNULL" })).split(":");
        for (final String cipher : split) {
            if (execute(new ProcessBuilder(new String[] { "openssl", "s_client", "-cipher", cipher, "-servername", host, "-connect", host + ":" + port })).contains("BEGIN CERTIFICATE")) {
                remoteEndpointSupportedCiphers = remoteEndpointSupportedCiphers + cipher + ": YES\n";
            }
            else {
                remoteEndpointSupportedCiphers = remoteEndpointSupportedCiphers + cipher + ": NO\n";
            }
        }
        return remoteEndpointSupportedCiphers;
    }
    
    private static String execute(final ProcessBuilder pb) throws IOException {
        final Process p = pb.start();
        final OutputStream stdin = p.getOutputStream();
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
        writer.write("\n");
        writer.flush();
        writer.close();
        final SequenceInputStream sis = new SequenceInputStream(p.getInputStream(), p.getErrorStream());
        final Scanner s = new Scanner(sis).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
