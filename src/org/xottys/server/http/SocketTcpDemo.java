/**
 * 使用参数：服务器地址localhost，端口号8000；
 * <p>
 * 本例通过HttpServlet启动ServerSocket，演示了Tcp Socket服务器的主要功能：
 * 1）在init()中实例化ServerSocket，通常要绑定一个1024以上的端口号
 * 2）在service()中启动ServerSocket，等待客户端发送数据以便获取到客户端的socket，收到后对其中的地址和数据进行解析和处理，
 * 并组织服务器数据输出到客户端，其中主要使用了：
 * --socket.getInputStream:接收客户端数据， 收到"end"：关闭服务器socket连接，"stop"：停止服务器socket服务
 * --socket.getOutputStream：发送服务器数据给客户端
 * 3) 在destroy() 中完成socket关闭
 * 4）本服务器端没有用心跳包来检测客户端是否断开，而是用了socket.sendUrgentData(0xFF)
 * 5) 用ServletContext保存服务器是否启动的信息
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
    private int socketCounter = 1;
    private ServletContext sc;

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

        //将该服务器是否启动过设置到Application属性中，并据此判断再次收到客户端启动请求时如何处理
        sc=getServletConfig().getServletContext();
        String startedFlag = (String) sc.getAttribute("TcpServerStart");
        //服务器初次启动或ServerSocket被关闭后需再次启动服务
        if (startedFlag == null || !startedFlag.equals("yes")) {
            socketCounter=1;
            sc.setAttribute("TcpServerStart", "yes");
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

            //可以接受多个客户端socket连接请求，正常时一直循环等待客户端socket连接
            while (flag1) {
                //等待客户端前来连接，从连接请求队列中取出一个连接
                Socket socket = tcpSocket.accept();

                //设置等待客户连接的超时时间，缺省为永久
                //socket.setSoTimeout(30000);
                System.out.println("第 " + (socketCounter++) + " 个连接到达,"+"ip:"+socket.getInetAddress().getHostAddress()+",port:"+socket.getPort());
                Thread t = new Thread(new ThreadServerSocket(socket));
                t.start();
            }
            //退出循环意味着需要关闭ServerSocket
            System.out.println("Tcp Server Closed.....");
            try {
                tcpSocket.close();
                flag2 = false;
                //ServerSocket被关闭后需再次启动服务，设置启动标识
                sc.setAttribute("TcpServerStart", "no");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }//根据Session Attribute判断服务器已经启动
        else {
            System.out.println("Tcp Server has been started!");
            response.setCharacterEncoding("UTF-8");
            try {
                out = response.getWriter();
                out.print("Tcp Server has been started!");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //作为线程来运行，用来接收和返回socket数据
    class ThreadServerSocket implements Runnable {

        private Socket socket;

        public ThreadServerSocket(Socket client) {
            socket = client;
        }

        @Override
        public void run() {
            System.out.println("Tcp服务器启动3");

            if (socket != null && !socket.isClosed() && socket.isConnected()) {
                //处理收到的数据
                socketDataHandle(socket);
            }
        }
    }

    //处理数据的方法
    private void socketDataHandle(Socket socket) {
        int BUFFER_SIZE = 1024;
        try {
            System.out.println("Tcp Socket数据处理1");
            //从客户端socket获取输入流
            InputStream in = socket.getInputStream();
            //从客户端socket获取输出流
            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            String clientMsg;
            String serverMsg;
            flag2 = true;
            while (flag2) {
                //读取客户端发送的信息
                byte[] buffer = new byte[BUFFER_SIZE];
                int length;
                //  System.out.println("Tcp Socket数据处理2：等待接收客户端数据");
                //将从客户端收到的数据放到buffer中
                while ((length = in.read(buffer)) != -1) {
                    //从客户端收到的字符串
                    clientMsg = new String(buffer, 0, length);
                    if (clientMsg.contains("XAH")) {
                        //收到心跳包信息，不做任何处理
                        serverMsg = clientMsg;
                    } else {
                        //收到非心跳包信息，添加服务器信息后返回
                        serverMsg = "Tcp Socket Server：" + clientMsg;
                    }

                    //socket输出
                    pw.println(serverMsg);
                    pw.flush();

                    System.out.println("客户端发来：" + clientMsg);
                    System.out.println("服务器返回："+ serverMsg);

                    //收到客户端发送的"end"则关闭socket
                    if ("end".equals(clientMsg)) {
                        System.out.println("准备关闭Socket");
                        pw.println("准备关闭Socket");
                        pw.flush();
                        flag2 = false;
                        in.close();
                        pw.close();
                        socket.close();
                        System.out.println("Socket Close.....");
                        break;
                    }
                    //收到客户端发送的"stop"则关闭server
                    else if ("stop".equals(clientMsg)) {
                        System.out.println("准备关闭Tcp ServerSocket");
                        pw.println("准备关闭Tcp ServerSocket");
                        pw.flush();
                        flag1 = false;
                        flag2 = false;
                        in.close();
                        pw.close();
                        socket.close();
                        tcpSocket.close();
                        System.out.println("Tcp ServerSocket Closed.....");
                        sc.setAttribute("TcpServerStart", "no");
                        break;
                    }
                }
                if (flag2) {
                    try {
                        //判断客户端socket是否关闭
                        socket.sendUrgentData(0xFF);
                        Thread.sleep(3000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    } catch (IOException e2) {
                        //客户端socket已关闭
                        e2.printStackTrace();
                        in.close();
                        pw.close();
                        socket.close();
                        System.out.println("Client Socket Close.....");
                        socketCounter--;
                        break;
                    }
                    System.out.println("当前流中的数据已读完，正在等待新数据......");
                }
            }

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
