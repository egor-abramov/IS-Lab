package core.handler.mapper;

import core.dto.ErrorDto;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

final class ErrorResponses {
    private ErrorResponses() {
    }

    static Response of(Response.StatusType status, List<String> errors) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8"))
                .entity(new ErrorDto(errors))
                .build();
    }

    static Response of(Response.StatusType status, String error) {
        return of(status, List.of(error));
    }
}
