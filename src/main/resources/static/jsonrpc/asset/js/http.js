require("app").register.controller("HttpController", function ($scope, $timeout, $myhttp) {
    $scope.jsonParams = {};
    $scope.jsonResults = {};
    // http 请求方法
    $scope.httpMethod = {};
    $scope.hasHttps = true;
    $scope.httpsLoading = true;
    $scope.loading = {};
    // $scope.https = {
    //     "com.wch.controller.Demo": [{
    //         "com.wch.controller.Demo.demo(java.lang.String,int)": {
    //             params: {
    //                 "name": "",
    //                 "i": 0
    //             },
    //             urls: ["/demo/{name}"]
    //         }
    //     }]
    // };
    $myhttp('httpsLoading', $scope).get("jsonrpc", function (data) {
        $scope.https = data.https;
        if (!$scope.https) {
            $scope.hasHttps = false;
        }
    });
    $scope.aceJson = function (obj) {
        return JSON.stringify(obj, null, '\t');
    };

    _.templateSettings.interpolate = /{([\s\S]+?)}/g;

    $scope.send = function (name, params, useRequestBody) {
        var key = $scope.jsonParamsKey(name, params);
        try {
            var param = JSON.parse($scope.jsonParams[key]);
        } catch (e) {
            alert("请求参数必须为正确的JSON格式!");
            return;
        }
        var url = _.template(params.urls[0])(param);
        var method = $scope.httpMethod[key];
        if (method === 'GET') {
            $myhttp(key, $scope.loading).get(url, param, function (data) {
                $scope.jsonResults[key] = $scope.aceJson(data);
            });
        } else {
            var p = {};
            _.each(param, function (v, k) {
                if (_.isObject(v)) {
                    _.extend(p, v);
                } else {
                    p[k] = v;
                }
            });
            if (useRequestBody) {
                window.CONTENT_TYPE = 'application/json;charset=UTF-8';
                p = JSON.stringify(p);
            }


            $myhttp(key, $scope.loading).post(url, p, function (data) {
                try {
                    data = JSON.parse(data);
                } catch (e) {
                }
                $scope.jsonResults[key] = $scope.aceJson(data);
            });
        }
    };

    $scope.jsonParamsKey = function (name, params) {
        return name + "-" + params.urls[0];
    };
    $scope.clearResult = function (name, params) {
        var key = $scope.jsonParamsKey(name, params);
        $scope.jsonResults[key] = "";
    }
});