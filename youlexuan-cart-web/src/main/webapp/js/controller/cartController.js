 //控制层 
app.controller('cartController' ,function($scope,$controller   ,cartService){
	
	$controller('baseController',{$scope:$scope});//继承


	$scope.findCartList = function () {
		cartService.findCartList().success(
			function (response) {
				$scope.cartList = response;
				$scope.totalValue = cartService.sum($scope.cartList);
			}
		)
    }

	$scope.addGoodsToCart = function (itemId,num) {
		cartService.addGoodsToCart(itemId,num).success(
			function (response) {
				if (response.success){
					$scope.findCartList();//刷新列表页
				}else{
					alert(response.message);
				}
			}
		)
	}

	$scope.findAddressList = function () {
		cartService.findAddressList().success(
			function (response) {

					$scope.addressList =  response;//刷新列表页
				for (var i = 0;i<$scope.addressList.length;i++){
					if ($scope.addressList[i].isDefault=='1'){
						$scope.Address = $scope.addressList[i];
						break;
					}
				}

			}
		)
	}

	$scope.selectAddress = function (addr) {
		$scope.Address = addr;

	}

	$scope.isSelectedAddress = function (addr) {
		if (addr==$scope.Address) {
			return true;
		}else{
			return false;
		}

	}
	$scope.order={'paymentType':'1'};
	$scope.selectPayType = function (type) {
		/*alert(type+"--");*/
		$scope.order.paymentType=type;


	}


    $scope.submitOrder = function () {
        $scope.order.receiverAreaName= $scope.Address.address;
        $scope.order.receiverMobile = $scope.Address.mobile;
        $scope.order.receiver = $scope.Address.contact;
      /*  alert($scope.order.paymentType+"pppp");*/

        cartService.submitOrder($scope.order).success(
            function (response) {
                if (response.success){
                    if ($scope.order.paymentType=='1'){
                        window.location.href="pay.html";
                    } else{
                        alert("订单提交成功");
                    }
                } else{
                    alert(response.message);
                }
            }
        )
    }
});	