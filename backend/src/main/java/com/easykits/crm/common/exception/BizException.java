package com.easykits.crm.common.exception;

import com.easykits.crm.common.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常。由 GlobalExceptionHandler 统一转换为 Result。
 * <p>约定：业务异常返回 HTTP 200 + 业务码，前端按 code 做提示。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.PARAM_ERROR.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public static BizException of(ResultCode resultCode) {
        return new BizException(resultCode);
    }

    public static BizException of(String message) {
        return new BizException(message);
    }
}
