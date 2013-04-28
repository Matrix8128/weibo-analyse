package relationship;

import weibo4j.org.json.*;
import weibo4j.Comments;
import weibo4j.Friendships;
import weibo4j.Timeline;
import weibo4j.Users;
import weibo4j.model.Comment;
import weibo4j.model.CommentWapper;
import weibo4j.model.Paging;
import weibo4j.model.User;
import weibo4j.model.UserWapper;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.Visible;
import weibo4j.model.WeiboException;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.*;

/*import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.Iterator;
 import java.util.List;
 import java.util.PriorityQueue;*/

public class SingleUserAnalyse {
	// String access_token ="2.00prQs_CixbFRE1a4bbda7e90jsdM8";
	 String access_token = "2.00N8EaxB08LsGa4ebcc4e9ac0YYqxw";
	//String access_token = "2.008w7_4DmapluDdf171919f00qPD39";
	/*
	 * String id = "1796533527"; String name = "胡新辰点点点";
	 */
	String id = null;
	String name = null;

	JSONObject json = new JSONObject();

	JSONObject friendsJson = null;
	// 中心用户
	User centreUser = null;
	String userId = null;
	// 关注用户
	UserWapper friends = null;
	// 互粉用户
	int biFriendNum = 0;
	UserWapper biFriends = null;
	JSONArray biFriendArray = new JSONArray();
	// 单向关注用户
	int uniFriendNum = 0;
	UserWapper uniFriends = null;
	JSONArray uniFriendArray = new JSONArray();
	// 转发用户微博的人
	int topRtStatusNum = 20;// 取转发数最多的topK条微博
	int topRtUserNum = 20;// 去转发数最多的topK用户数
	int rtUserNum = 0;
	JSONArray rtUserArray = new JSONArray();
	JSONObject rtUsers = new JSONObject();
	ArrayList<UserCount> rtUserList = null;
	int totalRtCount = 0;// rtUserList 中用户的总评论数，用户相似度归一化计算。
	// 评论微博用户的人
	int topComStatusNum = 20;// 取评论数最多的topK条微博
	int topComUserNum = 20;// 去评论数最多的topK用户数
	int comUserNum = 0;
	JSONArray comUserArray = new JSONArray();
	JSONObject comUsers = new JSONObject();
	ArrayList<UserCount> comUserList = null;
	static int newStatusWeight = 5;// 只要在一条微博下评论，就加该值，一条微博只加一次，不管留了多少评论
	int totalComCount = 0;// comUserList 中用户的总评论数，用户相似度归一化计算。
	// 用户转发的微博的作者
	int rtedAuthorNum = 0;
	JSONArray rtAuthorArray = new JSONArray();
	JSONObject rtAuthors = new JSONObject();
	ArrayList<UserCount> rtAuthorList = null;
	int totalRtAuthorCount = 0;
	// 用户回复的评论者的
	int repliedUserNum = 0;
	int topReUserNum = 20;
	JSONArray repliedUserArray = new JSONArray();
	JSONObject repliedUsers = new JSONObject();
	ArrayList<UserCount> repliedUserList = null;
	int totalRepliedCount = 0;
	// 根据标签。类别的系统推荐用户
	int tagRecomUserNum = 0;
	JSONArray tagRecomUserArray = new JSONArray();
	// 根据微博内容的系统推荐用户
	int cxtRecomUserNum = 0;
	JSONArray ctxRecomUserArray = new JSONArray();

	// 亲密用户
	int intimateNum = 0;
	JSONObject intimateUsers = new JSONObject();
	// 用户最新发布微博列表id
	JSONObject userTimelineIds = null;
	// 用户发布的微博列表
	int needStatusNum = 300;// 需要获得微博数量
	StatusWapper status = null;
	// 按评论数量排序（降序）的微博列表
	PriorityQueue<Status> comQueue = null;
	// 按转发数量排序（降序）的微博列表
	PriorityQueue<Status> rtQueue = null;

	Users um = null;
	Friendships fm = null;
	Timeline tm = null;
	Comments cm = null;

	class UserCount {
		public User user = null;
		public int count = 0;
		public String name = null;
		public int rtCount = 0;
		public int comCount = 0;
		public int repliedCount = 0;
		public int rtedCount = 0;
		public int friendType = 0;// 0:undifined 1:biFriend 2:uniFriend
									// 3:stranger
		public int comedStatus = 0;
		public int temp = 0;
		public double score = 0;

