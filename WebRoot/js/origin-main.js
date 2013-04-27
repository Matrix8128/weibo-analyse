
	var canvas, context;
	$(document).ready(function() {//画布准备，调整位置和大小
		canvas = $("#myCanvas");
		context = canvas.get(0).getContext("2d");
		width = $(window).get(0).innerWidth;
		height = $(window).get(0).innerHeight;
		canvas.attr("width", width);
		canvas.attr("height", height);
		var x = 0;
		var y = $("#top").height();
		context.translate(x, y);
	});

	function submitUser() {
		
		if ($('#username').val().length == 0) {
			alert("can't be blank");
		} else {
			//alert($('#username').val());
			$.ajax({
				type : "get",//使用post方法访问后台
				dataType : "json",//返回json格式的数据
				url : "service.jsp",//要访问的后台地址
				contentType : "application/json;charset=utf-8",
				data : {
					username : $('#username').val()
				},//要发送的数据
				beforeSend : function() {
					$("#status").html(
							"<font color='red'>ajax数据处理中,请稍后...</font>");
				},

				complete : function() {
					$("#status").html("<font color='red'>ajax数据处理完毕</font>");
				},//AJAX请求完成时
				success : function(data) {//data为返回的数据，在这里做数据绑定
					
					draw(data);

				},
				error : function(XMLResponse) {
					alert(XMLResponse.responseText)
				}
			});

		}
	}

	function draw(data) {
		
		var inf = "centre:\n";
		inf += data.centre.uid + ":" + data.centre.name;
		inf += "\nothers:\n\n";
		
		$.each(data.jsonArray, function(index,item) {
			inf +="uid:" +item.uid + "\n";
			inf +="dist:" +item.distance + "\n";
		});
		alert(inf);
		context.fillStyle = "rgb(0,255,0)";
		context.fillRect(0, 0, 100, 100);
	}
