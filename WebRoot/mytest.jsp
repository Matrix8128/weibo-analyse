<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<%@	page
	import="weibo4j.org.json.*,
				java.util.*,
				java.io.*,
				weibo4j.Friendships,
				weibo4j.Users,
				weibo4j.model.UserWapper,
				weibo4j.model.User,
				weibo4j.model.WeiboException,
				edward.test"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">

</head>
<body>

	<%
		test t = new test();
		String tomcatCharSet = "ISO-8859-1";
		String pageCharSet = "utf-8";
		JSONObject js=new JSONObject();
		String name = new String(request.getParameter("username").getBytes(
				tomcatCharSet), pageCharSet);
		String levels = new String(request.getParameter("levels").getBytes(
				tomcatCharSet), pageCharSet);
		String types = new String(request.getParameter("types").getBytes(
				tomcatCharSet), pageCharSet);
		t.setName(name);
		System.out.println(t.getName());
		js.put("name", t.getName());
		//没有以下三条(关键是新建PrintWriter),就会弹出返回
		PrintWriter pw = response.getWriter();//用导入java.io.*,或者java.io.PrintWriter否则错误
		pw.print(js.toString());
		pw.close();
	%>

</body>
</html>