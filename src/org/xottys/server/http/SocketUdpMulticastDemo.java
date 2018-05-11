/**
 * 使用参数：服务器地址localhost或实际ip地址，端口号9002；客户端端口号：9001
 *
 * 本例通过HttpServlet启动MulticastSocket，演示了Udp组播Socket服务器的主要功能：
 * 1）在init()中实例化MulticastSocket，通常要绑定一个1024以上的端口号
 * 2）在service()中启动MulticastSocket，等待客户端数据，收到后对数据进行解析和处理，并组织服务器数据输出到客户端，其中主要使用了：
 * --udpSocket.receive(dataPacket):接收客户端数据到dataPacket中
 * --udpSocket.send(dataPacket)：发送服务器数据给客户端
 * 其中 dataPacket = new DatagramPacket(buffer, buffer.length)---接收用
 *     dataPacket = new DatagramPacket(buffer, buffer.length,组播ip,port)---发送用
 * 3) 在destroy()中完成MulticastSocket关闭
 * 4） 用ServletContext保存组播服务器是否启动的信息
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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;

@WebServlet(name = "SocketUdpMulticast", urlPatterns = ("/multicast"))
public class SocketUdpMulticastDemo extends HttpServlet {
    //组播地址
    private static final String mhost  ="239.0.0.1";

    private byte[] buffer = new byte[1024];
    private MulticastSocket mUdpSocket=null;
    private DatagramPacket dataPacket = null;
    private int clientPort = 9001;
    private String clientData;
    private int serverPort = 9002;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // 创建组播数据报套接字，将其绑定到指定的本地地址
            mUdpSocket= new MulticastSocket(serverPort);
            mUdpSocket.joinGroup(InetAddress.getByName(mhost));
            mUdpSocket.setTimeToLive(32);

            System.out.println("Udp组播服务端启动1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        dataPacket = new DatagramPacket(buffer, buffer.length);
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out;

        //将该服务器是否启动过设置到Application属性中，并据此判断再次收到客户端启动请求时如何处理
        ServletContext sc=getServletConfig().getServletContext();
        String startedFlag = (String) sc.getAttribute("UdpMulticastServerStart");

        //服务器初次启动或ServerSocket被关闭后需再次启动服务
        if (startedFlag == null || startedFlag.equals("no")) {
            sc.setAttribute("UdpMulticastServerStart", "yes");
            System.out.println("Udp组播服务器启动2");

            //用Servlet启动本socket服务，首先向客户端发送一条信息
            response.setCharacterEncoding("UTF-8");
            try {
                out = response.getWriter();
                out.print("Udp Multicast Server Start!");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //启动Udp Socket，并收取报文
            new Thread(() -> {
                try {
                    if (mUdpSocket.isClosed()){
                        mUdpSocket= new MulticastSocket(serverPort);
                        mUdpSocket.joinGroup(InetAddress.getByName(mhost));
                        mUdpSocket.setTimeToLive(32);  }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Udp组播服务器准备接收数据");
                while (true) {
                    try {
                        mUdpSocket.setReceiveBufferSize(1024);
                        mUdpSocket.setSendBufferSize(1024);
                        buffer = new byte[1024];
                        dataPacket = new DatagramPacket(buffer, buffer.length);
                        System.out.println("Udp Server waiting for client data");
                        //从此套接字接收数据报文
                        mUdpSocket.receive(dataPacket);

                        //从收到的数据报中解析发送方的端口号
                        clientPort = dataPacket.getPort();

                        //从收到的数据报中获取发送方的数据
                        clientData = new String(dataPacket.getData(), 0, dataPacket.getLength(),"UTF-8");

                        System.out.println("Udp Server received:" + clientData);
                        if (clientData.contains("XAH")) {
                            //收到心跳包信息，不做任何加工,直接回送给客户端
                            send(clientData);
                        } else {
                            //收到非心跳包信息，添加服务器信息后，回送给客户端
                            send("Udp Socket Server:" + clientData);
                        }

                        if (clientData.equals("stop")) break;

                    } catch (IOException e1) {
                        e1.printStackTrace();
                       // if (udpSocket != null) udpSocket.close();
                       //  break;
                    }
                }
                sc.setAttribute("UdpMulticastServerStart", "no");
                mUdpSocket.close();
                System.out.println("Udp Multicast Server closed");
            }).start();
        } else {
            System.out.println("Udp Multicast Server has been started");

            response.setCharacterEncoding("UTF-8");
            try {
                out = response.getWriter();
                out.print("Udp Multicast Server has been started!");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //发送udp报文给客户端
    private void send(String info) throws IOException {
        //用于接收非Demo发送的数据
        clientPort=9001;
        buffer = new byte[1024];
        dataPacket = new DatagramPacket(buffer, buffer.length,InetAddress.getByName(mhost),clientPort);
        dataPacket.setData(info.getBytes("UTF-8"));
        mUdpSocket.send(dataPacket);

        System.out.println("Udp Multicast Server send：" + info);
    }

    @Override
    public void destroy() {
        System.out.println("Udp Multicast Server Closed");
        mUdpSocket=null;
        super.destroy();
    }
}
