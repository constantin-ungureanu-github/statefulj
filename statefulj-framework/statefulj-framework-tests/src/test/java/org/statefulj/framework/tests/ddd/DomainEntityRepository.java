package org.statefulj.framework.tests.ddd;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainEntityRepository extends JpaRepository<DomainEntity, Long> {
}
