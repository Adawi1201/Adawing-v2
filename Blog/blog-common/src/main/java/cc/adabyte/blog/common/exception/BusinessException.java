package cc.adabyte.blog.common.exception;

/**
 * 业务异常。status 即 HTTP 状态码语义，由全局异常处理器原样写入响应；
 * 默认 400（客户端输入/业务规则不满足），服务端自身故障请显式传 500。
 */
public class BusinessException extends RuntimeException {
    private final int status;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }

    public static BusinessException payloadTooLarge(String message) {
        return new BusinessException(413, message);
    }

    public int getStatus() {
        return status;
    }
}
