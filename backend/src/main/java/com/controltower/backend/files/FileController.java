package com.controltower.backend.files;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.controltower.backend.auth.AppUserPrincipal;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {
	private final FileIngestionService service;

	FileController(FileIngestionService service) { this.service = service; }

	@PostMapping(consumes = "multipart/form-data")
	@ResponseStatus(HttpStatus.CREATED)
	public FileResponse upload(@RequestParam("file") MultipartFile file,
			@RequestParam(required = false) String areaCode,
			@RequestParam(required = false) String description,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return service.upload(file, areaCode, description, principal);
	}

	@GetMapping("/{fileId}")
	public FileResponse findById(@PathVariable UUID fileId) { return service.findById(fileId); }
}
