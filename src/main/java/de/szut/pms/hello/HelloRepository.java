package de.szut.pms.hello;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HelloRepository extends JpaRepository<HelloEntity, Long> {

    List<HelloEntity> findAllByMessage(String message);
}
