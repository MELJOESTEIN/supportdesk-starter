package com.supportdesk.agent;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, String> {

	List<Agent> findByActifTrueOrderByNomCompletAsc();
}
