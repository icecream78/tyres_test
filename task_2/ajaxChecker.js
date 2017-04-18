"use strict";

function parseURLString(url) {
    if (url.length == 0) return

    var resourceURL = url.split("?")[0];
    var queryString = url.split("?")[1];

    // if (resourceURL === undefined || queryString === undefined) return
    if (resourceURL === undefined)
        return {"url": "", "query": ""}
    else if (queryString === undefined)
        return {"url": resourceURL, "query": ""}

    var resp_obj = {"url": resourceURL, "query": obj};
    var obj = {};

    queryString = queryString.split('#')[0];

    var arr = queryString.split('&');

    for (var i=0; i<arr.length; i++) {
      var a = arr[i].split('=');

      var paramNum = undefined;
      var paramName = a[0].replace(/\[\d*\]/, function(v) {
        paramNum = v.slice(1,-1);
        return '';
      });

      var paramValue = typeof(a[1])==='undefined' ? true : a[1];

      paramName = paramName.toLowerCase();
      paramValue = paramValue.toLowerCase();

      if (obj[paramName]) {
        if (typeof obj[paramName] === 'string') {
          obj[paramName] = [obj[paramName]];
        }
        if (typeof paramNum === 'undefined') {
          obj[paramName].push(paramValue);
        }
        else {
          obj[paramName][paramNum] = paramValue;
        }
      }
      else {
        obj[paramName] = paramValue;
      }
    }
    resp_obj["query"] = obj;
    return resp_obj;
}

function encodeToBase64(data) {
    if (data.length == 0) {
      return "";
    }
    var resultStr = ""
    for (var query in data) {
      resultStr += String(query) + ":" + data[query] + ";"
    }
    return btoa(resultStr);
}

function decodeFromBase64(encodedStr) {
    var data = atob(encodedStr).split(";");
    var resultStr = "";
    var query_obj = {};
    var splitted_data;
    for (var i = 0; i < data.length; i++) {
      splitted_data = data[i].split(":");
      if (splitted_data.length == 2) {
          query_obj[splitted_data[0]] = splitted_data[1];
      }
    }
    return query_obj;
}

function sendDataToServer(req_obj) {
    var data = JSON.stringify(req_obj);
    var headers = new Headers();
    headers.append("Content-Type", "text/plain");
    headers.append("Connection", "close");

    var initArgs  = {
                     method: "POST",
                     headers: headers,
                     mode: "cors",
                     cache: "default",
                     body: data
                    };
    var request = new Request("/service/front/statistics", initArgs);

    fetch(request)
        .then(function (response) {
          if (response.status < 400)
              console.log("Data was send");
          else
              console.log("Source is unreachable");
        })
        .catch(function (error) {
            console.log("Request was failed");
        })
}

XMLHttpRequest.prototype.realSend = XMLHttpRequest.prototype.send;
XMLHttpRequest.prototype.send = function (value) {
    this.addEventListener("progress", function () {
	// processing all request with response error status
        if (this.status > 400 ) {
            var req_obj = parseURLString(this.responseURL);
            req_obj["query"] = encodeToBase64(req_obj["query"]);
            sendDataToServer(req_obj);
        }
    }, false);
    this.realSend(value);
}
