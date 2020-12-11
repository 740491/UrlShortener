$(document).ready(
    function () {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: "/link",
                    data: $(this).serialize(),
                    success: function (msg) {
                        console.log($("#qrCheck"))
                        var checkedValue = document.getElementById("qrCheck").checked;
                        console.info(msg)

                        if(checkedValue === true){

                            fetch(msg.qr).then(response => response.json())
                                .then(data =>{
                                    console.log("JSON: " + data.qr)
                                    $('.qr-code').attr('src', "data:image/jpg;base64, " + data.qr);
                                    }

                                )
                            //$("#imagen").html(
                                //"<img id=\"profileImage\" src=\"data:image/jpg;base64," + resultsQr.qr +"\">"
                            //)


                        }
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + msg.uri
                            + "'>"
                            + msg.uri
                            + "</a></div>"
                            + "</br>");

                        // Generate the link that would be
                        // used to generate the QR Code
                        // with the given data
                        //let finalURL = 'https://chart.googleapis.com/chart?cht=qr&chl=' + msg.uri +
                        //    '&chs=160x160&chld=L|0'

                        // Replace the src of the image with
                        // the QR code image
                        //$('.qr-code').attr('src', finalURL);

                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });

        $("#csvFile").submit(
            function (event) {
                event.preventDefault();
                //Escalabilidad 15 puntos (WebSockets):

                var number;
                var socket = new SockJS('/csvfile');
                var output = "";
                stompClient = Stomp.over(socket);
                stompClient.connect({}, function(frame) {
                var reader = new FileReader();
                var file = document.getElementById('file').files[0]
                reader.readAsArrayBuffer(file);
                reader.onloadend = function (evt) {
                    // Get the Array Buffer
                    var data = evt.target.result;
                    var ui8a = new Uint8Array(data, 0);
                    // Grab our byte length
                    data = String.fromCharCode.apply(null,ui8a);
                    number = data.split(/\r\n|\r|\n/).length;
                    var rows = data.split('\n');
                    console.log("DATA:");
                    for(var i=0; i<number; i++){
                        console.log(rows);
                        stompClient.send("/app/csvfile", {},
                                           JSON.stringify(rows[i]));
                    }
                 }
                stompClient.subscribe('/csvmessages/messages', function(messageOutput) {
                output += messageOutput.body;
                number--;
                if(number == 0){
                    stompClient.disconnect();

                    var blob = new Blob([output], { type: 'application/csv' });
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.style.display = 'none';
                    a.href = url;
                    // the filename you want
                    a.download = 'shortenedURLs.csv';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);

                }
            });
        });



            /*
                //Escalabilidad 10 puntos (XHR Streaming):
                var xhr = new XMLHttpRequest();
                xhr.open('POST', '/csvFile', false);
                xhr.seenBytes = 0;
                xhr.onreadystatechange = function() {
                  if(xhr.readyState > 2) {
                    var newData = xhr.responseText.substr(xhr.seenBytes);
                    // process newData
                    xhr.seenBytes = xhr.responseText.length;
                    var blob = new Blob([newData], { type: 'application/csv' });
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.style.display = 'none';
                    a.href = url;
                    // the filename you want
                    a.download = 'shortenedURLs.csv';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                  }
                };
                xhr.send(new FormData(this));
            */
                /*
                //Cumplimiento:
                $.ajax({
                        url: "/csvFile",
                        type: "POST",
                        data: new FormData(this),
                        enctype: 'multipart/form-data',
                        processData: false,
                        contentType: false,
                        cache: false,
                        success: function (res) {
                            console.info(res);
                            var blob = new Blob([res], { type: 'application/csv' });
                            const url = window.URL.createObjectURL(blob);
                            const a = document.createElement('a');
                            a.style.display = 'none';
                            a.href = url;
                            // the filename you want
                            a.download = 'shortenedURLs.csv';
                            document.body.appendChild(a);
                            a.click();
                            window.URL.revokeObjectURL(url);
                        },
                        error: function (err) {
                            $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                        }
                        */
                    //});
            });
    });