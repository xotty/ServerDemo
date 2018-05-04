/**
 * 使用参数：服务器地址localhost，端口号3344；客户端端口号：3345
 *
 * 本例通过HttpServlet启动DatagramSocket，演示了Udp Socket服务器的主要功能：
 * 1）在init()中实例化DatagramSocket，通常要绑定一个1024以上的端口号
 * 2）在service()中启动DatagramSocket，等待客户端数据，收到后对数据进行解析和处理，并组织服务器数据输出到客户端，其中主要使用了：
 * --udpSocket.receive(dataPacket):接收客户端数据到dataPacket中
 * --udpSocket.send(dataPacket)：发送服务器数据给客户端
 * 其中 dataPacket = new DatagramPacket(buffer, buffer.length)，它封装了ip地址、端口号和数据
 * 3) 在destroy()中完成DatagramSocket关闭
 * <p>
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:SocketTcpDemo
 * <br/>Date:May，2018
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.server.http;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;

@WebServlet(name = "SocketUdp", urlPatterns = ("/udp"))
public class SocketUdpDemo extends HttpServlet {
    private byte[] buffer = new byte[1024];
    private DatagramSocket udpSocket = null;
    private DatagramPacket dataPacket = null;
    private InetSocketAddress socketAddress = null;
    private InetAddress clientAddress;
    private String clientIP;
    private int clientPort = 3345;
    private String clientData;
    private int serverPort = 3344;

    @Override
    public void init() throws ServletException {
        super.init();
        //根据端口号创建套接字地址,一个网卡则为本机当前唯一IP地址
        socketAddress = new InetSocketAddress(serverPort);
        try {
            // 创建数据报套接字，将其绑定到指定的本地地址
            udpSocket = new DatagramSocket(socketAddress);

            System.out.println("Udp服务端启动1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        dataPacket = new DatagramPacket(buffer, buffer.length);
    }


    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 将该服务器是否启动过设置进入session，并据此判断再次收到客户端启动请求时如何处理
        HttpSession session = request.getSession();
        String startedFlag = (String) session.getAttribute("UdpServerStart");
        PrintWriter out;
        //服务器初次启动或ServerSocket被关闭后需再次启动服务
        if (startedFlag == null || !startedFlag.equals("yes")) {
            session.setAttribute("UdpServerStart", "yes");
            System.out.println("Udp服务器启动2");

            //用Servlet启动本socket服务，首先向客户端发送一条信息
            response.setCharacterEncoding("UTF-8");
            try {
                out = response.getWriter();
                out.print("Udp Server Start!");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //启动Udp Socket，并收取报文
            new Thread(() -> {
                try {
                    if (udpSocket.isClosed())
                        udpSocket = new DatagramSocket(socketAddress);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (true) {
                    try {
                        if (udpSocket.isClosed()) udpSocket = new DatagramSocket(socketAddress);
                        //从此套接字接收数据报文
                        udpSocket.receive(dataPacket);

                        //从收到的数据报中解析发送方的IP地址
                        clientAddress = dataPacket.getAddress();
                        clientIP = clientAddress.getHostAddress();

                        //从收到的数据报中解析发送方的端口号
                        clientPort = dataPacket.getPort();

                        //从收到的数据报中获取发送方的数据
                        clientData = new String(dataPacket.getData(), 0, dataPacket.getLength());

                        System.out.println("服务器收到udp数据:" + clientIP + "--" + clientPort + "---" + clientData);

                        //向发送方发送服务器数据
                        send("UDP Server Response：" + clientData);

                        if (clientData.equals("end")) break;

                    } catch (IOException e1) {
                        e1.printStackTrace();
                        if (udpSocket != null) udpSocket.close();
                        break;
                    }
                }
                session.setAttribute("UdpServerStart", "no");
                udpSocket.close();
                System.out.println("Udp Socket已关闭");
            }).start();
        } else {
            System.out.println("Udp服务未重复启动");

            response.setCharacterEncoding("UTF-8");
            try {
                out = response.getWriter();
                out.print("Udp Server has been Started!");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //发送udp报文给客户端
    private void send(String info) throws IOException {
        clientPort = 3345;
        dataPacket.setAddress(clientAddress);
        dataPacket.setPort(clientPort);
        dataPacket.setData(info.getBytes());

        udpSocket.send(dataPacket);
        System.out.println("Udp服务器发送数据：" + info + "---" + clientIP + "---" + clientPort);
    }

    @Override
    public void destroy() {
        System.out.println("Udp服务器关闭");
        udpSocket.close();
        udpSocket = null;
        super.destroy();
    }

}
