package com.pettrip.place.repository;

import com.pettrip.place.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, String> {}
