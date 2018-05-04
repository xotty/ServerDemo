/**
 * 本例通过HttpServlet演示了HTTP服务器上传下载文件的功能：
 * 1）GET：下载文件，数据格式：application/octet-stream
 * 2）POST：上传文件，数据格式：multipart/form-data，commons-fileupload框架
 * 3）PUT：上传文件，数据格式：application/octet-stream
 * 4）DELETE：上传文件，数据格式：multipart/form-data,Servlet3.0框架
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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.*;
import java.util.*;

@WebServlet(name = "UpDownServletDemo", urlPatterns = ("/updown"))
@MultipartConfig
public class UpDownServletDemo extends HttpServlet {
    // 服务器返回信息
    private String message;

    private StringBuilder msg;
    private String savePath, repositoryPath;
    private File file;

    //初始化时将服务器文件和路径确定下来
    @Override
    public void init() throws ServletException {
        super.init();
        //设置上传文件的保存目录
        savePath = this.getServletContext().getRealPath("/upload");
        //设置上传文件的临时保存目录
        repositoryPath = this.getServletContext().getRealPath("/upload/temp");

        // 判断上传文件的保存目录是否存在
        file = new File(savePath);
        if (!file.exists() && !file.isDirectory()) {
            System.out.println(savePath + "目录不存在，需要创建");
            // 创建目录
            file.mkdir();
        }

        // 判断上传文件的临时保存目录是否存在
        file = new File(repositoryPath);
        if (!file.exists() && !file.isDirectory()) {
            System.out.println(repositoryPath + "目录不存在，需要创建");
            // 创建目录
            file.mkdir();
        }
    }

    //下载文件，数据格式：application/octet-stream
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //从客户端获取要下载的文件名
        String fileName = request.getParameter("filename");

        file = new File(savePath + "/" + fileName);
        if (!file.exists()) {
            message = fileName + "文件不存在";
        } else {
            //将下载的文件防放入流中
            FileInputStream in = new FileInputStream(savePath + "/" + fileName);
            //设置response的各项参数
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.addHeader("content-disposition", "attachment;filename=" + fileName);
            response.addHeader("Content-Length", "" + file.length());

            //准备reponse输出流
            ServletOutputStream out = response.getOutputStream();

            byte[] buffer = new byte[in.available()];

            //一次性将服务器文件读到缓冲区，然后写到输出流中发给客户端
            if (in.read(buffer)==file.length()) {
            /*另一种常用读写流的方法
            byte[] buffer = new byte[512];
            int byteSend;
            while ((byteSend = in.read(buffer))!= -1){
                out.write(buffer,0,byteSend);
            }*/

                in.close();

                //将读入的文件流输出到客户端
                out.write(buffer);
                out.flush();
                out.close();

                message = fileName + "下载成功";
            }else
                message = fileName + "下载失败";
        }

        //输出下载结果
        outputMsg(message, response);
    }

    //上传文件一：multipart/form-data，使用commons-fileupload框架，文件的key不能为空
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        msg = new StringBuilder();

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        //上传的文件最大不超过500M Bytes,默认临界值为10kB
        upload.setSizeMax(500 * 1024 * 1024);
        //如果文件查过50M Bytes则用临时文件暂存
        factory.setSizeThreshold(50 * 1024);
        //设置临时文件存储位置
        factory.setRepository(new File(repositoryPath));

        // 判断提交上来的数据格式是否是multipart/form-data
        if (!ServletFileUpload.isMultipartContent(request)) {
            outputMsg("未使用multipart/form-data数据格式，文件无法上传", response);
            return;
        }

        //使用ServletFileUpload解析器解析上传数据，解析结果返回的是一个List<FileItem>集合，每一个FileItem对应一个参数输入
        try {
            List<FileItem> list = upload.parseRequest(request);
            for (FileItem item : list) {
                // 如果fileitem中封装的是普通输入项的数据
                if (item.isFormField()) {
                    //获取Key
                    String name = item.getFieldName();
                    //获取Value
                    String value = item.getString("UTF-8");

                    msg.append(name + "=" + value + "\n");
                    System.out.println(name + "=" + value);
                }
                // 如果fileitem中封装的是上传文件
                else {
                    //得到文件名
                    String filename = item.getName();

                    if (filename == null || filename.trim().equals("")) {
                        continue;
                    }

                    //处理获取到的上传文件的文件名的路径部分，只保留文件名部分
                    filename = filename.substring(filename.lastIndexOf("/") + 1);

                    File uploadedFile = new File(savePath + "/" + filename);

                    //File-Upload方式直接写文件
                    try {
                        item.write(uploadedFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    /*也可以用stream方式输出文件
                    InputStream in = item.getInputStream();
                    byte buffer[] = new byte[1024];
                    FileOutputStream output = new FileOutputStream(savePath + "/" + filename);
                    Streams.copy(in, output, true);
                    in.close();
                    output.close();
                    */
                    msg.append(filename + "上传成功\n");
                }
            }
            message = msg.toString();
        } catch (FileUploadException e) {
            message = "文件上传失败";
            e.printStackTrace();
        }
        //输出文件上传结果到客户端
        outputMsg(message, response);
    }

    //上传文件二：application/octet-stream（二进制流）
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //从Header中提取文件名
        String fileName = request.getHeader("filename");
        if (fileName == null) {
            //从客户端传来的参数中提取文件名
            fileName = request.getParameter("filename");
            //若均无法提取到文件名，则将其设为当前时间值
            if (fileName == null) {
                fileName = String.valueOf((new Date()).getTime());
            }
        }
        //定义要输出的服务器的文件流
        FileOutputStream fis = new FileOutputStream(savePath + "/" + fileName);

        //读取客户端的输入字节流，然后写到服务器文件中去
        ServletInputStream in = request.getInputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;
        //边读（客户端的字节流）边写（服务器的文件流）
        while ((bytesRead = in.read(buffer, 0, 1024)) >= 0) {
            fis.write(buffer, 0, bytesRead);
        }

        fis.close();
        in.close();
        message = fileName + "上传成功";

        //输出文件上传结果到客户端
        outputMsg(message, response);
    }

    //上传文件三：multipart/form-data，使用Servlet3.0自带框架，文件的key不能为空
    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        msg = new StringBuilder();

        //获取文件上传域
        Collection<Part> parts = request.getParts();

        // 循环处理上传的文件
        for (Part part : parts) {
            //获取content-disposition请求头格式:form-data; name="xxx";filename="yyy"
            String header = part.getHeader("content-disposition");
            //属于文件
            if (header.contains("filename")) {
                String fileName = getFileName(header);
                //把文件写到指定路径
                part.write(savePath + File.separator + fileName);
                msg.append(fileName + "上传成功\n");
            }
            //属于字段，没有filename="yyy"这部分
            else {
                String fieldName = part.getName();
                String fieldValue = request.getParameter(fieldName);
                msg.append(fieldName + "=" + fieldValue + "\n");
            }
        }

        outputMsg(msg.toString(), response);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    //输出结果到客户端
    private void outputMsg(String msg, HttpServletResponse response) {
        response.setHeader("Content-type", "text/plain;charset=UTF-8");
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

    //获取文件名
    private String getFileName(String header) {
        String[] tempArr1 = header.split(";");
        String name;
        String[] tempArr2 = tempArr1[2].split("=");
        //去掉路径，获得文件名
        name = tempArr2[1].substring(tempArr2[1].lastIndexOf(File.separator) + 1).replaceAll("\"", "");

        return name;
    }
}