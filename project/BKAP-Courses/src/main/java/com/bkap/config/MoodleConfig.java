package com.bkap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Class này map trực tiếp với các property có prefix "moodle" trong
 * application.properties: moodle.base-url=... moodle.token=...
 * moodle.ws-format=json
 *
 * @ConfigurationProperties giúp Spring tự inject giá trị vào các field tương
 *                          ứng khi app khởi động.
 */
@Component
@ConfigurationProperties(prefix = "moodle")
public class MoodleConfig {

	/** URL gốc của Moodle Web Service REST endpoint */
	private String baseUrl;

	/** Token xác thực do admin BUNI cấp — dùng để gọi Moodle API */
	private String token;

	/** Format response: luôn là "json" */
	private String wsFormat;

	// --- Getters & Setters (Spring cần setter để inject) ---

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getWsFormat() {
		return wsFormat;
	}

	public void setWsFormat(String wsFormat) {
		this.wsFormat = wsFormat;
	}
}