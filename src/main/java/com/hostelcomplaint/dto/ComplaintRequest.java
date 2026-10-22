
package com.hostelcomplaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ComplaintRequest {

    @NotBlank(message = "Complaint title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Pattern(
        regexp = "Electrical & Appliances|Plumbing|Cleaning & Housekeeping|Furniture|Internet / Wi-Fi|Room / Door / Window|Other",
        message = "Invalid complaint category"
    )
    private String category;

    @NotBlank(message = "Severity is required")
    @Pattern(
        regexp = "(?i)LOW|MEDIUM|HIGH|CRITICAL",
        message = "Severity must be LOW, MEDIUM, HIGH or CRITICAL"
    )
    private String severity;

    public ComplaintRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
