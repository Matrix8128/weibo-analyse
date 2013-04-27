 var DataHandler=function(){		
		var self=this
		self.centreUsers=[]
			
		
		this.errorHandler=function(data) {
			if (data.hasOwnProperty("error")) {
				alert(data.error)
				return false
			}
			return true
		},
		this.getUser=function(name,Levels,Types,successCallback){
			
			$
					.ajax({
						type : "get",
						dataType : "json",
						url : "service.jsp",
						contentType : "application/json;charset=utf-8",
						data : {
							username : name,
							levels : Levels,
							types : Types,
						},
						beforeSend : function() {
							$("#ajaxtip").html(
									"<font color='red'>ajax数据处理中,请稍后...</font>");
						},

						complete : function() {
							$("#ajaxtip").html(
									"<font color='red'>ajax数据处理完毕</font>");
						},
						success : function(data) {
							
							if (self.errorHandler(data)) {
								successCallback(data)//必须用self，不能用this
							}
						},
						error : function(XMLResponse) {
							alert("ajax error:tips(mybe because the returned " +
									"dataType isnot the required type ")
							alert(XMLResponse.responseText)
						}
					});
			
		}
		this.usersManger=function(userName){
			var result=null
			self.getUser(username, 0,'anything',successCallback)
			
		}
		return this
	}
