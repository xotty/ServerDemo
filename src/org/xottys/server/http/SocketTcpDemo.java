/**
 * 使用参数：服务器地址localhost，端口号8000；
 *
 * 本例通过HttpServlet启动ServerSocket，演示了Tcp Socket服务器的主要功能：
 * 1）在init()中实例化ServerSocket，通常要绑定一个1024以上的端口号
 * 2）在service()中启动ServerSocket，等待客户端发送数据以便获取到客户端的socket，收到后对其中的地址和数据进行解析和处理，
 * 并组织服务器数据输出到客户端，其中主要使用了：
 * --socket.getInputStream:接收客户端数据
 * --socket.getOutputStream：发送服务器数据给客户端
 * 3) 在destroy()中完成socket关闭
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

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


@WebServlet(name = "SocketTcp", urlPatterns = ("/tcp"))
public class SocketTcpDemo extends HttpServlet {

    private int port = 8000;
    private ServerSocket tcpSocket;
    private boolean flag1, flag2;

    //生成ServerSocket对象
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            //连接请求队列的长度为3，系统缺省值通常为50
            tcpSocket = new ServerSocket(port, 3);
            flag1 = true;
            flag2 = true;
            System.out.println("Tcp服务器启动1");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //用线程启动socket等待接受数据，并对收到的数据进行处理
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out;
        // 将该服务器是否启动过设置进入session，并据此判断再次收到客户端启动请求时如何处理
        HttpSession session = request.getSession();
        String startedFlag = (String) session.getAttribute("TcpServerStart");

        //服务器初次启动或ServerSocket被关闭后需再次启动服务
        if (startedFlag == null || !startedFlag.equals("yes")) {
            session.setAttribute("TcpServerStart", "yes");
            System.out.println("Tcp服务器启动2");
            //用Servlet启动本socket服务，首先向客户端发送一条信息
            response.setCharacterEncoding("UTF-8");
            try {
                out = response.getWriter();
                out.print("Tcp Server Start!");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            flag1 = true;
            if (tcpSocket.isClosed())
                tcpSocket = new ServerSocket(port, 3);
            //启动socket线程
            new Thread(() -> {
                while (flag1) {
                    Socket socket = null;
                    try {
                        //等待客户端前来连接，从连接请求队列中取出一个连接
                        socket = tcpSocket.accept();

                        System.out.println("Tcp服务器启动3");

                        if (null != socket && !socket.isClosed()) {

                            //设置等待客户连接的超时时间，缺省为永久
                            //socket.setSoTimeout(30000);

                            //处理收到的数据
                            socketDataHandle(socket);
                        }

                    } catch (IOException e1) {
                        //与单个客户通信时遇到的异常，可能是由于客户端过早断开连接引起的，这种异常不应该中断整个while循环
                        e1.printStackTrace();
                    } finally {
                        try {
                            //与一个客户通信结束后，要关闭Socket
                            if (socket != null) socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("Tcp Server Close.....");
                try {
                    tcpSocket.close();

                    //ServerSocket被关闭后需再次启动服务，设置启动标识
                    session.setAttribute("TcpServerStart", "no");
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                try {
//                    out = response.getWriter();
//                    out.print("Tcp Server Stop!");
//                    out.flush();
//                    out.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }).start();
        } else {
            System.out.println("Tcp服务未重复启动");
            response.setCharacterEncoding("UTF-8");
            try {
                out = response.getWriter();
                out.print("Tcp Server Has Been Started!");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //处理数据的方法
    private void socketDataHandle(Socket socket) {
        try {
            System.out.println("Tcp Socket数据处理1");
            //从客户端socket获取输入流
            InputStream in = socket.getInputStream();
            //从客户端socket获取输出流
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            String clientMsg;
            String serverMsg;
            flag2 = true;
            while (flag2) {
                //读取客户端发送的信息
                byte[] buffer = new byte[512];
                int length;
                System.out.println("Tcp Socket数据处理2");
                //将从客户端收到的数据放到buffer中
                while ((length = in.read(buffer)) != -1) {
                    //从客户端收到的字符串
                    clientMsg = new String(buffer, 0, length);

                    //添加服务器信息后返回
                    serverMsg = "Tcp Socket服务器返回：" + clientMsg;

                    //socket输出
                    out.print(serverMsg);
                    out.flush();

                    System.out.println(serverMsg);
                    System.out.println("客户端发来：" + clientMsg);

                    //收到客户端发送的"end"则关闭socket
                    if ("end".equals(clientMsg)) {
                        System.out.println("准备关闭Socket");
                        out.print("准备关闭Socket");
                        out.flush();
                        flag2 = false;
                        break;
                    }
                    //收到客户端发送的"end"则关闭socket
                    else if ("quit".equals(clientMsg)) {
                        System.out.println("准备关闭Tcp ServerSocket");
                        out.print("准备关闭Tcp ServerSocket");
                        out.flush();
                        flag1 = false;
                        flag2 = false;
                        break;
                    }

                }
                out.close();
            }
            socket.close();
            System.out.println("Tcp ServerSocket Close.....");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        System.out.println("Tcp服务器关闭");
        try {
            tcpSocket.close();
            tcpSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.destroy();
    }
}
