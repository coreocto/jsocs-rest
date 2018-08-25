package org.coreocto.dev.jsocs.rest.repo;

import org.coreocto.dev.jsocs.rest.pojo.RequestEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepo extends JpaRepository<RequestEntry,Integer> {
}
