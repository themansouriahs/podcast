if (window.XMLHttpRequest)
	var syncQuery = new XMLHttpRequest();
else
	var syncQuery = new ActiveXObject('MSXML2.XMLHTTP.3.0');	// for ancient IE.

function checkForChange() {					// before probing for any page change,
	if (document.hidden) {					// let's see whether the page is visible?
		setTimeout(checkForChange, 5000);	// the user is not viewing the page,
		return;								// so check again in 5 seconds
	}
	syncQuery.open( 'GET', '/auth' );	// the page is visible, so let's check for any update
	syncQuery.onreadystatechange = function() {
		if ( syncQuery.readyState == 4 ) {
			if ( syncQuery.status == 200 ) {
				newSync = syncQuery.responseText;
				document.location.href = "/player.jsp"
				//if (typeof lastSync !== 'undefined' && lastSync != newSync)
				//	document.location.href = document.location.pathname.substring(location.pathname.lastIndexOf("/") + 1);
				//else
				//	lastSync = newSync;
			}
			setTimeout(checkForChange, 500); // after every query, successful or not, retrigger after 500msc.
		}
	}
	syncQuery.send(); // initiate the query to the 'sync.txt' object.
}

checkForChange();	// this launches the first instance of "checkForChange"
					// which then self-retriggers periodically to recheck.