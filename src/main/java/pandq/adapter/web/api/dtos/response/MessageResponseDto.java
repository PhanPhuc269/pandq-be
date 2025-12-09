package pandq.adapter.web.api.dtos.response;

import pandq.application.codes.Code;

import java.time.LocalDateTime;

public record MessageResponseDto(
        Meta meta,
        String code,
        String message

) {
    record Meta (LocalDateTime timestamp) {}

    public static MessageResponseDto of(String message) {
        return new MessageResponseDto( new MessageResponseDto.Meta(LocalDateTime.now()), Code.OK.toString(), message);
    }
}


