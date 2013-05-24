<%@ page language="java" pageEncoding="utf-8"%>
<%@	page
	import="weibo4j.org.json.*,
				java.util.*,
				java.io.*,
				weibo4j.Friendships,
				weibo4j.Users,
				weibo4j.model.UserWapper,
				weibo4j.model.User,
				weibo4j.model.WeiboException,
				relationship.SingleUserAnalyse,
				relationship.LuceneAnalyser,
				relationship.Test"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
	String absolutePath = this.getServletConfig().getServletContext()
			.getRealPath("/");
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<base href="<%=basePath%>">
<title>service page</title>
</head>
<body>

	<%
		String tomcatCharSet = "ISO-8859-1";
		String pageCharSet = "utf-8";
		String name = new String(request.getParameter("username").getBytes(
				tomcatCharSet), pageCharSet);
		String type = new String(request.getParameter("dataType").getBytes(
				tomcatCharSet), pageCharSet);
		System.out.println("=========");
		System.out.println(name);
		System.out.println(type);

	/* 	PrintWriter Pout = new PrintWriter(new FileWriter(
				"C:\\Users\\Edward\\Desktop\\test.txt")); */

		JSONObject json = null;
		SingleUserAnalyse sua = (SingleUserAnalyse) session
				.getAttribute(name);

		if (sua == null) {
		String URL=new File(application.getRealPath(request.getRequestURI())).getParentFile().getParent()+"\\usr.txt";
		System.out.println(URL);
			//String tokenFile = absolutePath + "\\usr.txt";
			sua = new SingleUserAnalyse(name,URL);
			session.setAttribute(name, sua);
			System.out.println("first for " + name);
		}

		if (type.equals("relation")) {
			json = (JSONObject) session.getAttribute(name + "-" + type);
			if (json == null) {
				json = sua.getFriends();
				System.out.println("json:"+json);
				if (!json.has("error")) {
					json.put("dataType", "relation");
					session.setAttribute(name + "-" + type, json);
				}
			}
		} else if (type.equals("intimacy")) {
			json = (JSONObject) session.getAttribute(name + "-" + type);
			if (json == null) {
				json = sua.getIntimateUsers();
				
				if (!json.has("error")) {
					json.put("dataType", "intimacy");
					session.setAttribute(name + "-" + type, json);
				}
			}
		} else if (type.equals("interest")) {
			json = (JSONObject) session.getAttribute(name + "-" + type);
			if (json == null) {
				json = sua.getIndexData();
				String id = sua.getId();
				if (id != null) {
					String luceneDir = absolutePath + "\\luceneDir\\"+id;
					String wekaFile = absolutePath + "\\"+id+"keywords.arff";
					LuceneAnalyser la = new LuceneAnalyser(luceneDir,
							wekaFile);
					System.out.println(luceneDir);
					System.out.println(wekaFile);
					json = la.getKeyWords(json);
				}
				
				if (!json.has("error")) {
					json.put("dataType", "keywords");
					session.setAttribute(name + "-" + type, json);
				}

			}

		} else if (type.equals("similarity")) {
			json = (JSONObject) session.getAttribute(name + "-" + type);
			if (json == null) {
				json.put("error", "developing");
				json.put("dataType", "similarity");
				session.setAttribute(name + "-" + type, json);
			}
		} else {
			json.put("error",
					"error within required name,no such type name ");
			throw new Exception("wrong type");
		}

		PrintWriter pw = response.getWriter();//用导入java.io.*,或者java.io.PrintWriter否则错误
		pw.print(json.toString());
		System.out.println("json object :" + json.toString());
		//System.out.println("session("+name+"-"+type+"):"+session.getAttribute(name+"-"+type));

	/* 	Pout.println(json.toString());
		Pout.close(); */

		pw.close();
	%>
</body>
</html>