		public UserCount(User user, int count) {
			this.user = user;
			this.count = count;
			this.name = user.getScreenName();
		}

		public UserCount(String name, int count) {
			this.name = name;
			this.count = count;
		}

		@Override
		public String toString() {
			return name + ":" + rtCount + ":" + comCount + ":" + repliedCount
					+ ":" + rtedCount;
		}

	}

	private void init() {
		um = new Users();
		um.client.setToken(access_token);
		fm = new Friendships();
		fm.client.setToken(access_token);
		tm = new Timeline();
		tm.client.setToken(access_token);
		cm = new Comments();
		cm.client.setToken(access_token);
	}
	/**
	 * 提供之一就行，另一个参数用null代替，不过如果都提供会默认等同于只用name，
	 * 
	 * @param id
	 * @param name
	 */
	public SingleUserAnalyse(String id, String name) {
		// json.put("name", name);
		if(name==null){
			this.id=id;
			;//getCenter by id
		}
		this.name=name;
		init();
	}

	public SingleUserAnalyse(String name) {
		// json.put("name", name);
		this.name=name;
		init();
	}

	/**
	 * find out if the user is existed by get user info form weibo-api
	 * @return the error attribute contain the problem occured.
	 */
	public JSONObject isExistByName(){
		
		JSONObject result=new JSONObject();
		try{
			result.put("name", this.name);
			this.getCentreUser();
		}catch (WeiboException e) {
			result.put("error", e.getError());
			e.printStackTrace();
		} catch (Exception e) {
			result.put("error", e.getMessage());
			e.printStackTrace();
		} finally {
			return result;
		}
	}
	// API 一次
	private User getCentreUser() throws Exception {

		if (this.name != null) {
			centreUser = um.showUserByScreenName(name);
		} else {
			centreUser = um.showUserById(this.id);
		}

		return centreUser;
	}

	// API 1+好友数/count 次
	public JSONObject getFriends() {

		int incaseNum = 0;
		int MAX = 5000;// 防止出错死循环不停地抓
		int count = 200;
		int cursor = 0;
		try {
			if (centreUser == null) {
				getCentreUser();
			}
			friendsJson=new JSONObject();
			friendsJson.put("id", centreUser.getId());
			friendsJson.put("name", centreUser.getScreenName());
			userId = centreUser.getId();
			friendsJson.put("head", centreUser.getProfileImageURL());
			// get all biFriend ids
			ArrayList biIds = fm.myGetFriendsBilateralIds(centreUser.getId());
			// the while loop intends to get all friend ids and get them tagged
			// with bi or uni

			ArrayList<User> biF = new ArrayList<User>();
			ArrayList<User> uniF = new ArrayList<User>();
			ArrayList<User> allF = new ArrayList<User>();
			while (true) {
				UserWapper users = fm.myGetFriendsByID(centreUser.getId(),
						count, cursor);
				for (User u : users.getUsers()) {
					incaseNum++;
					// System.out.println(u.getScreenName() + ":" +
					// u.isfollowMe());

					if (biIds.contains(u.getId())) {
						biF.add(u);
						biFriendNum++;
						JSONObject member = new JSONObject();
						member.put("name", u.getScreenName());
						member.put("id", u.getId());
						member.put("head", u.getProfileImageURL());
						biFriendArray.put(member);
					} else {
						uniF.add(u);
						uniFriendNum++;
						JSONObject member = new JSONObject();
						member.put("name", u.getScreenName());
						member.put("id", u.getId());
						member.put("head", u.getProfileImageURL()); //
						uniFriendArray.put(member);
					}
					//System.out.println(incaseNum + ":" + u.getScreenName());
				}
				cursor = (int) users.getNextCursor();
				if (cursor == 0 || incaseNum > MAX) {
					break;
				}
			}
			biFriends = new UserWapper(biF, 0, 0, 0, "");
			uniFriends = new UserWapper(uniF, 0, 0, 0, "");
			allF.addAll(biF);
			allF.addAll(uniF);
			friends = new UserWapper(allF, 0, 0, 0, "");
			friendsJson.put("biFriendNum", biFriendNum);
			friendsJson.put("uniFriendNum", uniFriendNum);
			friendsJson.put("biFriends", biFriendArray);
			friendsJson.put("uniFriends", uniFriendArray);

		} catch (WeiboException e) {
			friendsJson.put("error", e.getError());
			System.out.println("get weiboException+++++++++");
			e.printStackTrace();
			throw e;//be throwed out of the funciton,would't be caught by the follwing catch
					//eventhought WeiboException is subclass of Exception
		} catch (Exception e) {
			System.out.println("get Exception-------------");
			//e.getMessage() may return null,and e.toString() equals ObjectName:message
			friendsJson.put("error",e.toString());
			e.printStackTrace();		
			throw e;
		} finally {
		//	System.out.print("yesJson==========\n"+friendsJson.getString("error"));
			return friendsJson;
		}
	}

