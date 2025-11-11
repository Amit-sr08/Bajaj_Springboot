package com.bajaj.bfh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sql")
public class SqlProperties {
	private boolean preferPdf = true;
	private String oddPdfPath;
	private String evenPdfPath;

	public boolean isPreferPdf() {
		return preferPdf;
	}

	public void setPreferPdf(boolean preferPdf) {
		this.preferPdf = preferPdf;
	}

	public String getOddPdfPath() {
		return oddPdfPath;
	}

	public void setOddPdfPath(String oddPdfPath) {
		this.oddPdfPath = oddPdfPath;
	}

	public String getEvenPdfPath() {
		return evenPdfPath;
	}

	public void setEvenPdfPath(String evenPdfPath) {
		this.evenPdfPath = evenPdfPath;
	}
}


