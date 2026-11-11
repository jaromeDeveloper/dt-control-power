package com.controltower.backend.records;

import org.springframework.stereotype.Service;
import com.controltower.backend.auth.AppUser;
import com.controltower.backend.processes.BusinessProcess;

@Service
public class BusinessRecordInitializationService {
	private final BusinessRecordRepository records;

	BusinessRecordInitializationService(BusinessRecordRepository records) { this.records = records; }

	public BusinessRecord initialize(BusinessProcess process, AppUser actor) {
		String reference = "DEMO-" + process.getId().toString().substring(0, 8).toUpperCase();
		return records.save(new BusinessRecord(process, reference, "PENDIENTE", actor));
	}
}
