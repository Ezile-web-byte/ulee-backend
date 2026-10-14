package com.ulee.ulee_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns an over-the-limit file upload into a friendly, in-page message instead
 * of a raw Whitelabel error.
 *
 * MaxUploadSizeExceededException is thrown by Spring while parsing the multipart
 * request — before the target controller method ever runs — so it can't be
 * caught inside applyToProperty(). This advice catches it globally and bounces
 * the student back to the property they were applying to, with
 * ?applyError=file-too-large so property-detail.html can show the explanation.
 */
@ControllerAdvice
public class UploadExceptionHandler {

    private static final Pattern APPLY_PATH = Pattern.compile("/apply/(\\d+)");

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleFileTooLarge(HttpServletRequest request) {
        Matcher m = APPLY_PATH.matcher(request.getRequestURI());
        if (m.find()) {
            return "redirect:/property/" + m.group(1) + "?applyError=file-too-large";
        }
        // Fallback for any other upload form that hits the limit.
        return "redirect:/student-dashboard";
    }
}
