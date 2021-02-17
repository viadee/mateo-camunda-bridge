package de.viadee.mateocamundabridge.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Marcel_Flasskamp
 */
public enum JobStatus {
    QUEUED, RUNNING, FINISHED, FAILED;

    @Override
    public String toString() {
        return switch (this) {
            case QUEUED -> "In Warteschlange";
            case RUNNING -> "Wird ausgeführt";
            case FINISHED -> "Beendet";
            case FAILED -> "Fehler";
        };
    }

    public String toStringEnglish() {
        return switch (this) {
            case QUEUED -> "Queued";
            case RUNNING -> "Running";
            case FINISHED -> "Finished";
            case FAILED -> "Failed";
        };
    }

    public static JobStatus getByNameOrEnglishName(String nameOrEnglishName) {
        Optional<JobStatus> resultLevel = Arrays.stream(JobStatus.values())
                .filter(rl -> rl.toString().equals(nameOrEnglishName) || rl.toStringEnglish().equals(nameOrEnglishName))
                .findFirst();
        if (resultLevel.isPresent()) {
            return resultLevel.get();
        }
        throw new IllegalArgumentException(
                String.format("No JobStatus for entry Name or English Name '%s' found!", nameOrEnglishName));
    }
}
