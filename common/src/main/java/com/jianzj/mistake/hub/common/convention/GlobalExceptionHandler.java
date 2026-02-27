package com.jianzj.mistake.hub.common.convention;

import com.jianzj.mistake.hub.common.convention.exception.ApiAuthorizeException;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import com.jianzj.mistake.hub.common.convention.result.BaseResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * <p>
 * 全局异常处理器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-20
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理非业务异常异常，报 500
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResult<Object> handleException(Exception e) {
        if (e != null && e.getMessage() != null) {
            log.error("handleException error: {}", e.getMessage());
        } else {
            log.error("handleException error: ", e);
        }

        return BaseResult.error(-1, "系统内部错误：" + e.getMessage(), "System inner error: " + e.getMessage());
    }

    /**
     * 处理参数校验异常 validator，比如 @NotBlank
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResult<Object> handleValidationException(ConstraintViolationException e) {
        log.error("handleValidationException error: {}", e.getMessage());
        return BaseResult.error(400, "参数校验失败：" + e.getMessage(), "Invalid params: " + e.getMessage());
    }

    /**
     * 处理请求体参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResult<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException error: {}", e.getMessage());

        String message = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return BaseResult.error(400, "参数校验失败：" + message, "Invalid params: " + message);
    }

    /**
     * 处理参数缺失的异常
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResult<Object> handleServletRequestBindingException(ServletRequestBindingException e) {
        log.error("handleServletRequestBindingException error: {}", e.getMessage());
        return BaseResult.error(400, "参数校验失败：" + e.getMessage(), "Invalid params: " + e.getMessage());
    }

    /**
     * 处理业务异常，报 400
     */
    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResult<Object> handleBaseException(BaseException e) {
        log.error("handleBaseException error: {}", e.getMessage());
        return BaseResult.error(e.getCode(), e.getFinalMessage().getLeft(), e.getFinalMessage().getRight());
    }

    /**
     * 访问不存在资源，报 404
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public BaseResult<Object> handle404(NoHandlerFoundException e) {
        log.error("handle404 error: {}", e.getMessage());
        return BaseResult.error(404, "接口不存在", "API not found");
    }

    /**
     * 授权不通过，报 401
     */
    @ExceptionHandler(ApiAuthorizeException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public BaseResult<Object> handleApiAuthorizeException(ApiAuthorizeException e) {
        log.error("handleApiAuthorizeException error: {}", e.getMessage());
        return BaseResult.error(e.getCode(), e.getFinalMessage().getLeft(), e.getFinalMessage().getRight());
    }
}
