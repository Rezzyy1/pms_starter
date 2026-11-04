package de.szut.pms.hello;

import org.springframework.stereotype.Component;

@Component
public class HelloMapper {

    public HelloDto toDto(HelloEntity entity) {
        return new HelloDto(entity.getId(), entity.getMessage());
    }

    public HelloEntity toEntity(CreateHelloDto dto) {
        return new HelloEntity(dto.message());
    }
}
