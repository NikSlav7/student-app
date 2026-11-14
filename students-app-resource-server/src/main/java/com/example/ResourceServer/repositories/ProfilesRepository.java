package com.example.ResourceServer.repositories;

import com.example.ResourceServer.domains.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfilesRepository extends JpaRepository<Profile, String> {

    Optional<Profile> findProfileByUsername(String username);
    Optional<Profile> findProfileByEmail(String email);
    List<Profile> findProfileByEmailOrUsername(String email, String username);
    Optional<Profile> findProfileByProfileId(String profileId);
}
