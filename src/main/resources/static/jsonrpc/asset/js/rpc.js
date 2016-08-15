require("app").register.controller("RpcController", function ($scope, $timeout, $myhttp) {
    $scope.jsonParams = {};
    $scope.jsonResults = {};
    $scope.hasRpcs = true;
    $scope.rpcsLoading = true;
    $scope.loading = {};
    // $scope.rpcs = {
    //     "rpcs": [{
    //         "/jsonrpc/com/wch/jsonrpc/server/UserService": {
    //             "findUserByIdAndName": [
    //                 {"id": ""},
    //                 {
    //                     "name": ""
    //                 }
    //             ]
    //         }
    //     }]
    // }.rpcs;
    $myhttp('rpcsLoading', $scope).get("jsonrpc", function (data) {
        $scope.rpcs = data.rpcs;
        if (!$scope.rpcs || !$scope.rpcs.length) {
            $scope.hasRpcs = false;
        }
    });
    $scope.aceJson = function (obj) {
        return JSON.stringify(obj, null, '\t');
    };
    function result(key) {
        return function (result) {
            $scope.jsonResults[key] = $scope.aceJson(result.result);
            $scope.loading[key] = false;
            $scope.$apply();
        }
    }

    $scope.send = function (path, method) {
        var key = $scope.jsonParamsKey(path, method);
        var endpoint = path;
        try {
            var paramArr = JSON.parse($scope.jsonParams[key]);
        } catch (e) {
            alert("请求参数必须为正确的JSON格式!");
            return;
        }

        var params = [];
        $scope.loading[key] = true;
        _.each(paramArr, function (param) {
            _.each(param, function (val, key) {
                params.push(val);
            })
        });
        var cb = result(key);
        $.jsonRPC.setup({
            endPoint: endpoint.replace(/\/+/g, '/').replace("/", '') //不要以/开头
        }).request(method, {
            params: params,
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