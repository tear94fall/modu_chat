package com.example.modumessenger.picture.repository;

import com.example.modumessenger.picture.entity.Picture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PictureRepository extends JpaRepository<Picture, Long>, PictureCustomRepository {
}
