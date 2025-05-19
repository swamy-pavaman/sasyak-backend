package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.repository.TaskAdviceRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdviceService {

    private final TaskAdviceRepo taskAdviceRepository;

    @Autowired
    public AdviceService(TaskAdviceRepo taskAdviceRepository) {
        this.taskAdviceRepository = taskAdviceRepository;
    }

    public int countAdviceByTenant(UUID tenantId) {
        return taskAdviceRepository.countByTenantId(tenantId);
    }
}
