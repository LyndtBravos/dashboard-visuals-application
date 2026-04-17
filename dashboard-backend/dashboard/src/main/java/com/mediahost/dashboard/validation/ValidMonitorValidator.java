package com.mediahost.dashboard.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediahost.dashboard.model.entity.ApiMonitor;
import com.mediahost.dashboard.model.entity.SiteMonitor;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidMonitorValidator.class)
@interface ValidMonitor {
    String message() default "Invalid monitor configuration";
}

public class ValidMonitorValidator implements ConstraintValidator<ValidMonitor, Object> {

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj instanceof SiteMonitor site)
            return validateSiteMonitor(site, context);
        else if (obj instanceof ApiMonitor api)
            return validateApiMonitor(api, context);

        return true;
    }

    private boolean validateSiteMonitor(SiteMonitor site, ConstraintValidatorContext context) {
        boolean valid = true;

        if (site.getUrl() != null && !site.getUrl().matches("^(http|https)://.*$")) {
            context.buildConstraintViolationWithTemplate("Invalid URL format")
                    .addPropertyNode("url").addConstraintViolation();
            valid = false;
        }

        if (site.getBusinessHoursOnly()) {
            if (site.getBusinessHoursStart() == null || site.getBusinessHoursEnd() == null) {
                context.buildConstraintViolationWithTemplate("Business hours start/end required")
                        .addPropertyNode("businessHoursStart").addConstraintViolation();
                valid = false;
            }
        }

        return valid;
    }

    private boolean validateApiMonitor(ApiMonitor api, ConstraintValidatorContext context) {
        boolean valid = true;

        if (api.getExpectedJsonPath() != null && !api.getExpectedJsonPath().isEmpty())
            try {
                com.jayway.jsonpath.JsonPath.compile(api.getExpectedJsonPath());
            } catch (Exception e) {
                context.buildConstraintViolationWithTemplate("Invalid JSONPath expression")
                        .addPropertyNode("expectedJsonPath").addConstraintViolation();
                valid = false;
            }

        if (api.getRequestHeadersJson() != null && !api.getRequestHeadersJson().isEmpty())
            try {
                new ObjectMapper().readTree(api.getRequestHeadersJson());
            } catch (Exception e) {
                context.buildConstraintViolationWithTemplate("Invalid headers JSON")
                        .addPropertyNode("requestHeadersJson").addConstraintViolation();
                valid = false;
            }

        return valid;
    }
}