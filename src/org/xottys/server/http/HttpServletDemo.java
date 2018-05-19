/**
 * 本例通过HttpServlet演示了HTTP服务器的主要功能：GET/POST/PUT/DELETE
 * 1）在init()中做数据初始化
 * 2）在doGet/doPost/doPut/doDelete中完成传入数据解析和客户端相应数据输出，其中依次使用了：
 * --Url参数，text/plain格式返回数据
 * --application/x-www-form-urlencoded数据，text/plain格式返回数据
 * --application/json数据
 * --application/xml数据
 * 3) 在destroy()中完成变量清理
 * 4）使用@WebListener注解实现各种监听器：
 * --ServletRequestListener和ServletRequestAttributeListener
 * --HttpSessionListener和HttpSessionAttributeListener
 * --ServletContextListener和ServletContextAttributeListener
 * 5）使用@WebFilter注解实现过滤器，例如：
 * @WebFilter(filterName="ServletFilter",urlPatterns={"/MyServlet","/index.jsp"} 6)web.xml的加载顺序是：context-param->listener->filter->servlet，而个相同类型之间的顺序是按照mapping的顺序。
 * 6）通过request.getCookies()获取客户端Cookie，通过response.addCookie(cookie)向客户端发送Cookie，Cookie 内容由Servlet提供的Cookie类生成和管理
 * 7）通过request.getSession建立和获取Session，然后用session.getAttribute和session.setAttribute操作其中的属性键值对
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:HttpServletDemo
 * <br/>Date:May，2018
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.server.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@WebServlet(name = "HttpServletDemo", urlPatterns = ("/login"))
@WebListener
public class HttpServletDemo extends HttpServlet implements HttpSessionAttributeListener,
        HttpSessionListener {
    private ArrayList<HashMap> accounts;        //存储所有账户
    private HashMap<String, String> account;    //当前账户
    private ArrayList<String> users;//所有账户中的用户名
    static private String sessionID="";
    private HttpServletResponse httpServletResponse;
    //给账户赋初值：admin/admin，可用此账户成功登录
    @Override
    public void init() throws ServletException {
        super.init();
        accounts = new ArrayList<>();
        account = new HashMap<>();
        account.put("user", "admin");
        account.put("password", "admin");
        accounts.add(account);
        users = new ArrayList<>();
        users.add("admin");
        System.out.println("HttpServlet启动");
    }

    //登录账户
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        account = new HashMap<>();
        //获取客户端传来的Cookie
        Cookie cookies[] = request.getCookies();
        String cookieUser = "", cookiePassword = "", cookieLoginTime = "";
        if (cookies != null) {
            System.out.println("request.getCookies().length:" + cookies.length);
            for (Cookie cook : cookies) {
                //在这里对客户端传来的Cookie进行处理
                System.out.println("request.getCookies():" + cook + "/" + cook.getName() + "/" + cook.getValue());
                if (cook.getName().equals("user")) cookieUser = cook.getValue();
                if (cook.getName().equals("password")) cookiePassword = cook.getValue();
                if (cook.getName().equals("loginTime")) cookieLoginTime = cook.getValue();
            }
            //只能获取多条Cookie中最新的一条
            System.out.println("requst.getHeader('Cookie'):" + request.getHeader("Cookie"));
            //可以获取多条Cookie全部
            Enumeration ss = request.getHeaders("Cookie");
            while (ss.hasMoreElements()) {
                String value = (String) ss.nextElement();//调用nextElement方法获得元素
                System.out.println("request.getHeaders('Cookie'):" + value);
            }
        }
        //获取url中的参数
        String user = request.getParameter("user");
        String password = request.getParameter("password");

        //验证传入的用户名和密码
        String result = validateAccount(user, password);

        if (result.equals("登录成功")) {
            //设置三个Cookie返回给客户端，每个只有一个名值对，必须先设置后发送
            //也可以在一个Cookie中放多个名值对，这需要自己定义格式和解析，且其中不能使用Cookie专用字符，如"；"
            Cookie cookie = new Cookie("user", user); // 新建Cookie
            cookie.setMaxAge(1);        // 设置生命周期为1秒
            cookie.setHttpOnly(true);
            cookie.setPath("/login");
            response.addCookie(cookie);
            cookie = new Cookie("password", password); // 新建Cookie
            cookie.setMaxAge(10); // 设置生命周期为10秒
            response.addCookie(cookie);
            //cookie内容中不能有空格和分号
            SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd&HH:mm:ss");
            cookie = new Cookie("loginTime", sd.format(new Date())); // 新建Cookie
            cookie.setMaxAge(Integer.MAX_VALUE); // 设置生命周期为永久
            response.addCookie(cookie);

            System.out.println("New Cookie Set:" + response.getHeaders("Set-Cookie"));
        }

        //用text/plain格式返回验证结果
        outputMsg(result, "text/plain", response);
    }

    //新增账户，数据格式为：application/x-www-form-urlencoded
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        account = new HashMap<>();
        //获取application/x-www-form-urlencoded格式body中的参数
        String user = request.getParameter("user");
        String password = request.getParameter("password");

        if (user != null && password != null) {
            if (users.contains(user))
                outputMsg("已有同名账户", "application/x-www-form-urlencoded", response);
            else {
                //添加新账户到accounts中
                account.put("user", user);
                account.put("password", password);
                accounts.add(account);
                //同时添加新用户名到users中
                users.add(user);

                //用text/plain格式返回结果
                outputMsg("账户添加成功", "application/x-www-form-urlencoded", response);

      }
        } else
            outputMsg("参数错误：user/password为null", "application/x-www-form-urlencoded", response);
    }

    //修改账户密码，据格式为：application/json
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        account = new HashMap<>();
        String line, jsonStr = "";
        StringBuilder param = new StringBuilder();

        //获取数据格式为application/json的body中数据
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) param.append(line);

        //获取客户端SessionID，然后根据获得的SessionID进行判断和处理
        HttpSession session = request.getSession(false);
        System.out.println("SessionID?"+session.getId()+"="+sessionID);
        if (session!=null && session.getId().equals(sessionID)){
            String sessionUser = (String) session.getAttribute("user");
            String sessionPassword = (String) session.getAttribute("password");
            Date sessionPutTime = (Date) session.getAttribute("updateTime");
            if (sessionUser!=null&&sessionPassword!=null&&sessionPutTime!=null)
                System.out.println("Session Read:"  + sessionUser+"/"+sessionPassword+"/"+sessionPutTime);
            else
                System.out.println("Session 失效！");
        }

        //将读取到的全部内容转String后直接封装到Json对象中，然后处理
        try {
            JSONObject jsonObj = new JSONObject(param.toString());
            String user = jsonObj.getString("user");
            //将对象转为String，否则可能得到数字或String
            Object value = jsonObj.get("password");
            String password;
            if (value instanceof Number)
                password = JSONObject.valueToString(jsonObj.get("password"));
            else
                password = jsonObj.getString("password");

            int idx = users.indexOf(user);
            if (idx == -1)
                jsonStr = "{'code':1,'message':'未找到账户，密码修改失败'}";
            else {
                //修改accounts中user对应的密码
                account.put("user", user);
                account.put("password", password);
                accounts.set(idx, account);
                //将返回结果封装成json字符串
                jsonStr = "{'code':0,'message':'密码修改成功'}";

                //建立或获取已有的Session，然后对其Attribute进行更新
                session = request.getSession();
                session.setMaxInactiveInterval(300);//设置超时，单位：分钟
                session.setAttribute("user", user);
                session.setAttribute("password", password);
                session.setAttribute("updateTime", new Date());
                sessionID=session.getId();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            jsonStr = "{'code':2,'message':'json格式异常，密码修改失败'}";
        } finally {
            //用application/json格式输出返回结果
            JSONObject jsonObj = new JSONObject(jsonStr);
            //让浏览器用utf8来解析返回的数据
            response.setHeader("Content-type", "application/json;charset=UTF-8");
            //让servlet用UTF-8转码，而不是用默认的ISO8859
            response.setCharacterEncoding("UTF-8");
            try {
                PrintWriter out = response.getWriter();
                out.print(jsonObj);
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
              }
    }

    //删除账户,数据格式为：application/xml，通过Body传递如下格式的xml数据
    //<request>
    //    <account>
    //        <user>xottys</user>
    //        <password>123456</password>
    //    </account>
    //</request>
    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int code = 0;
        String message = "";
        //将客户端body中的xml数据读取并解析到ArrayList中
        ArrayList<HashMap> requestList = readxmlByDom(request.getInputStream());
        //执行删除账户操作
        if (requestList != null) {
            if (requestList.size() != 0)
                for (HashMap mAccount : requestList) {
                    if (deleteAccount(mAccount)) {
                        code = 0;
                        message = "账户删除成功";
                    } else {
                        code = 1;
                        message = "账户删除失败：账户验证无效";
                    }
                }

        } else {
            code = 2;
            message = "账户删除失败:xml解析错误";
        }
        //每删除一个账户向客户端发送一次xml格式的结果
        StringBuilder sb = new StringBuilder();
        sb.append("<response>");
        sb.append("<head>");
        sb.append("<version>1.0.0</version>");
        sb.append("<dateTime>" + new Date() + "</dateTime>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<code>" + code + "</code>");
        sb.append("<info>" + message + "</info>");
        sb.append("</body>");
        sb.append("</response>");
        outputMsg(sb.toString(), "application/xml", response);

    }

    @Override
    public void destroy() {
        accounts.clear();
        users.clear();
        System.out.print("HttpServlet销毁");
        super.destroy();
    }

    //验证用户名/密码是否正确
    private String validateAccount(String user, String password) {
        String msg;
        if (user != null && password != null) {
            account.put("user", user);
            account.put("password", password);
            int idx = accounts.indexOf(account);
            if (idx != -1)
                msg = "登录成功";
            else
                msg = "账户或密码错误";

        } else
            msg = "参数错误：user/password为null";

        return msg;
    }


    //删除一个已存在和账户：用户名/密码
    private boolean deleteAccount(HashMap account) {
        boolean deleteResult;
        if (accounts.contains(account)) {
            deleteResult = accounts.remove(account);
            if (deleteResult)
                deleteResult = users.remove(account.get("user"));
        } else
            deleteResult = false;

        return deleteResult;
    }

    //用DOM方式解析xml
    private ArrayList<HashMap> readxmlByDom(InputStream xmlInput) {
        ArrayList<HashMap> accounts = new ArrayList<>();
        HashMap<String, String> account;
        DocumentBuilderFactory dbFactory;
        DocumentBuilder db = null;
        Document document = null;
        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            db = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        //将给定 URI 的内容解析为一个 XML 文档,并返回Document对象
        try {
            if (db != null) document = db.parse(xmlInput);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (document != null) {
            document.getDocumentElement().normalize();
            //按文档顺序返回包含在文档中且具有给定标记名称的所有 Element 的 NodeList
            NodeList accountList = document.getElementsByTagName("account");
            for (int temp = 0; temp < accountList.getLength(); temp++) {
                Node nNode = accountList.item(temp);
                System.out.println("\nCurrent Element :"
                        + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    account = new HashMap<>();
                    Element eElement = (Element) nNode;

                    //get element content
                    account.put("user", eElement.getElementsByTagName("user")
                            .item(0)
                            .getTextContent());
                    account.put("password", eElement.getElementsByTagName("password")
                            .item(0)
                            .getTextContent());

                    accounts.add(account);
                }
            }
        }
        return accounts;
    }

    //将信息输出到客户端
    private void outputMsg(String msg, String contentType, HttpServletResponse response) {
        //让浏览器用utf8来解析返回的数据
        response.addHeader("Content-type", contentType + ";charset=UTF-8");
        //让servlet用UTF-8转码，而不是用默认的ISO8859
        response.setCharacterEncoding("UTF-8");
        try {
            PrintWriter out = response.getWriter();
            out.print(msg);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Session监听器
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        System.out.println("Session created:"+se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {

        System.out.println("Session destroyed");
    }

    //Attribute监听器
    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        System.out.println("session attribute added");
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        System.out.println("session attribute removed");
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        System.out.println("session attribute replaced");
    }

}


