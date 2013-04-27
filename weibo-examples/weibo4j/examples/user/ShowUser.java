package weibo4j.examples.user;

import weibo4j.Users;
import weibo4j.examples.oauth2.Log;
import weibo4j.model.User;
import weibo4j.model.WeiboException;

public class ShowUser {

	public static void main(String[] args) {
		//String access_token = args[0];
		String access_token = "2.00N8EaxB08LsGa4ebcc4e9ac0YYqxw";
		//String uid =args[1];
		String uid="1796533527";
		Users um = new Users();
		um.client.setToken(access_token);
		try {
			User user = um.showUserById(uid);
			user.getProfileImageUrl();
			user.getProfileImageUrl();
			Log.logInfo(user.toString());
		} catch (WeiboException e) {
			e.printStackTrace();
		}
	}

}
