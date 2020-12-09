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
                        if (msg.safe) {
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
                                + "</br>"
                                + msg.requestInfo
                                + "<div class='alert alert-success lead'>La URL parece segura</div>");
                        } else {
                            $("#result").html(
                                "<div class='alert alert-success lead'><a target='_blank' href='"
                                + msg.uri
                                + "'>"
                                + msg.uri
                                + "</a></div>"
                                + "</br>"
                                + msg.requestInfo
                                + "<div class='alert alert-danger lead' >¡¡¡ LA URL NO ES SEGURA !!!</div>");
                        }
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
                    });
            });
    });