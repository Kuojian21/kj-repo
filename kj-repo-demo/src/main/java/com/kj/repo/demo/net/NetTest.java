package com.kj.repo.demo.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.kj.repo.infra.utils.RunUtil;

/**
 * @author kj
 * Created on 2020-05-17
 */
public class NetTest {

    public static void bind() throws IOException {
        ServerSocket server = new ServerSocket(8888);
        do {
            Socket socket = server.accept();
            new Thread(() -> {
                RunUtil.run(() -> {
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    do {
                        System.out.println(br.readLine());
                    } while (true);
                });
            }).start();
            new Thread(() -> {
                RunUtil.run(() -> {
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    do {
                        pw.println(socket.getLocalPort() + "#" + socket.getPort());
                    } while (true);
                });
            }).start();
        } while (true);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new Thread(() -> RunUtil.run(() -> {
            bind();
            return null;
        })).start();
        Thread.sleep(10000);
        Socket client = new Socket("127.0.0.1", 8888);
        new Thread(() -> {
            RunUtil.run(() -> {
                BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
                do {
                    System.out.println(br.readLine());
                } while (true);
            });
        }).start();
        new Thread(() -> {
            RunUtil.run(() -> {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                do {
                    pw.println(client.getLocalPort() + "#" + client.getPort());
                } while (true);
            });
        }).start();
    }

}
