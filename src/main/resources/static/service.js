$(function () {
        var Path, Method, Params;
        //https://github.com/datagraph/jquery-jsonrpc
        $.get("/services", function (data) {
            console.log(data);
            _.each(data.rpcs, function (service) {
                _.each(service, function (klass, path) {
                    Path = path;
                    _.each(klass, function (params, method) {
                        Method = method;
                        Params = params;
                    })
                })
            });
            $("#path").text(Path);
            $("#method").text(Method);
            $("#params").val(JSON.stringify(Params, "    "));
        });

        $("#send").click(function () {
            $.jsonRPC.setup({
                endPoint: Path
            }).request('findUserByIdAndName', {
                params: JSON.parse($("#params").val()),
                success: function (result) {
                    $("#result").text(JSON.stringify(result, null, 4));
                },
                error: function (result) {
                    $("#result").text(JSON.stringify(result, null, 4));
                }
            });
        });
    }
);
