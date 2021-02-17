package de.viadee.mateocamundabridge.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcel_Flasskamp
 */
class JobStatusTest {

    @Test
    void testToString() {
        JobStatus queued = JobStatus.QUEUED;
        JobStatus running = JobStatus.RUNNING;
        JobStatus finished = JobStatus.FINISHED;

        assertEquals("In Warteschlange", queued.toString());
        assertEquals("Wird ausgeführt", running.toString());
        assertEquals("Beendet", finished.toString());
    }

    @Test
    void toStringEnglish() {
        JobStatus queued = JobStatus.QUEUED;
        JobStatus running = JobStatus.RUNNING;
        JobStatus finished = JobStatus.FINISHED;

        assertEquals("Queued", queued.toStringEnglish());
        assertEquals("Running", running.toStringEnglish());
        assertEquals("Finished", finished.toStringEnglish());
    }

    @Test
    void getByNameOrEnglishName() {
        assertEquals(JobStatus.QUEUED, JobStatus.getByNameOrEnglishName("In Warteschlange"));
        assertEquals(JobStatus.QUEUED, JobStatus.getByNameOrEnglishName("Queued"));

        assertEquals(JobStatus.RUNNING, JobStatus.getByNameOrEnglishName("Wird ausgeführt"));
        assertEquals(JobStatus.RUNNING, JobStatus.getByNameOrEnglishName("Running"));

        assertEquals(JobStatus.FINISHED, JobStatus.getByNameOrEnglishName("Beendet"));
        assertEquals(JobStatus.FINISHED, JobStatus.getByNameOrEnglishName("Finished"));
    }

    @Test
    void valueOf() {
        assertEquals(JobStatus.QUEUED, JobStatus.valueOf("QUEUED"));
        assertEquals(JobStatus.RUNNING, JobStatus.valueOf("RUNNING"));
        assertEquals(JobStatus.FINISHED, JobStatus.valueOf("FINISHED"));
    }
}