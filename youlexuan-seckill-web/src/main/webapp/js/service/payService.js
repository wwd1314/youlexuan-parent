app.service('payService',function($http){
    //本地支付
    this.createNative=function(){
        return $http.get('../pay/creatNative.do');
    }


    this.queryPayStatus=function(out_trade_no){
        return $http.get('../pay/queryPayStatu.do?out_trade_no='+out_trade_no);
    }
});