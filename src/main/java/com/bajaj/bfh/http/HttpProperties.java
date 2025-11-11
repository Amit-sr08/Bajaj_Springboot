package com.bajaj.bfh.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "http")
public class HttpProperties {
	private String baseUrl;
	private String generateWebhookPath;
	private String fallbackSubmitPath;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getGenerateWebhookPath() {
		return generateWebhookPath;
	}

	public void setGenerateWebhookPath(String generateWebhookPath) {
		this.generateWebhookPath = generateWebhookPath;
	}

	public String getFallbackSubmitPath() {
		return fallbackSubmitPath;
	}

	public void setFallbackSubmitPath(String fallbackSubmitPath) {
		this.fallbackSubmitPath = fallbackSubmitPath;
	}
}


