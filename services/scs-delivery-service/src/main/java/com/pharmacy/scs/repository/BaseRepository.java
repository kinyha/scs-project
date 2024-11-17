package com.pharmacy.scs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    Iterable<T> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
