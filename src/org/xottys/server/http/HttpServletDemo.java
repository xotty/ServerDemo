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
 *
 * @WebFilter(filterName="ServletFilter",urlPatterns={"/MyServlet","/index.jsp"} 6)web.xml的加载顺序是：context-param->listener->filter->servlet，而个相同类型之间的顺序是按照mapping的顺序。
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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
    private ArrayList<String> users;            //所有账户中的用户名

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
        System.out.print("HttpServlet启动");
    }

    //登录账户
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        account = new HashMap<>();
        //获取url中的参数
        String user = request.getParameter("user");
        String password = request.getParameter("password");

        //验证传入的用户名和密码
        String result = validateAccount(user, password);

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
                outputMsg("已有同名账户", "text/plain", response);
            else {
                //添加新账户到accounts中
                account.put("user", user);
                account.put("password", password);
                accounts.add(account);
                //同时添加新用户名到users中
                users.add(user);

                //用text/plain格式返回结果
                outputMsg("账户添加成功", "text/plain", response);
            }
        } else
            outputMsg("参数错误：user/password为null", "text/plain", response);

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

        //将读取到的全部内容转String后直接封装到Json对象中，然后处理
        try {
            JSONObject jsonObj = new JSONObject(param.toString());
            String user = jsonObj.getString("user");
            String password = jsonObj.getString("password");

            int idx = users.indexOf(user);
            if (idx == -1)
                jsonStr = "{'resultcode':1,'message':'未找到账户，密码修改失败'}";
            else {
                //修改accounts中user对应的密码
                account.put("user", user);
                account.put("password", password);
                accounts.set(idx, account);
                //将返回结果封装成json字符串
                jsonStr = "{'resultcode':0,'message':'密码修改成功'}";

            }

        } catch (JSONException e) {
            e.printStackTrace();
            jsonStr = "{'resultcode':2,'message':'json格式异常，密码修改失败'}";
        } finally {
            //用application/json格式输出返回结果
            outputMsg(jsonStr, "application/json", response);
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
        int code;
        String msg;

        //将客户端body中的xml数据读取并解析到ArrayList中
        ArrayList<HashMap> requestList = readxmlByDom(request.getInputStream());
        //执行删除账户操作
        if (requestList.size() != 0)
            for (HashMap mAccount : requestList) {
                if (deleteAccount(mAccount)) {
                    code = 0;
                    msg = "账户删除成功";
                } else {
                    code = 1;
                    msg = "账户删除失败";
                }

                //每删除一个账户向客户端发送一次xml格式的结果
                StringBuilder sb = new StringBuilder();
                sb.append("<response>");
                sb.append("<head>");
                sb.append("<version>1.0.0</version>");
                sb.append("<dateTime>" + new Date() + "</dateTime>");
                sb.append("</head>");
                sb.append("<body>");
                sb.append("<resultcode>" + code + "</resultcode>");
                sb.append("<info>" + msg + "</info>");
                sb.append("</body>");
                sb.append("</response>");
                outputMsg(sb.toString(), "application/xml", response);
            }
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
        return accounts;

    }

    //将信息输出到客户端
    private void outputMsg(String msg, String contentType, HttpServletResponse response) {
        //让浏览器用utf8来解析返回的数据
        response.setHeader("Content-type", contentType + ";charset=UTF-8");
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

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        System.out.println("session created");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        System.out.println("session destroyed");
    }

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


