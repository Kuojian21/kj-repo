package com.kj.repo.demo.quartz;

import java.util.Date;
import java.util.HashMap;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class TeQuartz {

    public static void main(String[] args) throws Exception {
        JobDetail jobDetail = JobBuilder.newJob(MyJob.class).withIdentity("name", "group").usingJobData("args", "kj")
                .build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger", "group").startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever()).build();

        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
        Thread.sleep(6000);

        scheduler.shutdown();

        HashMap<String, String> m = new HashMap<String, String>();
        m.get(null);
    }

    public static class MyJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            JobDetail detail = jobExecutionContext.getJobDetail();
            System.out.println("my job args is  " + detail.getJobDataMap().getString("args") + " at " + new Date());
        }
    }

}
