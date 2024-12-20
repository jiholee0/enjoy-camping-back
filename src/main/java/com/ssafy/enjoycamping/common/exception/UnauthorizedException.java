package com.ssafy.enjoycamping.common.exception;

import com.ssafy.enjoycamping.common.response.BaseResponseStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Setter
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(BaseResponseStatus status) {
        super(status);
    }
}
