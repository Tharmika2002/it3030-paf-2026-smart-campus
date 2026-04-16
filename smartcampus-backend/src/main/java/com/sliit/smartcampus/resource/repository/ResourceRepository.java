package com.sliit.smartcampus.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sliit.smartcampus.resource.entity.Resource;
import com.sliit.smartcampus.resource.entity.ResourceStatus;
import com.sliit.smartcampus.resource.entity.ResourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    // 1. Check duplicate resource
    Optional<Resource> findByNameAndLocation(String name, String location);

    // 2. Get all non-archived resources
    List<Resource> findByArchivedFalse();

    // 3. Filter by type
    List<Resource> findByTypeAndArchivedFalse(ResourceType type);

    // 4. Filter by status
    List<Resource> findByStatusAndArchivedFalse(ResourceStatus status);

    // 5. Search by location (case insensitive)
    List<Resource> findByLocationContainingIgnoreCaseAndArchivedFalse(String location);

    // 6. Pagination support
    Page<Resource> findAllByArchivedFalse(Pageable pageable);

    // 7. Combined filtering
    Page<Resource> findByTypeAndStatusAndArchivedFalse(ResourceType type, ResourceStatus status, Pageable pageable);

}
