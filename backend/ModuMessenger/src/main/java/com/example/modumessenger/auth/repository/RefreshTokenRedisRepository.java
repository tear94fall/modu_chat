package com.example.modumessenger.auth.repository;

import com.example.modumessenger.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRedisRepository extends CrudRepository<RefreshToken, String> {
}

