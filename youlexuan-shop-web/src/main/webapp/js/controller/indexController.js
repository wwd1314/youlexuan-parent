app.controller("indexController",function ($scope,$controller,loginService) {
    $controller('baseController',{$scope:$scope});
    $scope.getLoginName=function () {

        loginService.getLoginName().success(
            function (response) {
                $scope.username = response.username;
                $scope.lastLoginTime = response.lastLoginTime;
            }
        )
    }
})