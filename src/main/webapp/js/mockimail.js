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
		$.getJSON( "rest/mail/search",params).done(function( data ) {
			
			$('#resultats').hide();
			var tbody=$('#resultats tbody').empty();
			var trSample=$('<tr>');
			var tdSample=$('<td>');
			var today=new Date();
			for(var i=0;i<data.hits.hits.length;i++) {
				var hit=data.hits.hits[i];
				var smtpMessage=hit._source;
				var tr=trSample.clone();
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
				tr.append(tdSample.clone().html(datetime.getUTCHours()+':'+datetime.getUTCMinutes()+':'+datetime.getUTCSeconds()));
				tr.append(tdSample.clone().html(smtpMessage.subject));
				tr.append(tdSample.clone().html(smtpMessage.from));
				var toText="";
				for(var j=0;j<smtpMessage.to.length;j++){
					toText=toText+smtpMessage.to[j]+'<br/>';
				}
				tr.append(tdSample.clone().html(toText));
				tbody.append(tr);
			 }
			$('#resultats').show();
		});
	}

})(jQuery);