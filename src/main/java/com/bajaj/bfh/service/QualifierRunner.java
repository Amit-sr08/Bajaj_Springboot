package com.bajaj.bfh.service;

import com.bajaj.bfh.config.AppProperties;
import com.bajaj.bfh.config.SqlProperties;
import com.bajaj.bfh.http.HttpProperties;
import com.bajaj.bfh.model.GenerateWebhookRequest;
import com.bajaj.bfh.model.GenerateWebhookResponse;
import com.bajaj.bfh.model.SubmitSolutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Component
public class QualifierRunner implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(QualifierRunner.class);

	private final WebClient webClient;
	private final AppProperties appProperties;
	private final HttpProperties httpProperties;
	private final SqlProperties sqlProperties;
	private final PdfSqlExtractor pdfSqlExtractor;

	public QualifierRunner(WebClient webClient,
	                       AppProperties appProperties,
	                       HttpProperties httpProperties,
	                       SqlProperties sqlProperties,
	                       PdfSqlExtractor pdfSqlExtractor) {
		this.webClient = webClient;
		this.appProperties = appProperties;
		this.httpProperties = httpProperties;
		this.sqlProperties = sqlProperties;
		this.pdfSqlExtractor = pdfSqlExtractor;
	}

	@Override
	public void run(ApplicationArguments args) {
		var candidate = appProperties.getCandidate();

		log.info("Starting Qualifier flow for candidate: name='{}', regNo='{}', email='{}'",
				candidate.getName(), candidate.getRegNo(), candidate.getEmail());

		var request = new GenerateWebhookRequest(candidate.getName(), candidate.getRegNo(), candidate.getEmail());

		GenerateWebhookResponse webhookResponse = webClient.post()
				.uri(httpProperties.getGenerateWebhookPath())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(GenerateWebhookResponse.class)
				.doOnError(error -> log.error("Failed to generate webhook: {}", error.getMessage()))
				.block();

		if (webhookResponse == null) {
			log.error("No response received from generateWebhook API. Exiting.");
			return;
		}

		String webhookUrl = webhookResponse.getWebhookUrl();
		String accessToken = webhookResponse.getAccessToken();

		if (accessToken == null || accessToken.isBlank()) {
			log.error("accessToken missing in response. Exiting.");
			return;
		}

		if (webhookUrl == null || webhookUrl.isBlank()) {
			webhookUrl = httpProperties.getFallbackSubmitPath();
			log.warn("webhook URL missing in response. Falling back to '{}'", webhookUrl);
		} else {
			log.info("Received webhook URL: {}", webhookUrl);
		}

		String sql = resolveSqlByRegNo(candidate.getRegNo());
		if (sql == null || sql.isBlank()) {
			log.error("SQL could not be resolved. Ensure PDFs or resources/sql/odd.sql and even.sql are populated.");
			return;
		}

		SubmitSolutionRequest submit = new SubmitSolutionRequest(sql);

		Mono<String> submitCall;
		if (webhookUrl.startsWith("http://") || webhookUrl.startsWith("https://")) {
			submitCall = WebClient.create()
					.post()
					.uri(webhookUrl)
					.header(HttpHeaders.AUTHORIZATION, accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(submit))
					.retrieve()
					.bodyToMono(String.class);
		} else {
			submitCall = webClient.post()
					.uri(webhookUrl)
					.header(HttpHeaders.AUTHORIZATION, accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(submit))
					.retrieve()
					.bodyToMono(String.class);
		}

		try {
			String responseBody = submitCall
					.doOnError(err -> log.error("Submission failed: {}", err.getMessage()))
					.block();
			log.info("Submission successful. Server response: {}", responseBody);
		} catch (Exception e) {
			log.error("Submission encountered an error: {}", e.getMessage());
		}
	}

	private String resolveSqlByRegNo(String regNo) {
		boolean preferPdf = sqlProperties.isPreferPdf();
		String digits = (regNo == null ? "" : regNo.replaceAll("\\D+", ""));
		int lastTwo = 0;
		boolean isOdd = false;
		if (!digits.isBlank()) {
			lastTwo = Integer.parseInt(digits.substring(Math.max(0, digits.length() - 2)));
			isOdd = (lastTwo % 2) != 0;
		}
		log.info("regNo lastTwo='{}' => {}", lastTwo, isOdd ? "ODD" : "EVEN");

		// Try PDF first if enabled
		if (preferPdf) {
			String pdfPath = isOdd ? sqlProperties.getOddPdfPath() : sqlProperties.getEvenPdfPath();
			if (pdfPath != null && !pdfPath.isBlank() && new File(pdfPath).exists()) {
				log.info("Attempting to extract SQL from PDF: {}", pdfPath);
				String fromPdf = pdfSqlExtractor.extractSql(pdfPath);
				if (fromPdf != null && !fromPdf.isBlank()) {
					log.info("SQL extracted from PDF successfully.");
					return fromPdf.trim();
				}
			} else {
				log.warn("PDF path not set or file missing: {}", pdfPath);
			}
		}

		// Fallback to classpath SQL files
		String path = isOdd ? "sql/odd.sql" : "sql/even.sql";
		log.info("Falling back to classpath SQL at {}", path);
		return readClasspathSql(path);
	}

	private String readClasspathSql(String path) {
		try {
			ClassPathResource resource = new ClassPathResource(path);
			if (!resource.exists()) {
				log.error("SQL resource not found at {}", path);
				return null;
			}
			try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
				return FileCopyUtils.copyToString(reader).trim();
			}
		} catch (Exception e) {
			log.error("Failed to read SQL from {}: {}", path, e.getMessage());
			return null;
		}
	}
}


