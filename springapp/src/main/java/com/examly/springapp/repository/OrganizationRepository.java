package com.examly.springapp.repository;

import com.examly.springapp.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    List<Organization> findAllBySlug(String slug);

    default Optional<Organization> findBySlug(String slug) {
        List<Organization> list = findAllBySlug(slug);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    boolean existsBySlug(String slug);
}
