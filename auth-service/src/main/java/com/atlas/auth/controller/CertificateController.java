package com.atlas.auth.controller;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.CertificateClientGeneratedRequest;
import com.atlas.auth.domain.dto.CertificateInspectResponse;
import com.atlas.auth.domain.dto.CertificateServerGeneratedRequest;
import com.atlas.auth.enums.CertificateIdentityType;
import com.atlas.auth.service.CertificateService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/13 13:11
 */
@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
public class CertificateController {


    private final CertificateService certificateService;

    // Atlas 生成密钥对并签发证书，直接返回 PKCS#12 文件。
    @PostMapping("/serverGenerated")
    public ResponseEntity<byte[]> serverGenerated(@RequestBody @Validated CertificateServerGeneratedRequest request) {

        byte[] bytes = certificateService.serverGenerated(request, CertificateIdentityType.USER);

        return fileResponse(bytes, "atlas-client.p12", MediaType.parseMediaType("application/x-pkcs12"));
    }

    @PostMapping("/clientGenerated")
    public ResponseEntity<byte[]> clientGenerated(@RequestBody @Validated CertificateClientGeneratedRequest request) {

        byte[] bytes = certificateService.clientGenerated(request, CertificateIdentityType.USER);

        return fileResponse(bytes, "atlas-client.crt", MediaType.parseMediaType("application/x-pem-file"));
    }

    @PostMapping(value = "/clientGenerated/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> clientGeneratedUpload(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AuthServiceException("CSR file is required");
        }
        String csr;
        try {
            csr = new String(file.getBytes(), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new AuthServiceException("Failed to read CSR file", e);
        }
        byte[] bytes = certificateService.clientGenerated(new CertificateClientGeneratedRequest(csr), CertificateIdentityType.USER);
        return fileResponse(bytes, "atlas-client.crt", MediaType.parseMediaType("application/x-pem-file"));
    }

    @PostMapping(value = "/inspect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CertificateInspectResponse> inspect(@RequestPart("file") MultipartFile file, @RequestParam(value = "password", required = false) String password) {
        CertificateInspectResponse response = certificateService.inspect(file, password);
        return ResultGenerator.ok(response);
    }

    private ResponseEntity<byte[]> fileResponse(byte[] bytes, String filename, MediaType mediaType) {

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .cacheControl(CacheControl.noStore())
                .body(bytes);
    }

}
