require("app").register.controller("RpcController", function ($scope, $timeout, $myhttp) {
    $scope.rpcs = {"rpcs": [{"/jsonrpc/com/wch/jsonrpc/server/UserService": {"findUserByIdAndName": ["", ""]}}]}.rpcs;
    // $myhttp.get(APPROOT + "services", function (data) {
    //     rpcs = data.rpcs;
    // });
});