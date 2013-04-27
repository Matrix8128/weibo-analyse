	
	
	var EventHandler = function(info,workplace){
		
		information=$(info)
		
		var that={
			showUser:function(node){
				var name=node.data.screenName
				var ul=information.find("ul")
				ul.empty()
				ul.append("<li style=\"color:blue\">"+name+"</li>")
				//information.html("<font color='blue'>"+name+"</font>")
			},
			
		}
		
		return that
	}
