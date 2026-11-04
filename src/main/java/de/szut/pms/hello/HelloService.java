package de.szut.pms.hello;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HelloService {

    private final HelloRepository repository;
    private final HelloMapper mapper;

    public HelloService(HelloRepository repository, HelloMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public HelloDto create(CreateHelloDto dto) {
        HelloEntity saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<HelloDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HelloDto> findAllByMessage(String message) {
        return repository.findAllByMessage(message).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteById(Long id) {
        HelloEntity entity = repository.findById(id)
                .orElseThrow(() -> new HelloNotFoundException(id));
        repository.delete(entity);
    }
}
