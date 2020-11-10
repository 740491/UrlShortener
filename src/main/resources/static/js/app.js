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
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + msg.uri
                            + "'>"
                            + msg.uri
                            + "</a></div>"
                            + "</br>"
                            + msg.requestInfo
                    );
                        // Generate the link that would be
                        // used to generate the QR Code
                        // with the given data
                        let finalURL = 'https://chart.googleapis.com/chart?cht=qr&chl=' + msg.uri +
                            '&chs=160x160&chld=L|0'

                        // Replace the src of the image with
                        // the QR code image
                        $('.qr-code').attr('src', finalURL);
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });