package com.careeradvisor.backend.service;

import com.careeradvisor.backend.exception.FileValidationException;
import com.careeradvisor.backend.exception.ResumeParsingException;
import com.careeradvisor.backend.exception.UnsupportedFileTypeException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;

@Service
public class ResumeParserService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeParserService.class);

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    public String extractText(MultipartFile file) {
        validateFile(file);

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String lowerName = fileName != null ? fileName.toLowerCase() : "";

        try {
            if (lowerName.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(contentType)) {
                return extractPdfText(file);
            } else if (lowerName.endsWith(".docx") ||
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType)) {
                return extractDocxText(file);
            } else {
                throw new UnsupportedFileTypeException("Unsupported resume file format. Please upload a .pdf or .docx file.");
            }
        } catch (UnsupportedFileTypeException | FileValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse resume file: {}", fileName, e);
            throw new ResumeParsingException("Failed to extract text from resume: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Uploaded file is empty. Please select a valid resume file.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileValidationException("File size exceeds 5MB limit. Please upload a smaller resume.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new FileValidationException("Invalid file name.");
        }

        String lowerName = originalName.toLowerCase();
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx")) {
            throw new UnsupportedFileTypeException("Invalid file extension. Only .pdf and .docx files are accepted.");
        }
    }

    private String extractPdfText(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        if (bytes.length < 4) {
            throw new FileValidationException("Corrupted PDF file.");
        }

        // Validate PDF Magic Bytes: %PDF
        if (bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46) {
            throw new UnsupportedFileTypeException("File does not have a valid PDF header.");
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new FileValidationException("Encrypted/Password-protected PDFs are not supported.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String rawText = stripper.getText(document);
            return sanitizeText(rawText);
        }
    }

    private String extractDocxText(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        if (bytes.length < 2) {
            throw new FileValidationException("Corrupted DOCX file.");
        }

        // Validate ZIP/DOCX Magic Bytes: PK (0x50, 0x4B)
        if (bytes[0] != 0x50 || bytes[1] != 0x4B) {
            throw new UnsupportedFileTypeException("File does not have a valid DOCX container header.");
        }

        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String rawText = extractor.getText();
            return sanitizeText(rawText);
        }
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        // Replace non-printable control characters, normalize line breaks
        String sanitized = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (sanitized.isEmpty()) {
            throw new FileValidationException("Uploaded resume contains no readable text. Scanned images or empty documents are not supported.");
        }

        return sanitized;
    }
}
