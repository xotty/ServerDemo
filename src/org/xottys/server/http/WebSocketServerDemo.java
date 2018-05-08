/**
 * 本例通过Tomcat实现的JavaEE中的WebSocket演示了WebSocket服务器的功能：
 * 1）用@ServerEndpoint标注整个类，这样该类会在Tomcat启动时自动加载
 * 2）覆写 @OnOpen， @OnClose，@OnError和 @OnMessage方法
 * 3）在@OnMessage方法中直接处理收到的信息：文本、二进制、自定义对象等
 * 4）利用Session可以向客户端发送数据：文本、二进制和自定义对象等
 * 5）自定义对象处理时需要提供一个编码/解码器类，该类需实现相关接口：
 * Encoder.Text/Encoder.Binary和Decoder.Text/Decoder.Binary，并用注解声明这个编码/解码器,例如：
 * @ServerEndpoint(value = "/chat/{sessionId}",encoders = MyCodec.class,decoders = MyCodec.class)
 *
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:UpDownServletDemo
 * <br/>Date:May，2018
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.server.http;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

    //该注解用来指定一个URI（这里是：/websocket），客户端可以通过这个URI来连接到WebSocket。
    @ServerEndpoint("/websocket")
    public class WebSocketServerDemo {
        //静态变量，用来记录当前在线连接数。
        private static int onlineCount = 0;

        //concurrent包的线程安全Set，用来存放每个客户端对应的WebSocketServerDemo对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
        private static CopyOnWriteArraySet<WebSocketServerDemo> webSocketSet = new CopyOnWriteArraySet<WebSocketServerDemo>();


        //与某个客户端的连接会话，需要通过它来给客户端发送数据
        private Session session;

        /**
         * 连接建立成功调用的方法
         * @param session  可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
         */
        @OnOpen
        public void onOpen(Session session){
            this.session = session;
            webSocketSet.add(this);     //加入set中
            addOnlineCount();           //在线数加1
            System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        }


        /**
         * 连接关闭时调用的方法
         */
        @OnClose
        public void onClose(){
            webSocketSet.remove(this);  //从set中删除
            subOnlineCount();              //在线数减1
            System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
        }


        /**
         * 收到客户端消息后调用的方法
         * @param message 客户端发送过来的消息
         * @param session 可选的参数
         */
        @OnMessage
        public void OnMessage(String message,Session session) {
            System.out.println("来自客户端的消息:" + message);

            //给所有连接上的WebSocket客户端群发消息
            for(WebSocketServerDemo item: webSocketSet)
                try {
                    sendMessage("WebSocket服务器返回："+message,item );
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

        /*接收数据的方法只能有一个，但方法名可以自定义，参数数量和类型必须符合规定：
          --原始数据类型：int，float,byte等
          --文本：String、Reader
          --二进制：ByteBuffer，byte[]，InputStream
          --自定义对象：此时需要提供一个解码器类（继承自Decoder.Text或Decoder.Binary）
          另外对于较大数据可以加一个boolean参数以标识其是否结束，还可以另外加一个Session参数用来和客户端通信，如：
        @OnMessage
        public void processUpload(byte[] buffer，boolean last) {
            // process partial data here, which check on last to see if these is more on the way
        }*/


        /**
         * 发生错误时调用
         * @param session
         * @param error
         */
        @OnError
        public void onError(Session session, Throwable error){
            System.out.println("WebSockt发生错误");
            error.printStackTrace();
        }

        /**
         * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
         * @param message
         * @throws IOException
         */
        public void sendMessage(String message,WebSocketServerDemo webSocketServerDemo) throws IOException{
            //同步发送消息
            webSocketServerDemo.session.getBasicRemote().sendText(message);
            /*sendBinary(ByteBuffer data)：发送二进制
            * sendObject(Object data)：发送对象，此时需要将提供一个对象编码器类(继承自Encoder.Text或Encoder.Binary)：
            * @ServerEndpoint(value="/websocket", encoders = { ServerEncoder.class })
            */

            //异步发送消息
            //this.session.getAsyncRemote().sendText(message);
        }

        public static synchronized int getOnlineCount() {
            return onlineCount;
        }

        public static synchronized void addOnlineCount() {
            WebSocketServerDemo.onlineCount++;
        }

        public static synchronized void subOnlineCount() {
            WebSocketServerDemo.onlineCount--;
        }
    }
