
// 创建改对象----传入一个标识
function testWeb(url){
    this.baseURL=url;
}

testWeb.prototype.baseURL="";

testWeb.prototype.testFuntion=function () {
    alert("进来了");
}