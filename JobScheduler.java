import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

class Job implements Comparable<Job> {
    String jobName;
    int executionTimeInSeconds;
    List<String> requiredResources;
    List<String> dependencies;
    int importance;
    AtomicBoolean isRunning = new AtomicBoolean(false);

    public Job(String jobName, int executionTimeInSeconds, List<String> requiredResources, List<String> dependencies, int importance) {
        this.jobName = jobName;
        this.executionTimeInSeconds = executionTimeInSeconds;
        this.requiredResources = requiredResources;
        this.dependencies = dependencies;
        this.importance = importance;
    }

    @Override
    public int compareTo(Job other) {
        return Integer.compare(other.importance, this.importance);
    }
}

public class JobScheduler {
    private static final Set<String> availableResources = new HashSet<>();
    private static final Map<String, Job> jobMap = new HashMap<>();
    private static final PriorityBlockingQueue<Job> jobQueue = new PriorityBlockingQueue<>();
    private static final Map<String, Boolean> completedJobs = new ConcurrentHashMap<>();

    public static void job_scheduler(List<Job> jobs) {
        for (Job job : jobs) {
            jobMap.put(job.jobName, job);
            if (job.dependencies.isEmpty()) {
                jobQueue.add(job);
            }
        }

        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        while (!jobQueue.isEmpty() || jobMap.size() != completedJobs.size()) {
            try {
                Job job = jobQueue.take();
                if (canExecute(job)) {
                    executeJob(job, startTime, executor);
                } else {
                    jobQueue.add(job);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static boolean canExecute(Job job) {
        for (String dependency : job.dependencies) {
            if (!completedJobs.getOrDefault(dependency, false)) {
                return false;
            }
        }
        for (String resource : job.requiredResources) {
            if (!availableResources.add(resource)) {
                return false;
            }
        }
        return true;
    }

    private static void executeJob(Job job, long startTime, ExecutorService executor) {
        executor.execute(() -> {
            job.isRunning.set(true);
            long jobStartTime = System.currentTimeMillis();
            System.out.println(job.jobName + " started at " + (jobStartTime - startTime) / 1000 + " seconds, using resources " + job.requiredResources);
            try {
                Thread.sleep(job.executionTimeInSeconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long jobEndTime = System.currentTimeMillis();
            System.out.println(job.jobName + " ended at " + (jobEndTime - startTime) / 1000 + " seconds, using resources " + job.requiredResources);
            for (String resource : job.requiredResources) {
                availableResources.remove(resource);
            }
            completedJobs.put(job.jobName, true);
            job.isRunning.set(false);
            for (Job dependentJob : jobMap.values()) {
                if (dependentJob.dependencies.contains(job.jobName) && !dependentJob.isRunning.get()) {
                    jobQueue.add(dependentJob);
                }
            }
        });
    }

    public static void main(String[] args) {
        List<Job> jobs = Arrays.asList(
                new Job("Job1", 6, Arrays.asList("CPU"), Collections.emptyList(), 3),
                new Job("Job2", 4, Arrays.asList("GPU"), Collections.emptyList(), 2),
                new Job("Job3", 8, Arrays.asList("CPU", "GPU"), Collections.emptyList(), 4),
                new Job("Job4", 3, Arrays.asList("CPU"), Arrays.asList("Job1"), 1),
                new Job("Job5", 5, Arrays.asList("GPU"), Arrays.asList("Job2"), 3),
                new Job("Job6", 7, Arrays.asList("CPU", "GPU"), Arrays.asList("Job4"), 2),
                new Job("Job7", 2, Arrays.asList("CPU"), Collections.emptyList(), 5),
                new Job("Job8", 4, Arrays.asList("GPU"), Collections.emptyList(), 3),
                new Job("Job9", 6, Arrays.asList("CPU", "GPU"), Arrays.asList("Job7", "Job8"), 2),
                new Job("Job10", 3, Arrays.asList("CPU"), Arrays.asList("Job1", "Job7"), 1),
                new Job("Job11", 5, Arrays.asList("GPU"), Arrays.asList("Job2", "Job8"), 3),
                new Job("Job12", 4, Arrays.asList("CPU"), Arrays.asList("Job10"), 2),
                new Job("Job13", 6, Arrays.asList("GPU"), Arrays.asList("Job5"), 4),
                new Job("Job14", 3, Arrays.asList("CPU", "GPU"), Arrays.asList("Job12", "Job13"), 1),
                new Job("Job15", 7, Arrays.asList("CPU"), Arrays.asList("Job3", "Job6"), 3),
                new Job("Job16", 5, Arrays.asList("GPU"), Arrays.asList("Job3", "Job9"), 2),
                new Job("Job17", 4, Arrays.asList("CPU", "GPU"), Arrays.asList("Job11", "Job14"), 4),
                new Job("Job18", 3, Arrays.asList("CPU"), Arrays.asList("Job10", "Job12"), 1),
                new Job("Job19", 6, Arrays.asList("GPU"), Arrays.asList("Job13", "Job16"), 3),
                new Job("Job20", 2, Arrays.asList("CPU", "GPU"), Arrays.asList("Job17", "Job18"), 2)
        );

        job_scheduler(jobs);
    }
}
