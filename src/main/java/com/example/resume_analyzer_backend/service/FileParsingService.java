package com.example.resume_analyzer_backend.service;

import com.example.resume_analyzer_backend.exception.FileProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileParsingService {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("pdf", "doc", "docx", "txt");

    // ── Save uploaded file to disk ────────────────────────────────
    public String saveFile(MultipartFile file, Long userId) {
        validateFile(file);
        try {
            String ext      = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "." + ext;
            Path   userDir  = Paths.get(uploadDir, String.valueOf(userId));
            Files.createDirectories(userDir);

            Path destination = userDir.resolve(fileName);
            Files.copy(file.getInputStream(), destination,
                    StandardCopyOption.REPLACE_EXISTING);

            log.info("File saved: {}", destination);
            return destination.toString();

        } catch (IOException e) {
            throw new FileProcessingException(
                    "Failed to save file: " + e.getMessage(), e);
        }
    }

    // ── Extract plain text from file ──────────────────────────────
    public String extractText(String filePath, String fileType) {
        try {
            return switch (fileType.toLowerCase()) {
                case "pdf"  -> extractFromPdf(filePath);
                case "docx" -> extractFromDocx(filePath);
                case "doc"  -> extractFromDoc(filePath);
                case "txt"  -> extractFromTxt(filePath);
                default     -> throw new FileProcessingException(
                        "Unsupported file type: " + fileType);
            };
        } catch (IOException e) {
            throw new FileProcessingException(
                    "Failed to extract text: " + e.getMessage(), e);
        }
    }

    // ── PDF — PDFBox 3.x uses Loader.loadPDF() ───────────────────
    private String extractFromPdf(String filePath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    // ── DOCX ──────────────────────────────────────────────────────
    private String extractFromDocx(String filePath) throws IOException {
        try (InputStream in = Files.newInputStream(Paths.get(filePath));
             XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    // ── DOC (legacy) ──────────────────────────────────────────────
    private String extractFromDoc(String filePath) throws IOException {
        try (InputStream in = Files.newInputStream(Paths.get(filePath));
             HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    // ── TXT ───────────────────────────────────────────────────────
    private String extractFromTxt(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }

    // ── Validation ────────────────────────────────────────────────
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("File is empty or missing");
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new FileProcessingException(
                    "File type not allowed. Allowed: " + ALLOWED_EXTENSIONS);
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new FileProcessingException("File size exceeds 10 MB limit");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    public String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", filePath, e.getMessage());
        }
    }
}