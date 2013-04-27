
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
				weibo4j.model.WeiboException"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">

<title>My JSP 'Output.jsp' starting page</title>
</head>
<body>

	<%
		String tomcatCharSet = "ISO-8859-1";
		String pageCharSet = "utf-8";
		String name = new String(request.getParameter("username").getBytes(
				tomcatCharSet), pageCharSet);
		String levels = new String(request.getParameter("levels").getBytes(
				tomcatCharSet), pageCharSet);
		String types = new String(request.getParameter("types").getBytes(
				tomcatCharSet), pageCharSet);
		System.out.println("=========");
		System.out.println(name);
		System.out.println(levels);
		System.out.println(types);

		JSONObject json = null;
		json = (JSONObject) session.getAttribute(name);

		
		if (json == null) {

			/* 		System.out.println("names:"+session.getAttributeNames());
			 System.out.println("Ctime:"+session.getCreationTime());
			 System.out.println("Mtime:"+session.getMaxInactiveInterval());
			 System.out.println("new:"+session.isNew()); */
			 System.out.println("first for "+name);
			json = new JSONObject();
			json.put("name", name);
			int biFriendNum = 0;
			int uniFriendNum = 0;
			JSONArray biFriendArray = new JSONArray();
			JSONArray uniFriendArray = new JSONArray();

			//String access_token = "2.00N8EaxB08LsGa4ebcc4e9ac0YYqxw";
			String access_token = "2.008w7_4DmapluDdf171919f00qPD39";
			Users um = new Users();
			um.client.setToken(access_token);

			Friendships fm = new Friendships();

			fm.client.setToken(access_token);
			//String id = "1796533527";

			int incaseNum = 0;
			int MAX = 5000;//防止出错死循环不停地抓
			int count = 50;
			int cursor = 0;
			//============temp=================
			/* 	 json.put("id","111111");
				json.put("head","head");  */
			//============temp===============
			try {
				User centreUser = um.showUserByScreenName(name);
				json.put("id", centreUser.getId());
				json.put("head", centreUser.getProfileImageURL());

				ArrayList biIds = fm.myGetFriendsBilateralIds(centreUser
						.getId());
				System.out.println(biIds.size());
				System.out.println(biIds.toString());
				while (true) {
					UserWapper users = fm.myGetFriendsByID(
							centreUser.getId(), count, cursor);
					//UserWapper users = fm.myGetFriendsByScreenName(name, count,cursor);
					for (User u : users.getUsers()) {
						incaseNum++;
						//System.out.println(u.getScreenName()+":"+u.isfollowMe());
						if (biIds.contains(u.getId())) {
							biFriendNum++;
							JSONObject member = new JSONObject();
							member.put("name", u.getScreenName());
							member.put("id", u.getId());
							member.put("head", u.getProfileImageURL());
							biFriendArray.put(member);
						} else {
							uniFriendNum++;
							JSONObject member = new JSONObject();
							member.put("name", u.getScreenName());
							member.put("id", u.getId());
							member.put("head", u.getProfileImageURL());
							uniFriendArray.put(member);
						}
						// System.out.println(incaseNum+":"+u.getScreenName());
					}
					cursor = (int) users.getNextCursor();

					if (cursor == 0 || incaseNum > MAX)
						break;
				}

				json.put("biFriendNum", biFriendNum);
				json.put("uniFriendNum", uniFriendNum);
				json.put("biFriends", biFriendArray);
				json.put("uniFriends", uniFriendArray);
				session.setAttribute(name,json);
			} catch (WeiboException e) {
				json.put("error", e.getError());
				e.printStackTrace();
			}
			//=============temp==================
			/* for(int i=0;i<5;i++){
				JSONObject member = new JSONObject();
				member.put("name", i*10+"iiiiiiiiii");
				member.put("id",i+"");
				member.put("head",i*100+"");
				biFriendArray.put(member);
			}
			json.put("biFriends", biFriendArray);
			json.put("uniFriends", uniFriendArray);  
			session.setAttribute(name,"11111"); */
			//==============temp===================
			
		}

		PrintWriter pw = response.getWriter();//用导入java.io.*,或者java.io.PrintWriter否则错误
		pw.print(json.toString());
		System.out.println("json object :" + json.toString());
		pw.close();
	%>
</body>
</html>