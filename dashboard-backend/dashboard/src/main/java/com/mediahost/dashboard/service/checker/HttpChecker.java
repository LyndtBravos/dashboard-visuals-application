package com.mediahost.dashboard.service.checker;

public interface HttpChecker {
    CheckResult check(String url, int timeoutSeconds, boolean followRedirects);
}