	// API 貌似一次count条，所以num/count，count在使用的函数中定义。
	private StatusWapper getStatus(int num) throws Exception {
		// get user timeline imfornation //get user timeline ids
		// userTimelineIds=tm.getUserTimelineIdsByUid(centreUser.getId());
		// get user timeline status 1700780034

		if (centreUser == null) {
			getCentreUser();
		}
		status = tm.myGetUserTimelineByUid(centreUser.getId(), num);

		return status;
	}

	// 该优先队列可以被多个公用。count分别是评论数和转发数。
	PriorityQueue<UserCount> SortedUserQueue = new PriorityQueue<UserCount>(1,
			new Comparator<UserCount>() {
				@Override
				public int compare(UserCount a, UserCount b) {// 一般是升序，小的在前。不过我写成了降序排列，只要改一下小于大于方向就好
					int aCount = a.count;
					int bCount = b.count;
					if (aCount < bCount) {
						return 1;
					} else if (aCount > bCount) {
						return -1;
					} else {
						return 0;
					}
				}
			});

	// 看topComStatusNum,是几就取多少条微博的评论信息，就消耗几次api
	// 前提函数：getCentreUsers-->getStatus
	/**
	 * @prerequisite function getCentre-->getStatus
	 * @author Edward
	 * @return
	 * @throws Exception
	 */
	private void getComAndReplyUser() throws Exception {

		if (this.status == null) {
			this.getStatus(needStatusNum);
		}

		// 将微博按评论次数排序
		comQueue = new PriorityQueue<Status>(1, new Comparator<Status>() {
			@Override
			public int compare(Status a, Status b) {// 一般是升序，小的在前。不过我写成了降序排列，只要改一下小于大于方向就好
				int aCount = a.getCommentsCount();
				int bCount = b.getCommentsCount();
				if (aCount < bCount) {
					return 1;
				} else if (aCount > bCount) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		for (Status s : status.getStatuses()) {
			if (s.getCommentsCount() > 0) {
				comQueue.add(s);
			}
		}

		Map<String, UserCount> comMap = new HashMap<String, UserCount>();
		Map<String, UserCount> replyMap = new HashMap<String, UserCount>();

		// 取前10条评论次数最多的微博，获得评论者列表
		for (int i = 0; i < topComStatusNum && (!comQueue.isEmpty()); i++) {
			Status s = comQueue.poll();
			System.out.println(i);
			System.out.println(s.getText());
			System.out.println(s.getCommentsCount());

			int count = 100;// 每页100条
			int page = 1;
			int needNum = 200;// 一共取多少条评论,如果总数不足，则全取
			int getNum = 0;

			while (getNum < needNum) {
				CommentWapper comment = cm.getCommentById(s.getId(),
						new Paging(page, count), 0);
				if (needNum > comment.getTotalNumber()) {
					needNum = (int) comment.getTotalNumber();
				}
				getNum += comment.getComments().size();
				page++;
				for (Comment c : comment.getComments()) {
					User u = c.getUser();
					// 得到评论者
					if (u == null) {
						continue;
					}
					if (comMap.containsKey(u.getId())) {
						UserCount exiUc = comMap.get(u.getId());
						exiUc.count += 1;
						if (exiUc.temp != i) {
							exiUc.comedStatus += 1;
							exiUc.temp = i;
						}
					} else {
						UserCount newUc = new UserCount(u, 1);
						newUc.temp = i;
						newUc.comedStatus = 1;
						comMap.put(u.getId(), newUc);
					}
					// 得到中心用户回复的评论者
					if (u.equals(centreUser)) {
						Comment reC = c.getReplycomment();
						if (reC != null) {
							User reU = reC.getUser();
							if (reU != null) {
								if (replyMap.containsKey(reU.getId())) {
									replyMap.get(reU.getId()).count++;
								} else {
									replyMap.put(reU.getId(), new UserCount(
											reU, 1));
								}
							}
						}
					} else {
						assert u.getId() == centreUser.getId()
								|| u.getScreenName() == centreUser
										.getScreenName() : "myMsg:wrong with user equals operation";
					}
				}
			}
		}
		// 去掉中心用户
		comMap.remove(centreUser.getId());
		// 得到评论者
		// 将评论者按评论次数排序
		SortedUserQueue.clear();// 先清空
		SortedUserQueue.addAll(comMap.values());

		// 取评论前20多的用户
		comUserNum = SortedUserQueue.size() > topRtUserNum ? topRtUserNum
				: SortedUserQueue.size();
		comUserList = new ArrayList<UserCount>();
		totalComCount = 0;
		for (int i = 0; i < comUserNum; i++) {
			UserCount uc = SortedUserQueue.poll();
			JSONObject member = new JSONObject();
			member.put("name", uc.user.getScreenName());
			member.put("id", uc.user.getId());
			member.put("comCount", uc.count);
			member.put("comedStatus", uc.comedStatus);
			totalComCount = uc.count + newStatusWeight * uc.comedStatus;
			comUserArray.put(member);
			uc.comCount = uc.count;
			comUserList.add(uc);
			// System.out.println(uc.user.getScreenName() + ":" + uc);
		}
		comUsers.put("comUserNum", comUserNum);
		comUsers.put("comUsers", comUserArray);

		// 去掉中心用户
		replyMap.remove(centreUser.getId());
		// 得到中心用户回复的评论者
		SortedUserQueue.clear();// 先清空
		SortedUserQueue.addAll(replyMap.values());
		repliedUserNum = SortedUserQueue.size() > topReUserNum ? topReUserNum
				: SortedUserQueue.size();
		repliedUserList = new ArrayList<UserCount>();
		totalRepliedCount = 0;
		for (int i = 0; i < repliedUserNum; i++) {
			UserCount uc = SortedUserQueue.poll();
			JSONObject member = new JSONObject();
			member.put("name", uc.user.getScreenName());
			member.put("id", uc.user.getId());
			member.put("repliedCount", uc.count);
			repliedUserArray.put(member);
			uc.repliedCount = uc.count;
			totalRepliedCount += uc.repliedCount;
			repliedUserList.add(uc);
		}
		repliedUsers.put("repliedUserNum", repliedUserNum);
		repliedUsers.put("repliedUsers", repliedUserArray);

	}

	// 看topRtStatusNum,是几就取多少条微博的转发信息，就消耗几次api
	/**
	 * 看topRtStatusNum,是几就取多少条微博的转发信息，就消耗几次api
	 * 
	 * @prerequisite function getCentre-->getStatus
	 * @author Edward
	 * @return
	 * @throws Exception
	 */
	public JSONObject getRtUser() throws Exception {

		try {
			if (this.status == null) {
				this.getStatus(needStatusNum);
			}
			rtQueue = new PriorityQueue<Status>(1, new Comparator<Status>() {
				@Override
				public int compare(Status a, Status b) {
					int aCount = a.getRepostsCount();
					int bCount = b.getRepostsCount();
					if (aCount < bCount) {
						return 1;
					} else if (aCount > bCount) {
						return -1;
					} else {
						return 0;
					}
				}
			});

			for (Status s : status.getStatuses()) {
				if (s.getRepostsCount() > 0) {
					rtQueue.add(s);
				}
			}

			Map<String, UserCount> map = new HashMap<String, UserCount>();

			// 取前10条转发次数最多的微博，获得转发者列表topRtStatusNum
			for (int i = 0; i < topRtStatusNum && (!rtQueue.isEmpty()); i++) {
				Status s = rtQueue.poll();
				System.out.println(i);
				System.out.println(s.getText());
				System.out.println(s.getRepostsCount());
				// 只取了第一页，前100条转发
				StatusWapper reStatus = tm.getRepostTimeline(s.getId(),
						new Paging(1, 100));
				for (Status res : reStatus.getStatuses()) {
					User u = res.getUser();
					if (map.containsKey(u.getId())) {
						map.get(u.getId()).count += 1;
					} else {
						map.put(u.getId(), new UserCount(u, 1));
					}
				}
			}
			// 去掉中心用户
			map.remove(centreUser.getId());
			// 将转发者按转发次数排序
			SortedUserQueue.clear();
			SortedUserQueue.addAll(map.values());
			// 取前转发20多的用户
			rtUserNum = SortedUserQueue.size() > topRtUserNum ? topRtUserNum
					: SortedUserQueue.size();
			// System.out.println("====="+rtUserNum);
			rtUserList = new ArrayList<UserCount>();
			totalRtCount = 0;
			for (int i = 0; i < rtUserNum; i++) {
				UserCount uc = SortedUserQueue.poll();
				// System.out.println(uc.user.getScreenName() + ":" + uc);
				JSONObject member = new JSONObject();
				member.put("name", uc.user.getScreenName());
				member.put("id", uc.user.getId());
				member.put("rtCount", uc.count);
				rtUserArray.put(member);
				uc.rtCount = uc.count;
				totalRtCount += uc.rtCount;
				rtUserList.add(uc);
			}
			rtUsers.put("rtUserNum", rtUserNum);
			rtUsers.put("rtUsers", rtUserArray);
		} catch (WeiboException e) {
			rtUsers.put("error", e.getError());
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			//e.getMessage() may return null,and e.toString() equals ObjectName:message
			rtUsers.put("error", e.toString());
			e.printStackTrace();
			throw e;
		} finally {
			return rtUsers;
		}
	}

	// 不消耗多余api，如果已有朋友列表和微博列表
	/**
	 * 不消耗多余api，如果已有朋友列表和微博列表
	 * 
	 * @prerequisite function getCentre-->(getStatus,getFriends)
	 * @author Edward
	 * @return
	 * @throws Exception
	 */
	public JSONObject getRtAuthors() throws Exception {

		try {
			if (this.biFriends == null) {
				this.getFriends();
			}

			if (this.status == null) {
				this.getStatus(needStatusNum);
			}
			
			Pattern namePatt = Pattern.compile("//@([^@:]*)(:|\\z)");
			Map<String, UserCount> map = new HashMap<String, UserCount>();
			for (Status s : status.getStatuses()) {
				Status reStatus = s.getRetweetedStatus();
				if (reStatus != null) {// 该微博为转发微博
					// author
					User author = reStatus.getUser();
					if (author != null) {// 原微博被删除时，原作者user字段会为null
						if (map.containsKey(author.getScreenName())) {
							map.get(author.getScreenName()).count += 1;
						} else {
							map.put(author.getScreenName(), new UserCount(
									author, 1));
						}
					}

					// other rt users
					String text = s.getText();
					Matcher mat = namePatt.matcher(text);
					while (mat.find()) {
						int start = mat.start(1);
						int end = mat.end(1);
						String name = text.substring(start, end);
						if (map.containsKey(name)) {
							map.get(name).count += 1;
						} else {
							map.put(name, new UserCount(name, 1));
						}
					}

				}
			}

			// 去掉中心用户
			map.remove(centreUser.getScreenName());
			SortedUserQueue.clear();
			SortedUserQueue.addAll(map.values());

			Map<String, User> myBiFriends = new HashMap<String, User>();
			for (User u : biFriends.getUsers()) {
				myBiFriends.put(u.getScreenName(), u);
			}
			int topRtedAuthorNum = 10;
			int threshold = 5;// 不是朋友，但是被中心用户转发前3多的用户。
			rtedAuthorNum = 0;
			rtAuthorList = new ArrayList<UserCount>();
			totalRtAuthorCount = 0;
			for (int i = 0; !SortedUserQueue.isEmpty(); i++) {
				if (rtedAuthorNum > topRtedAuthorNum) {
					break;
				}
				UserCount uc = SortedUserQueue.poll();
				JSONObject mem = new JSONObject();
				mem.put("name", uc.name);
				if (myBiFriends.containsKey(uc.name)) {
					User u = myBiFriends.get(uc.name);
					uc.user = u;
					mem.put("id", u.getId());
					mem.put("isBiFriend", true);
					mem.put("rtedCount", uc.count);
					uc.rtedCount = uc.count;
					totalRtAuthorCount += uc.rtedCount;
					uc.friendType = 1;
					rtAuthorList.add(uc);
					rtedAuthorNum += 1;
					rtAuthorArray.put(mem);
					// System.out.println("yes" + uc.name + ":" + uc.count);
				} else if (i < threshold && uc.user != null) {
					mem.put("id", uc.user.getId());
					mem.put("isBiFriend", false);
					mem.put("rtedCount", uc.count);
					uc.rtedCount = uc.count;
					totalRtAuthorCount += uc.rtedCount;
					uc.friendType = 0;
					rtAuthorList.add(uc);
					rtedAuthorNum += 1;
					rtAuthorArray.put(mem);
					// System.out.println("no" + uc.name + ":" + uc.count);
				} else {
					continue;
				}
				// System.out.println(uc.name+":"+uc.count);
			}
			rtAuthors.put("rtedAuthorNum", rtedAuthorNum);
			rtAuthors.put("rtAuthorArray", rtAuthorArray);
		} catch (WeiboException e) {
			rtAuthors.put("error", e.getError());
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			//e.getMessage() may return null,and e.toString() equals ObjectName:message
			rtAuthors.put("error", e.toString());
			e.printStackTrace();
			throw e;
		} finally {
			return rtAuthors;
		}

	}

	// 计算亲密度的时候，各部分所占比例
	double rtRatio = 0.25;
	double comRatio = 0.2;
	double repliedRatio = 0.25;
	double rtedRatio = 0.3;

	private int getIntimateScore(UserCount uc) {

		double score = uc.rtCount * rtRatio / totalRtCount
				+ (uc.comCount + uc.comedStatus * newStatusWeight) * comRatio
				/ totalComCount + uc.repliedCount * repliedRatio
				/ totalRepliedCount + uc.rtedCount * rtedRatio
				/ totalRtAuthorCount;
		int result = (int) (score * 1000);
		return result;

	}

	/**
	 * @prerequisite function getCentre-->(getStatus,getFriends)-->
	 *               (getComAndReplyUser,getRtUser,getRtAuthors)
	 * @author Edward
	 * @return
	 * @throws Exception
	 */
	public JSONObject getIntimateUsers() throws Exception {

		Map<String, UserCount> map = new HashMap<String, UserCount>();
		try {
			if (this.rtAuthorList == null) {
				this.getRtAuthors();
			}
			ArrayList<UserCount> all = new ArrayList<UserCount>();
			if (this.rtUserList == null) {
				this.getRtUser();
			}
			if (this.comUserList == null || this.repliedUserList == null) {
				this.getComAndReplyUser();
			}
			
			all.addAll(this.rtUserList);
			all.addAll(this.comUserList);
			all.addAll(this.rtAuthorList);
			all.addAll(this.repliedUserList);
			// assert all.addAll(rtUserList) : "wrong with geting rtUserList";
			// assert all.addAll(comUserList) : "wrong with geting comUserList";
			// assert all.addAll(rtAuthorList) :
			// "wrong with geting rtAuthorList";
			// assert all.addAll(repliedUserList) :
			// "wrong with geting repliedUserList";
			System.out.print("all size:" + all.size());

			int size = all.size();

			Map<String, User> myBiFriends = new HashMap<String, User>();
			for (User u : biFriends.getUsers()) {
				myBiFriends.put(u.getId(), u);
			}
			for (int i = 0; i < size; i++) {
				UserCount uc = all.get(i);
				assert uc.user != null : "in function getIntimateUsers: user is null";
				if (uc.user == null) {
					System.out.println(uc);
					continue;
				}
				if (map.containsKey(uc.user.getId())) {
					UserCount exUc = map.get(uc.user.getId());
					exUc.rtCount = uc.rtCount > 0 ? uc.rtCount : exUc.rtCount;
					exUc.comCount = uc.comCount > 0 ? uc.comCount
							: exUc.comCount;
					exUc.comedStatus = uc.comedStatus > 0 ? uc.comedStatus
							: exUc.comedStatus;
					exUc.repliedCount = uc.repliedCount > 0 ? uc.repliedCount
							: exUc.repliedCount;
					exUc.rtedCount = uc.rtedCount > 0 ? uc.rtedCount
							: exUc.rtedCount;
					// exUc.friendType = uc.friendType > 0 ? uc.friendType:
					// exUc.friendType;
					exUc.count = getIntimateScore(exUc);
				} else {
					if (myBiFriends.containsKey(uc.user.getId())) {
						uc.friendType = 1;
					}
					uc.count = getIntimateScore(uc);
					map.put(uc.user.getId(), uc);
				}
			}
			System.out.print("map size:" + map.size());
			SortedUserQueue.clear();
			SortedUserQueue.addAll(map.values());

			int checkRtCount = 0;
			int checkComCount = 0;
			int checkRepliedCount = 0;
			int checkRtAuthorCount = 0;
			/*
			 * int topNum = 30; int num = topNum > SortedUserQueue.size() ?
			 * SortedUserQueue.size() : topNum;
			 */
			int num = SortedUserQueue.size();
			System.out.print("queue size:" + map.size());
			assert num < 500 : "in getIntimateUsers:too many intimate Users(over 500)";
			JSONArray intiUsers = new JSONArray();
			for (int i = 0; i < num; i++) {
				UserCount uc = SortedUserQueue.poll();
				JSONObject mem = new JSONObject();
				mem.put("name", uc.user.getScreenName());
				mem.put("id", uc.user.getId());
				mem.put("rtCount", uc.rtCount);
				mem.put("comCount", uc.comCount);
				mem.put("comedStatus", uc.comedStatus);
				mem.put("repliedCount", uc.repliedCount);
				mem.put("rtedCount", uc.rtedCount);
				mem.put("friendType", uc.friendType);
				mem.put("score", uc.count);
				intiUsers.put(mem);
				checkRtCount += uc.rtCount;
				checkComCount += uc.comCount;
				checkRepliedCount += uc.repliedCount;
				checkRtAuthorCount += uc.rtedCount;

			}
			assert checkRtCount == totalRtCount : "in getIntimateUsers: wrong with totalRtCount";
			assert checkComCount == totalComCount : "in getIntimateUsers: wrong with totalComCount";
			assert checkRepliedCount == totalRepliedCount : "in getIntimateUsers: wrong with totalRepliedCount";
			assert checkRtAuthorCount == totalRtAuthorCount : "in getIntimateUsers: wrong with totalRtAuthorCount";
			intimateUsers.put("id", this.centreUser.getId());
			intimateUsers.put("name", this.centreUser.getScreenName());
			intimateUsers.put("num", num);
			intimateUsers.put("totalRtCount", totalRtCount);
			intimateUsers.put("totalComCount", totalComCount);
			intimateUsers.put("totalRepliedCount", totalRepliedCount);
			intimateUsers.put("totalRtAuthorCount", totalRtAuthorCount);
			intimateUsers.put("users", intiUsers);
		} catch (WeiboException e) {
			System.out.print("initimate++++++weibo\n");
			System.out.print("error:"+e.getError()+"toString:"+e.toString());
			intimateUsers.put("error", e.getError());
			e.printStackTrace();
		} catch (Exception e) {
			System.out.print("initimate++++++Exception");
			//e.getMessage() may return null,and e.toString() equals ObjectName:message
			intimateUsers.put("error", e.toString());
			e.printStackTrace();
		} finally {
			return intimateUsers;
		}

	}

	int num=0;
	public int getNum(){
		num++;
		return num;
	}
	// 方法之间有依赖关系，前3个方法(getCentreUser,getFriends,getStatus)必须先执行。
	public static void main(String[] args) {
		try {

			PrintWriter Pout = new PrintWriter(new FileWriter(
					"C:\\Users\\Edward\\Desktop\\test.txt"));
			SingleUserAnalyse ts = new SingleUserAnalyse("1796533527", "胡新辰点点点");
			ts.getCentreUser();// API一次 ts.getFriends();//API 1+好友数/count 次
		//	ts.getFriends();
		//	ts.getStatus(200);// API貌似一次100条，所以num/100
		//	ts.getComAndReplyUser();// 看topComStatusNum,是几就取多少条微博的评论信息 ，就消耗几次api
		//	ts.getRtUser();
		//	ts.getRtAuthors();
			// ts.json.put("rtUsers", ts.rtUsers);
			// ts.json.put("comUsers",ts.comUsers);
			// ts.json.put("rtAuthors", ts.rtAuthors);
			// ts.json.put("repliedUsers", ts.repliedUsers);
		//	JSONObject js = ts.getIntimateUsers();
		//	Pout.println(js.toString());
		//	Pout.close();

			// System.out.println("json object :" + json.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
