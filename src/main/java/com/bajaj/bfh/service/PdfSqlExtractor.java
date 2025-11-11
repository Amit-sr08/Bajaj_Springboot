package com.bajaj.bfh.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfSqlExtractor {
	private static final Logger log = LoggerFactory.getLogger(PdfSqlExtractor.class);

	// Try to capture a final SQL that starts with SELECT or WITH and ends with a semicolon or end-of-text
	private static final Pattern SQL_BLOCK_PATTERN = Pattern.compile(
			"(?is)(?:^|\\s)(WITH\\s+.+?|SELECT\\s+.+?)(?:;|$)"
	);

	public String extractSql(String absolutePdfPath) {
		try {
			File file = new File(absolutePdfPath);
			if (!file.exists()) {
				log.error("PDF not found at path: {}", absolutePdfPath);
				return null;
			}
			try (PDDocument doc = PDDocument.load(file)) {
				PDFTextStripper stripper = new PDFTextStripper();
				String text = stripper.getText(doc);
				if (text == null || text.isBlank()) {
					log.error("Extracted empty text from PDF: {}", absolutePdfPath);
					return null;
				}
				String normalized = normalizeWhitespace(text);
				String sql = findFinalSql(normalized);
				if (sql == null || sql.isBlank()) {
					log.warn("Could not detect a SQL statement in PDF. Returning all extracted text as fallback.");
					return normalized.trim();
				}
				return sql.trim();
			}
		} catch (Exception e) {
			log.error("Failed to extract SQL from PDF '{}': {}", absolutePdfPath, e.getMessage());
			return null;
		}
	}

	private String normalizeWhitespace(String s) {
		return s.replace('\u00A0', ' ') // non-breaking spaces
				.replaceAll("[\\t\\r]+", " ")
				.replaceAll(" +", " ")
				.trim();
	}

	private String findFinalSql(String text) {
		Matcher matcher = SQL_BLOCK_PATTERN.matcher(text);
		String lastMatch = null;
		while (matcher.find()) {
			lastMatch = matcher.group(1);
		}
		if (lastMatch != null) {
			// Ensure it starts with WITH or SELECT
			String lead = lastMatch.trim().toUpperCase(Locale.ROOT);
			if (lead.startsWith("WITH") || lead.startsWith("SELECT")) {
				return lastMatch.trim().replaceAll("\\s+", " ");
			}
		}
		return null;
	}
}


