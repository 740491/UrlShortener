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
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });