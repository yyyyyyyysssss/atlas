package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.CertificateClientGeneratedRequest;
import com.atlas.auth.domain.dto.CertificateServerGeneratedRequest;
import com.atlas.auth.enums.CertificateIdentityType;
import com.atlas.auth.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/13 13:11
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
