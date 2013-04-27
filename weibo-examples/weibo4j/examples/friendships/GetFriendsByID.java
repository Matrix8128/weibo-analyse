package weibo4j.examples.friendships;

import weibo4j.Friendships;
import weibo4j.model.User;
import weibo4j.model.UserWapper;
import weibo4j.model.WeiboException;
import java.util.Iterator;
import java.util.List;

public class GetFriendsByID {

	public static void main(String[] args) {
		String access_token = "2.00N8EaxB08LsGa4ebcc4e9ac0YYqxw";
		String id = "1796533527";
		Friendships fm = new Friendships();
		fm.client.setToken(access_token);
		try {
			
			int cursor=0;
			int count=50;
			int num=0;
			while(true){
				UserWapper users = fm.myGetFriendsByID(id,count,cursor);
				System.out.println("+++++++"+users.getUsers().size());
				for(User u : users.getUsers()){
					num++;
					System.out.println(num+":"+u.getScreenName()+":"+u.getId());
				}
				cursor=(int)users.getNextCursor();
				System.out.println(num+":"+cursor+"==============");
				System.out.println(users.getNextCursor());
				System.out.println(users.getPreviousCursor());
				System.out.println(users.getTotalNumber());
				if(users.getNextCursor()==0)
					break;
			}
			


		} catch (WeiboException e) {
			e.printStackTrace();
		}

	}

}
