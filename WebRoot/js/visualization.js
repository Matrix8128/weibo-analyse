//
//  main.js
//
//  A project template for using arbor.js
//

(function(){
Visualization = function(info,workplace, canvasId) {
	
	var canvas = $(canvasId).get(0)
	var dataHandler = new DataHandler()
	var sys = arbor.ParticleSystem(1000, 600, 0.5) // create the system with
													// sensible
													// repulsion/stiffness/friction
	sys.parameters({
		gravity : false
	}) // use center-gravity to make the graph settle nicely (ymmv)
	sys.parameters({stiffness:1000, repulsion:500, gravity:true, dt:0.015})
	//sys.parameters({stiffness:4000,repulsion:200,Friction:9,graviey:true,fps:30,dt:.035,precision:.6})

	var eventHandler=EventHandler(info,workplace)
	sys.renderer = Renderer(canvasId,eventHandler) // our newly created renderer will have
									// its .init() method called shortly by
										// sys...

	var that = {
		
		init : function() {
		
			$(window).resize(that.resize)

			canvas.width =  $(workplace).width()
			canvas.height = $(workplace).height()
			sys.screenSize(canvas.width, canvas.height)
			//sys.renderer.redraw() //��һ�β�����sys.renderer.redraw()������ʵ�����������ѭ��һֱredraw��ȥ
			//$("#search").bind("click",that.getData())//为什么不能用这句而一定要用下面那句？
			$("#submitButton").bind("click",function(){
				that.getData()
			})
			
		},
		getData:function(){
			//alert("in")
			if ($('#username').val().length == 0) {
				alert("can't be blank");
			} else {
				username = $('#username').val()
				type = $("#type").val()
	 			dataHandler.getUser(username,type,function(data){
					that.handleData(data)
					that.resize()
				}) 
			}
		},
		resize : function() {
			var w = $(workplace).width()
			var h = $(workplace).height()
			canvas.width = w
			canvas.height = h
			sys.screenSize(w, h)
			sys.renderer.redraw()
		},
		
		handleData	:	function(data){
			
			var result={nodes:{},edges:{}}
			
			sys.merge(result)//clear
			//forTest=data	
			if(data.dataType=="relation"){
				result=that.handleRelationData(data)
			}else if(data.dataType=="intimacy"){
				result=that.handleIntimacyData(data)
			}else if(data.dataType=="keywords"){
				result=that.handleKeyWordsData(data)
			}else{
				alert("wrong dataType:"+data.dataType)
				return
			}
			
			sys.merge(result)
		},
		handleRelationData: function(data){
			sys.parameters({stiffness:2000, repulsion:6000, gravity:true, dt:0.015,friction:0.3})
			var nodesData={}
			var edgesData={}
			nodesData[data.id]={mass:50,fixed:true,color:"red",dataType:data.dataType,type:"centre",screenName:data.name
								,head:data.head}
			edgesData[data.id]={}
			$.each(data.biFriends,function(index,user){
				nodesData[user.id]={mass:20,color:"green",type:"bi",alpha:1,screenName:user.name
									,head:user.head}
				edgesData[data.id][user.id]={length:.5}
			})
			/*$.each(data.uniFriends,function(index,user){
				nodesData[user.id]={mass:20,color:"blue",type:"uni",alpha:1,screenName:user.name
									,head:user.head}
				edgesData[data.id][user.id]={}
			})*/
			return {nodes:nodesData,edges:edgesData}
		},
		handleIntimacyData: function(data){
			
			sys.parameters({stiffness:2000, repulsion:5000, gravity:true, dt:0.015,friction:0.3})
			nodesData={}
			edgesData={}
			nodesData[data.id]={mass:70,fixed:true,color:"red",type:"centre",screenName:data.name}
			edgesData[data.id]={}
			
			showNum=50;
			maxScore=data.users[0].score
			var step=1.15
			var begin=1
			var newArray=[]
			var totalLevel=50
			for(i=0;begin<50;i++){
				begin*=step
				newArray.push(begin)
			}
			
			$.each(data.users,function(index,user){
				if(index>showNum)return
				distance=(1/user.score)*maxScore	

				for(i=0;i<newArray.length;i++){
					if(distance<newArray[i]||i==newArray.length-1){
						distance=i+1
						break
					}
				}
				//user mass can't be too small, or they will stick together in the begining
				nodesData[user.id]={mass:5,color:"green",type:"intimacy",dataType:data.dataType,screenName:user.name,
									friendType:user.friendType,score:user.score,
									repliedCount:user.repliedCount,comedStatus:user.comedStatus,
									rtedCount:user.rtedCount,rtCount:user.rtCount,comCount:user.comCount
									}
				edgesData[data.id][user.id]={length:distance}
			})

			return {nodes:nodesData,edges:edgesData}
		},
		
		
		handleKeyWordsData: function(data){
			sys.parameters({stiffness:200, repulsion:10000, gravity:true, dt:0.015,friction:0.3})
			var nodesData={}
			var edgesData={}
			var max=data.maxFreq
			
			var time=max/80
			
			$.each(data.WordList,function(index,array){
				//nodesData[index]={mass:1,color:"red",dataType:data.dataType,type:"centre",text:index}
				//edgesData[index]={};
				
				$.each(array,function(i,word){
					nodesData[word.text]={mass:10,color:"green",dataType:data.dataType,type:"keyWord",weight:word.freq/time,text:word.text}
					
					/*edgesData[word.text]={}
					$.each(words,function(j,text){
						edgesData[text][word.text]={alpha:.1}
					})
					words.push(word.text);*/
					
				})
			})
			
			return {nodes:nodesData,edges:edgesData}
		},
		
		
	}
	return that.init()
}

	$(document).ready(function(){
		var vi = Visualization("#info", "#workplace", "#viewport");
	})

})()