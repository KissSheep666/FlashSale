package com.kisssheep.flashsalebackend.common;

public class GlobalException extends RuntimeException {
    //比默认的异常多了一个数据 能传递枚举类携带的信息
    private final CodeMsg codeMsg;

    public GlobalException(CodeMsg codeMsg) {
        super(codeMsg.getMsg());
        this.codeMsg = codeMsg;
    }

    public CodeMsg getCodeMsg(){
        return this.codeMsg;
    }
}
