package org.example.DTO;

public class SpamCheckResult {
    private boolean isSpam;
    private double score;
    private double requiredScore;
    private String report;

    public SpamCheckResult(boolean isSpam, double score, double requiredScore, String report) {
        this.isSpam = isSpam;
        this.score = score;
        this.requiredScore = requiredScore;
        this.report = report;
    }

    public boolean isSpam() {
        return isSpam;
    }

    public double getScore() {
        return score;
    }

    public double getRequiredScore() {
        return requiredScore;
    }

    public String getReport() {
        return report;
    }
}
