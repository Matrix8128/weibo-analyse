	
	
	var EventHandler = function(info,workplace){
		
		information=$(info)
		
		var that={
			showUser:function(node){
				var ul=information.find("ul")
				ul.empty()
				
				if(node.data.hasOwnProperty("screenName")){
					ul.append("<li style=\"color:blue\"><span>"+node.data.screenName+"</span></li>")
				}if(node.data.hasOwnProperty("friendType")){
					type=node.data.friendType
					if(type==1){
						ul.append("<li>互粉用户</li>")
					}
					
				}if(node.data.hasOwnProperty("score")){
					ul.append("<li>亲密度得分：<span>"+node.data.score+"</span></li>")
				}if(node.data.hasOwnProperty("comedStatus")){
					ul.append("<li>评论了中心用户<span>"+node.data.comedStatus+"</span>条微博</li>")
				}if(node.data.hasOwnProperty("comCount")){
					ul.append("<li>共计<span>"+node.data.comCount+"</span>条评论</li>")
				}if(node.data.hasOwnProperty("repliedCount")){
					ul.append("<li>被中心用户回复<span>"+node.data.repliedCount+"</span>条</li>")
				}if(node.data.hasOwnProperty("rtCount")){
					ul.append("<li>转发中心用户<span>"+node.data.rtCount+"</span>条微博</li>")
				}if(node.data.hasOwnProperty("rtedCount")){
					ul.append("<li>被中心用户转发了<span>"+node.data.rtedCount+"</span>条微博</li>")
				}
				
				//information.html("<font color='blue'>"+name+"</font>")
			},
			
		}
		
		return that
	}
