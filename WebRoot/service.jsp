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
				relationship.Test,
				relationship.UserTopicModel,
				segment.Segment;"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
	String absolutePath = this.getServletConfig().getServletContext()
			.getRealPath("/");
	File resource = new File(absolutePath, "resource");
	String URL = new File(resource, "usr.txt").getAbsolutePath();
	String segmentInitLib = new File(resource, "segmentLib")
			.getAbsolutePath();
	System.out.println(segmentInitLib);
	String chineseStopWord = new File(resource, "chinese_stopword.dic")
			.getAbsolutePath();
	String englishStopWord = new File(resource, "english_stopword.dic")
			.getAbsolutePath();
	System.out.println(chineseStopWord);
	String inferFile = new File(resource,
			"weibo-XNV-removedLowFreq3-2000.infer").getAbsolutePath();
	/* String URL = new File(application.getRealPath(request
					.getRequestURI())).getParentFile().getParent()
					+ "\\resource\\usr.txt";
	String segmentInitLib = new File(application.getRealPath(request
	.getRequestURI())).getParentFile().getParent()
	+ "\\resource\\segmentLib";
	String chineseStopWord=new File(application.getRealPath(request
	.getRequestURI())).getParentFile().getParent()
	+ "\\resource\\segmentLib"; */
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<base href="<%=basePath%>">
<title>service page</title>
</head>
<body>
	<%
		
	%>
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

		if (sua == null || sua.tokenURL == null) {
			System.out.println(URL);
			//String tokenFile = absolutePath + "\\usr.txt";
	sua = new SingleUserAnalyse(name, URL);
			session.setAttribute(name, sua);
			System.out.println("first for " + name);
		}

		if (type.equals("relation")) {
			json = (JSONObject) session.getAttribute(name + "-" + type);
			if (json == null) {
				json = sua.getFriends();
				System.out.println("json:" + json);
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
				if(!json.has("error")){
					String semidata = sua.transToString(json);
					//String semidata="tests yes 导入停用词表，";
					String id = sua.getId();
					if (id != null) {
						File srcFile = new File(absolutePath + "\\" + id
								+ "-userData.txt");
						System.out.println("file name:"
								+ srcFile.getAbsolutePath());
						srcFile.createNewFile();
	
						PrintWriter Pout = new PrintWriter(new BufferedWriter(
								new OutputStreamWriter(new FileOutputStream(
										srcFile), "utf8")));
						Pout.println(semidata);
						Pout.close();
	
						Segment test = new Segment(segmentInitLib, "UTF8");
						// 导入停用词表，第二个参数是停用词表文件编码，包中自带的是utf8编码。
						test.importStopWords(chineseStopWord, "utf8");
						test.importStopWords(englishStopWord, "utf8");
						Set<String> filter = new HashSet<String>();
						// filter.add("url");
						filter.add("x");
						filter.add("n");
						filter.add("vn");
						test.setMinTermlength(2);
						test.setTypeFilter(filter, true);
	
						File segmentedFile = new File(srcFile.getParentFile(),
								id + "-segmentdFile");
						System.out.println("segmenteing data......");
						test.segmentFile(srcFile.getAbsolutePath(), "utf8",
								segmentedFile.getAbsolutePath(), false);
						test.segmentExit();
						UserTopicModel utm = new UserTopicModel();
						utm.loadInfer(new File(inferFile));
						System.out.println("model loaded!");
						json = utm.getKeyWord(segmentedFile);
	
					}
	
					if (!json.has("error")) {
						json.put("dataType", "keywords");
						session.setAttribute(name + "-" + type, json);
					}
	
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
			//throw new Exception("wrong type");
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