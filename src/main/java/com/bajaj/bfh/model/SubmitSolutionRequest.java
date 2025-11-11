package com.bajaj.bfh.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmitSolutionRequest {
	@JsonProperty("finalQuery")
	private String finalQuery;

	public SubmitSolutionRequest() {}

	public SubmitSolutionRequest(String finalQuery) {
		this.finalQuery = finalQuery;
	}

	public String getFinalQuery() {
		return finalQuery;
	}

	public void setFinalQuery(String finalQuery) {
		this.finalQuery = finalQuery;
	}
}


