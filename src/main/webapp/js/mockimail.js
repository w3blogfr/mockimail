(function($){
	
	$(document).ready(function(){
		search();
	});
	
	$('#searchform').submit(function(){
		search();
	
		return false;
	});
	
	function search(){
		var params={};
		if($('#queryinput').val()!=''){
			params.query=$('#queryinput').val();
		}
		if(jQuery('#timeinput').val()!='all'){
			params.timeDiff=jQuery('#timeinput').val();
		}
		$.ajax({
			type:"GET",
			url:"api/search",
		    contentType: "application/json; charset=utf-8",
		    dataType: "json",
		    data:params
		   }).done(function( data ) {
			
			$('#resultats').hide();
			var tbody=$('#resultats tbody').empty();
			var trSample=$('<tr>');
			var tdSample=$('<td>');
			var tdSampleBody=$('<td>').attr('colspan','5');
			var today=new Date();
			for(var i=0;i<data.hits.hits.length;i++) {
				var hit=data.hits.hits[i];
				var smtpMessage=hit._source;
				var tr=trSample.clone().addClass('subject');
				tr.click(function(trClosure,smtpMessageClosure){
					return function(){
						if($(this).hasClass('open')){
							//On ferme le body
							$(this).next('.message-body').hide();
							$(this).removeClass('open');
						}else{
							//On ajoute le body
							$(this).next('.message-body').show();
							$(this).addClass('open');
						}
						
					}
				}(tr,smtpMessage));
				var datetime=new Date(smtpMessage.date);
				var dateText="";
				if(today.getFullYear()==datetime.getFullYear() 
						&& today.getMonth()==datetime.getMonth() 
							&& today.getDate()==datetime.getDate() ){
					dateText="Today";
				}else{
					dateText=datetime.getFullYear()+'-'+(parseInt(datetime.getMonth())+1)+'-'+datetime.getDate()
				}
				tr.append(tdSample.clone().html(dateText));
				tr.append(tdSample.clone().html(datetime.getHours()+':'+datetime.getUTCMinutes()+':'+datetime.getUTCSeconds()));
				tr.append(tdSample.clone().html(smtpMessage.subject));
				tr.append(tdSample.clone().html(smtpMessage.from));
				//Destinataires
				var toText="";
				if(smtpMessage.to){
					for(var j=0;j<smtpMessage.to.length;j++){
						toText=toText+smtpMessage.to[j]+'<br/>';
					}
				}
				if(smtpMessage.cc){
					for(var j=0;j<smtpMessage.cc.length;j++){
						toText=toText+"CC :"+smtpMessage.cc[j]+'<br/>';
					}
				}
				if(smtpMessage.bcc){
					for(var j=0;j<smtpMessage.bcc.length;j++){
						toText=toText+"Bcc :"+smtpMessage.bcc[j]+'<br/>';
					}
				}
				tr.append(tdSample.clone().html(toText));
				tbody.append(tr);
				
				//On ajoute une ligne pour le body
				var trBody=trSample.clone().addClass('message-body');
				var bodyMessage="";
				if(smtpMessage.parts){
					for(var j=0;j<smtpMessage.parts.length;j++){
						var partMessage=smtpMessage.parts[j];
						if(partMessage.contentType.indexOf("text/")>=0){
							bodyMessage=bodyMessage+partMessage.body;
							bodyMessage=bodyMessage+'<hr/>';
						}else {
							//Attached file
							bodyMessage=bodyMessage+"<br/>Attached File : "+partMessage.fileName;
						}
					}
				}else{
					//TODO to remove
					bodyMessage=bodyMessage+smtpMessage.body;
				}
				
				trBody.append(tdSampleBody.clone().html(bodyMessage));
				
				tbody.append(trBody);
			 }
			$('#resultats').show();
		});
	}

})(jQuery);