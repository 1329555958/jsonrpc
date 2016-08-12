require("app").register.controller("RpcController", function ($scope, $timeout, $myhttp) {
    $scope.jsonParams = {};
    $scope.jsonResults = {};
    $scope.rpcs = {"rpcs": [{"/jsonrpc/com/wch/jsonrpc/server/UserService": {"findUserByIdAndName": ["", ""]}}]}.rpcs;
    $myhttp.get(APPROOT + "services", function (data) {
        rpcs = data.rpcs;
    });
    $scope.aceJson = function (obj) {
        return JSON.stringify(obj, null, '\t');
    };
    function result(key) {
        return function (result) {
            $scope.jsonResults[key] = $scope.aceJson(result.result);
            $scope.$apply();
        }
    }

    $scope.send = function (path, method) {
        var key = $scope.jsonParamsKey(path, method);
        var endpoint = APPROOT + path;
        var param = JSON.parse($scope.jsonParams[key]);
        var cb = result(key);
        $.jsonRPC.setup({
            endPoint: endpoint.replace(/\/+/g, '/').replace("/", '') //不要以/开头
        }).request(method, {
            params: param,
            success: cb,
            error: cb
        });
    };

    $scope.jsonParamsKey = function (path, method) {
        return path + "/" + method;
    };
    $scope.clearResult = function (path, method) {
        var key = $scope.jsonParamsKey(path, method);
        $scope.jsonResults[key] = "";
    }
});